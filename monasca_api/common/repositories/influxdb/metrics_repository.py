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

import json

from influxdb import client
from oslo_config import cfg
from oslo_log import log

from monasca_api.common.repositories import exceptions
from monasca_api.common.repositories import metrics_repository

LOG = log.getLogger(__name__)


class MetricsRepository(metrics_repository.MetricsRepository):
    MULTIPLE_METRICS_MESSAGE = ("Found multiple metrics matching metric name"
                                + " and dimensions. Please refine your search"
                                + " criteria using a unique"
                                + " metric name or additional dimensions."
                                + " Alternatively, you may specify"
                                + " 'merge_metrics=True' as a query"
                                + " parameter to combine all metrics"
                                + " matching search criteria into a single"
                                + " series.")

    def __init__(self):

        try:

            self.conf = cfg.CONF
            self.influxdb_client = client.InfluxDBClient(
                self.conf.influxdb.ip_address, self.conf.influxdb.port,
                self.conf.influxdb.user, self.conf.influxdb.password,
                self.conf.influxdb.database_name)

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def _build_show_series_query(self, dimensions, name, tenant_id, region):

        where_clause = self._build_where_clause(dimensions, name, tenant_id,
                                                region)

        query = 'show series ' + where_clause

        return query

    def _build_select_measurement_query(self, dimensions, name, tenant_id,
                                        region, start_timestamp, end_timestamp,
                                        offset, limit):

        from_clause = self._build_from_clause(dimensions, name, tenant_id,
                                              region, start_timestamp,
                                              end_timestamp)

        offset_clause = self._build_offset_clause(offset, limit)

        query = 'select value, value_meta ' + from_clause + offset_clause

        return query

    def _build_statistics_query(self, dimensions, name, tenant_id,
                                region, start_timestamp, end_timestamp,
                                statistics, period, offset, limit):

        from_clause = self._build_from_clause(dimensions, name, tenant_id,
                                              region, start_timestamp,
                                              end_timestamp)
        if offset:
            offset_clause = (" and time > '{}'".format(offset))
            from_clause += offset_clause

        statistics = [statistic.replace('avg', 'mean') for statistic in
                      statistics]
        statistics = [statistic + '(value)' for statistic in statistics]

        statistic_string = ",".join(statistics)

        query = 'select ' + statistic_string + ' ' + from_clause

        if period is None:
            period = str(300)

        query += " group by time(" + period + "s)"

        limit_clause = " limit {}".format(str(limit + 1))

        query += limit_clause

        return query

    def _build_where_clause(self, dimensions, name, tenant_id, region,
                            start_timestamp=None, end_timestamp=None):

        where_clause = ''

        # name - optional
        if name:
            where_clause += ' from  "{}" '.format(name.encode('utf8'))

        # tenant id
        where_clause += " where _tenant_id = '{}' ".format(tenant_id.encode(
            "utf8"))

        # region
        where_clause += " and _region = '{}' ".format(region.encode('utf8'))

        # dimensions - optional
        if dimensions:
            for dimension_name, dimension_value in iter(
                    sorted(dimensions.iteritems())):
                where_clause += " and {} = '{}'".format(
                    dimension_name.encode('utf8'), dimension_value.encode(
                        'utf8'))

        if start_timestamp:
            where_clause += " and time > " + str(int(start_timestamp)) + "s"

            if end_timestamp:
                where_clause += " and time < " + str(int(end_timestamp)) + "s"

        return where_clause

    def _build_from_clause(self, dimensions, name, tenant_id, region,
                           start_timestamp=None, end_timestamp=None):

        from_clause = self._build_where_clause(dimensions, name, tenant_id,
                                               region, start_timestamp,
                                               end_timestamp)
        return from_clause

    def list_metrics(self, tenant_id, region, name, dimensions, offset,
                     limit):

        try:

            query = self._build_show_series_query(dimensions, name, tenant_id,
                                                  region)

            query += " limit {}".format(limit + 1)

            if offset:
                query += ' offset {}'.format(int(offset) + 1)

            result = self.influxdb_client.query(query)

            json_metric_list = self._build_serie_metric_list(result)

            return json_metric_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def _build_serie_metric_list(self, series_names):

        json_metric_list = []

        if not series_names:
            return json_metric_list

        if 'series' in series_names.raw['results'][0]:

            id = 0

            for series in series_names.raw['results'][0]['series']:

                for tag_values in series[u'values']:

                    dimensions = {}
                    i = 0
                    for name in series[u'columns']:
                        if name not in [u'_id', u'_tenant_id', u'_region']:
                            if tag_values[i]:
                                dimensions[name] = tag_values[i]
                        i += 1

                    metric = {u'id': str(id),
                              u'name': series[u'name'],
                              u'dimensions': dimensions}
                    id += 1

                    json_metric_list.append(metric)

        return json_metric_list

    def _build_serie_name_list(self, series_names):

        json_metric_list = []

        if not series_names:
            return json_metric_list

        if 'series' in series_names.raw['results'][0]:

            id = 0

            for series in series_names.raw['results'][0]['series']:
                id += 1

                name = {u'id': str(id),
                        u'name': series[u'name']}

                json_metric_list.append(name)

        return json_metric_list

    def measurement_list(self, tenant_id, region, name, dimensions,
                         start_timestamp, end_timestamp, offset,
                         limit, merge_metrics_flag):

        json_measurement_list = []

        try:

            if not merge_metrics_flag:

                metrics_list = self.list_metrics(tenant_id, region, name,
                                                 dimensions, None, 2)

                if len(metrics_list) > 1:
                    raise (exceptions.RepositoryException(
                        MetricsRepository.MULTIPLE_METRICS_MESSAGE))

            query = self._build_select_measurement_query(dimensions, name,
                                                         tenant_id,
                                                         region,
                                                         start_timestamp,
                                                         end_timestamp,
                                                         offset, limit)

            if not merge_metrics_flag:
                query += " slimit 1"

            result = self.influxdb_client.query(query)

            if not result:
                return json_measurement_list

            if 'error' in result.raw['results'][0]:
                if (result.raw['results'][0]['error'].startswith(
                        "measurement not found")):
                    return json_measurement_list

            for serie in result.raw['results'][0]['series']:

                if 'values' in serie:

                    measurements_list = []
                    for point in serie['values']:
                        value_meta = json.loads(point[2]) if point[2] else None
                        measurements_list.append([point[0],
                                                  point[1],
                                                  value_meta])

                    measurement = {u'name': serie['name'],
                                   u'id': measurements_list[-1][0],
                                   u'dimensions': dimensions,
                                   u'columns': [u'timestamp', u'value',
                                                u'value_meta'],
                                   u'measurements': measurements_list}

                    json_measurement_list.append(measurement)

            return json_measurement_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def list_metric_names(self, tenant_id, region, dimensions, offset, limit):

        try:

            query = self._build_show_series_query(dimensions, None, tenant_id,
                                                  region)

            query += " limit {}".format(limit + 1)

            if offset:
                query += ' offset {}'.format(int(offset) + 1)

            result = self.influxdb_client.query(query)

            json_name_list = self._build_serie_name_list(result)

            return json_name_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def metrics_statistics(self, tenant_id, region, name, dimensions,
                           start_timestamp,
                           end_timestamp, statistics, period, offset, limit,
                           merge_metrics_flag):

        json_statistics_list = []

        try:

            if not merge_metrics_flag:
                metrics_list = self.list_metrics(tenant_id, region, name,
                                                 dimensions, None, 2)

                if len(metrics_list) > 1:
                    raise (exceptions.RepositoryException(
                        MetricsRepository.MULTIPLE_METRICS_MESSAGE))

            query = self._build_statistics_query(dimensions, name, tenant_id,
                                                 region,
                                                 start_timestamp,
                                                 end_timestamp, statistics,
                                                 period, offset, limit)

            if not merge_metrics_flag:
                query += " slimit 1"

            result = self.influxdb_client.query(query)

            if not result:
                return json_statistics_list

            if 'error' in result.raw['results'][0]:
                if (result.raw['results'][0]['error'].startswith(
                        "measurement not found")):
                    return json_statistics_list

            for serie in result.raw['results'][0]['series']:

                if 'values' in serie:
                    columns = ([column.replace('mean', 'avg') for column in
                                result.raw['results'][0]['series'][0][
                                    'columns']])

                    stats_list = []
                    for stats in serie['values']:
                        stats[1] = stats[1] or 0
                        stats_list.append(stats)

                    statistic = {u'name': serie['name'],
                                 u'id': stats_list[-1][0],
                                 u'dimensions': dimensions,
                                 u'columns': columns,
                                 u'statistics': stats_list}

                    json_statistics_list.append(statistic)

            return json_statistics_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def _build_offset_clause(self, offset, limit):

        if offset:

            offset_clause = (
                " and time > '{}' limit {}".format(offset, str(limit + 1)))
        else:

            offset_clause = " limit {}".format(str(limit + 1))

        return offset_clause

    def alarm_history(self, tenant_id, alarm_id_list,
                      offset, limit, start_timestamp=None,
                      end_timestamp=None):

        try:

            json_alarm_history_list = []

            if not alarm_id_list:
                return json_alarm_history_list

            for alarm_id in alarm_id_list:
                if '\'' in alarm_id or ';' in alarm_id:
                    raise Exception(
                        "Input from user contains single quote ['] or "
                        "semi-colon [;] characters[ {} ]".format(alarm_id))

            query = """
              select alarm_id, metrics, new_state, old_state,
                     reason, reason_data, sub_alarms, tenant_id
              from alarm_state_history
              """

            where_clause = (
                " where tenant_id = '{}' ".format(tenant_id.encode('utf8')))

            alarm_id_where_clause_list = (
                [" alarm_id = '{}' ".format(id.encode('utf8'))
                 for id in alarm_id_list])

            alarm_id_where_clause = " or ".join(alarm_id_where_clause_list)

            where_clause += ' and (' + alarm_id_where_clause + ')'

            time_clause = ''
            if start_timestamp:
                time_clause += (" and time > " + str(int(start_timestamp)) +
                                "s ")

            if end_timestamp:
                time_clause += " and time < " + str(int(end_timestamp)) + "s "

            offset_clause = self._build_offset_clause(offset, limit)

            query += where_clause + time_clause + offset_clause

            result = self.influxdb_client.query(query)

            if 'error' in result.raw['results'][0]:
                if (result.raw['results'][0]['error'].startswith(
                        "measurement not found")):
                    return json_alarm_history_list

            if not result:
                return json_alarm_history_list

            if 'values' in result.raw['results'][0]['series'][0]:

                for point in result.raw['results'][0]['series'][0]['values']:
                    alarm_point = {u'timestamp': point[0],
                                   u'alarm_id': point[1],
                                   u'metrics': json.loads(point[2]),
                                   u'new_state': point[3],
                                   u'old_state': point[4],
                                   u'reason': point[5],
                                   u'reason_data': point[6],
                                   u'sub_alarms': json.loads(point[7]),
                                   u'tenant_id': point[8]}

                    # java api formats these during json serialization
                    if alarm_point[u'sub_alarms']:
                        for sub_alarm in alarm_point[u'sub_alarms']:
                            sub_expr = sub_alarm['sub_alarm_expression']
                            metric_def = sub_expr['metric_definition']
                            sub_expr['metric_name'] = metric_def['name']
                            sub_expr['dimensions'] = metric_def['dimensions']
                            del sub_expr['metric_definition']

                    json_alarm_history_list.append(alarm_point)

            return json_alarm_history_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)
