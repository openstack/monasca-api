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
from oslo_log import log

from sqlalchemy.engine.url import URL, make_url
from sqlalchemy import MetaData

from monasca_api.common.repositories import exceptions

LOG = log.getLogger(__name__)


class SQLRepository(object):

    def __init__(self):

        try:

            super(SQLRepository, self).__init__()

            self.conf = cfg.CONF
            url = None
            if self.conf.mysql.database_name is not None:
                settings_db = (self.conf.mysql.username,
                               self.conf.mysql.password,
                               self.conf.mysql.hostname,
                               self.conf.mysql.database_name)
                url = make_url("mysql+pymysql://{}:{}@{}/{}" % settings_db)
            else:
                if self.conf.database.url is not None:
                    url = make_url(self.conf.database.url)
                else:
                    database_conf = dict(self.conf.database)
                    if 'url' in database_conf:
                        del database_conf['url']
                    url = URL(**database_conf)

            from sqlalchemy import create_engine
            self._db_engine = create_engine(url)

            self.metadata = MetaData()

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
