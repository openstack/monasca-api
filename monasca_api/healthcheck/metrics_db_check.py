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

import requests

from oslo_config import cfg
from oslo_log import log
from oslo_utils import importutils

from monasca_api.common.repositories import exceptions
from monasca_api.healthcheck import base

LOG = log.getLogger(__name__)
CONF = cfg.CONF


class MetricsDbCheck(base.BaseHealthCheck):
    """Evaluates metrics db health

    Healthcheck what type of database is used (InfluxDB, Cassandra)
    and provide health according to the given db.

    Healthcheck for InfluxDB verifies if:
    * check the db status by the /ping endpoint.

    Healthcheck for the Cassandra verifies if:
    * Cassandra is up and running (it is possible to create new connection)
    * keyspace exists

    If following conditions are met health check return healthy status.
    Otherwise unhealthy status is returned with explanation.
    """

    def __init__(self):
        # Try to import cassandra. Not a problem if it can't be imported as long
        # as the metrics db is influx
        self._cluster = importutils.try_import('cassandra.cluster', None)
        metric_driver = CONF.repositories.metrics_driver
        self._db = self._detected_database_type(metric_driver)
        if self._db == 'cassandra' and self._cluster is None:
            # Should not happen, but log if it does somehow
            LOG.error("Metrics Database is Cassandra but cassandra.cluster"
                      "not importable. Unable to do health check")

    def health_check(self):
        if self._db == 'influxdb':
            status = self._check_influxdb_status()
        else:
            status = self._check_cassandra_status()

        return base.CheckResult(healthy=status[0],
                                message=status[1])

    def _detected_database_type(self, driver):
        if 'influxdb' in driver:
            return 'influxdb'
        elif 'cassandra' in driver:
            return 'cassandra'
        else:
            raise exceptions.UnsupportedDriverException(
                'Driver {0} is not supported by Healthcheck'.format(driver))

    def _check_influxdb_status(self):
        uri = 'http://{0}:{1}/ping'.format(CONF.influxdb.ip_address,
                                           CONF.influxdb.port)
        try:
            resp = requests.head(url=uri)
        except Exception as ex:
            LOG.exception(str(ex))
            return False, str(ex)
        return resp.ok, 'OK' if resp.ok else 'Error: {0}'.format(
            resp.status_code)

    def _check_cassandra_status(self):
        if self._cluster is None:
            return False, "Cassandra driver not imported"
        try:
            cassandra = self._cluster.Cluster(
                CONF.cassandra.cluster_ip_addresses.split(',')
            )
            session = cassandra.connect(CONF.cassandra.keyspace)
            session.shutdown()
        except Exception as ex:
            LOG.exception(str(ex))
            return False, str(ex)
        return True, 'OK'
