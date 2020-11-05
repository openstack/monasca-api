# (C) Copyright 2014-2017 Hewlett Packard Enterprise Development LP
# Copyright 2016 FUJITSU LIMITED
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

import datetime
import six

from oslo_utils import encodeutils
from oslo_utils import uuidutils
from sqlalchemy import MetaData, update, delete, insert
from sqlalchemy import select, text, bindparam, null, literal_column
from sqlalchemy import or_

from monasca_api.common.repositories import alarm_definitions_repository as adr
from monasca_api.common.repositories import exceptions
from monasca_api.common.repositories.model import sub_alarm_definition
from monasca_api.common.repositories.sqla import models
from monasca_api.common.repositories.sqla import sql_repository


class AlarmDefinitionsRepository(sql_repository.SQLRepository,
                                 adr.AlarmDefinitionsRepository):

    def __init__(self):
        """
        Initialize the database.

        Args:
            self: (todo): write your description
        """
        super(AlarmDefinitionsRepository, self).__init__()

        metadata = MetaData()
        self.a = models.create_a_model(metadata)
        self.aa = models.create_aa_model(metadata)
        self.ad = models.create_ad_model(metadata)
        self.am = models.create_am_model(metadata)
        self.nm = models.create_nm_model(metadata)
        self.md = models.create_md_model(metadata)
        self.mde = models.create_mde_model(metadata)
        self.mdd = models.create_mdd_model(metadata)
        self.sa = models.create_sa_model(metadata)
        self.sad = models.create_sad_model(metadata)
        self.sadd = models.create_sadd_model(metadata)
        a = self.a
        aa = self.aa
        ad = self.ad
        am = self.am
        nm = self.nm
        md = self.md
        sa = self.sa
        mdd = self.mdd
        mde = self.mde
        sad = self.sad
        sadd = self.sadd

        a_s = a.alias('a')
        ad_s = ad.alias('ad')
        self.ad_s = ad_s
        am_s = am.alias('am')
        nm_s = nm.alias('nm')
        md_s = md.alias('md')
        sa_s = sa.alias('sa')
        mdd_s = mdd.alias('mdd')
        mde_s = mde.alias('mde')
        sad_s = sad.alias('sad')
        sadd_s = sadd.alias('sadd')

        aaa_aa = aa.alias('aaa_aa')
        aaa = (select([aaa_aa.c.alarm_definition_id,
                       models.group_concat([aaa_aa.c.action_id]).label('alarm_actions')])
               .select_from(aaa_aa)
               .where(aaa_aa.c.alarm_state == text("'ALARM'"))
               .group_by(aaa_aa.c.alarm_definition_id)
               .alias('aaa'))

        aao_aa = aa.alias('aao_aa')
        aao = (select([aao_aa.c.alarm_definition_id,
                       models.group_concat([aao_aa.c.action_id]).label('ok_actions')])
               .select_from(aao_aa)
               .where(aao_aa.c.alarm_state == text("'OK'"))
               .group_by(aao_aa.c.alarm_definition_id)
               .alias('aao'))

        aau_aa = aa.alias('aau_aa')
        aau = (select([aau_aa.c.alarm_definition_id,
                       models.group_concat([aau_aa.c.action_id]).label('undetermined_actions')])
               .select_from(aau_aa)
               .where(aau_aa.c.alarm_state == text("'UNDETERMINED'"))
               .group_by(aau_aa.c.alarm_definition_id)
               .alias('aau'))

        self.base_query_from = (ad_s.outerjoin(aaa, aaa.c.alarm_definition_id == ad_s.c.id)
                                .outerjoin(aao, aao.c.alarm_definition_id == ad_s.c.id)
                                .outerjoin(aau, aau.c.alarm_definition_id == ad_s.c.id))

        self.base_query = (select([ad_s.c.id,
                                   ad_s.c.name,
                                   ad_s.c.description,
                                   ad_s.c.expression,
                                   ad_s.c.match_by,
                                   ad_s.c.severity,
                                   ad_s.c.actions_enabled,
                                   aaa.c.alarm_actions,
                                   aao.c.ok_actions,
                                   aau.c.undetermined_actions]))

        self.get_sub_alarms_query = (
            select(
                [
                    sa_s.c.id.label('sub_alarm_id'),
                    sa_s.c.alarm_id,
                    sa_s.c.expression]) .select_from(
                sa_s.join(
                    a_s,
                    a_s.c.id == sa_s.c.alarm_id) .join(
                        ad_s,
                        ad_s.c.id == a_s.c.alarm_definition_id)) .where(
                            ad_s.c.tenant_id == bindparam('b_tenant_id')) .where(
                                ad_s.c.id == bindparam('b_id')) .distinct())

        mdg = (select([md_s.c.dimension_set_id,
                       models.group_concat(
                           [md_s.c.name + text("'='") + md_s.c.value]).label('dimensions')])
               .select_from(md_s)
               .group_by(md_s.c.dimension_set_id)
               .alias('mdg'))

        self.get_alarm_metrics_query = (
            select(
                [a_s.c.id.label('alarm_id'),
                 mde_s.c.name,
                 mdg.c.dimensions]) .select_from(
                a_s.join(
                    ad_s,
                    ad_s.c.id == a_s.c.alarm_definition_id) .join(
                    am_s,
                    am_s.c.alarm_id == a_s.c.id) .join(
                    mdd_s,
                    mdd_s.c.id == am_s.c.metric_definition_dimensions_id) .join(
                    mde_s,
                    mde_s.c.id == mdd_s.c.metric_definition_id) .outerjoin(
                    mdg,
                    mdg.c.dimension_set_id == mdd_s.c.metric_dimension_set_id)) .where(
                ad_s.c.tenant_id == bindparam('b_tenant_id')) .where(
                ad_s.c.id == bindparam('b_id')) .order_by(
                a_s.c.id) .distinct())

        self.soft_delete_ad_query = (update(ad)
                                     .where(ad.c.tenant_id == bindparam('b_tenant_id'))
                                     .where(ad.c.id == bindparam('b_id'))
                                     .where(ad.c.deleted_at == null())
                                     .values(deleted_at=datetime.datetime.utcnow()))

        self.delete_a_query = (delete(a)
                               .where(a.c.alarm_definition_id == bindparam('b_id')))

        columns_gc = [sadd_s.c.dimension_name + text("'='") + sadd_s.c.value]
        saddg = (select([sadd_s.c.sub_alarm_definition_id,
                         models.group_concat(columns_gc).label('dimensions')])
                 .select_from(sadd_s)
                 .group_by(sadd_s.c.sub_alarm_definition_id)
                 .alias('saddg'))

        self.get_sub_alarm_definitions_query = (
            select(
                [
                    sad_s,
                    saddg.c.dimensions]) .select_from(
                sad_s.outerjoin(
                    saddg,
                    saddg.c.sub_alarm_definition_id == sad_s.c.id)) .where(
                        sad_s.c.alarm_definition_id == bindparam('b_alarm_definition_id')))

        self.create_alarm_definition_insert_ad_query = (
            insert(ad) .values(
                id=bindparam('b_id'),
                tenant_id=bindparam('b_tenant_id'),
                name=bindparam('b_name'),
                description=bindparam('b_description'),
                expression=bindparam('b_expression'),
                severity=bindparam('b_severity'),
                match_by=bindparam('b_match_by'),
                actions_enabled=bindparam('b_actions_enabled'),
                created_at=bindparam('b_created_at'),
                updated_at=bindparam('b_updated_at')))

        self.create_alarm_definition_insert_sad_query = (
            insert(sad) .values(
                id=bindparam('b_id'),
                alarm_definition_id=bindparam('b_alarm_definition_id'),
                function=bindparam('b_function'),
                metric_name=bindparam('b_metric_name'),
                operator=bindparam('b_operator'),
                threshold=bindparam('b_threshold'),
                period=bindparam('b_period'),
                periods=bindparam('b_periods'),
                is_deterministic=bindparam('b_is_deterministic'),
                created_at=bindparam('b_created_at'),
                updated_at=bindparam('b_updated_at')))

        b_sad_id = bindparam('b_sub_alarm_definition_id')
        self.create_alarm_definition_insert_sadd_query = (
            insert(sadd) .values(
                sub_alarm_definition_id=b_sad_id,
                dimension_name=bindparam('b_dimension_name'),
                value=bindparam('b_value')))

        self.update_or_patch_alarm_definition_update_ad_query = (
            update(ad) .where(
                ad.c.tenant_id == bindparam('b_tenant_id')) .where(
                ad.c.id == bindparam('b_id')))

        self.update_or_patch_alarm_definition_delete_sad_query = (
            delete(sad) .where(sad.c.id == bindparam('b_id')))

        self.update_or_patch_alarm_definition_update_sad_query = (
            update(sad) .where(
                sad.c.id == bindparam('b_id')) .values(
                operator=bindparam('b_operator'),
                threshold=bindparam('b_threshold'),
                is_deterministic=bindparam('b_is_deterministic'),
                updated_at=bindparam('b_updated_at')))

        b_ad_id = bindparam('b_alarm_definition_id'),
        self.update_or_patch_alarm_definition_insert_sad_query = (
            insert(sad) .values(
                id=bindparam('b_id'),
                alarm_definition_id=b_ad_id,
                function=bindparam('b_function'),
                metric_name=bindparam('b_metric_name'),
                operator=bindparam('b_operator'),
                threshold=bindparam('b_threshold'),
                period=bindparam('b_period'),
                periods=bindparam('b_periods'),
                is_deterministic=bindparam('b_is_deterministic'),
                created_at=bindparam('b_created_at'),
                updated_at=bindparam('b_updated_at')))

        self.update_or_patch_alarm_definition_insert_sadd_query = (
            insert(sadd) .values(
                sub_alarm_definition_id=b_sad_id,
                dimension_name=bindparam('b_dimension_name'),
                value=bindparam('b_value')))

        self.delete_aa_query = (delete(aa)
                                .where(aa.c.alarm_definition_id ==
                                       bindparam('b_alarm_definition_id')))

        self.delete_aa_state_query = (
            delete(aa) .where(
                aa.c.alarm_definition_id == bindparam('b_alarm_definition_id')) .where(
                aa.c.alarm_state == bindparam('b_alarm_state')))

        self.select_nm_query = (select([nm_s.c.id])
                                .select_from(nm_s)
                                .where(nm_s.c.id == bindparam('b_id')))

        self.insert_aa_query = (insert(aa)
                                .values(
                                    alarm_definition_id=bindparam('b_alarm_definition_id'),
                                    alarm_state=bindparam('b_alarm_state'),
                                    action_id=bindparam('b_action_id')))

    @sql_repository.sql_try_catch_block
    def get_alarm_definition(self, tenant_id, _id):
        """
        Get alarm definition.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            _id: (str): write your description
        """
        with self._db_engine.connect() as conn:
            return self._get_alarm_definition(conn, tenant_id, _id)

    def _get_alarm_definition(self, conn, tenant_id, _id):
        """
        Retrieves the alarm definition.

        Args:
            self: (todo): write your description
            conn: (todo): write your description
            tenant_id: (str): write your description
            _id: (str): write your description
        """
        ad = self.ad_s
        query = (self.base_query
                 .select_from(self.base_query_from)
                 .where(ad.c.tenant_id == bindparam('b_tenant_id'))
                 .where(ad.c.id == bindparam('b_id'))
                 .where(ad.c.deleted_at == null()))

        row = conn.execute(query,
                           b_tenant_id=tenant_id,
                           b_id=_id).fetchone()

        if row is not None:
            return dict(row)
        else:
            raise exceptions.DoesNotExistException

    @sql_repository.sql_try_catch_block
    def get_alarm_definitions(self, tenant_id, name=None, dimensions=None, severity=None,
                              sort_by=None, offset=None, limit=1000):
        """
        Get all alarm information.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            name: (str): write your description
            dimensions: (dict): write your description
            severity: (str): write your description
            sort_by: (str): write your description
            offset: (todo): write your description
            limit: (todo): write your description
        """

        with self._db_engine.connect() as conn:
            ad = self.ad_s
            sad = self.sad.alias('sad')
            sadd = self.sadd.alias('sadd')
            query_from = self.base_query_from

            parms = {'b_tenant_id': tenant_id}

            if dimensions:
                sadi = sad.c.alarm_definition_id
                query_from = query_from.join(sad, sadi == ad.c.id)

                i = 0
                for n, v in dimensions.items():
                    bind_dimension_name = 'b_sadd_dimension_name_{}'.format(i)
                    bind_value = 'b_sadd_value_{}'.format(i)
                    sadd_ = (select([sadd.c.sub_alarm_definition_id])
                             .select_from(sadd)
                             .where(sadd.c.dimension_name == bindparam(bind_dimension_name))
                             .where(sadd.c.value == bindparam(bind_value))
                             .distinct().alias('saad_{}'.format(i)))

                    sadd_id = sadd_.c.sub_alarm_definition_id
                    query_from = query_from.join(sadd_, sadd_id == sad.c.id)
                    parms[bind_dimension_name] = n.encode('utf8') if six.PY2 else n
                    parms[bind_value] = v.encode('utf8') if six.PY2 else v

                    i += 1

            query = (self.base_query
                     .select_from(query_from)
                     .where(ad.c.tenant_id == bindparam('b_tenant_id'))
                     .where(ad.c.deleted_at == null()))

            if name:
                query = query.where(ad.c.name == bindparam('b_name'))
                parms['b_name'] = name.encode('utf-8') if six.PY2 else name

            if severity:
                severities = severity.split('|')
                query = query.where(or_(ad.c.severity == bindparam(
                    'b_severity' + str(i)) for i in range(len(severities))))
                for i, s in enumerate(severities):
                    parms['b_severity' + str(i)] = s.encode('utf-8') if six.PY2 else s

            order_columns = []
            if sort_by is not None:
                order_columns = [literal_column('ad.' + col) for col in sort_by]
                if 'id' not in sort_by:
                    order_columns.append(ad.c.id)
            else:
                order_columns = [ad.c.id]

            if offset:
                query = query.offset(bindparam('b_offset'))
                parms['b_offset'] = offset

            query = query.order_by(*order_columns)

            query = query.limit(bindparam('b_limit'))

            parms['b_limit'] = limit + 1

            return [dict(row) for row in conn.execute(query, parms).fetchall()]

    @sql_repository.sql_try_catch_block
    def get_sub_alarms(self, tenant_id, alarm_definition_id):
        """
        Retrieves all alarms.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            alarm_definition_id: (str): write your description
        """

        with self._db_engine.connect() as conn:
            return [dict(row) for row in conn.execute(self.get_sub_alarms_query,
                                                      b_tenant_id=tenant_id,
                                                      b_id=alarm_definition_id).fetchall()]

    @sql_repository.sql_try_catch_block
    def get_alarm_metrics(self, tenant_id, alarm_definition_id):
        """
        Returns a list of alarm metrics.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            alarm_definition_id: (str): write your description
        """
        with self._db_engine.connect() as conn:
            return [dict(row) for row in conn.execute(self.get_alarm_metrics_query,
                                                      b_tenant_id=tenant_id,
                                                      b_id=alarm_definition_id).fetchall()]

    @sql_repository.sql_try_catch_block
    def delete_alarm_definition(self, tenant_id, alarm_definition_id):
        """Soft delete the alarm definition.

        Soft delete the alarm definition and hard delete any associated
        alarms.

        :param tenant_id:
        :param alarm_definition_id:
        :returns True: -- if alarm definition exists and was deleted.
        :returns False: -- if the alarm definition does not exists.
        :raises RepositoryException:
        """

        with self._db_engine.begin() as conn:
            cursor = conn.execute(self.soft_delete_ad_query,
                                  b_tenant_id=tenant_id,
                                  b_id=alarm_definition_id)

            if cursor.rowcount < 1:
                return False

            conn.execute(self.delete_a_query,
                         b_tenant_id=tenant_id,
                         b_id=alarm_definition_id)

            return True

    @sql_repository.sql_try_catch_block
    def get_sub_alarm_definitions(self, alarm_definition_id):
        """
        Returns the alarm definition for a given alarm.

        Args:
            self: (todo): write your description
            alarm_definition_id: (str): write your description
        """
        with self._db_engine.connect() as conn:
            return self._get_sub_alarm_definitions(conn, alarm_definition_id)

    def _get_sub_alarm_definitions(self, conn, alarm_definition_id):
        """
        Returns a list of alarm definitions.

        Args:
            self: (todo): write your description
            conn: (todo): write your description
            alarm_definition_id: (str): write your description
        """
        return [
            dict(row) for row in conn.execute(
                self.get_sub_alarm_definitions_query,
                b_alarm_definition_id=alarm_definition_id).fetchall()]

    @sql_repository.sql_try_catch_block
    def create_alarm_definition(self, tenant_id, name, expression,
                                sub_expr_list, description, severity, match_by,
                                alarm_actions, undetermined_actions,
                                ok_actions):
        """
        Create an alarm definition.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            name: (str): write your description
            expression: (str): write your description
            sub_expr_list: (list): write your description
            description: (str): write your description
            severity: (str): write your description
            match_by: (todo): write your description
            alarm_actions: (todo): write your description
            undetermined_actions: (todo): write your description
            ok_actions: (todo): write your description
        """
        with self._db_engine.begin() as conn:

            now = datetime.datetime.utcnow()
            alarm_definition_id = uuidutils.generate_uuid()

            conn.execute(self.create_alarm_definition_insert_ad_query,
                         b_id=alarm_definition_id,
                         b_tenant_id=tenant_id,
                         b_name=name.encode('utf8'),
                         b_description=description.encode('utf8'),
                         b_expression=expression.encode('utf8'),
                         b_severity=severity.upper().encode('utf8'),
                         b_match_by=",".join(match_by).encode('utf8'),
                         b_actions_enabled=True,
                         b_created_at=now,
                         b_updated_at=now)

            for sub_expr in sub_expr_list:
                sub_alarm_definition_id = uuidutils.generate_uuid()
                sub_expr.id = sub_alarm_definition_id
                metric_name = sub_expr.metric_name.encode("utf8")
                operator = sub_expr.normalized_operator.encode('utf8')
                conn.execute(self.create_alarm_definition_insert_sad_query,
                             b_id=sub_alarm_definition_id,
                             b_alarm_definition_id=alarm_definition_id,
                             b_function=sub_expr.normalized_func.encode('utf8'),
                             b_metric_name=metric_name,
                             b_operator=operator,
                             b_threshold=sub_expr.threshold,
                             b_period=sub_expr.period,
                             b_periods=sub_expr.periods,
                             b_is_deterministic=sub_expr.deterministic,
                             b_created_at=now,
                             b_updated_at=now)

                for dimension in sub_expr.dimensions_as_list:
                    parsed_dimension = dimension.split('=')
                    query = self.create_alarm_definition_insert_sadd_query
                    sadi = sub_alarm_definition_id
                    dimension_name = parsed_dimension[0].encode('utf8')
                    conn.execute(query,
                                 b_sub_alarm_definition_id=sadi,
                                 b_dimension_name=dimension_name,
                                 b_value=parsed_dimension[1].encode('utf8'))

            self._insert_into_alarm_action(conn, alarm_definition_id,
                                           alarm_actions, u"ALARM")
            self._insert_into_alarm_action(conn, alarm_definition_id,
                                           undetermined_actions,
                                           u"UNDETERMINED")
            self._insert_into_alarm_action(conn, alarm_definition_id,
                                           ok_actions, u"OK")

            return alarm_definition_id

    @sql_repository.sql_try_catch_block
    def update_or_patch_alarm_definition(self, tenant_id, alarm_definition_id,
                                         name, expression,
                                         sub_expr_list, actions_enabled,
                                         description, alarm_actions,
                                         ok_actions, undetermined_actions,
                                         match_by, severity, patch=False):
        """
        Updates a alarm definition.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            alarm_definition_id: (str): write your description
            name: (str): write your description
            expression: (todo): write your description
            sub_expr_list: (list): write your description
            actions_enabled: (bool): write your description
            description: (str): write your description
            alarm_actions: (todo): write your description
            ok_actions: (todo): write your description
            undetermined_actions: (todo): write your description
            match_by: (todo): write your description
            severity: (todo): write your description
            patch: (todo): write your description
        """

        with self._db_engine.begin() as conn:
            original_row = self._get_alarm_definition(conn,
                                                      tenant_id,
                                                      alarm_definition_id)
            rows = self._get_sub_alarm_definitions(conn, alarm_definition_id)

            old_sub_alarm_defs_by_id = {}

            for row in rows:
                sad = sub_alarm_definition.SubAlarmDefinition(row=row)
                old_sub_alarm_defs_by_id[sad.id] = sad

            if expression:
                (
                    changed_sub_alarm_defs_by_id,
                    new_sub_alarm_defs_by_id,
                    old_sub_alarm_defs_by_id,
                    unchanged_sub_alarm_defs_by_id
                ) = self._determine_sub_expr_changes(
                    alarm_definition_id, old_sub_alarm_defs_by_id,
                    sub_expr_list)

                if old_sub_alarm_defs_by_id or new_sub_alarm_defs_by_id:
                    new_count = (len(new_sub_alarm_defs_by_id) +
                                 len(changed_sub_alarm_defs_by_id) +
                                 len(unchanged_sub_alarm_defs_by_id))
                    old_count = len(old_sub_alarm_defs_by_id)
                    if new_count != old_count:
                        msg = 'number of subexpressions must not change'
                    else:
                        msg = 'metrics in subexpression must not change'
                    raise exceptions.InvalidUpdateException(msg.encode('utf8'))
            else:
                unchanged_sub_alarm_defs_by_id = old_sub_alarm_defs_by_id
                changed_sub_alarm_defs_by_id = {}
                new_sub_alarm_defs_by_id = {}
                old_sub_alarm_defs_by_id = {}

            # Get a common update time
            now = datetime.datetime.utcnow()

            if name is None:
                new_name = original_row['name']
            else:
                new_name = name.encode('utf-8') if six.PY2 else name

            if description is None:
                if patch:
                    new_description = original_row['description']
                else:
                    new_description = ''
            else:
                new_description = description.encode('utf-8') if six.PY2 else description

            if expression is None:
                new_expression = original_row['expression']
            else:
                new_expression = expression.encode('utf8') if six.PY2 else expression

            if severity is None:
                if patch:
                    new_severity = original_row['severity']
                else:
                    new_severity = 'LOW'
            else:
                new_severity = severity.encode('utf8') if six.PY2 else severity

            if match_by is None:
                if patch:
                    new_match_by = original_row['match_by']
                else:
                    new_match_by = None
            else:
                match = ",".join(match_by)
                new_match_by = match.encode('utf8') if six.PY2 else match

            if new_match_by != original_row['match_by']:
                msg = u"match_by must not change"
                raise exceptions.InvalidUpdateException(msg)

            if actions_enabled is None:
                new_actions_enabled = original_row['actions_enabled']
            else:
                new_actions_enabled = actions_enabled

            conn.execute(self.update_or_patch_alarm_definition_update_ad_query
                         .values(
                             name=bindparam('b_name'),
                             description=bindparam('b_description'),
                             expression=bindparam('b_expression'),
                             match_by=bindparam('b_match_by'),
                             severity=bindparam('b_severity'),
                             actions_enabled=bindparam('b_actions_enabled'),
                             updated_at=bindparam('b_updated_at')
                         ),
                         b_name=new_name,
                         b_description=new_description,
                         b_expression=new_expression,
                         b_match_by=new_match_by,
                         b_severity=new_severity,
                         b_actions_enabled=bool(new_actions_enabled),
                         b_updated_at=now,
                         b_tenant_id=tenant_id,
                         b_id=alarm_definition_id)
            parms = []
            for sub_alarm_definition_id, sub_alarm_def in (
                    changed_sub_alarm_defs_by_id.items()):
                parms.append({'b_operator': sub_alarm_def.operator,
                              'b_threshold': sub_alarm_def.threshold,
                              'b_is_deterministic': sub_alarm_def.deterministic,
                              'b_updated_at': now,
                              'b_id': sub_alarm_definition_id})
            if len(parms) > 0:
                query = self.update_or_patch_alarm_definition_update_sad_query
                conn.execute(query, parms)

            # Delete old alarm actions
            if patch:
                if alarm_actions is not None:
                    self._delete_alarm_actions(conn, alarm_definition_id,
                                               'ALARM')
                if ok_actions is not None:
                    self._delete_alarm_actions(conn, alarm_definition_id,
                                               'OK')
                if undetermined_actions is not None:
                    self._delete_alarm_actions(conn, alarm_definition_id,
                                               'UNDETERMINED')
            else:
                conn.execute(self.delete_aa_query,
                             b_alarm_definition_id=alarm_definition_id)

            # Insert new alarm actions
            self._insert_into_alarm_action(conn, alarm_definition_id,
                                           alarm_actions,
                                           u"ALARM")

            self._insert_into_alarm_action(conn, alarm_definition_id,
                                           undetermined_actions,
                                           u"UNDETERMINED")

            self._insert_into_alarm_action(conn, alarm_definition_id,
                                           ok_actions,
                                           u"OK")

            ad = self.ad_s
            query = (self.base_query
                     .select_from(self.base_query_from)
                     .where(ad.c.tenant_id == bindparam('b_tenant_id'))
                     .where(ad.c.id == bindparam('b_id'))
                     .where(ad.c.deleted_at == null()))

            updated_row = conn.execute(query,
                                       b_id=alarm_definition_id,
                                       b_tenant_id=tenant_id).fetchone()

            if updated_row is None:
                raise Exception("Failed to find current alarm definition")

            sub_alarm_defs_dict = {'old': old_sub_alarm_defs_by_id,
                                   'changed': changed_sub_alarm_defs_by_id,
                                   'new': new_sub_alarm_defs_by_id,
                                   'unchanged': unchanged_sub_alarm_defs_by_id}

            # Return the alarm def and the sub alarm defs
            return updated_row, sub_alarm_defs_dict

    def _determine_sub_expr_changes(self, alarm_definition_id,
                                    old_sub_alarm_defs_by_id,
                                    sub_expr_list):
        """
        Determine whether the given.

        Args:
            self: (todo): write your description
            alarm_definition_id: (str): write your description
            old_sub_alarm_defs_by_id: (todo): write your description
            sub_expr_list: (list): write your description
        """

        old_sub_alarm_defs_set = set(
            old_sub_alarm_defs_by_id.values())

        new_sub_alarm_defs_set = set()
        for sub_expr in sub_expr_list:
            sad = sub_alarm_definition.SubAlarmDefinition(
                sub_expr=sub_expr)
            # Inject the alarm definition id.
            sad.alarm_definition_id = alarm_definition_id.decode('utf8') if six.PY2 \
                else alarm_definition_id
            new_sub_alarm_defs_set.add(sad)

        # Identify old or changed expressions
        old_or_changed_sub_alarm_defs_set = (
            old_sub_alarm_defs_set - new_sub_alarm_defs_set)
        # Identify new or changed expressions
        new_or_changed_sub_alarm_defs_set = (
            new_sub_alarm_defs_set - old_sub_alarm_defs_set)
        # Find changed expressions. O(n^2) == bad!
        # This algo may not work if sub expressions are duplicated.
        changed_sub_alarm_defs_by_id = {}
        old_or_changed_sub_alarm_defs_set_to_remove = set()
        new_or_changed_sub_alarm_defs_set_to_remove = set()
        for old_or_changed in old_or_changed_sub_alarm_defs_set:
            for new_or_changed in new_or_changed_sub_alarm_defs_set:
                if old_or_changed.same_key_fields(new_or_changed):
                    old_or_changed_sub_alarm_defs_set_to_remove.add(
                        old_or_changed
                    )
                    new_or_changed_sub_alarm_defs_set_to_remove.add(
                        new_or_changed
                    )
                    changed_sub_alarm_defs_by_id[
                        old_or_changed.id] = (
                        new_or_changed)
                    # patch id:
                    changed_sub_alarm_defs_by_id[
                        old_or_changed.id].id = old_or_changed.id
        old_or_changed_sub_alarm_defs_set = (
            old_or_changed_sub_alarm_defs_set -
            old_or_changed_sub_alarm_defs_set_to_remove
        )
        new_or_changed_sub_alarm_defs_set = (
            new_or_changed_sub_alarm_defs_set -
            new_or_changed_sub_alarm_defs_set_to_remove
        )
        # Create the list of unchanged expressions
        unchanged_sub_alarm_defs_by_id = (
            old_sub_alarm_defs_by_id.copy())
        for old_sub_alarm_def in old_or_changed_sub_alarm_defs_set:
            del unchanged_sub_alarm_defs_by_id[old_sub_alarm_def.id]
        for sub_alarm_definition_id in (
                changed_sub_alarm_defs_by_id.keys()):
            del unchanged_sub_alarm_defs_by_id[
                sub_alarm_definition_id]

        # Remove old sub expressions
        temp = {}
        for old_sub_alarm_def in old_or_changed_sub_alarm_defs_set:
            temp[old_sub_alarm_def.id] = old_sub_alarm_def
        old_sub_alarm_defs_by_id = temp
        # Create IDs for new expressions
        new_sub_alarm_defs_by_id = {}
        for new_sub_alarm_def in new_or_changed_sub_alarm_defs_set:
            sub_alarm_definition_id = uuidutils.generate_uuid()
            new_sub_alarm_def.id = sub_alarm_definition_id
            new_sub_alarm_defs_by_id[sub_alarm_definition_id] = (
                new_sub_alarm_def)

        return (changed_sub_alarm_defs_by_id,
                new_sub_alarm_defs_by_id,
                old_sub_alarm_defs_by_id,
                unchanged_sub_alarm_defs_by_id)

    def _delete_alarm_actions(self, conn, _id, alarm_action_name):
        """
        Delete an alarm actions.

        Args:
            self: (todo): write your description
            conn: (todo): write your description
            _id: (str): write your description
            alarm_action_name: (str): write your description
        """
        conn.execute(self.delete_aa_state_query,
                     b_alarm_definition_id=_id,
                     b_alarm_state=alarm_action_name)

    def _insert_into_alarm_action(self, conn, alarm_definition_id, actions,
                                  alarm_state):
        """
        Insert an alarm into the alarm.

        Args:
            self: (todo): write your description
            conn: (todo): write your description
            alarm_definition_id: (str): write your description
            actions: (str): write your description
            alarm_state: (todo): write your description
        """

        if actions is None:
            return

        for action in actions:
            b_id = action.encode('utf8') if six.PY2 else action
            row = conn.execute(self.select_nm_query,
                               b_id=b_id).fetchone()
            if row is None:
                raise exceptions.InvalidUpdateException(
                    "Non-existent notification id {} submitted for {} "
                    "notification action".format(encodeutils.to_utf8(action),
                                                 encodeutils.to_utf8(alarm_state)))
            conn.execute(self.insert_aa_query,
                         b_alarm_definition_id=alarm_definition_id,
                         b_alarm_state=alarm_state.encode('utf8') if six.PY2 else alarm_state,
                         b_action_id=action.encode('utf8') if six.PY2 else action
                         )
