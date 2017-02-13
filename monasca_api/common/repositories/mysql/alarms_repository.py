# (C) Copyright 2014-2017 Hewlett Packard Enterprise Development LP
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

from monasca_common.repositories.mysql import mysql_repository
from oslo_log import log

from monasca_api.common.repositories import alarms_repository
from monasca_api.common.repositories import exceptions


LOG = log.getLogger(__name__)


class AlarmsRepository(mysql_repository.MySQLRepository,
                       alarms_repository.AlarmsRepository):

    base_query = """
          select distinct a.id as alarm_id, a.state,
          a.state_updated_at as state_updated_timestamp,
          a.updated_at as updated_timestamp,
          a.created_at as created_timestamp, a.lifecycle_state, a.link,
          ad.id as alarm_definition_id, ad.name as alarm_definition_name,
          ad.severity,
          md.name as metric_name, mdg.dimensions as metric_dimensions
          from alarm as a
          inner join alarm_definition as ad
             on ad.id = a.alarm_definition_id
          inner join alarm_metric as am on am.alarm_id = a.id
          inner join metric_definition_dimensions as mdd
             on mdd.id = am.metric_definition_dimensions_id
          inner join metric_definition as md
             on md.id = mdd.metric_definition_id
          left join (select dimension_set_id, name, value,
                      group_concat(name, '=', value) as dimensions
                    from metric_dimension group by dimension_set_id) as mdg
            on mdg.dimension_set_id = mdd.metric_dimension_set_id
           """

    base_list_query = """
          select distinct a.id as alarm_id, a.state,
          a.state_updated_at as state_updated_timestamp,
          a.updated_at as updated_timestamp,
          a.created_at as created_timestamp, a.lifecycle_state, a.link,
          ad.id as alarm_definition_id, ad.name as alarm_definition_name,
          ad.severity,
          md.name as metric_name, mdg.dimensions as metric_dimensions
          from alarm as a
          inner join ({0}) as alarm_id_list on alarm_id_list.id = a.id
          inner join alarm_definition as ad
             on ad.id = a.alarm_definition_id
          inner join alarm_metric as am on am.alarm_id = a.id
          inner join metric_definition_dimensions as mdd
             on mdd.id = am.metric_definition_dimensions_id
          inner join metric_definition as md
             on md.id = mdd.metric_definition_id
          left join (select dimension_set_id, name, value,
                      group_concat(name, '=', value) as dimensions
                    from metric_dimension group by dimension_set_id) as mdg
            on mdg.dimension_set_id = mdd.metric_dimension_set_id
           """

    def __init__(self):

        super(AlarmsRepository, self).__init__()

    @mysql_repository.mysql_try_catch_block
    def get_alarm_definition(self, tenant_id, alarm_id):

        query = """
            select *
            from alarm_definition as ad
            inner join alarm as a on a.alarm_definition_id = ad.id
            where ad.tenant_id = %s and a.id = %s"""

        alarm_definition_rows = self._execute_query(query,
                                                    (tenant_id, alarm_id))

        if not alarm_definition_rows:
            raise exceptions.DoesNotExistException

        # There should only be 1 row.
        return alarm_definition_rows[0]

    @mysql_repository.mysql_try_catch_block
    def get_alarm_metrics(self, alarm_id):

        parms = [alarm_id]

        query = """select distinct a.id as alarm_id, md.name,
                      mdg.dimensions
                   from alarm as a
                   inner join alarm_metric as am on am.alarm_id = a.id
                   inner join metric_definition_dimensions as mdd
                      on mdd.id = am.metric_definition_dimensions_id
                   inner join metric_definition as md
                      on md.id = mdd.metric_definition_id
                   left join (select dimension_set_id,
                   group_concat(name, '=', value) as dimensions
                      from metric_dimension group by dimension_set_id) as mdg
                         on mdg.dimension_set_id = mdd.metric_dimension_set_id
                   where a.id = %s
                   order by a.id
                   """

        return self._execute_query(query, parms)

    @mysql_repository.mysql_try_catch_block
    def get_sub_alarms(self, tenant_id, alarm_id):

        parms = [tenant_id, alarm_id]

        query = """select distinct sa.id as sub_alarm_id, sa.alarm_id,
                                   sa.expression, ad.id as alarm_definition_id
                    from sub_alarm as sa
                    inner join alarm as a
                      on a.id = sa.alarm_id
                    inner join alarm_definition as ad
                      on ad.id = a.alarm_definition_id
                    where ad.tenant_id = %s and a.id = %s
                """

        return self._execute_query(query, parms)

    @mysql_repository.mysql_try_catch_block
    def update_alarm(self, tenant_id, id, state, lifecycle_state, link):

        cnxn, cursor = self._get_cnxn_cursor_tuple()
        time_ms = int(round(time() * 1000.0))
        now = datetime.utcfromtimestamp(time_ms / 1000.0)

        with cnxn:

            select_query = """
                select a.state, a.link, a.lifecycle_state
                from alarm as a
                inner join alarm_definition as ad
                  on ad.id = a.alarm_definition_id
                where ad.tenant_id = %s and a.id = %s"""

            cursor.execute(select_query, (tenant_id, id))

            if cursor.rowcount < 1:
                raise exceptions.DoesNotExistException

            prev_alarm = cursor.fetchone()

            parms = [lifecycle_state, link, now]
            set_str = "lifecycle_state = %s, link = %s, updated_at = %s"

            if state != prev_alarm['state']:
                parms.append(state)
                parms.append(now)
                set_str += ",state = %s, state_updated_at = %s"

            parms.extend([tenant_id, id])

            update_query = """
                update alarm
                set {}
                where alarm.id in
                (select distinct id
                  from
                    (select distinct alarm.id
                     from alarm
                     inner join alarm_definition
                      on alarm_definition.id = alarm.alarm_definition_id
                  where alarm_definition.tenant_id = %s and alarm.id = %s)
                  as tmptable
                )""".format(set_str)

            cursor.execute(update_query, parms)

            return prev_alarm, time_ms

    @mysql_repository.mysql_try_catch_block
    def delete_alarm(self, tenant_id, id):

        parms = [tenant_id, id]

        query = """
          delete alarm.*
          from alarm
          join
            (select distinct a.id
             from alarm as a
             inner join alarm_definition as ad
                on ad.id = a.alarm_definition_id
             where ad.tenant_id = %s and a.id = %s) as b
            on b.id = alarm.id
            """

        cnxn, cursor = self._get_cnxn_cursor_tuple()

        with cnxn:

            cursor.execute(query, parms)

            if cursor.rowcount < 1:
                raise exceptions.DoesNotExistException

    @mysql_repository.mysql_try_catch_block
    def get_alarm(self, tenant_id, id):

        parms = [tenant_id, id]

        select_clause = AlarmsRepository.base_query

        where_clause = """ where ad.tenant_id = %s
                            and a.id = %s """

        query = select_clause + where_clause

        rows = self._execute_query(query, parms)

        if not rows:
            raise exceptions.DoesNotExistException
        else:
            return rows

    @mysql_repository.mysql_try_catch_block
    def get_alarms(self, tenant_id, query_parms, offset, limit):

        parms = [tenant_id]

        select_clause = AlarmsRepository.base_list_query

        sub_query = "select a.id " \
                    "from alarm as a " \
                    "join alarm_definition as ad on a.alarm_definition_id = ad.id " \
                    "where ad.tenant_id = %s "

        if 'alarm_definition_id' in query_parms:
            parms.append(query_parms['alarm_definition_id'])
            sub_query += " and ad.id = %s "

        if 'metric_name' in query_parms:
            sub_select_clause = """
                and a.id in (select distinct a.id from alarm as a
                            inner join alarm_metric as am on am.alarm_id
                            = a.id
                            inner join metric_definition_dimensions as mdd
                                on mdd.id =
                                am.metric_definition_dimensions_id
                            inner join (select distinct id from
                            metric_definition
                                        where name = %s) as md
                            on md.id = mdd.metric_definition_id)
                """

            parms.append(query_parms['metric_name'].encode('utf8'))
            sub_query += sub_select_clause

        if 'state' in query_parms:
            parms.append(query_parms['state'].encode('utf8'))
            sub_query += " and a.state = %s "

        if 'severity' in query_parms:
            severities = query_parms['severity'].split('|')
            parms.extend([s.encode('utf8') for s in severities])
            sub_query += " and (" + " or ".join(["ad.severity = %s" for s in severities]) + ")"

        if 'lifecycle_state' in query_parms:
            parms.append(query_parms['lifecycle_state'].encode('utf8'))
            sub_query += " and a.lifecycle_state = %s"

        if 'link' in query_parms:
            parms.append(query_parms['link'].encode('utf8'))
            sub_query += " and a.link = %s"

        if 'state_updated_start_time' in query_parms:
            parms.append(query_parms['state_updated_start_time']
                         .encode("utf8"))
            sub_query += " and state_updated_at >= %s"

        if 'metric_dimensions' in query_parms:
            sub_select_clause = """
                and a.id in (select distinct a.id from alarm as a
                            inner join alarm_metric as am on am.alarm_id
                            = a.id
                            inner join metric_definition_dimensions as mdd
                                on mdd.id =
                                am.metric_definition_dimensions_id
                """
            sub_select_parms = []
            i = 0
            for metric_dimension in query_parms['metric_dimensions'].items():
                if not metric_dimension[1]:
                    values = None
                    value_sql = ""
                elif '|' in metric_dimension[1]:
                    values = metric_dimension[1].encode('utf8').split('|')
                    value_sql = " and ("
                    value_sql += " or ".join(["value = %s" for j in xrange(len(values))])
                    value_sql += ') '
                else:
                    values = [metric_dimension[1]]
                    value_sql = " and value = %s "
                sub_select_clause += """
                    inner join (select distinct dimension_set_id
                                from metric_dimension
                                where name = %s {}) as md{}
                    on md{}.dimension_set_id = mdd.metric_dimension_set_id
                    """.format(value_sql, i, i)
                i += 1
                sub_select_parms.append(metric_dimension[0].encode('utf8'))
                if len(metric_dimension) > 1 and values:
                    sub_select_parms.extend(values)

            sub_select_clause += ")"
            parms += sub_select_parms
            sub_query += sub_select_clause

        if 'sort_by' in query_parms:
            # Convert friendly names to column names
            columns_mapper = {'alarm_id': 'a.id',
                              'alarm_definition_id': 'ad.id',
                              'alarm_definition_name': 'ad.name',
                              # check this here to avoid conflict with updated_timestamp
                              'state_updated_timestamp': 'a.state_updated_at',
                              'updated_timestamp': 'a.updated_at',
                              'created_timestamp': 'a.created_at',
                              # use custom ordering instead of alphanumeric
                              'severity': 'FIELD(severity, "LOW", "MEDIUM", "HIGH", "CRITICAL")',
                              'state': 'FIELD(state, "OK", "UNDETERMINED", "ALARM")'}

            order_columns, received_cols = self._remap_columns(query_parms['sort_by'], columns_mapper)

            if not received_cols.get('alarm_id', False):
                order_columns.append('a.id')

            order_by_clause = " order by {} ".format(','.join(order_columns))
        else:
            order_by_clause = " order by a.id "

        if offset:
            offset_clause = " offset {}".format(offset)
        else:
            offset_clause = ""

        if limit:
            limit_clause = " limit %s "
            parms.append(limit + 1)
        else:
            limit_clause = ""

        query = select_clause.format(sub_query + order_by_clause + limit_clause + offset_clause) + order_by_clause

        LOG.debug("Query: {}".format(query))

        return self._execute_query(query, parms)

    def _remap_columns(self, columns, columns_mapper):
        received_cols = {}
        order_columns = []
        for col in columns:
            col_values = col.split()
            col_name = col_values[0]
            order_column = columns_mapper.get(col_name, col_name)
            if len(col_values) > 1:
                mode = col_values[1]
                order_column = "{} {}".format(order_column, mode)

            order_columns.append(order_column)
            received_cols[col_name] = True

        return order_columns, received_cols

    @mysql_repository.mysql_try_catch_block
    def get_alarms_count(self, tenant_id, query_parms, offset, limit):

        select_clause = """select count(*) as count{}
                        from alarm as a
                        join alarm_definition as ad on ad.id = a.alarm_definition_id
                        """

        if 'group_by' in query_parms:
            group_by_str = ",".join(query_parms['group_by'])

            metric_group_by = {'metric_name',
                               'dimension_name',
                               'dimension_value'}.intersection(set(query_parms['group_by']))
            if metric_group_by:
                metric_select = """
                    join (  select distinct am.alarm_id{}
                            from metric_definition as md
                            join metric_definition_dimensions as mdd on md.id = mdd.metric_definition_id
                            join metric_dimension as mdim on mdd.metric_dimension_set_id = mdim.dimension_set_id
                            join alarm_metric as am on am.metric_definition_dimensions_id = mdd.id
                         ) as metrics on a.id = metrics.alarm_id """
                sub_select_clause = ""
                if 'metric_name' in metric_group_by:
                    sub_select_clause += ', md.name as metric_name'
                if 'dimension_name' in metric_group_by:
                    sub_select_clause += ', mdim.name as dimension_name'
                if 'dimension_value' in metric_group_by:
                    sub_select_clause += ', mdim.value as dimension_value'

                select_clause += metric_select.format(sub_select_clause)

        else:
            group_by_str = ""

        parms = []

        where_clause = " where ad.tenant_id = %s "
        parms.append(tenant_id)

        if 'alarm_definition_id' in query_parms:
            parms.append(query_parms['alarm_definition_id'])
            where_clause += " and ad.id = %s "

        if 'state' in query_parms:
            parms.append(query_parms['state'].encode('utf8'))
            where_clause += " and a.state = %s "

        if 'severity' in query_parms:
            severities = query_parms['severity'].split('|')
            parms.extend([s.encode('utf8') for s in severities])
            where_clause += " and (" + " or ".join(["ad.severity = %s" for s in severities]) + ")"

        if 'lifecycle_state' in query_parms:
            parms.append(query_parms['lifecycle_state'].encode('utf8'))
            where_clause += " and a.lifecycle_state = %s "

        if 'link' in query_parms:
            parms.append(query_parms['link'].encode('utf8'))
            where_clause += " and a.link = %s "

        if 'state_updated_start_time' in query_parms:
            parms.append(query_parms['state_updated_start_time']
                         .encode("utf8"))
            where_clause += " and state_updated_at >= %s "

        if 'metric_name' in query_parms:
            sub_select_clause = """
                and a.id in (select distinct a.id from alarm as a
                            inner join alarm_metric as am on am.alarm_id
                            = a.id
                            inner join metric_definition_dimensions as mdd
                                on mdd.id =
                                am.metric_definition_dimensions_id
                            inner join (select distinct id from
                            metric_definition
                                        where name = %s) as md
                            on md.id = mdd.metric_definition_id)
                """
            parms.append(query_parms['metric_name'].encode('utf8'))
            where_clause += sub_select_clause

        if 'metric_dimensions' in query_parms:
            sub_select_clause = """
                and a.id in (select distinct a.id from alarm as a
                            inner join alarm_metric as am on am.alarm_id = a.id
                            inner join metric_definition_dimensions as mdd
                                on mdd.id = am.metric_definition_dimensions_id
                """
            sub_select_parms = []
            i = 0
            for metric_dimension in query_parms['metric_dimensions']:
                parsed_dimension = metric_dimension.split(':')
                sub_select_clause += """
                    inner join (select distinct dimension_set_id
                                from metric_dimension
                                where name = %s and value = %s) as md{}
                    on md{}.dimension_set_id = mdd.metric_dimension_set_id
                    """.format(i, i)
                i += 1
                sub_select_parms += [parsed_dimension[0].encode('utf8'),
                                     parsed_dimension[1].encode('utf8')]

            sub_select_clause += ")"
            parms += sub_select_parms
            where_clause += sub_select_clause

        if group_by_str:
            group_order_by_clause = " group by {} order by {} ".format(group_by_str, group_by_str)
        else:
            group_order_by_clause = ""

        if limit:
            limit_clause = " limit %s "
            parms.append(limit + 1)
        else:
            limit_clause = ""

        if offset:
            offset_clause = " offset {} ".format(offset)
        else:
            offset_clause = ""

        select_group_by = ',' + group_by_str if group_by_str else ""
        select_clause = select_clause.format(select_group_by)

        query = select_clause + where_clause + group_order_by_clause + limit_clause + offset_clause

        return self._execute_query(query, parms)
