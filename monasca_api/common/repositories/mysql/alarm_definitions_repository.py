# Copyright 2014,2016 Hewlett Packard Enterprise Development Company, L.P.
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

from monasca_common.repositories.mysql import mysql_repository
from oslo_log import log
from oslo_utils import uuidutils

from monasca_api.common.repositories import alarm_definitions_repository as adr
from monasca_api.common.repositories import exceptions
from monasca_api.common.repositories.model import sub_alarm_definition

LOG = log.getLogger(__name__)


class AlarmDefinitionsRepository(mysql_repository.MySQLRepository,
                                 adr.AlarmDefinitionsRepository):

    base_query = """
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

    @mysql_repository.mysql_try_catch_block
    def get_alarm_definition(self, tenant_id, id):

        parms = [tenant_id, id]

        where_clause = """ where ad.tenant_id = %s
                            and ad.id = %s
                            and deleted_at is NULL """

        query = AlarmDefinitionsRepository.base_query + where_clause

        rows = self._execute_query(query, parms)

        if rows:
            return rows[0]
        else:
            raise exceptions.DoesNotExistException

    @mysql_repository.mysql_try_catch_block
    def get_alarm_definitions(self, tenant_id, name, dimensions, severity,
                              sort_by, offset, limit):

        parms = [tenant_id]

        select_clause = AlarmDefinitionsRepository.base_query

        where_clause = " where ad.tenant_id = %s and deleted_at is NULL "

        if name:
            where_clause += " and ad.name = %s "
            parms.append(name.encode('utf8'))

        if severity:
            severities = severity.split('|')
            parms.extend([s.encode('utf8') for s in severities])
            where_clause += " and (" + " or ".join(["ad.severity = %s" for s in severities]) + ")"

        if sort_by is not None:
            order_by_clause = " order by ad." + ",ad.".join(sort_by)
            if 'id' not in sort_by:
                order_by_clause += ",ad.id "
            else:
                order_by_clause += " "
        else:
            order_by_clause = " order by ad.id "

        limit_offset_clause = " limit %s "
        parms.append(limit + 1)

        if offset:
            limit_offset_clause += ' offset {}'.format(offset)

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
                             where dimension_name = %s and value = %s) as
                               sadd{}
                        on sadd{}.sub_alarm_definition_id = sad.id
                        """.format(i, i)
                inner_join_parms += [n.encode('utf8'), v.encode('utf8')]
                i += 1

            select_clause += inner_join
            parms = inner_join_parms + parms

        query = select_clause + where_clause + order_by_clause + limit_offset_clause

        LOG.debug("Query: {}".format(query))

        return self._execute_query(query, parms)

    @mysql_repository.mysql_try_catch_block
    def get_sub_alarms(self, tenant_id, alarm_definition_id):

        parms = [tenant_id, alarm_definition_id]

        query = """select distinct sa.id as sub_alarm_id, sa.alarm_id,
                                   sa.expression
                    from sub_alarm as sa
                    inner join alarm as a
                      on a.id = sa.alarm_id
                    inner join alarm_definition as ad
                      on ad.id = a.alarm_definition_id
                    where ad.tenant_id = %s and ad.id = %s
                """

        return self._execute_query(query, parms)

    @mysql_repository.mysql_try_catch_block
    def get_alarm_metrics(self, tenant_id, alarm_definition_id):

        parms = [tenant_id, alarm_definition_id]

        query = """select distinct a.id as alarm_id, md.name,
                      mdg.dimensions
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
                   where ad.tenant_id = %s and ad.id = %s
                   order by a.id
                   """

        return self._execute_query(query, parms)

    @mysql_repository.mysql_try_catch_block
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

        cnxn, cursor = self._get_cnxn_cursor_tuple()

        with cnxn:

            cursor.execute("""update alarm_definition
                       set deleted_at = NOW()
                       where tenant_id = %s and id = %s and deleted_at is
                       NULL""",
                           [tenant_id, alarm_definition_id])

            if cursor.rowcount < 1:
                return False

            cursor.execute(
                """delete from alarm where alarm_definition_id = %s""",
                [alarm_definition_id])

            return True

    @mysql_repository.mysql_try_catch_block
    def get_sub_alarm_definitions(self, alarm_definition_id):

        parms = [alarm_definition_id]

        query = """select sad.*, sadd.dimensions
                       from sub_alarm_definition as sad
                       left join (select sub_alarm_definition_id,
                                    group_concat(dimension_name, '=', value)
                                    as dimensions
                                    from sub_alarm_definition_dimension
                                    group by sub_alarm_definition_id)
                                    as sadd
                          on sadd.sub_alarm_definition_id = sad.id
                          where sad.alarm_definition_id = %s
                          """

        return self._execute_query(query, parms)

    @mysql_repository.mysql_try_catch_block
    def create_alarm_definition(self, tenant_id, name, expression,
                                sub_expr_list, description, severity, match_by,
                                alarm_actions, undetermined_actions,
                                ok_actions):
        cnxn, cursor = self._get_cnxn_cursor_tuple()

        with cnxn:

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
                                   values (%s, %s, %s, %s, %s, %s, %s, %s, %s,
                                   %s)""", (
                alarm_definition_id, tenant_id, name.encode('utf8'),
                description.encode('utf8'), expression.encode('utf8'),
                severity.upper().encode('utf8'),
                ",".join(match_by).encode('utf8'), 1, now, now))

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
                                       is_deterministic,
                                       created_at,
                                       updated_at)
                                        values(%s,%s,%s,%s,%s,%s,%s,%s,%s,
                                        %s, %s)""",
                               (
                                   sub_alarm_definition_id,
                                   alarm_definition_id,
                                   sub_expr.normalized_func.encode('utf8'),
                                   sub_expr.normalized_metric_name.encode(
                                       "utf8"),
                                   sub_expr.normalized_operator.encode('utf8'),
                                   sub_expr.threshold.encode('utf8'),
                                   sub_expr.period.encode('utf8'),
                                   sub_expr.periods.encode('utf8'),
                                   sub_expr.deterministic,
                                   now,
                                   now))

                for dimension in sub_expr.dimensions_as_list:
                    parsed_dimension = dimension.split('=')
                    cursor.execute("""insert into
                    sub_alarm_definition_dimension(
                                sub_alarm_definition_id,
                                dimension_name,
                                value)
                                values(%s,%s,%s)""", (
                        sub_alarm_definition_id,
                        parsed_dimension[0].encode('utf8'),
                        parsed_dimension[1].encode('utf8')))

            self._insert_into_alarm_action(cursor, alarm_definition_id,
                                           alarm_actions, u"ALARM")
            self._insert_into_alarm_action(cursor, alarm_definition_id,
                                           undetermined_actions,
                                           u"UNDETERMINED")
            self._insert_into_alarm_action(cursor, alarm_definition_id,
                                           ok_actions, u"OK")

            return alarm_definition_id

    @mysql_repository.mysql_try_catch_block
    def update_or_patch_alarm_definition(self, tenant_id, alarm_definition_id,
                                         name, expression,
                                         sub_expr_list, actions_enabled,
                                         description, alarm_actions,
                                         ok_actions, undetermined_actions,
                                         match_by, severity, patch=False):

        cnxn, cursor = self._get_cnxn_cursor_tuple()

        with cnxn:

            # Get the original alarm definition from the DB
            parms = [tenant_id, alarm_definition_id]

            where_clause = """ where ad.tenant_id = %s
                            and ad.id = %s
                            and deleted_at is NULL """

            query = AlarmDefinitionsRepository.base_query + where_clause

            cursor.execute(query, parms)

            if cursor.rowcount < 1:
                raise exceptions.DoesNotExistException

            original_row = cursor.fetchall()[0]

            query = """
                select sad.*, sadd.dimensions
                from sub_alarm_definition as sad
                left join (select sub_alarm_definition_id,
                              group_concat(dimension_name, '=',
                              value) as dimensions
                           from sub_alarm_definition_dimension
                           group by sub_alarm_definition_id) as sadd
                    on sadd.sub_alarm_definition_id = sad.id
                where sad.alarm_definition_id = %s"""

            cursor.execute(query, [alarm_definition_id])

            rows = cursor.fetchall()

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
                    raise exceptions.InvalidUpdateException(
                        msg.encode('utf8'))
            else:
                unchanged_sub_alarm_defs_by_id = old_sub_alarm_defs_by_id
                changed_sub_alarm_defs_by_id = {}
                new_sub_alarm_defs_by_id = {}
                old_sub_alarm_defs_by_id = {}

            # Get a common update time
            now = datetime.datetime.utcnow()

            # Update the alarm definition
            query = """
                update alarm_definition
                set name = %s,
                    description = %s,
                    expression = %s,
                    match_by = %s,
                    severity = %s,
                    actions_enabled = %s,
                    updated_at = %s
                    where tenant_id = %s and id = %s"""

            if name is None:
                new_name = original_row['name']
            else:
                new_name = name.encode('utf8')

            if description is None:
                if patch:
                    new_description = original_row['description']
                else:
                    new_description = ''
            else:
                new_description = description.encode('utf8')

            if expression is None:
                new_expression = original_row['expression']
            else:
                new_expression = expression.encode('utf8')

            if severity is None:
                if patch:
                    new_severity = original_row['severity']
                else:
                    new_severity = 'LOW'
            else:
                new_severity = severity.encode('utf8')

            if match_by is None:
                if patch:
                    new_match_by = original_row['match_by']
                else:
                    new_match_by = None
            else:
                new_match_by = ",".join(match_by).encode('utf8')

            if new_match_by != original_row['match_by']:
                msg = "match_by must not change".encode('utf8')
                raise exceptions.InvalidUpdateException(msg)

            if actions_enabled is None:
                new_actions_enabled = original_row['actions_enabled']
            else:
                new_actions_enabled = actions_enabled

            parms = [new_name,
                     new_description,
                     new_expression,
                     new_match_by,
                     new_severity,
                     1 if new_actions_enabled else 0,
                     now,
                     tenant_id,
                     alarm_definition_id]

            cursor.execute(query, parms)

            # Delete the old sub alarm definitions
            query = """
                delete from sub_alarm_definition where id = %s"""

            for sub_alarm_def_id in old_sub_alarm_defs_by_id.values():
                parms = [sub_alarm_def_id]
                cursor.execute(query, parms)

            # Update changed sub alarm definitions
            query = """
                update sub_alarm_definition
                set operator = %s,
                threshold = %s,
                is_deterministic = %s,
                updated_at = %s,
                where id = %s"""

            for sub_alarm_definition_id, sub_alarm_def in (
                    changed_sub_alarm_defs_by_id.iteritems()):
                parms = [sub_alarm_def.operator,
                         sub_alarm_def.threshold,
                         sub_alarm_def.deterministic,
                         now,
                         sub_alarm_definition_id]
                cursor.execute(query, parms)

            # Insert new sub alarm definitions
            query = """
                insert into sub_alarm_definition(
                   id,
                   alarm_definition_id,
                   function,
                   metric_name,
                   operator,
                   threshold,
                   period,
                   periods,
                   is_deterministic,
                   created_at,
                   updated_at)
                values(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)"""

            sub_query = """
                insert into sub_alarm_definition_dimension(
                  sub_alarm_definition_id,
                  dimension_name,
                  value)
                values(%s, %s,%s)"""

            for sub_alarm_def in new_sub_alarm_defs_by_id.values():
                parms = [sub_alarm_def.id,
                         sub_alarm_def.alarm_definition_id,
                         sub_alarm_def.function.encode('utf8'),
                         sub_alarm_def.metric_name.encode('utf8'),
                         sub_alarm_def.operator.encode('utf8'),
                         str(sub_alarm_def.threshold).encode('utf8'),
                         str(sub_alarm_def.period).encode('utf8'),
                         str(sub_alarm_def.periods).encode('utf8'),
                         sub_alarm_def.deterministic,
                         now,
                         now]

                cursor.execute(query, parms)

                for name, value in sub_alarm_def.dimensions.items():
                    parms = [sub_alarm_def.id, name.encode('utf8'),
                             value.encode('utf8')]

                    cursor.execute(sub_query, parms)

            # Delete old alarm actions
            if patch:
                if alarm_actions is not None:
                    self._delete_alarm_actions(cursor, alarm_definition_id,
                                               'ALARM')
                if ok_actions is not None:
                    self._delete_alarm_actions(cursor, alarm_definition_id,
                                               'OK')
                if undetermined_actions is not None:
                    self._delete_alarm_actions(cursor, alarm_definition_id,
                                               'UNDETERMINED')
            else:
                query = """
                    delete from alarm_action
                    where alarm_definition_id = %s"""

                parms = [alarm_definition_id]

                cursor.execute(query, parms)

            # Insert new alarm actions
            self._insert_into_alarm_action(cursor, alarm_definition_id,
                                           alarm_actions,
                                           u"ALARM")

            self._insert_into_alarm_action(cursor, alarm_definition_id,
                                           undetermined_actions,
                                           u"UNDETERMINED")

            self._insert_into_alarm_action(cursor, alarm_definition_id,
                                           ok_actions,
                                           u"OK")

            # Get the updated alarm definition from the DB
            parms = [tenant_id, alarm_definition_id]

            where_clause = """ where ad.tenant_id = %s
                            and ad.id = %s
                            and deleted_at is NULL """

            query = AlarmDefinitionsRepository.base_query + where_clause

            cursor.execute(query, parms)

            if cursor.rowcount < 1:
                raise Exception("Failed to find current alarm definition")

            updated_row = cursor.fetchall()[0]

            sub_alarm_defs_dict = {'old': old_sub_alarm_defs_by_id,
                                   'changed':
                                       changed_sub_alarm_defs_by_id,
                                   'new':
                                       new_sub_alarm_defs_by_id,
                                   'unchanged':
                                       unchanged_sub_alarm_defs_by_id}

            # Return the alarm def and the sub alarm defs
            return updated_row, sub_alarm_defs_dict

    def _determine_sub_expr_changes(self, alarm_definition_id,
                                    old_sub_alarm_defs_by_id,
                                    sub_expr_list):

        old_sub_alarm_defs_set = set(
            old_sub_alarm_defs_by_id.values())

        new_sub_alarm_defs_set = set()
        for sub_expr in sub_expr_list:
            sad = sub_alarm_definition.SubAlarmDefinition(
                sub_expr=sub_expr)
            # Inject the alarm definition id.
            sad.alarm_definition_id = alarm_definition_id.decode('utf8')
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

    def _delete_alarm_actions(self, cursor, id, alarm_action_name):

        query = """
            delete
            from alarm_action
            where alarm_definition_id = %s and alarm_state = %s
            """
        parms = [id, alarm_action_name]

        cursor.execute(query, parms)

    def _insert_into_alarm_action(self, cursor, alarm_definition_id, actions,
                                  alarm_state):

        if actions is None:
            return

        for action in actions:
            cursor.execute("select id from notification_method where id = %s",
                           (action.encode('utf8'),))
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
                               values(%s,%s,%s)""", (
                alarm_definition_id, alarm_state.encode('utf8'),
                action.encode('utf8')))
