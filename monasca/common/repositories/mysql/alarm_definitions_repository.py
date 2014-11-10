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
import datetime
import pyodbc

from oslo.config import cfg

from monasca.common.repositories import alarm_definitions_repository
from monasca.common.repositories.exceptions import DoesNotExistException
from monasca.common.repositories.mysql.mysql_repository import MySQLRepository
from monasca.openstack.common import log
from monasca.openstack.common import uuidutils
from monasca.common.repositories import exceptions


LOG = log.getLogger(__name__)


class AlarmDefinitionsRepository(MySQLRepository,
        alarm_definitions_repository.AlarmDefinitionsRepository):

    base_query = \
        """
          select ad.id, ad.name, ad.description, ad.expression,
            ad.match_by, ad.severity, ad.actions_enabled,
            aaa.alarm_actions, aao.ok_actions, aau.undetermined_actions
          from alarm_definition as ad
          left join (select alarm_definition_id,
           group_concat(action_id) as alarm_actions
              from alarm_action
              where alarm_state = 'ALARM'
              group by alarm_definition_id) as aaa
              on aaa.alarm_definition_id = ad.id
          left join (select alarm_definition_id,
            group_concat(action_id) as ok_actions
              from alarm_action
              where alarm_state = 'OK'
              group by alarm_definition_id) as aao
              on aao.alarm_definition_id = ad.id
          left join (select alarm_definition_id,
            group_concat(action_id) as undetermined_actions
              from alarm_action
              where alarm_state = 'UNDETERMINED'
              group by alarm_definition_id) as aau
              on aau.alarm_definition_id = ad.id
        """

    def __init__(self):

        super(AlarmDefinitionsRepository, self).__init__()


    def get_alarm_definition(self, tenant_id, id):

        try:

            parms = [tenant_id, id]

            where_clause = \
                " where ad.tenant_id = ? and ad.id = ? and deleted_at is NULL "

            query = \
                AlarmDefinitionsRepository.base_query \
                + where_clause

            cnxn, cursor = self._get_cnxn_cursor_tuple()

            cursor.execute(query, parms)

            row = cursor.fetchall()

            self._commit_close_cnxn(cnxn)

            if row:
                return row[0]
            else:
                raise DoesNotExistException

        except DoesNotExistException:
            raise
        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def get_alarm_definitions(self, tenant_id, name, dimensions):

        try:

            parms = [tenant_id]

            select_clause = \
                AlarmDefinitionsRepository.base_query

            where_clause = " where ad.tenant_id = ? and deleted_at is NULL "

            if name:
                where_clause += " and ad.name = ? "
                parms.append(name.encode('utf8'))

            order_by_clause = " order by ad.created_at "

            if dimensions:
                inner_join = """ inner join sub_alarm_definition as sad
                            on sad.alarm_definition_id = ad.id """

                i = 0
                inner_join_parms = []
                for n, v in dimensions.iteritems():
                    inner_join += """
                        inner join
                            (select distinct sub_alarm_definition_id
                             from sub_alarm_definition_dimension
                              where dimension_name = ? and value = ?) as sadd{}
                        on sadd{}.sub_alarm_definition_id = sad.id
                        """.format(i, i)
                    inner_join_parms += [n.encode('utf8'), v.encode('utf8')]
                    i += 1

                select_clause += inner_join
                parms = inner_join_parms + parms

            query = select_clause + where_clause + order_by_clause
            cnxn, cursor = self._get_cnxn_cursor_tuple()
            cursor.execute(query, parms)

            rows = cursor.fetchall()

            self._commit_close_cnxn(cnxn)

            return rows

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def get_sub_alarms(self, tenant_id, alarm_definition_id):

        try:

            cnxn, cursor = self._get_cnxn_cursor_tuple()
            cursor.execute(
                """select distinct sa.id as sub_alarm_id, sa.alarm_id,
                                   sa.expression
                    from sub_alarm as sa
                    inner join alarm as a
                      on a.id = sa.alarm_id
                    inner join alarm_definition as ad
                      on ad.id = a.alarm_definition_id
                    where ad.tenant_id = ? and ad.id = ?
                """, [tenant_id, alarm_definition_id])

            rows = cursor.fetchall()

            self._commit_close_cnxn(cnxn)

            return rows

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def get_alarm_metrics(self, tenant_id, alarm_definition_id):
        try:
            cnxn, cursor = self._get_cnxn_cursor_tuple()
            cursor.execute(
                """select distinct a.id as alarm_id, md.name, mdg.dimensions
                   from alarm as a
                   inner join alarm_definition as ad
                      on ad.id = a.alarm_definition_id
                   inner join alarm_metric as am on am.alarm_id = a.id
                   inner join metric_definition_dimensions as mdd
                      on mdd.id = am.metric_definition_dimensions_id
                   inner join metric_definition as md
                      on md.id = mdd.metric_definition_id
                   left join (select dimension_set_id,
                   group_concat(name, '=', value) as dimensions
                      from metric_dimension group by dimension_set_id) as mdg
                          on mdg.dimension_set_id = mdd.metric_dimension_set_id
                   where ad.tenant_id = ? and ad.id = ?
                   order by a.id
                   """, [tenant_id, alarm_definition_id])

            rows = cursor.fetchall()

            self._commit_close_cnxn(cnxn)

            return rows

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

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
        try:
            cnxn, cursor = self._get_cnxn_cursor_tuple()
            cursor.execute(
                """update alarm_definition
                   set deleted_at = NOW()
                   where tenant_id = ? and id = ? and deleted_at is NULL""",
                [tenant_id, alarm_definition_id]
            )

            if cursor.rowcount < 1:
                self._commit_close_cnxn(cnxn)
                return False

            cursor.execute(
                """delete from alarm where alarm_definition_id = ?""",
                [alarm_definition_id]
            )

            self._commit_close_cnxn(cnxn)

            return True

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def get_sub_alarm_definitions(self, alarm_definition_id):

        try:
            cnxn, cursor = self._get_cnxn_cursor_tuple()

            cursor.execute(
                """select sad.*, sadd.dimensions
                   from sub_alarm_definition as sad
                   left join (select sub_alarm_definition_id,
                                group_concat(dimension_name, '=', value)
                                as dimensions
                                from sub_alarm_definition_dimension
                                group by sub_alarm_definition_id)
                                as sadd
                      on sadd.sub_alarm_definition_id = sad.id
                      where sad.alarm_definition_id = ?""",
                [alarm_definition_id])

            rows = cursor.fetchall()

            self._commit_close_cnxn(cnxn)

            return rows

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def create_alarm_definition(self, tenant_id, name, expression,
                                sub_expr_list, description, severity, match_by,
                                alarm_actions, undetermined_actions,
                                ok_actions):

        try:
            cnxn, cursor = self._get_cnxn_cursor_tuple()
            now = datetime.datetime.utcnow()
            alarm_definition_id = uuidutils.generate_uuid()
            cursor.execute("""insert into alarm_definition(
                           id,
                           tenant_id,
                           name,
                           description,
                           expression,
                           severity,
                           match_by,
                           actions_enabled,
                           created_at,
                           updated_at)
                           values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                           alarm_definition_id, tenant_id, name.encode('utf8'),
                           description.encode('utf8'),
                           expression.encode('utf8'),
                           severity.upper().encode('utf8'),
                           ",".join(match_by).encode('utf8'), 1, now, now)

            for sub_expr in sub_expr_list:
                sub_alarm_definition_id = uuidutils.generate_uuid()
                sub_expr.id = sub_alarm_definition_id
                cursor.execute("""insert into sub_alarm_definition(
                               id,
                               alarm_definition_id,
                               function,
                               metric_name,
                               operator,
                               threshold,
                               period,
                               periods,
                               created_at,
                               updated_at)
                                values(?,?,?,?,?,?,?,?,?,?)""",
                               sub_alarm_definition_id, alarm_definition_id,
                               sub_expr.normalized_func.encode('utf8'),
                               sub_expr.normalized_metric_name.encode(
                                   "utf8"),
                               sub_expr.normalized_operator.encode(
                                   'utf8'),
                               sub_expr.threshold.encode('utf8'),
                               sub_expr.period.encode('utf8'),
                               sub_expr.periods.encode('utf8'), now, now)

                for dimension in sub_expr.dimensions_as_list:
                    parsed_dimension = dimension.split('=')
                    cursor.execute(
                        """insert into sub_alarm_definition_dimension(
                        sub_alarm_definition_id,
                        dimension_name,
                        value)
                        values(?,?,?)""", sub_alarm_definition_id,
                        parsed_dimension[0].encode('utf8'),
                        parsed_dimension[1].encode('utf8'))

            self._insert_into_alarm_action(cursor, alarm_definition_id,
                                           alarm_actions, u"ALARM")
            self._insert_into_alarm_action(cursor, alarm_definition_id,
                                           undetermined_actions,
                                           u"UNDETERMINED")
            self._insert_into_alarm_action(cursor, alarm_definition_id,
                                           ok_actions, u"OK")

            self._commit_close_cnxn(cnxn)

            return alarm_definition_id

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def _insert_into_alarm_action(self, cursor, alarm_definition_id, actions,
                                  alarm_state):
        for action in actions:
            cursor.execute("select id from notification_method where id = ?",
                           action.encode('utf8'))
            row = cursor.fetchone()
            if not row:
                raise exceptions.RepositoryException(
                    "Non-existent notification id {} submitted for {} "
                    "notification action".format(action.encode('utf8'),
                                                 alarm_state.encode('utf8')))
            cursor.execute("""insert into alarm_action(
                           alarm_definition_id,
                           alarm_state,
                           action_id)
                           values(?,?,?)""", alarm_definition_id,
                           alarm_state.encode('utf8'), action.encode('utf8'))


