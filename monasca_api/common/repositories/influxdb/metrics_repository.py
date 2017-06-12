# (C) Copyright 2014-2017 Hewlett Packard Enterprise Development LP
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
from distutils import version
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
            self._init_serie_builders()
        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def _init_serie_builders(self):
        '''Initializes functions for serie builders that are specific to different versions
        of InfluxDB.
        '''
        try:
            influxdb_version = self._get_influxdb_version()
            if influxdb_version < version.StrictVersion('0.11.0'):
                self._init_serie_builders_to_v0_11_0()
            else:
                self._init_serie_builders_from_v0_11_0()
        except Exception as ex:
            LOG.exception(ex)
            # Initialize the serie builders to v0_11_0. Not sure when SHOW DIAGNOSTICS added
            # support for a version string so to address backward compatibility initialize
            # InfluxDB serie builders <  v0.11.0
            self._init_serie_builders_to_v0_11_0()

    def _init_serie_builders_to_v0_11_0(self):
        '''Initialize function for InfluxDB serie builders <  v0.11.0
        '''
        LOG.info('Initialize InfluxDB serie builders <  v0.11.0')
        self._build_serie_dimension_values = self._build_serie_dimension_values_to_v0_11_0
        self._build_serie_metric_list = self._build_serie_metric_list_to_v0_11_0

    def _init_serie_builders_from_v0_11_0(self):
        '''Initialize function for InfluxDB serie builders >= v0.11.0.
        In InfluxDB v0.11.0 the SHOW SERIES output changed. See,
        https://github.com/influxdata/influxdb/blob/master/CHANGELOG.md#v0110-2016-03-22
        '''
        LOG.info('Initialize InfluxDB serie builders >=  v0.11.0')
        self._build_serie_dimension_values = self._build_serie_dimension_values_from_v0_11_0
        self._build_serie_metric_list = self._build_serie_metric_list_from_v0_11_0

    def _get_influxdb_version(self):
        '''If Version found in the result set, return the InfluxDB Version,
        otherwise raise an exception. InfluxDB has changed the format of their
        result set and SHOW DIAGNOSTICS was introduced at some point so earlier releases
        of InfluxDB might not return a Version.
        '''
        try:
            result = self.influxdb_client.query('SHOW DIAGNOSTICS')
        except InfluxDBClientError as ex:
            LOG.exception(ex)
            raise

        if 'series' not in result.raw:
            LOG.exception('series not in result.raw')
            raise Exception('Series not in SHOW DIAGNOSTICS result set')

        for series in result.raw['series']:
            if 'columns' not in series:
                continue
            columns = series['columns']
            if u'Version' not in series['columns']:
                continue
            if u'values' not in series:
                continue
            for value in series[u'values']:
                version_index = columns.index(u'Version')
                version_str = value[version_index]
                return version.StrictVersion(version_str)
        raise Exception('Version not found in SHOW DIAGNOSTICS result set')

    def _build_show_series_query(self, dimensions, name, tenant_id, region,
                                 start_timestamp=None, end_timestamp=None):

        where_clause = self._build_where_clause(dimensions, name, tenant_id,
                                                region, start_timestamp,
                                                end_timestamp)

        query = 'show series ' + where_clause

        return query

    def _build_show_measurements_query(self, dimensions, name, tenant_id, region):

        where_clause = self._build_where_clause(dimensions, name, tenant_id,
                                                region)

        query = 'show measurements ' + where_clause

        return query

    def _build_show_tag_values_query(self, metric_name, dimension_name,
                                     tenant_id, region):
        from_with_clause = ''
        if metric_name:
            from_with_clause += ' from "{}"'.format(metric_name)

        if dimension_name:
            from_with_clause += ' with key = {}'.format(dimension_name)

        where_clause = self._build_where_clause(None, None, tenant_id, region)

        query = 'show tag values' + from_with_clause + where_clause

        return query

    def _build_show_tag_keys_query(self, metric_name, tenant_id, region):
        from_with_clause = ''
        if metric_name:
            from_with_clause += ' from "{}"'.format(metric_name)

        where_clause = self._build_where_clause(None, None, tenant_id, region)

        query = 'show tag keys' + from_with_clause + where_clause

        return query

    def _build_select_measurement_query(self, dimensions, name, tenant_id,
                                        region, start_timestamp, end_timestamp,
                                        offset, group_by, limit):

        from_clause = self._build_from_clause(dimensions, name, tenant_id,
                                              region, start_timestamp,
                                              end_timestamp)

        offset_clause = self._build_offset_clause(offset)

        group_by_clause = self._build_group_by_clause(group_by)

        limit_clause = self._build_limit_clause(limit)

        query = 'select value, value_meta '\
                + from_clause + offset_clause\
                + group_by_clause + limit_clause

        return query

    def _build_statistics_query(self, dimensions, name, tenant_id,
                                region, start_timestamp, end_timestamp,
                                statistics, period, offset, group_by, limit):

        from_clause = self._build_from_clause(dimensions, name, tenant_id,
                                              region, start_timestamp,
                                              end_timestamp)
        if period is None:
            period = str(300)

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

        query += self._build_group_by_clause(group_by, period)

        limit_clause = self._build_limit_clause(limit)

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
                    where_clause += " and \"{}\" =~ /.+/ ".format(
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

    def _build_serie_dimension_values_to_v0_11_0(self, series_names, dimension_name):
        dim_value_set = set()
        json_dim_value_list = []

        if not series_names:
            return json_dim_value_list
        if 'series' not in series_names.raw:
            return json_dim_value_list
        if not dimension_name:
            return json_dim_value_list

        for series in series_names.raw['series']:
            if 'columns' not in series:
                continue
            if u'values' not in series:
                continue
            for value in series[u'values']:
                dim_value_set.add(value[0])

        for value in dim_value_set:
            json_dim_value_list.append({u'dimension_value': value})

        json_dim_value_list = sorted(json_dim_value_list)
        return json_dim_value_list

    def _build_serie_dimension_values_from_v0_11_0(self, series_names, dimension_name):
        '''In InfluxDB v0.11.0 the SHOW TAG VALUES output changed.
        See, https://github.com/influxdata/influxdb/blob/master/CHANGELOG.md#v0110-2016-03-22
        '''
        dim_value_set = set()
        json_dim_value_list = []

        if not series_names:
            return json_dim_value_list
        if 'series' not in series_names.raw:
            return json_dim_value_list
        if not dimension_name:
            return json_dim_value_list

        for series in series_names.raw['series']:
            if 'columns' not in series:
                continue
            columns = series['columns']
            if 'key' not in columns:
                continue
            if u'values' not in series:
                continue
            for value in series[u'values']:
                if len(value) < 2:
                    continue
                for tag in value[1:]:
                    dim_value_set.add(tag)

        for value in dim_value_set:
            json_dim_value_list.append({u'dimension_value': value})

        json_dim_value_list = sorted(json_dim_value_list)
        return json_dim_value_list

    def _build_serie_dimension_names(self, series_names):
        dim_name_set = set()
        json_dim_name_list = []

        if not series_names:
            return json_dim_name_list
        if 'series' not in series_names.raw:
            return json_dim_name_list

        for series in series_names.raw['series']:
            if 'columns' not in series:
                continue
            if u'values' not in series:
                continue
            for value in series[u'values']:
                tag_key = value[0]
                if tag_key.startswith(u'_'):
                    continue
                dim_name_set.add(tag_key)

        for name in dim_name_set:
            json_dim_name_list.append({u'dimension_name': name})

        json_dim_name_list = sorted(json_dim_name_list)
        return json_dim_name_list

    def _build_serie_metric_list_to_v0_11_0(self, series_names, tenant_id, region,
                                            start_timestamp, end_timestamp,
                                            offset):
        json_metric_list = []

        if not series_names:
            return json_metric_list

        if 'series' not in series_names.raw:
            return json_metric_list

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

    def _build_serie_metric_list_from_v0_11_0(self, series_names, tenant_id, region,
                                              start_timestamp, end_timestamp, offset):
        '''In InfluxDB v0.11.0 the SHOW SERIES output changed.
        See, https://github.com/influxdata/influxdb/blob/master/CHANGELOG.md#v0110-2016-03-22
        '''
        json_metric_list = []

        if not series_names:
            return json_metric_list

        if 'series' not in series_names.raw:
            return json_metric_list

        metric_id = 0
        if offset:
            metric_id = int(offset) + 1

        for series in series_names.raw['series']:
            if 'columns' not in series:
                continue
            columns = series['columns']
            if 'key' not in columns:
                continue
            key_index = columns.index('key')
            if u'values' not in series:
                continue
            for value in series[u'values']:
                split_value = value[key_index].split(',')
                if len(split_value) < 2:
                    continue
                serie_name = split_value[0]
                dimensions = {}
                for tag in split_value[1:]:
                    tag_key_value = tag.split('=')
                    if len(tag_key_value) < 2:
                        continue
                    tag_key = tag_key_value[0]
                    tag_value = tag_key_value[1]
                    if tag_key.startswith(u'_'):
                        continue
                    dimensions[tag_key] = tag_value
                if not self._has_measurements(tenant_id,
                                              region,
                                              serie_name,
                                              dimensions,
                                              start_timestamp,
                                              end_timestamp):
                    continue
                metric = {u'id': str(metric_id),
                          u'name': serie_name,
                          u'dimensions': dimensions}
                metric_id += 1
                json_metric_list.append(metric)

        return json_metric_list

    def _build_measurement_name_list(self, measurement_names):
        """read measurement names from InfluxDB response

        Extract the measurement names (InfluxDB terminology) from the SHOW MEASURMENTS result to yield metric names
        :param measurement_names: result from SHOW MEASUREMENTS call (json-dict)
        :return: list of metric-names (Monasca terminology)
        """

        json_metric_list = []

        if not measurement_names:
            return json_metric_list

        for name in measurement_names.raw.get(u'series', [{}])[0].get(u'values', []):
            entry = {u'name': name[0]}
            json_metric_list.append(entry)

        json_metric_list = sorted(json_metric_list)
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
                         limit, merge_metrics_flag, group_by):

        json_measurement_list = []

        try:
            query = self._build_select_measurement_query(dimensions, name,
                                                         tenant_id,
                                                         region,
                                                         start_timestamp,
                                                         end_timestamp,
                                                         offset, group_by,
                                                         limit)

            if not group_by and not merge_metrics_flag:
                dimensions = self._get_dimensions(tenant_id, region, name, dimensions)
                query += " slimit 1"

            result = self.influxdb_client.query(query)

            if not result:
                return json_measurement_list

            offset_id = 0
            if offset is not None:
                offset_tuple = offset.split('_')
                offset_id = int(offset_tuple[0]) if len(offset_tuple) > 1 else 0
            index = offset_id

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
                                   u'id': str(index),
                                   u'columns': [u'timestamp', u'value',
                                                u'value_meta'],
                                   u'measurements': measurements_list}

                    if not group_by:
                        measurement[u'dimensions'] = dimensions
                    else:
                        measurement[u'dimensions'] = {key: value for key, value in serie['tags'].iteritems()
                                                      if not key.startswith('_')}

                    json_measurement_list.append(measurement)
                    index += 1

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

    def list_metric_names(self, tenant_id, region, dimensions):

        try:

            query = self._build_show_measurements_query(dimensions, None, tenant_id,
                                                        region)

            result = self.influxdb_client.query(query)

            json_name_list = self._build_measurement_name_list(result)
            return json_name_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def metrics_statistics(self, tenant_id, region, name, dimensions,
                           start_timestamp, end_timestamp, statistics,
                           period, offset, limit, merge_metrics_flag,
                           group_by):

        json_statistics_list = []

        try:
            query = self._build_statistics_query(dimensions, name, tenant_id,
                                                 region, start_timestamp,
                                                 end_timestamp, statistics,
                                                 period, offset, group_by, limit)

            if not group_by and not merge_metrics_flag:
                dimensions = self._get_dimensions(tenant_id, region, name, dimensions)
                query += " slimit 1"

            result = self.influxdb_client.query(query)

            if not result:
                return json_statistics_list

            offset_id = 0
            if offset is not None:
                offset_tuple = offset.split('_')
                # If offset_id is given, add 1 since we want the next one
                if len(offset_tuple) > 1:
                    offset_id = int(offset_tuple[0]) + 1
            index = offset_id

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
                        for stat in stats[1:]:
                            # Only add row if there is a valid value in the row
                            if stat is not None:
                                stats_list.append(stats)
                                break

                    statistic = {u'name': serie['name'],
                                 u'id': str(index),
                                 u'columns': columns,
                                 u'statistics': stats_list}

                    if not group_by:
                        statistic[u'dimensions'] = dimensions
                    else:
                        statistic[u'dimensions'] = {key: value for key, value in serie['tags'].iteritems()
                                                    if not key.startswith('_')}

                    json_statistics_list.append(statistic)
                    index += 1

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

    def _build_offset_clause(self, offset):
        if offset:
            # offset may be given as a timestamp or as epoch time in ms
            if str(offset).isdigit():
                # epoch time
                offset_clause = " and time > {}ms".format(offset)
            else:
                # timestamp
                offset_clause = " and time > '{}'".format(offset)
        else:
            offset_clause = ""

        return offset_clause

    def _build_group_by_clause(self, group_by, period=None):
        if group_by is not None and not isinstance(group_by, list):
            group_by = str(group_by).split(',')
        if group_by or period:
            items = []
            if group_by:
                items.extend(group_by)
            if period:
                items.append("time(" + str(period) + "s)")
            clause = " group by " + ','.join(items)
        else:
            clause = ""

        return clause

    def _build_limit_clause(self, limit):
        return " limit {} ".format(str(limit + 1))

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
                                             None,
                                             1,
                                             False,
                                             None)

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

            offset_clause = self._build_offset_clause(offset)

            limit_clause = self._build_limit_clause(limit)

            query += where_clause + time_clause + offset_clause + limit_clause

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
                              dimension_name):
        try:
            query = self._build_show_tag_values_query(metric_name,
                                                      dimension_name,
                                                      tenant_id, region)
            result = self.influxdb_client.query(query)
            json_dim_name_list = self._build_serie_dimension_values(
                result, dimension_name)
            return json_dim_name_list
        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def list_dimension_names(self, tenant_id, region, metric_name):
        try:
            query = self._build_show_tag_keys_query(metric_name,
                                                    tenant_id, region)
            result = self.influxdb_client.query(query)
            json_dim_name_list = self._build_serie_dimension_names(result)
            return json_dim_name_list
        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)
