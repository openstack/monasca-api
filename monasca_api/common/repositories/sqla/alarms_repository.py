# -*- coding: utf-8 -*-
# Copyright 2014 Hewlett-Packard
# Copyright 2016 FUJITSU LIMITED
# (C) Copyright 2016 Hewlett Packard Enterprise Development Company LP
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

from datetime import datetime
from time import time

from monasca_api.common.repositories import alarms_repository
from monasca_api.common.repositories import exceptions
from monasca_api.common.repositories.sqla import models
from monasca_api.common.repositories.sqla import sql_repository
from sqlalchemy import MetaData, update, delete, select, text, bindparam, func, literal_column, asc, desc
from sqlalchemy import or_


class AlarmsRepository(sql_repository.SQLRepository,
                       alarms_repository.AlarmsRepository):

    def __init__(self):
        super(AlarmsRepository, self).__init__()

        metadata = MetaData()
        self.a_du = models.create_a_model(metadata)
        self.aa = models.create_aa_model(metadata).alias('aa')
        self.sa = models.create_sa_model(metadata).alias('sa')
        self.ad = models.create_ad_model(metadata).alias('ad')
        self.am = models.create_am_model(metadata).alias('am')
        self.md = models.create_md_model(metadata).alias('md')
        self.mdd = models.create_mdd_model(metadata).alias('mdd')
        self.mde = models.create_mde_model(metadata).alias('mde')
        self.sad = models.create_sad_model(metadata).alias('sad')
        self.sadd = models.create_sadd_model(metadata).alias('sadd')
        a = self.a_du
        self.a = a.alias('a')
        a_s = self.a
        sa = self.sa
        ad = self.ad
        am = self.am
        md = self.md
        mdd = self.mdd
        mde = self.mde

        gc_columns = [md.c.name + text("'='") + md.c.value]

        mdg = (select([md.c.dimension_set_id,
                      models.group_concat(gc_columns).label('dimensions')])
               .select_from(md)
               .group_by(md.c.dimension_set_id).alias('mdg'))

        self.base_query_from = (a_s.join(ad, ad.c.id == a_s.c.alarm_definition_id)
                                .join(am, am.c.alarm_id == a_s.c.id)
                                .join(mdd, mdd.c.id == am.c.metric_definition_dimensions_id)
                                .join(mde, mde.c.id == mdd.c.metric_definition_id)
                                .outerjoin(mdg, mdg.c.dimension_set_id == mdd.c.metric_dimension_set_id))

        self.base_query = select([a_s.c.id.label('alarm_id'),
                                  a_s.c.state,
                                  a_s.c.state_updated_at.
                                  label('state_updated_timestamp'),
                                  a_s.c.updated_at.label('updated_timestamp'),
                                  a_s.c.created_at.label('created_timestamp'),
                                  a_s.c.lifecycle_state,
                                  a_s.c.link,
                                  ad.c.id.label('alarm_definition_id'),
                                  ad.c.name.label('alarm_definition_name'),
                                  ad.c.severity,
                                  mde.c.name.label('metric_name'),
                                  mdg.c.dimensions.label('metric_dimensions')])

        self.base_subquery_list = (select([a_s.c.id])
                                   .select_from(a_s.join(ad, a_s.c.alarm_definition_id == ad.c.id)))

        self.get_ad_query = (select([ad])
                             .select_from(ad.join(a, ad.c.id == a.c.alarm_definition_id))
                             .where(ad.c.tenant_id == bindparam('b_tenant_id'))
                             .where(a.c.id == bindparam('b_id')))

        self.get_am_query = (select([a_s.c.id.label('alarm_id'),
                                     mde.c.name,
                                     mdg.c.dimensions])
                             .select_from(a_s.join(am, am.c.alarm_id == a_s.c.id)
                                          .join(mdd,
                                                mdd.c.id ==
                                                am.c.metric_definition_dimensions_id)
                                          .join(mde, mde.c.id == mdd.c.metric_definition_id)
                                          .outerjoin(mdg,
                                                     mdg.c.dimension_set_id ==
                                                     mdd.c.metric_dimension_set_id))
                             .where(a_s.c.id == bindparam('b_id'))
                             .order_by(a_s.c.id)
                             .distinct())

        self.get_sa_query = (select([sa.c.id.label('sub_alarm_id'),
                                     sa.c.alarm_id,
                                     sa.c.expression,
                                     ad.c.id.label('alarm_definition_id')])
                             .select_from(sa.join(a_s,
                                                  a_s.c.id == sa.c.alarm_id)
                                          .join(ad,
                                                ad.c.id == a_s.c.alarm_definition_id))
                             .where(ad.c.tenant_id == bindparam('b_tenant_id'))
                             .where(a_s.c.id == bindparam('b_id'))
                             .distinct())

        self.get_a_query = (select([a_s.c.state, a_s.c.link, a_s.c.lifecycle_state])
                            .select_from(a_s.join(ad,
                                                  ad.c.id == a_s.c.alarm_definition_id))
                            .where(ad.c.tenant_id == bindparam('b_tenant_id'))
                            .where(a_s.c.id == bindparam('b_id')))

        self.get_a_ad_query = (select([a_s.c.id])
                               .select_from(a_s.join(ad,
                                                     ad.c.id ==
                                                     a_s.c.alarm_definition_id))
                               .where(ad.c.tenant_id == bindparam('b_tenant_id'))
                               .where(a_s.c.id == bindparam('b_id'))
                               .alias('a_ad'))

        select_tmp = (select([literal_column('id')])
                      .select_from(self.get_a_ad_query)
                      .distinct()
                      .alias('temporarytable'))

        self.delete_alarm_query = (delete(a)
                                   .where(a.c.id.in_(select_tmp)))

        md_ = (select([mde.c.id])
               .where(mde.c.name == bindparam('b_md_name')).alias('md_'))

        self.get_a_am_query = (select([a_s.c.id])
                               .select_from(a_s.join(am,
                                                     am.c.alarm_id ==
                                                     a_s.c.id)
                                            .join(mdd,
                                                  mdd.c.id ==
                                                  am.c.metric_definition_dimensions_id)
                                            .join(md_,
                                                  md_.c.id ==
                                                  mdd.c.metric_definition_id)))

    @sql_repository.sql_try_catch_block
    def get_alarm_definition(self, tenant_id, alarm_id):
        with self._db_engine.connect() as conn:
            row = conn.execute(self.get_ad_query,
                               b_tenant_id=tenant_id,
                               b_id=alarm_id).fetchone()

            if row is not None:
                return dict(row)
            else:
                raise exceptions.DoesNotExistException

    @sql_repository.sql_try_catch_block
    def get_alarm_metrics(self, alarm_id):

        with self._db_engine.connect() as conn:
            rows = conn.execute(self.get_am_query, b_id=alarm_id).fetchall()
            return [dict(row) for row in rows]

    @sql_repository.sql_try_catch_block
    def get_sub_alarms(self, tenant_id, alarm_id):

        with self._db_engine.connect() as conn:
            rows = conn.execute(self.get_sa_query,
                                b_tenant_id=tenant_id,
                                b_id=alarm_id).fetchall()
            return [dict(row) for row in rows]

    @sql_repository.sql_try_catch_block
    def update_alarm(self, tenant_id, _id, state, lifecycle_state, link):

        time_ms = int(round(time() * 1000.0))
        with self._db_engine.connect() as conn:
            self.get_a_query.bind = self._db_engine
            prev_alarm = conn.execute(self.get_a_query,
                                      b_tenant_id=tenant_id,
                                      b_id=_id).fetchone()

            if prev_alarm is None:
                raise exceptions.DoesNotExistException

            parms = {'b_lifecycle_state': lifecycle_state,
                     'b_link': link}
            set_values = {'lifecycle_state':
                          bindparam('b_lifecycle_state'),
                          'link': bindparam('b_link'),
                          'updated_at': func.now()}

            if state != prev_alarm['state']:
                parms['b_state'] = state
                set_values['state'] = bindparam('b_state')
                set_values['state_updated_at'] = func.now()

            parms['b_tenant_id'] = tenant_id
            parms['b_id'] = _id

            select_tmp = (select([literal_column('id')])
                          .select_from(self.get_a_ad_query)
                          .distinct()
                          .alias('temporarytable'))

            a = self.a_du
            update_query = (update(a)
                            .values(set_values)
                            .where(a.c.id.in_(select_tmp)))

            conn.execute(update_query, parms)

            return prev_alarm, time_ms

    @sql_repository.sql_try_catch_block
    def delete_alarm(self, tenant_id, _id):

        with self._db_engine.connect() as conn:
            cursor = conn.execute(self.delete_alarm_query,
                                  b_tenant_id=tenant_id,
                                  b_id=_id)

            if cursor.rowcount < 1:
                raise exceptions.DoesNotExistException

    @sql_repository.sql_try_catch_block
    def get_alarm(self, tenant_id, _id):

        with self._db_engine.connect() as conn:
            ad = self.ad
            a = self.a
            query = (self.base_query
                     .select_from(self.base_query_from)
                     .where(ad.c.tenant_id == bindparam('b_tenant_id'))
                     .where(a.c.id == bindparam('b_id'))
                     .distinct())

            rows = conn.execute(query,
                                b_tenant_id=tenant_id,
                                b_id=_id).fetchall()

            if rows is None or len(rows) == 0:
                raise exceptions.DoesNotExistException

            return [dict(row) for row in rows]

    @sql_repository.sql_try_catch_block
    def get_alarms(self, tenant_id, query_parms=None, offset=None, limit=None):
        if not query_parms:
            query_parms = {}

        with self._db_engine.connect() as conn:
            parms = {}
            ad = self.ad
            am = self.am
            mdd = self.mdd
            md = self.md
            a = self.a

            query = (self.base_subquery_list
                     .where(ad.c.tenant_id == bindparam('b_tenant_id')))

            parms['b_tenant_id'] = tenant_id

            if 'alarm_definition_id' in query_parms:
                query = query.where(ad.c.id ==
                                    bindparam('b_alarm_definition_id'))
                parms['b_alarm_definition_id'] = query_parms['alarm_definition_id']

            if 'metric_name' in query_parms:
                query = query.where(a.c.id.in_(self.get_a_am_query))
                parms['b_md_name'] = query_parms['metric_name'].encode('utf8')

            if 'severity' in query_parms:
                severities = query_parms['severity'].split('|')
                query = query.where(
                    or_(ad.c.severity == bindparam('b_severity' + str(i)) for i in xrange(len(severities))))
                for i, s in enumerate(severities):
                    parms['b_severity' + str(i)] = s.encode('utf8')

            if 'state' in query_parms:
                query = query.where(a.c.state == bindparam('b_state'))
                parms['b_state'] = query_parms['state'].encode('utf8')

            if 'lifecycle_state' in query_parms:
                query = (query
                         .where(a.c.lifecycle_state ==
                                bindparam('b_lifecycle_state')))
                parms['b_lifecycle_state'] = query_parms['lifecycle_state'].encode('utf8')

            if 'link' in query_parms:
                query = query.where(a.c.link == bindparam('b_link'))
                parms['b_link'] = query_parms['link'].encode('utf8')

            if 'state_updated_start_time' in query_parms:
                query = (query
                         .where(a.c.state_updated_at >=
                                bindparam('b_state_updated_at')))
                date_str = query_parms['state_updated_start_time'].encode('utf8')
                date_param = datetime.strptime(date_str,
                                               '%Y-%m-%dT%H:%M:%S.%fZ')
                parms['b_state_updated_at'] = date_param

            if 'metric_dimensions' in query_parms:
                sub_query = select([a.c.id])
                sub_query_from = (a.join(am, am.c.alarm_id == a.c.id)
                                  .join(mdd,
                                        mdd.c.id ==
                                        am.c.metric_definition_dimensions_id))

                sub_query_md_base = select([md.c.dimension_set_id]).select_from(md)

                for i, metric_dimension in enumerate(query_parms['metric_dimensions']):
                    md_name = "b_md_name_{}".format(i)

                    values_cond = None
                    values_cond_flag = False

                    parsed_dimension = metric_dimension.split(':')
                    if parsed_dimension and len(parsed_dimension) > 1:
                        if '|' in parsed_dimension[1]:
                            values = parsed_dimension[1].encode('utf8').split('|')
                            sub_values_cond = []
                            for j, value in enumerate(values):
                                sub_md_value = "b_md_value_{}_{}".format(i, j)
                                sub_values_cond.append(md.c.value == bindparam(sub_md_value))
                                parms[sub_md_value] = value
                            values_cond = or_(*sub_values_cond)
                            values_cond_flag = True
                        else:
                            md_value = "b_md_value_{}".format(i)
                            values_cond = (md.c.value == bindparam(md_value))
                            values_cond_flag = True
                            parms[md_value] = parsed_dimension[1]

                    sub_query_md = (sub_query_md_base
                                    .where(md.c.name == bindparam(md_name)))
                    if values_cond_flag:
                        sub_query_md = (sub_query_md
                                        .where(values_cond))

                    sub_query_md = (sub_query_md
                                    .distinct()
                                    .alias('md_{}'.format(i)))

                    sub_query_from = (sub_query_from
                                      .join(sub_query_md,
                                            sub_query_md.c.dimension_set_id ==
                                            mdd.c.metric_dimension_set_id))

                    parms[md_name] = parsed_dimension[0].encode('utf8')

                    sub_query = (sub_query
                                 .select_from(sub_query_from)
                                 .distinct())
                    query = query.where(a.c.id.in_(sub_query))

            order_columns = []
            if 'sort_by' in query_parms:
                columns_mapper = {'alarm_id': a.c.id,
                                  'alarm_definition_id': ad.c.id,
                                  'alarm_definition_name': ad.c.name,
                                  'state_updated_timestamp': a.c.state_updated_at,
                                  'updated_timestamp': a.c.updated_at,
                                  'created_timestamp': a.c.created_at,
                                  'severity': models.field_sort(ad.c.severity, map(text, ["'LOW'",
                                                                                          "'MEDIUM'",
                                                                                          "'HIGH'",
                                                                                          "'CRITICAL'"])),
                                  'state': models.field_sort(a.c.state, map(text, ["'OK'",
                                                                                   "'UNDETERMINED'",
                                                                                   "'ALARM'"]))}

                order_columns, received_cols = self._remap_columns(query_parms['sort_by'], columns_mapper)

                if not received_cols.get('alarm_id', False):
                    order_columns.append(a.c.id)
            else:
                order_columns = [a.c.id]

            if limit:
                query = query.limit(bindparam('b_limit'))
                parms['b_limit'] = limit + 1

            if offset:
                query = query.offset(bindparam('b_offset'))
                parms['b_offset'] = offset

            query = (query
                     .order_by(*order_columns)
                     .alias('alarm_id_list'))

            main_query = (self.base_query
                          .select_from(self.base_query_from
                                       .join(query, query.c.id == a.c.id))
                          .distinct())

            main_query = main_query.order_by(*order_columns)

            return [dict(row) for row in conn.execute(main_query, parms).fetchall()]

    def _remap_columns(self, columns, columns_mapper):
        received_cols = {}
        order_columns = []
        for col in columns:
            col_values = col.split()
            col_name = col_values[0]
            order_column = columns_mapper.get(col_name, literal_column(col_name))
            if len(col_values) > 1:
                mode = col_values[1]
                if mode:
                    if mode == 'asc':
                        order_column = asc(order_column)
                    elif mode == 'desc':
                        order_column = desc(order_column)
            order_columns.append(order_column)
            received_cols[col_name] = True
        return order_columns, received_cols

    @sql_repository.sql_try_catch_block
    def get_alarms_count(self, tenant_id, query_parms=None, offset=None, limit=None):
        if not query_parms:
            query_parms = {}

        with self._db_engine.connect() as conn:
            parms = {}
            ad = self.ad
            am = self.am
            mdd = self.mdd
            mde = self.mde
            md = self.md
            a = self.a

            query_from = a.join(ad, ad.c.id == a.c.alarm_definition_id)

            parms['b_tenant_id'] = tenant_id

            group_by_columns = []

            if 'group_by' in query_parms:
                group_by_columns = query_parms['group_by']
                sub_group_by_columns = []
                metric_group_by = {'metric_name',
                                   'dimension_name',
                                   'dimension_value'}.intersection(set(query_parms['group_by']))
                if metric_group_by:
                    sub_query_columns = [am.c.alarm_id]
                    if 'metric_name' in metric_group_by:
                        sub_group_by_columns.append(mde.c.name.label('metric_name'))
                    if 'dimension_name' in metric_group_by:
                        sub_group_by_columns.append(md.c.name.label('dimension_name'))
                    if 'dimension_value' in metric_group_by:
                        sub_group_by_columns.append(md.c.value.label('dimension_value'))

                    sub_query_columns.extend(sub_group_by_columns)

                    sub_query_from = (mde.join(mdd, mde.c.id == mdd.c.metric_definition_id)
                                      .join(md, mdd.c.metric_dimension_set_id == md.c.dimension_set_id)
                                      .join(am, am.c.metric_definition_dimensions_id == mdd.c.id))

                    sub_query = (select(sub_query_columns)
                                 .select_from(sub_query_from)
                                 .distinct()
                                 .alias('metrics'))

                    query_from = query_from.join(sub_query, sub_query.c.alarm_id == a.c.id)

            query_columns = [func.count().label('count')]
            query_columns.extend(group_by_columns)

            query = (select(query_columns)
                     .select_from(query_from)
                     .where(ad.c.tenant_id == bindparam('b_tenant_id')))

            parms['b_tenant_id'] = tenant_id

            if 'alarm_definition_id' in query_parms:
                parms['b_alarm_definition_id'] = query_parms['alarm_definition_id']
                query = query.where(ad.c.id == bindparam('b_alarm_definition_id'))

            if 'state' in query_parms:
                parms['b_state'] = query_parms['state'].encode('utf8')
                query = query.where(a.c.state == bindparam('b_state'))

            if 'severity' in query_parms:
                severities = query_parms['severity'].split('|')
                query = query.where(
                    or_(ad.c.severity == bindparam('b_severity' + str(i)) for i in xrange(len(severities))))
                for i, s in enumerate(severities):
                    parms['b_severity' + str(i)] = s.encode('utf8')

            if 'lifecycle_state' in query_parms:
                parms['b_lifecycle_state'] = query_parms['lifecycle_state'].encode('utf8')
                query = query.where(a.c.lifecycle_state == bindparam('b_lifecycle_state'))

            if 'link' in query_parms:
                parms['b_link'] = query_parms['link'].encode('utf8')
                query = query.where(a.c.link == bindparam('b_link'))

            if 'state_updated_start_time' in query_parms:
                parms['b_state_updated_at'] = query_parms['state_updated_start_time'].encode("utf8")
                query = query.where(a.c.state_updated_at >= bindparam('b_state_updated_at'))

            if 'metric_name' in query_parms:
                subquery_md = (select([md])
                               .where(md.c.name == bindparam('b_metric_name'))
                               .distinct()
                               .alias('md_'))
                subquery = (select([a.c.id])
                            .select_from(am.join(a, a.c.id == am.c.alarm_id)
                                         .join(mdd, mdd.c.id == am.c.metric_definition_dimensions_id)
                                         .join(subquery_md, subquery_md.c.id == mdd.c.metric_definition_id))
                            .distinct())
                query = query.where(a.c.id.in_(subquery))
                parms['b_metric_name'] = query_parms['metric_name'].encode('utf8')

            if 'metric_dimensions' in query_parms:
                sub_query = select([a.c.id])
                sub_query_from = (a.join(am, am.c.alarm_id == a.c.id)
                                  .join(mdd,
                                        mdd.c.id ==
                                        am.c.metric_definition_dimensions_id))

                sub_query_md_base = select([md.c.dimension_set_id]).select_from(md)

                for i, metric_dimension in enumerate(query_parms['metric_dimensions']):
                    md_name = "b_md_name_{}".format(i)
                    md_value = "b_md_value_{}".format(i)

                    sub_query_md = (sub_query_md_base
                                    .where(md.c.name == bindparam(md_name))
                                    .where(md.c.value == bindparam(md_value))
                                    .distinct()
                                    .alias('md_{}'.format(i)))

                    parsed_dimension = metric_dimension.split(':')
                    sub_query_from = (sub_query_from
                                      .join(sub_query_md,
                                            sub_query_md.c.dimension_set_id ==
                                            mdd.c.metric_dimension_set_id))

                    parms[md_name] = parsed_dimension[0].encode('utf8')
                    parms[md_value] = parsed_dimension[1].encode('utf8')

                    sub_query = (sub_query
                                 .select_from(sub_query_from)
                                 .distinct())
                    query = query.where(a.c.id.in_(sub_query))

            if group_by_columns:
                query = (query
                         .order_by(*group_by_columns)
                         .group_by(*group_by_columns))

            if limit:
                query = query.limit(bindparam('b_limit'))
                parms['b_limit'] = limit + 1

            if offset:
                query = query.offset(bindparam('b_offset'))
                parms['b_offset'] = offset

            query = query.distinct()

            return [dict(row) for row in conn.execute(query, parms).fetchall()]
