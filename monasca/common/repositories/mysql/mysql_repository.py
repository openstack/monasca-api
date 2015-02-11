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

import MySQLdb as mdb
from oslo.config import cfg

from monasca.common.repositories import exceptions
from monasca.openstack.common import log


LOG = log.getLogger(__name__)


class MySQLRepository(object):

    def __init__(self):

        try:

            super(MySQLRepository, self).__init__()

            self.conf = cfg.CONF
            self.database_name = self.conf.mysql.database_name
            self.database_server = self.conf.mysql.hostname
            self.database_uid = self.conf.mysql.username
            self.database_pwd = self.conf.mysql.password

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def _get_cnxn_cursor_tuple(self):

        cnxn = mdb.connect(self.database_server, self.database_uid,
                           self.database_pwd, self.database_name,
                           use_unicode=True)

        cursor = cnxn.cursor(mdb.cursors.DictCursor)

        return cnxn, cursor

    def _execute_query(self, query, parms):

        cnxn, cursor = self._get_cnxn_cursor_tuple()

        with cnxn:

            cursor.execute(query, parms)
            return cursor.fetchall()


def mysql_try_catch_block(fun):

    def try_it(*args, **kwargs):

        try:

            return fun(*args, **kwargs)

        except exceptions.DoesNotExistException:
            raise
        except exceptions.InvalidUpdateException:
            raise
        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    return try_it
