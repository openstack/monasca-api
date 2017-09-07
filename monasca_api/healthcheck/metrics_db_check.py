# Copyright 2017 FUJITSU LIMITED
# (C) Copyright 2017 Hewlett Packard Enterprise Development LP
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

from monasca_common.simport import simport
from oslo_config import cfg
from oslo_log import log

from monasca_api.healthcheck import base

CONF = cfg.CONF
LOG = log.getLogger(__name__)


class MetricsDbCheck(base.BaseHealthCheck):
    """Evaluates metrics db health

    Healthcheck what type of database is used (InfluxDB, Cassandra)
    and provide health according to the given db.

    If following conditions are met health check return healthy status.
    Otherwise unhealthy status is returned with explanation.
    """

    def __init__(self):
        try:
            self._metrics_repo = simport.load(
                CONF.repositories.metrics_driver)

        except Exception as ex:
            LOG.exception(ex)
            raise

    def health_check(self):
        status = self._metrics_repo.check_status()
        return base.CheckResult(healthy=status[0],
                                message=status[1])
