# Copyright 2014 Hewlett-Packard
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

from oslo.config import cfg
from monasca.common.repositories import metrics_repository
from monasca.common.repositories import exceptions
from monasca.openstack.common import log
from influxdb import InfluxDBClient

LOG = log.getLogger(__name__)

class MetricsRepository(metrics_repository.MetricsRepository):

    def __init__(self):
        try:
            self.conf = cfg.CONF
            self.influxdb_client = InfluxDBClient(self.conf.influxdb.ip_address,
                                                  self.conf.influxdb.port,
                                                  self.conf.influxdb.user,
                                                  self.conf.influxdb.password,
                                                  self.conf.influxdb.database_name)
        except Exception as ex:
            LOG.exception()
            raise exceptions.RepositoryException(ex)

    def list_metrics(self, tenant_id, name, dimensions):
        try:
            # TODO: This code is just a placeholder right now. It actually returns metrics,
            # TODO: but the implementation is not complete
            #String serieNameRegex = buildSerieNameRegex(tenantId, name, dimensions);

            #String query = String.format("list series /%1$s/", serieNameRegex);
            #logger.debug("Query string: {}", query);
            query = 'list series'

            #List<Serie> result = this.influxDB.Query(this.config.influxDB.getName(), query,
            #                                         TimeUnit.SECONDS);
            result = self.influxdb_client.query(query, 's')
            #return buildMetricDefList(result);
            return result
        except Exception as ex:
            LOG.exception()
            raise exceptions.RepositoryException(ex)