# -*- coding: utf-8 -*-
# (C) Copyright 2015,2016 Hewlett Packard Enterprise Development Company LP
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

import binascii
from datetime import datetime
from datetime import timedelta
import hashlib
import itertools
import json
import urllib

from cassandra.cluster import Cluster
from cassandra.query import SimpleStatement
from oslo_config import cfg
from oslo_log import log
from oslo_utils import timeutils

from monasca_api.common.repositories import exceptions
from monasca_api.common.repositories import metrics_repository

LOG = log.getLogger(__name__)


class MetricsRepository(metrics_repository.AbstractMetricsRepository):
    def __init__(self):

        try:

            self.conf = cfg.CONF

            self._cassandra_cluster = Cluster(
                self.conf.cassandra.cluster_ip_addresses.split(','))

            self.cassandra_session = self._cassandra_cluster.connect(
                self.conf.cassandra.keyspace)

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def list_metrics(self, tenant_id, region, name, dimensions, offset,
                     limit, start_timestamp=None, end_timestamp=None,
                     include_metric_hash=False):

        or_dimensions = []
        sub_dimensions = {}

        if dimensions:
            for key, value in dimensions.iteritems():
                if not value:
                    sub_dimensions[key] = value

                elif '|' in value:

                    def f(val):
                        return {key: val}

                    or_dimensions.append(list(map(f, value.split('|'))))

                else:
                    sub_dimensions[key] = value

            if or_dimensions:
                or_dims_list = list(itertools.product(*or_dimensions))
                metrics_list = []

                for or_dims_tuple in or_dims_list:
                    extracted_dimensions = sub_dimensions.copy()

                    for dims in iter(or_dims_tuple):
                        for k, v in dims.iteritems():
                            extracted_dimensions[k] = v

                    metrics = self._list_metrics(tenant_id, region, name,
                                                 extracted_dimensions, offset,
                                                 limit, start_timestamp,
                                                 end_timestamp,
                                                 include_metric_hash)
                    metrics_list += metrics

                return sorted(metrics_list, key=lambda metric: metric['id'])

        return self._list_metrics(tenant_id, region, name, dimensions,
                                  offset, limit, start_timestamp,
                                  end_timestamp, include_metric_hash)

    def _list_metrics(self, tenant_id, region, name, dimensions, offset,
                      limit, start_timestamp=None, end_timestamp=None,
                      include_metric_hash=False):

        try:

            select_stmt = """
              select tenant_id, region, metric_hash, metric_map
              from metric_map
              where tenant_id = %s and region = %s
              """

            parms = [tenant_id.encode('utf8'), region.encode('utf8')]

            name_clause = self._build_name_clause(name, parms)

            dimension_clause = self._build_dimensions_clause(dimensions, parms)

            select_stmt += name_clause + dimension_clause

            if offset:
                select_stmt += ' and metric_hash > %s '
                parms.append(bytearray(offset.decode('hex')))

            if limit:
                select_stmt += ' limit %s '
                parms.append(limit + 1)

            select_stmt += ' allow filtering '

            json_metric_list = []

            stmt = SimpleStatement(select_stmt,
                                   fetch_size=2147483647)

            rows = self.cassandra_session.execute(stmt, parms)

            if not rows:
                return json_metric_list

            for (tenant_id, region, metric_hash, metric_map) in rows:

                metric = {}

                dimensions = {}

                if include_metric_hash:
                    metric[u'metric_hash'] = metric_hash

                for name, value in metric_map.iteritems():

                    if name == '__name__':

                        name = urllib.unquote_plus(value)

                        metric[u'name'] = name

                    else:

                        name = urllib.unquote_plus(name)

                        value = urllib.unquote_plus(value)

                        dimensions[name] = value

                metric[u'dimensions'] = dimensions

                metric[u'id'] = binascii.hexlify(bytearray(metric_hash))

                json_metric_list.append(metric)

            return json_metric_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def _build_dimensions_clause(self, dimensions, parms):

        dimension_clause = ''
        if dimensions:

            for name, value in dimensions.iteritems():
                if not value:
                    dimension_clause += ' and metric_map contains key %s '

                    parms.append(urllib.quote_plus(name).encode('utf8'))
                else:
                    dimension_clause += ' and metric_map[%s] = %s '

                    parms.append(urllib.quote_plus(name).encode('utf8'))
                    parms.append(urllib.quote_plus(value).encode('utf8'))
        return dimension_clause

    def _build_name_clause(self, name, parms):

        name_clause = ''
        if name:
            name_clause = ' and metric_map[%s] = %s '

            parms.append(urllib.quote_plus('__name__').encode('utf8'))
            parms.append(urllib.quote_plus(name).encode('utf8'))

        return name_clause

    def measurement_list(self, tenant_id, region, name, dimensions,
                         start_timestamp, end_timestamp, offset,
                         limit, merge_metrics_flag):

        try:

            json_measurement_list = []

            rows = self._get_measurements(tenant_id, region, name, dimensions,
                                          start_timestamp, end_timestamp,
                                          offset, limit, merge_metrics_flag)

            if not rows:
                return json_measurement_list

            if not merge_metrics_flag:
                dimensions = self._get_dimensions(tenant_id, region, name, dimensions)

            measurements_list = (
                [[self._isotime_msec(time_stamp),
                  value,
                  json.loads(value_meta) if value_meta else {}]
                 for (time_stamp, value, value_meta) in rows])

            measurement = {u'name': name,
                           # The last date in the measurements list.
                           u'id': measurements_list[-1][0],
                           u'dimensions': dimensions,
                           u'columns': [u'timestamp', u'value', u'value_meta'],
                           u'measurements': measurements_list}

            json_measurement_list.append(measurement)

            return json_measurement_list

        except exceptions.RepositoryException as ex:
            LOG.exception(ex)
            raise ex

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def _get_measurements(self, tenant_id, region, name, dimensions,
                          start_timestamp, end_timestamp, offset, limit,
                          merge_metrics_flag):

        metric_list = self.list_metrics(tenant_id, region, name,
                                        dimensions, None, None,
                                        start_timestamp, end_timestamp,
                                        include_metric_hash=True)
        if not metric_list:
            return None

        if len(metric_list) > 1:

            if not merge_metrics_flag:
                raise exceptions.MultipleMetricsException(
                    self.MULTIPLE_METRICS_MESSAGE)

        select_stmt = """
          select time_stamp, value, value_meta
          from measurements
          where tenant_id = %s and region = %s
          """

        parms = [tenant_id.encode('utf8'), region.encode('utf8')]

        metric_hash_list = [bytearray(metric['metric_hash']) for metric in
                            metric_list]

        place_holders = ["%s"] * len(metric_hash_list)

        in_clause = ' and metric_hash in ({}) '.format(",".join(place_holders))

        select_stmt += in_clause

        parms.extend(metric_hash_list)

        if offset:

            select_stmt += ' and time_stamp > %s '
            parms.append(offset)

        elif start_timestamp:

            select_stmt += ' and time_stamp >= %s '
            parms.append(int(start_timestamp * 1000))

        if end_timestamp:
            select_stmt += ' and time_stamp <= %s '
            parms.append(int(end_timestamp * 1000))

        select_stmt += ' order by time_stamp '

        if limit:
            select_stmt += ' limit %s '
            parms.append(limit + 1)

        stmt = SimpleStatement(select_stmt,
                               fetch_size=2147483647)
        rows = self.cassandra_session.execute(stmt, parms)

        return rows

    def _get_dimensions(self, tenant_id, region, name, dimensions):
        metrics_list = self.list_metrics(tenant_id, region, name,
                                         dimensions, None, 2)

        if len(metrics_list) > 1:
            raise exceptions.MultipleMetricsException(self.MULTIPLE_METRICS_MESSAGE)

        if not metrics_list:
            return {}

        return metrics_list[0]['dimensions']

    def list_metric_names(self, tenant_id, region, dimensions, offset, limit):

        try:

            select_stmt = """
              select metric_hash, metric_map
              from metric_map
              where tenant_id = %s and region = %s
              """

            parms = [tenant_id.encode('utf8'), region.encode('utf8')]

            dimension_clause = self._build_dimensions_clause(dimensions, parms)

            select_stmt += dimension_clause

            if offset:
                select_stmt += ' and metric_hash > %s '
                parms.append(bytearray(offset.decode('hex')))

            if limit:
                select_stmt += ' limit %s '
                parms.append(limit + 1)

            select_stmt += ' allow filtering'

            json_name_list = []

            stmt = SimpleStatement(select_stmt,
                                   fetch_size=2147483647)

            rows = self.cassandra_session.execute(stmt, parms)

            if not rows:
                return json_name_list

            for (metric_hash, metric_map) in rows:

                metric = {}

                for name, value in metric_map.iteritems():

                    if name == '__name__':
                        name = urllib.unquote_plus(value)

                        metric[u'name'] = name

                        break

                metric[u'id'] = binascii.hexlify(bytearray(metric_hash))

                json_name_list.append(metric)

            return json_name_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def metrics_statistics(self, tenant_id, region, name, dimensions,
                           start_timestamp, end_timestamp, statistics,
                           period, offset, limit, merge_metrics_flag):

        try:

            if not period:
                period = 300
            period = int(period)

            json_statistics_list = []

            rows = self._get_measurements(tenant_id, region, name, dimensions,
                                          start_timestamp, end_timestamp,
                                          offset, limit, merge_metrics_flag)

            if not rows:
                return json_statistics_list

            requested_statistics = [stat.lower() for stat in statistics]

            columns = [u'timestamp']

            if 'avg' in requested_statistics:
                columns.append(u'avg')

            if 'min' in requested_statistics:
                columns.append(u'min')

            if 'max' in requested_statistics:
                columns.append(u'max')

            if 'count' in requested_statistics:
                columns.append(u'count')

            if 'sum' in requested_statistics:
                columns.append(u'sum')

            first_row = rows[0]
            stats_count = 0
            stats_sum = 0
            stats_max = first_row.value
            stats_min = first_row.value
            start_period = first_row.time_stamp

            stats_list = []

            tmp_start_period = datetime.utcfromtimestamp(start_timestamp)
            while start_period >= tmp_start_period + timedelta(seconds=period):
                stat = [
                    tmp_start_period.strftime('%Y-%m-%dT%H:%M:%SZ')
                    .decode('utf8')
                ]
                for _statistics in requested_statistics:
                    stat.append(0)
                tmp_start_period += timedelta(seconds=period)
                stats_list.append(stat)

            for (time_stamp, value, value_meta) in rows:

                if (time_stamp - start_period).seconds >= period:

                    stat = [
                        start_period.strftime('%Y-%m-%dT%H:%M:%SZ').decode(
                            'utf8')]

                    if 'avg' in requested_statistics:
                        stat.append(stats_sum / stats_count)

                    if 'min' in requested_statistics:
                        stat.append(stats_min)

                        stats_min = value

                    if 'max' in requested_statistics:
                        stat.append(stats_max)

                        stats_max = value

                    if 'count' in requested_statistics:
                        stat.append(stats_count)

                    if 'sum' in requested_statistics:
                        stat.append(stats_sum)

                    stats_list.append(stat)

                    tmp_start_period = start_period + timedelta(seconds=period)
                    while time_stamp > tmp_start_period:
                        stat = [
                            tmp_start_period.strftime('%Y-%m-%dT%H:%M:%SZ')
                            .decode('utf8')
                        ]
                        for _statistics in requested_statistics:
                            stat.append(0)
                        tmp_start_period += timedelta(seconds=period)
                        stats_list.append(stat)

                    start_period = time_stamp

                    stats_sum = 0
                    stats_count = 0

                stats_count += 1
                stats_sum += value

                if 'min' in requested_statistics:

                    if value < stats_min:
                        stats_min = value

                if 'max' in requested_statistics:

                    if value > stats_max:
                        stats_max = value

            if stats_count:

                stat = [start_period.strftime('%Y-%m-%dT%H:%M:%SZ').decode(
                    'utf8')]

                if 'avg' in requested_statistics:
                    stat.append(stats_sum / stats_count)

                if 'min' in requested_statistics:
                    stat.append(stats_min)

                if 'max' in requested_statistics:
                    stat.append(stats_max)

                if 'count' in requested_statistics:
                    stat.append(stats_count)

                if 'sum' in requested_statistics:
                    stat.append(stats_sum)

                stats_list.append(stat)

                if end_timestamp:
                    time_stamp = datetime.utcfromtimestamp(end_timestamp)
                else:
                    time_stamp = datetime.now()
                tmp_start_period = start_period + timedelta(seconds=period)
                while time_stamp > tmp_start_period:
                    stat = [
                        tmp_start_period.strftime('%Y-%m-%dT%H:%M:%SZ')
                        .decode('utf8')
                    ]
                    for _statistics in requested_statistics:
                        stat.append(0)
                    tmp_start_period += timedelta(seconds=period)
                    stats_list.append(stat)

            statistic = {u'name': name.decode('utf8'),
                         # The last date in the stats list.
                         u'id': stats_list[-1][0],
                         u'dimensions': dimensions,
                         u'columns': columns,
                         u'statistics': stats_list}

            json_statistics_list.append(statistic)

            return json_statistics_list

        except exceptions.RepositoryException as ex:
            LOG.exception(ex)
            raise ex

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def alarm_history(self, tenant_id, alarm_id_list,
                      offset, limit, start_timestamp=None,
                      end_timestamp=None):

        try:

            json_alarm_history_list = []

            if not alarm_id_list:
                return json_alarm_history_list

            select_stmt = """
              select alarm_id, time_stamp, metrics, new_state, old_state,
              reason, reason_data, sub_alarms, tenant_id
              from alarm_state_history
              where tenant_id = %s
              """

            parms = [tenant_id.encode('utf8')]

            place_holders = ["%s"] * len(alarm_id_list)

            in_clause = ' and alarm_id in ({}) '.format(
                ",".join(place_holders))

            select_stmt += in_clause

            parms.extend(alarm_id_list)

            if offset and offset != '0':

                select_stmt += ' and time_stamp > %s '
                dt = timeutils.normalize_time(timeutils.parse_isotime(offset))
                parms.append(self._get_millis_from_timestamp(dt))

            elif start_timestamp:

                select_stmt += ' and time_stamp >= %s '
                parms.append(int(start_timestamp * 1000))

            if end_timestamp:
                select_stmt += ' and time_stamp <= %s '
                parms.append(int(end_timestamp * 1000))

            if limit:
                select_stmt += ' limit %s '
                parms.append(limit + 1)

            stmt = SimpleStatement(select_stmt,
                                   fetch_size=2147483647)

            rows = self.cassandra_session.execute(stmt, parms)

            if not rows:
                return json_alarm_history_list

            sorted_rows = sorted(rows, key=lambda row: row.time_stamp)

            for (alarm_id, time_stamp, metrics, new_state, old_state, reason,
                 reason_data, sub_alarms, tenant_id) in sorted_rows:

                alarm = {u'timestamp': self._isotime_msec(time_stamp),
                         u'alarm_id': alarm_id,
                         u'metrics': json.loads(metrics),
                         u'new_state': new_state,
                         u'old_state': old_state,
                         u'reason': reason,
                         u'reason_data': reason_data,
                         u'sub_alarms': json.loads(sub_alarms),
                         u'id': str(self._get_millis_from_timestamp(time_stamp)
                                    ).decode('utf8')}

                if alarm[u'sub_alarms']:
                    for sub_alarm in alarm[u'sub_alarms']:
                        sub_expr = sub_alarm['sub_alarm_expression']
                        metric_def = sub_expr['metric_definition']
                        sub_expr['metric_name'] = metric_def['name']
                        sub_expr['dimensions'] = metric_def['dimensions']
                        del sub_expr['metric_definition']

                json_alarm_history_list.append(alarm)

            return json_alarm_history_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    @staticmethod
    def _isotime_msec(timestamp):
        """Stringify datetime in ISO 8601 format + millisecond.
        """
        st = timestamp.isoformat()
        if '.' in st:
            st = st[:23] + 'Z'
        else:
            st += '.000Z'
        return st.decode('utf8')

    @staticmethod
    def _get_millis_from_timestamp(dt):
        dt = timeutils.normalize_time(dt)
        return int((dt - datetime(1970, 1, 1)).total_seconds() * 1000)

    def _generate_dimension_values_id(self, metric_name, dimension_name):
        sha1 = hashlib.sha1()
        hashstr = "metricName=" + (metric_name or "") + "dimensionName=" + dimension_name
        sha1.update(hashstr)
        return sha1.hexdigest()

    def list_dimension_values(self, tenant_id, region, metric_name,
                              dimension_name, offset, limit):

        try:

            select_stmt = """
              select metric_map
              from metric_map
              where tenant_id = %s and region = %s
              """

            parms = [tenant_id.encode('utf8'), region.encode('utf8')]

            name_clause = self._build_name_clause(metric_name, parms)

            dimensions = {dimension_name: None}

            dimension_clause = self._build_dimensions_clause(dimensions, parms)

            select_stmt += name_clause + dimension_clause

            if offset:
                select_stmt += ' and metric_hash > %s '
                parms.append(bytearray(offset.decode('hex')))

            if limit:
                select_stmt += ' limit %s '
                parms.append(limit + 1)

            select_stmt += ' allow filtering '

            stmt = SimpleStatement(select_stmt,
                                   fetch_size=2147483647)

            rows = self.cassandra_session.execute(stmt, parms)

            sha1_id = self._generate_dimension_values_id(metric_name, dimension_name)
            json_dim_vals = {u'id': sha1_id,
                             u'dimension_name': dimension_name,
                             u'values': []}
            #
            # Only return metric name if one was provided
            #
            if metric_name:
                json_dim_vals[u'metric_name'] = metric_name

            if not rows:
                return json_dim_vals

            dim_vals = set()

            for row in rows:

                metric_map = row.metric_map
                for name, value in metric_map.iteritems():

                    name = urllib.unquote_plus(name)
                    value = urllib.unquote_plus(value)

                    if name == dimension_name:
                        dim_vals.add(value)

            json_dim_vals[u'values'] = sorted(list(dim_vals))

            return json_dim_vals

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)
