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
from monasca.openstack.common import log
from monasca.openstack.common import uuidutils
from monasca.common.repositories import exceptions


LOG = log.getLogger(__name__)


class AlarmDefinitionsRepository(
    alarm_definitions_repository.AlarmDefinitionsRepository):
    database_driver = 'MySQL ODBC 5.3 Unicode Driver'
    database_cnxn_template = 'DRIVER={' \
                             '%s};Server=%s;CHARSET=UTF8;Database=%s;Uid=%s' \
                             ';Pwd=%s'

    def __init__(self):

        try:
            self.conf = cfg.CONF
            database_name = self.conf.mysql.database_name
            database_server = self.conf.mysql.hostname
            database_uid = self.conf.mysql.username
            database_pwd = self.conf.mysql.password
            self._cnxn_string = (
                AlarmDefinitionsRepository.database_cnxn_template % (
                    AlarmDefinitionsRepository.database_driver,
                    database_server, database_name, database_uid,
                    database_pwd))

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)


    def create_alarm_definition(self, tenant_id, name, expression,
                                sub_expr_list, description, severity, match_by,
                                alarm_actions, undetermined_actions,
                                ok_actions):

        try:
            cnxn = pyodbc.connect(self._cnxn_string)
            cursor = cnxn.cursor()
            now = datetime.datetime.utcnow()
            alarm_definition_id = uuidutils.generate_uuid()
            cursor.execute("insert into alarm_definition("
                           "id, "
                           "tenant_id, "
                           "name, "
                           "description, "
                           "expression, "
                           "severity, "
                           "match_by,"
                           "actions_enabled, "
                           "created_at, "
                           "updated_at) "
                           "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                           alarm_definition_id, tenant_id, name.encode('utf8'),
                           description.encode('utf8'),
                           expression.encode('utf8'),
                           severity.upper().encode('utf8'),
                           ",".join(match_by).encode('utf8'), 1, now, now)

            for sub_expr in sub_expr_list:
                sub_alarm_definition_id = uuidutils.generate_uuid()
                sub_expr.id = sub_alarm_definition_id
                cursor.execute("insert into sub_alarm_definition("
                               "id, "
                               "alarm_definition_id,"
                               "function, "
                               "metric_name, "
                               "operator, "
                               "threshold,"
                               "period, "
                               "periods, "
                               "created_at, "
                               "updated_at)"
                               " values(?,?,?,?,?,?,?,?,?,?)",
                               sub_alarm_definition_id, alarm_definition_id,
                               sub_expr.get_normalized_func().encode('utf8'),
                               sub_expr.get_normalized_metric_name().encode(
                                   "utf8"),
                               sub_expr.get_normalized_operator().encode(
                                   'utf8'),
                               sub_expr.get_threshold().encode('utf8'),
                               sub_expr.get_period().encode('utf8'),
                               sub_expr.get_periods().encode('utf8'), now, now)

                for dimension in sub_expr.get_dimensions_as_list():
                    parsed_dimension = dimension.split('=')
                    cursor.execute(
                        "insert into sub_alarm_definition_dimension("
                        "sub_alarm_definition_id,"
                        "dimension_name,"
                        "value)"
                        "values(?,?,?)", sub_alarm_definition_id,
                        parsed_dimension[0].encode('utf8'),
                        parsed_dimension[1].encode('utf8'))

            self._insert_into_alarm_action(cursor, alarm_definition_id,
                                           alarm_actions, u"ALARM")
            self._insert_into_alarm_action(cursor, alarm_definition_id,
                                           undetermined_actions,
                                           u"UNDETERMINED")
            self._insert_into_alarm_action(cursor, alarm_definition_id,
                                           ok_actions, u"OK")

            cnxn.commit()
            cnxn.close()

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
            cursor.execute("insert into alarm_action("
                           "alarm_definition_id,"
                           "alarm_state,"
                           "action_id)"
                           "values(?,?,?)", alarm_definition_id,
                           alarm_state.encode('utf8'), action.encode('utf8'))
