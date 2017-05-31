# Copyright 2017 FUJITSU LIMITED
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

from oslo_log import log
from sqlalchemy import text

from monasca_api.common.repositories.sqla import sql_repository
from monasca_api.healthcheck import base

LOG = log.getLogger(__name__)


class AlarmsDbHealthCheck(base.BaseHealthCheck,
                          sql_repository.SQLRepository):
    """Evaluates alarm db health

    Healthcheck verifies if:
    * database is up and running, it is possible to establish connection
    * sample sql query can be executed

    If following conditions are met health check return healthy status.
    Otherwise unhealthy status is returned with explanation.
    """

    def health_check(self):
        status = self.check_db_status()
        return base.CheckResult(healthy=status[0],
                                message=status[1])

    def check_db_status(self):
        try:
            with self._db_engine.connect() as con:
                query = text('SELECT 1')
                con.execute(query)
        except Exception as ex:
            LOG.exception(str(ex))
            return False, str(ex)
        return True, 'OK'
