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
import re
import urllib

from influxdb import InfluxDBClient
from oslo.config import cfg

from monasca.common.repositories import exceptions
from monasca.common.repositories import metrics_repository
from monasca.openstack.common import log


LOG = log.getLogger(__name__)


class MetricsRepository(metrics_repository.MetricsRepository):
    def __init__(self):
        try:
            self.conf = cfg.CONF
            self.influxdb_client = InfluxDBClient(
                self.conf.influxdb.ip_address, self.conf.influxdb.port,
                self.conf.influxdb.user, self.conf.influxdb.password,
                self.conf.influxdb.database_name)

            # compile regex only once for efficiency
            self._serie_name_reqex = re.compile(
                '([^?&=]+)\?([^?&=]+)&([^?&=]+)(&[^?&=]+=[^?&=]+)*')
            self._serie_name_tenant_id_region_regex = re.compile(
                '[^?&=]+\?[^?&=]+&[^?&=]+')
            self._serie_name_dimension_regex = re.compile('&[^?&=]+=[^?&=]+')
            self._serie_name_dimension_parts_regex = re.compile(
                '&([^?&=]+)=([^?&=]+)')

        except Exception as ex:
            LOG.exception()
            raise exceptions.RepositoryException(ex)

    def _build_query(self, dimensions, name, tenant_id):

        query = 'list series from /^'

        if name:
            query += urllib.quote(name.encode('utf8'),
                                  safe='') + '\?' + urllib.quote(
                tenant_id.encode('utf8'), safe='')
        else:
            query += '.+\?' + urllib.quote(tenant_id.encode('utf8'), safe='')

        if dimensions:
            for dimension_name, dimension_value in iter(
                    sorted(dimensions.iteritems())):
                query += '.*&'
                query += urllib.quote(dimension_name.encode('utf8'), safe='')
                query += '='
                query += urllib.quote(dimension_value.encode('utf8'), safe='')

        query += '/'

        return query

    def list_metrics(self, tenant_id, name, dimensions):
        try:

            query = self._build_query(dimensions, name, tenant_id)

            result = self.influxdb_client.query(query, 's')

            json_metric_list = self._decode_influxdb_serie_names(result)

            return json_metric_list

        except Exception as ex:
            LOG.exception()
            raise exceptions.RepositoryException(ex)

    def _decode_influxdb_serie_names(self, series_names):

        json_metric_list = []
        serie_names_list_list = series_names[0]['points']
        for serie_name_list in serie_names_list_list:
            serie_name = serie_name_list[1]
            match = self._serie_name_reqex.match(serie_name)
            if match:
                name = urllib.unquote(match.group(1))
                # throw tenant_id (group 2) and region (group 3) away

                # only returns the last match. we need all dimensions.
                dimensions = match.group(4)
                if dimensions:
                    # remove the name, tenant_id, and region; just
                    # dimensions remain
                    dimensions_part = \
                        self._serie_name_tenant_id_region_regex.sub(
                        '', serie_name)
                    dimensions = {}
                    dimension_list = self._serie_name_dimension_regex.findall(
                        dimensions_part)
                    for dimension in dimension_list:
                        match = self._serie_name_dimension_parts_regex.match(
                            dimension)
                        dimension_name = urllib.unquote(match.group(1))
                        dimension_value = urllib.unquote(match.group(2))
                        dimensions[dimension_name.encode(
                            'utf8')] = dimension_value.encode('utf8')
                    metric = {"name": name.encode('utf8'),
                              "dimensions": dimensions}
                else:
                    metric = {"name": name.encode('utf8')}

                json_metric_list.append(metric)

        return json_metric_list

