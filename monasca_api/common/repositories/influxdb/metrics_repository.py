# -*- coding: utf-8 -*-
# Copyright 2014 Hewlett-Packard
# (C) Copyright 2015,2016 Hewlett Packard Enterprise Development LP
# Copyright 2015 Cray Inc. All Rights Reserved.
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
from datetime import datetime
from datetime import timedelta
import hashlib
import json

from influxdb import client
from influxdb.exceptions import InfluxDBClientError
from oslo_config import cfg
from oslo_log import log
from oslo_utils import timeutils

from monasca_api.common.repositories import exceptions
from monasca_api.common.repositories import metrics_repository

MEASUREMENT_NOT_FOUND_MSG = "measurement not found"

LOG = log.getLogger(__name__)


class MetricsRepository(metrics_repository.AbstractMetricsRepository):

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

    def _build_show_series_query(self, dimensions, name, tenant_id, region,
                                 start_timestamp=None, end_timestamp=None):

        where_clause = self._build_where_clause(dimensions, name, tenant_id,
                                                region, start_timestamp,
                                                end_timestamp)

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
            if '_' in offset:
                tmp = datetime.strptime(str(offset).split('_')[1], "%Y-%m-%dT%H:%M:%SZ")
                tmp = tmp + timedelta(seconds=int(period))
                # Leave out any ID as influx doesn't understand it
                offset = tmp.isoformat()
            else:
                tmp = datetime.strptime(offset, "%Y-%m-%dT%H:%M:%SZ")
                offset = tmp + timedelta(seconds=int(period))

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
            # replace ' with \' to make query parsable
            clean_name = name.replace("'", "\\'")
            where_clause += ' from  "{}" '.format(clean_name.encode('utf8'))

        # tenant id
        where_clause += " where _tenant_id = '{}' ".format(tenant_id.encode(
            "utf8"))

        # region
        where_clause += " and _region = '{}' ".format(region.encode('utf8'))

        # dimensions - optional
        if dimensions:
            for dimension_name, dimension_value in iter(
                    sorted(dimensions.iteritems())):
                # replace ' with \' to make query parsable
                clean_dimension_name = dimension_name.replace("\'", "\\'")
                if dimension_value == "":
                    where_clause += " and \"{}\" =~ /.*/ ".format(
                        clean_dimension_name)
                elif '|' in dimension_value:
                    # replace ' with \' to make query parsable
                    clean_dimension_value = dimension_value.replace("\'", "\\'")

                    where_clause += " and \"{}\" =~ /^{}$/ ".format(
                        clean_dimension_name.encode('utf8'),
                        clean_dimension_value.encode('utf8'))
                else:
                    # replace ' with \' to make query parsable
                    clean_dimension_value = dimension_value.replace("\'", "\\'")

                    where_clause += " and \"{}\" = '{}' ".format(
                        clean_dimension_name.encode('utf8'),
                        clean_dimension_value.encode('utf8'))

        if start_timestamp is not None:
            where_clause += " and time > " + str(int(start_timestamp *
                                                     1000000)) + "u"

            if end_timestamp is not None:
                where_clause += " and time < " + str(int(end_timestamp *
                                                         1000000)) + "u"

        return where_clause

    def _build_from_clause(self, dimensions, name, tenant_id, region,
                           start_timestamp=None, end_timestamp=None):

        from_clause = self._build_where_clause(dimensions, name, tenant_id,
                                               region, start_timestamp,
                                               end_timestamp)
        return from_clause

    def list_metrics(self, tenant_id, region, name, dimensions, offset,
                     limit, start_timestamp=None, end_timestamp=None):

        try:

            query = self._build_show_series_query(dimensions, name, tenant_id, region)

            query += " limit {}".format(limit + 1)

            if offset:
                query += ' offset {}'.format(int(offset) + 1)

            result = self.influxdb_client.query(query)

            json_metric_list = self._build_serie_metric_list(result,
                                                             tenant_id,
                                                             region,
                                                             start_timestamp,
                                                             end_timestamp,
                                                             offset)

            return json_metric_list

        except InfluxDBClientError as ex:
            if ex.message.startswith(MEASUREMENT_NOT_FOUND_MSG):
                return []
            else:
                LOG.exception(ex)
                raise exceptions.RepositoryException(ex)

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def _generate_dimension_values_id(self, metric_name, dimension_name):
        sha1 = hashlib.sha1()
        hashstr = "metricName=" + (metric_name or "") + "dimensionName=" + dimension_name
        sha1.update(hashstr)
        return sha1.hexdigest()

    def _build_serie_dimension_values(self, series_names, metric_name, dimension_name,
                                      tenant_id, region, offset):
        dim_vals = []
        sha1_id = self._generate_dimension_values_id(metric_name, dimension_name)
        json_dim_vals = {u'id': sha1_id,
                         u'dimension_name': dimension_name,
                         u'values': dim_vals}

        #
        # Only return metric name if one was provided
        #
        if metric_name:
            json_dim_vals[u'metric_name'] = metric_name

        if not series_names:
            return json_dim_vals

        if 'series' in series_names.raw:
            for series in series_names.raw['series']:
                for tag_values in series[u'values']:

                    dims = {
                        name: value
                        for name, value in zip(series[u'columns'], tag_values)
                        if value and not name.startswith(u'_')
                    }

                    if dimension_name in dims and dims[dimension_name] not in dim_vals:
                        dim_vals.append(dims[dimension_name])

        dim_vals = sorted(dim_vals)
        json_dim_vals[u'values'] = dim_vals
        return json_dim_vals

    def _build_serie_metric_list(self, series_names, tenant_id, region,
                                 start_timestamp, end_timestamp,
                                 offset):

        json_metric_list = []

        if not series_names:
            return json_metric_list

        if 'series' in series_names.raw:

            metric_id = 0
            if offset:
                metric_id = int(offset) + 1

            for series in series_names.raw['series']:

                for tag_values in series[u'values']:

                    dimensions = {
                        name: value
                        for name, value in zip(series[u'columns'], tag_values)
                        if value and not name.startswith(u'_')
                    }

                    if self._has_measurements(tenant_id,
                                              region,
                                              series[u'name'],
                                              dimensions,
                                              start_timestamp,
                                              end_timestamp):

                        metric = {u'id': str(metric_id),
                                  u'name': series[u'name'],
                                  u'dimensions': dimensions}
                        metric_id += 1

                        json_metric_list.append(metric)

        return json_metric_list

    def _build_serie_name_list(self, series_names):

        json_metric_list = []

        if not series_names:
            return json_metric_list

        if 'series' in series_names.raw:

            id = 0

            for series in series_names.raw['series']:
                id += 1

                name = {u'id': str(id),
                        u'name': series[u'name']}

                json_metric_list.append(name)

        return json_metric_list

    def _get_dimensions(self, tenant_id, region, name, dimensions):
        metrics_list = self.list_metrics(tenant_id, region, name,
                                         dimensions, None, 2)

        if len(metrics_list) > 1:
            raise exceptions.MultipleMetricsException(self.MULTIPLE_METRICS_MESSAGE)

        if not metrics_list:
            return {}

        return metrics_list[0]['dimensions']

    def measurement_list(self, tenant_id, region, name, dimensions,
                         start_timestamp, end_timestamp, offset,
                         limit, merge_metrics_flag):

        json_measurement_list = []

        try:
            query = self._build_select_measurement_query(dimensions, name,
                                                         tenant_id,
                                                         region,
                                                         start_timestamp,
                                                         end_timestamp,
                                                         offset, limit)

            if not merge_metrics_flag:
                dimensions = self._get_dimensions(tenant_id, region, name, dimensions)
                query += " slimit 1"

            result = self.influxdb_client.query(query)

            if not result:
                return json_measurement_list

            for serie in result.raw['series']:

                if 'values' in serie:

                    measurements_list = []
                    for point in serie['values']:
                        value_meta = json.loads(point[2]) if point[2] else {}
                        timestamp = point[0][:19] + '.' + point[0][20:-1].ljust(3, '0') + 'Z'

                        measurements_list.append([timestamp,
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

        except exceptions.RepositoryException as ex:

            if (isinstance(ex.message, InfluxDBClientError) and
                    ex.message.message.startswith(MEASUREMENT_NOT_FOUND_MSG)):

                return json_measurement_list

            else:

                LOG.exception(ex)

                raise ex

        except InfluxDBClientError as ex:

            if ex.message.startswith(MEASUREMENT_NOT_FOUND_MSG):

                return json_measurement_list

            else:

                LOG.exception(ex)

                raise exceptions.RepositoryException(ex)

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
            query = self._build_statistics_query(dimensions, name, tenant_id,
                                                 region,
                                                 start_timestamp,
                                                 end_timestamp, statistics,
                                                 period, offset, limit)

            if not merge_metrics_flag:
                dimensions = self._get_dimensions(tenant_id, region, name, dimensions)
                query += " slimit 1"

            result = self.influxdb_client.query(query)

            if not result:
                return json_statistics_list

            for serie in result.raw['series']:

                if 'values' in serie:
                    columns = [column.replace('time', 'timestamp').replace('mean', 'avg')
                               for column in serie['columns']]

                    stats_list = []
                    for stats in serie['values']:
                        # remove sub-second timestamp values (period can never be less than 1)
                        timestamp = stats[0]
                        if '.' in timestamp:
                            stats[0] = str(timestamp)[:19] + 'Z'
                        stats[1] = stats[1] or 0
                        stats_list.append(stats)

                    statistic = {u'name': serie['name'],
                                 u'id': stats_list[-1][0],
                                 u'dimensions': dimensions,
                                 u'columns': columns,
                                 u'statistics': stats_list}

                    json_statistics_list.append(statistic)

            return json_statistics_list

        except exceptions.RepositoryException as ex:

            if (isinstance(ex.message, InfluxDBClientError) and
                    ex.message.message.startswith(MEASUREMENT_NOT_FOUND_MSG)):

                return json_statistics_list

            else:

                LOG.exception(ex)

                raise ex

        except InfluxDBClientError as ex:

            if ex.message.startswith(MEASUREMENT_NOT_FOUND_MSG):

                return json_statistics_list

            else:

                LOG.exception(ex)

                raise exceptions.RepositoryException(ex)

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

    def _has_measurements(self, tenant_id, region, name, dimensions,
                          start_timestamp, end_timestamp):

        has_measurements = True

        #
        # No need for the additional query if we don't have a start timestamp.
        #
        if not start_timestamp:
            return True

        #
        # We set limit to 1 for the measurement_list call, as we are only
        # interested in knowing if there is at least one measurement, and
        # not ask too much of influxdb.
        #
        measurements = self.measurement_list(tenant_id,
                                             region,
                                             name,
                                             dimensions,
                                             start_timestamp,
                                             end_timestamp,
                                             0,
                                             1,
                                             False)

        if len(measurements) == 0:
            has_measurements = False

        return has_measurements

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
                time_clause += " and time >= " + str(int(start_timestamp *
                                                         1000000)) + "u "

            if end_timestamp:
                time_clause += " and time <= " + str(int(end_timestamp *
                                                         1000000)) + "u "

            offset_clause = self._build_offset_clause(offset, limit)

            query += where_clause + time_clause + offset_clause

            result = self.influxdb_client.query(query)

            if not result:
                return json_alarm_history_list

            if 'values' in result.raw['series'][0]:

                for point in result.raw['series'][0]['values']:
                    alarm_point = {u'timestamp': point[0],
                                   u'alarm_id': point[1],
                                   u'metrics': json.loads(point[2]),
                                   u'new_state': point[3],
                                   u'old_state': point[4],
                                   u'reason': point[5],
                                   u'reason_data': point[6],
                                   u'sub_alarms': json.loads(point[7]),
                                   u'id': str(self._get_millis_from_timestamp(
                                       timeutils.parse_isotime(point[0])))}

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

    def _get_millis_from_timestamp(self, dt):
        dt = dt.replace(tzinfo=None)
        return int((dt - datetime(1970, 1, 1)).total_seconds() * 1000)

    def list_dimension_values(self, tenant_id, region, metric_name,
                              dimension_name, offset, limit):

        try:
            query = self._build_show_series_query(None, metric_name, tenant_id, region)
            result = self.influxdb_client.query(query)

            json_dim_vals = self._build_serie_dimension_values(result,
                                                               metric_name,
                                                               dimension_name,
                                                               tenant_id,
                                                               region,
                                                               offset)

            return json_dim_vals

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)
