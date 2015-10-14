# Copyright 2014 Hewlett-Packard
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

from oslo_log import log

from monasca_api.common.repositories import alarms_repository
from monasca_api.common.repositories import exceptions
from monasca_api.common.repositories.mysql import mysql_repository


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

        with cnxn:

            select_query = """
                select a.state
                from alarm as a
                inner join alarm_definition as ad
                  on ad.id = a.alarm_definition_id
                where ad.tenant_id = %s and a.id = %s"""

            cursor.execute(select_query, (tenant_id, id))

            if cursor.rowcount < 1:
                raise exceptions.DoesNotExistException

            prev_alarm = cursor.fetchone()

            parms = [lifecycle_state, link]
            set_str = "lifecycle_state = %s, link = %s, updated_at = NOW()"

            if state != prev_alarm['state']:
                parms.append(state)
                set_str += ",state = %s, state_updated_at = NOW()"

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

            return prev_alarm['state']

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

        select_clause = AlarmsRepository.base_query

        order_by_clause = " order by a.id "

        where_clause = " where ad.tenant_id = %s "

        if offset:
            where_clause += " and ad.id > %s"
            parms.append(offset.encode('utf8'))

        if 'alarm_definition_id' in query_parms:
            parms.append(query_parms['alarm_definition_id'])
            where_clause += " and ad.id = %s "

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

        if 'state' in query_parms:
            parms.append(query_parms['state'].encode('utf8'))
            where_clause += " and a.state = %s "

        if 'lifecycle_state' in query_parms:
            parms.append(query_parms['lifecycle_state'].encode('utf8'))
            where_clause += " and a.lifecycle_state = %s"

        if 'link' in query_parms:
            parms.append(query_parms['link'].encode('utf8'))
            where_clause += " and a.link = %s"

        if 'state_updated_start_time' in query_parms:
            parms.append(query_parms['state_updated_start_time']
                         .encode("utf8"))
            where_clause += " and state_updated_at >= %s"

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
            for metric_dimension in query_parms['metric_dimensions'].split(
                    ','):
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

        if limit:
            limit_clause = " limit %s "
            parms.append(limit + 1)
        else:
            limit_clause = ""

        query = select_clause + where_clause + order_by_clause + limit_clause

        return self._execute_query(query, parms)
