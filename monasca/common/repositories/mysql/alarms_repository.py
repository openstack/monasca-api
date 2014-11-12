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
from monasca.common.repositories.exceptions import DoesNotExistException

from monasca.common.repositories import alarms_repository
from monasca.common.repositories.mysql.mysql_repository import MySQLRepository
from monasca.common.repositories.mysql.mysql_repository import try_catch_block
from monasca.openstack.common import log


LOG = log.getLogger(__name__)


class AlarmsRepository(MySQLRepository, alarms_repository.AlarmsRepository):

    base_query = """
          select distinct a.id as alarm_id, a.state,
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

    @try_catch_block
    def get_alarm(self, tenant_id, id):

        parms = [tenant_id, id]

        select_clause = AlarmsRepository.base_query

        where_clause = """ where ad.tenant_id = ?
                            and a.id = ? """

        query = select_clause + where_clause

        rows = self._execute_query(query, parms)

        if not rows:
            raise DoesNotExistException
        else:
            return rows

    @try_catch_block
    def get_alarms(self, tenant_id, query_parms):

        parms = [tenant_id]

        select_clause = AlarmsRepository.base_query

        order_by_clause = " order by a.id "

        where_clause = " where ad.tenant_id = ? "

        if 'alarm_definition_id' in query_parms:
            parms.append(query_parms['alarm_definition_id'])
            where_clause += " and ad.id = ? "

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
                                        where name = ?) as md
                            on md.id = mdd.metric_definition_id)
                """

            parms.append(query_parms['metric_name'].encode('utf8'))
            where_clause += sub_select_clause

        if 'state' in query_parms:
            parms.append(query_parms['state'].encode('utf8'))
            where_clause += " and a.state = ? "

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
                                where name = ? and value = ?) as md{}
                    on md{}.dimension_set_id = mdd.metric_dimension_set_id
                    """.format(i, i)
                i += 1
                sub_select_parms += [parsed_dimension[0].encode('utf8'),
                                     parsed_dimension[1].encode('utf8')]

            sub_select_clause += ")"
            parms += sub_select_parms
            where_clause += sub_select_clause

        query = select_clause + where_clause + order_by_clause

        return self._execute_query(query, parms)
