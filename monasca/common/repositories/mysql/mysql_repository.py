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

from monasca.common.repositories import alarms_repository
from monasca.openstack.common import log
from monasca.openstack.common import uuidutils
from monasca.common.repositories import exceptions


LOG = log.getLogger(__name__)


class MySQLRepository(object):

    database_driver = 'MySQL ODBC 5.3 ANSI Driver'
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
                MySQLRepository.database_cnxn_template % (
                    MySQLRepository.database_driver,
                    database_server, database_name, database_uid,
                    database_pwd))

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def _get_cnxn_cursor_tuple(self):

        cnxn = pyodbc.connect(self._cnxn_string)
        cursor = cnxn.cursor()
        return cnxn, cursor

    def _commit_close_cnxn(self, cnxn):
        cnxn.commit()
        cnxn.close()

