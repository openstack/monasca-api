# -*- coding: utf8 -*-
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
from time import strftime
from time import gmtime

from influxdb import InfluxDBClient
from influxdb.client import InfluxDBClientError
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

    def _build_list_series_query(self, dimensions, name, tenant_id):

        from_clause = self._build_from_clause(dimensions, name, tenant_id)

        query = 'list series ' + from_clause

        return query

    def _build_select_query(self, dimensions, name, tenant_id, start_timestamp,
                            end_timestamp):

        from_clause = self._build_from_clause(dimensions, name, tenant_id,
                                              start_timestamp, end_timestamp)

        query = 'select * ' + from_clause

        return query

    def _build_statistics_query(self, dimensions, name, tenant_id,
                                start_timestamp, end_timestamp, statistics,
                                period):

        from_clause = self._build_from_clause(dimensions, name, tenant_id,
                                              start_timestamp, end_timestamp)

        statistics = [statistic.replace('avg', 'mean') for statistic in
                      statistics]
        statistics = [statistic + '(value)' for statistic in statistics]

        statistic_string = ",".join(statistics)

        query = 'select ' + statistic_string + ' ' + from_clause

        if period is None:
            period = str(300)

        query += " group by time(" + period + "s)"

        return query

    def _build_from_clause(self, dimensions, name, tenant_id,
                           start_timestamp=None, end_timestamp=None):

        from_clause = 'from /^'

        if name:
            from_clause += urllib.quote(name.encode('utf8'),
                                        safe='') + '\?' + urllib.quote(
                tenant_id.encode('utf8'), safe='')
        else:
            from_clause += '.+\?' + urllib.quote(tenant_id.encode('utf8'),
                                                 safe='')

        if dimensions:
            for dimension_name, dimension_value in iter(
                    sorted(dimensions.iteritems())):
                from_clause += '.*&'
                from_clause += urllib.quote(dimension_name.encode('utf8'),
                                            safe='')
                from_clause += '='
                from_clause += urllib.quote(dimension_value.encode('utf8'),
                                            safe='')

        from_clause += '/'

        if start_timestamp is not None:
            # subtract 1 from timestamp to get >= semantics
            from_clause += " where time > " + str(start_timestamp - 1) + "s"
            if end_timestamp is not None:
                # add 1 to timestamp to get <= semantics
                from_clause += " and time < " + str(end_timestamp + 1) + "s"

        return from_clause

    def list_metrics(self, tenant_id, name, dimensions):

        try:

            query = self._build_list_series_query(dimensions, name, tenant_id)

            result = self.influxdb_client.query(query, 's')

            json_metric_list = self._decode_influxdb_serie_name_list(result)

            return json_metric_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def _decode_influxdb_serie_name_list(self, series_names):

        """
        Example series_names from InfluxDB.
        [
          {
            "points": [
              [
                0,
                "%E5%8D%83?tenant&useast&dim1=%E5%8D%83&dim2=%E5%8D%83"
              ]
            ],
            "name": "list_series_result",
            "columns": [
              "time",
              "name"
            ]
          }
        ]


        :param series_names:
        :return:
        """

        json_metric_list = []
        serie_names_list_list = series_names[0]['points']
        for serie_name_list in serie_names_list_list:
            serie_name = serie_name_list[1]
            metric = self._decode_influxdb_serie_name(serie_name)
            if metric is None:
                continue
            json_metric_list.append(metric)

        return json_metric_list


    def _decode_influxdb_serie_name(self, serie_name):

        """
        Decodes a serie name from InfluxDB.  The raw serie name is
        formed by url encoding the name, tenant id, region, and dimensions,
        and concatenating them into a quasi URL query string.

        urlencode(name)?urlencode(tenant)&urlencode(region)[&urlencode(
        dim_name)=urlencode(dim_value)]...


        :param serie_name:
        :return:
        """

        match = self._serie_name_reqex.match(serie_name)
        if match:
            name = urllib.unquote_plus(match.group(1).encode('utf8')).decode(
                'utf8')
            metric = {"name": name}

            # throw tenant_id (group 2) and region (group 3) away

            # only returns the last match. we need all dimensions.
            dimensions = match.group(4)
            if dimensions:
                # remove the name, tenant_id, and region; just
                # dimensions remain
                dimensions_part = self._serie_name_tenant_id_region_regex.sub(
                    '', serie_name)
                dimensions = {}
                dimension_list = self._serie_name_dimension_regex.findall(
                    dimensions_part)
                for dimension in dimension_list:
                    match = self._serie_name_dimension_parts_regex.match(
                        dimension)
                    dimension_name = urllib.unquote(
                        match.group(1).encode('utf8')).decode('utf8')
                    dimension_value = urllib.unquote(
                        match.group(2).encode('utf8')).decode('utf8')
                    dimensions[dimension_name] = dimension_value

                metric["dimensions"] = dimensions
        else:
            metric = None

        return metric


    def measurement_list(self, tenant_id, name, dimensions, start_timestamp,
                         end_timestamp):
        """
        Example result from InfluxDB.
        [
          {
            "points": [
              [
                1413230362,
                5369370001,
                99.99
              ]
            ],
            "name": "%E5%8D%83?tenant&useast&dim1=%E5%8D%83&dim2=%E5%8D%83",
            "columns": [
              "time",
              "sequence_number",
              "value"
            ]
          }
        ]

        After url decoding the result would look like this. In this example
        the name, dim1 value, and dim2 value were non-ascii chars.

        [
          {
            "points": [
              [
                1413230362,
                5369370001,
                99.99
              ]
            ],
            "name": "千?tenant&useast&dim1=千&dim2=千",
            "columns": [
              "time",
              "sequence_number",
              "value"
            ]
          }
        ]

        :param tenant_id:
        :param name:
        :param dimensions:
        :return:
        """

        json_measurement_list = []

        try:
            query = self._build_select_query(dimensions, name, tenant_id,
                                             start_timestamp, end_timestamp)

            try:
                result = self.influxdb_client.query(query, 's')
            except InfluxDBClientError as ex:
                if ex.code == 400 and ex.content == 'Couldn\'t look up ' \
                                                    'columns':
                    return json_measurement_list
                else:
                    raise ex

            for serie in result:

                metric = self._decode_influxdb_serie_name(serie['name'])

                if metric is None:
                    continue

                # Replace 'sequence_number' -> 'id' for column name
                columns = [column.replace('sequence_number', 'id') for column
                           in serie['columns']]
                # Replace 'time' -> 'timestamp' for column name
                columns = [column.replace('time', 'timestamp') for column in
                           columns]

                # format the utc date in the points
                fmtd_pts = [[strftime("%Y-%m-%dT%H:%M:%SZ", gmtime(point[0])),
                             point[1], point[2]] for point in serie['points']]

                measurement = {"name": metric['name'],
                               "dimensions": metric['dimensions'],
                               "columns": columns, "measurements": fmtd_pts}

                json_measurement_list.append(measurement)

            return json_measurement_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)


    def metrics_statistics(self, tenant_id, name, dimensions, start_timestamp,
                           end_timestamp, statistics, period):

        json_statistics_list = []

        try:
            query = self._build_statistics_query(dimensions, name, tenant_id,
                                                 start_timestamp,
                                                 end_timestamp, statistics,
                                                 period)

            try:
                result = self.influxdb_client.query(query, 's')
            except InfluxDBClientError as ex:
                if ex.code == 400 and ex.content == 'Couldn\'t look up ' \
                                                    'columns':
                    return json_statistics_list
                else:
                    raise ex

            for serie in result:

                metric = self._decode_influxdb_serie_name(serie['name'])

                if metric is None:
                    continue

                # Replace 'avg' -> 'mean' for column name
                columns = [column.replace('mean', 'avg') for column in
                           serie['columns']]
                # Replace 'time' -> 'timestamp' for column name
                columns = [column.replace('time', 'timestamp') for column in
                           columns]

                fmtd_pts_list_list = [[strftime("%Y-%m-%dT%H:%M:%SZ",
                                           gmtime(pts_list[0]))] + pts_list[1:]
                                 for pts_list in serie['points']]

                measurement = {"name": metric['name'],
                               "dimensions": metric['dimensions'],
                               "columns": columns,
                               "measurements": fmtd_pts_list_list}

                json_statistics_list.append(measurement)

            return json_statistics_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)