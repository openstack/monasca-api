# Copyright 2014 Hewlett-Packard
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

from oslo_config import cfg
from oslo_db.sqlalchemy import enginefacade
from oslo_log import log

import sqlalchemy

from monasca_api.common.repositories import exceptions

LOG = log.getLogger(__name__)
CONF = cfg.CONF


def _get_db_conf(conf_group, connection=None):
    return dict(
        connection=connection or conf_group.connection,
        slave_connection=conf_group.slave_connection,
        sqlite_fk=False,
        __autocommit=True,
        expire_on_commit=False,
        mysql_sql_mode=conf_group.mysql_sql_mode,
        idle_timeout=conf_group.idle_timeout,
        connection_debug=conf_group.connection_debug,
        max_pool_size=conf_group.max_pool_size,
        max_overflow=conf_group.max_overflow,
        pool_timeout=conf_group.pool_timeout,
        sqlite_synchronous=conf_group.sqlite_synchronous,
        connection_trace=conf_group.connection_trace,
        max_retries=conf_group.max_retries,
        retry_interval=conf_group.retry_interval
    )


def create_context_manager(connection=None):
    """Create a database context manager object.

    :param connection: The database connection string
    """
    ctxt_mgr = enginefacade.transaction_context()
    ctxt_mgr.configure(**_get_db_conf(CONF.database, connection=connection))
    return ctxt_mgr


def get_engine(use_slave=False, connection=None):
    """Get a database engine object.

    :param use_slave: Whether to use the slave connection
    :param connection: The database connection string
    """
    ctxt_mgr = create_context_manager(connection=connection)
    return ctxt_mgr.get_legacy_facade().get_engine(use_slave=use_slave)


class SQLRepository(object):

    def __init__(self):

        try:
            super(SQLRepository, self).__init__()
            self.conf = CONF
            self._db_engine = get_engine()
            self.metadata = sqlalchemy.MetaData()

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)


def sql_try_catch_block(fun):
    def try_it(*args, **kwargs):

        try:

            return fun(*args, **kwargs)

        except exceptions.DoesNotExistException:
            raise
        except exceptions.InvalidUpdateException:
            raise
        except exceptions.AlreadyExistsException:
            raise
        except Exception as ex:
            LOG.exception(ex)
            raise
        # exceptions.RepositoryException(ex)

    return try_it
