# (C) Copyright 2015,2016 Hewlett Packard Enterprise Development Company LP
# (C) Copyright 2017-2018 SUSE LLC
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
from collections import namedtuple
from datetime import datetime
from datetime import timedelta
import itertools
import six
import urllib

from cassandra.auth import PlainTextAuthProvider
from cassandra.cluster import Cluster
from cassandra.cluster import DCAwareRoundRobinPolicy
from cassandra.cluster import TokenAwarePolicy
from cassandra.query import FETCH_SIZE_UNSET
from cassandra.query import SimpleStatement
from monasca_api.common.rest import utils as rest_utils
from oslo_config import cfg
from oslo_log import log
from oslo_utils import encodeutils
from oslo_utils import timeutils

from monasca_api.common.repositories import exceptions
from monasca_api.common.repositories import metrics_repository


CONF = cfg.CONF
LOG = log.getLogger(__name__)

LIMIT_CLAUSE = 'limit %s'
ALLOW_FILTERING = 'allow filtering'

MEASUREMENT_LIST_CQL = ('select time_stamp, value, value_meta '
                        'from measurements where %s %s %s %s')
METRIC_ID_EQ = 'metric_id = %s'
METRIC_ID_IN = 'metric_id in %s'
OFFSET_TIME_GT = "and time_stamp > %s"
START_TIME_GE = "and time_stamp >= %s"
END_TIME_LE = "and time_stamp <= %s"

METRIC_LIST_CQL = ('select metric_name, dimensions, metric_id '
                   'from metrics where %s %s %s %s %s %s %s %s %s %s')
REGION_EQ = 'region = %s'
TENANT_EQ = 'and tenant_id = %s'
METRIC_NAME_EQ = 'and metric_name = %s'
DIMENSIONS_CONTAINS = 'and dimensions contains %s '
DIMENSIONS_NAME_CONTAINS = 'and dimension_names contains %s '
CREATED_TIME_LE = "and created_at <= %s"
UPDATED_TIME_GE = "and updated_at >= %s"
DIMENSIONS_GT = 'and dimensions > %s'

DIMENSION_VALUE_BY_METRIC_CQL = ('select dimension_value as value from metrics_dimensions '
                                 'where region = ? and tenant_id = ? and metric_name = ? '
                                 'and dimension_name = ? group by dimension_value')

DIMENSION_VALUE_CQL = ('select value from dimensions '
                       'where region = ? and tenant_id = ? and name = ? '
                       'group by value order by value')

DIMENSION_NAME_BY_METRIC_CQL = ('select dimension_name as name from metrics_dimensions where '
                                'region = ? and tenant_id = ? and metric_name = ? '
                                'group by dimension_name order by dimension_name')

DIMENSION_NAME_CQL = ('select name from dimensions where region = ? and tenant_id = ? '
                      'group by name allow filtering')

METRIC_NAME_BY_DIMENSION_CQL = ('select metric_name from dimensions_metrics where region = ? and '
                                'tenant_id = ? and dimension_name = ? and dimension_value = ? '
                                'group by metric_name order by metric_name')

METRIC_NAME_BY_DIMENSION_OFFSET_CQL = (
    'select metric_name from dimensions_metrics where region = ? and '
    'tenant_id = ? and dimension_name = ? and dimension_value = ? and '
    'metric_name >= ?'
    'group by metric_name order by metric_name')

METRIC_NAME_CQL = ('select distinct region, tenant_id, metric_name from metrics_dimensions '
                   'where region = ? and tenant_id = ? allow filtering')

METRIC_NAME_OFFSET_CQL = ('select distinct region, tenant_id, metric_name from metrics_dimensions '
                          'where region = ? and tenant_id = ? and metric_name >= ? allow filtering')

METRIC_BY_ID_CQL = ('select region, tenant_id, metric_name, dimensions from measurements '
                    'where metric_id = ? limit 1')

Metric = namedtuple('metric', 'id name dimensions')

ALARM_HISTORY_CQL = (
    'select tenant_id, alarm_id, time_stamp, metric, new_state, old_state, reason, reason_data, '
    'sub_alarms from alarm_state_history where %s %s %s %s %s')

ALARM_ID_EQ = 'and alarm_id = %s'

ALARM_ID_IN = 'and alarm_id in %s'

ALARM_TENANT_ID_EQ = 'tenant_id = %s'


class MetricsRepository(metrics_repository.AbstractMetricsRepository):
    def __init__(self):

        try:
            self.conf = cfg.CONF

            if self.conf.cassandra.user:
                auth_provider = PlainTextAuthProvider(username=self.conf.cassandra.user,
                                                      password=self.conf.cassandra.password)
            else:
                auth_provider = None

            self.cluster = Cluster(self.conf.cassandra.contact_points,
                                   port=self.conf.cassandra.port,
                                   auth_provider=auth_provider,
                                   connect_timeout=self.conf.cassandra.connection_timeout,
                                   load_balancing_policy=TokenAwarePolicy(
                                       DCAwareRoundRobinPolicy(
                                           local_dc=self.conf.cassandra.local_data_center))
                                   )
            self.session = self.cluster.connect(self.conf.cassandra.keyspace)

            self.dim_val_by_metric_stmt = self.session.prepare(DIMENSION_VALUE_BY_METRIC_CQL)

            self.dim_val_stmt = self.session.prepare(DIMENSION_VALUE_CQL)

            self.dim_name_by_metric_stmt = self.session.prepare(DIMENSION_NAME_BY_METRIC_CQL)

            self.dim_name_stmt = self.session.prepare(DIMENSION_NAME_CQL)

            self.metric_name_by_dimension_stmt = self.session.prepare(METRIC_NAME_BY_DIMENSION_CQL)

            self.metric_name_by_dimension_offset_stmt = self.session.prepare(
                METRIC_NAME_BY_DIMENSION_OFFSET_CQL)

            self.metric_name_stmt = self.session.prepare(METRIC_NAME_CQL)

            self.metric_name_offset_stmt = self.session.prepare(METRIC_NAME_OFFSET_CQL)

            self.metric_by_id_stmt = self.session.prepare(METRIC_BY_ID_CQL)

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

        self.epoch = datetime.utcfromtimestamp(0)

    def list_dimension_values(self, tenant_id, region, metric_name,
                              dimension_name, start_timestamp=None,
                              end_timestamp=None):

        if start_timestamp or end_timestamp:
            # NOTE(brtknr): For more details, see story
            # https://storyboard.openstack.org/#!/story/2006204
            LOG.info("Scoping by timestamp not implemented for cassandra.")

        try:
            if metric_name:
                rows = self.session.execute(
                    self.dim_val_by_metric_stmt,
                    [region, tenant_id, metric_name, dimension_name])
            else:
                rows = self.session.execute(
                    self.dim_val_stmt,
                    [region, tenant_id, dimension_name])

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

        json_dim_value_list = []

        if not rows:
            return json_dim_value_list

        for row in rows:
            json_dim_value_list.append({u'dimension_value': row.value})

        json_dim_value_list.sort(key=lambda x: x[u'dimension_value'])

        return json_dim_value_list

    def list_dimension_names(self, tenant_id, region, metric_name,
                             start_timestamp=None, end_timestamp=None):

        if start_timestamp or end_timestamp:
            # NOTE(brtknr): For more details, see story
            # https://storyboard.openstack.org/#!/story/2006204
            LOG.info("Scoping by timestamp not implemented for cassandra.")

        try:
            if metric_name:
                rows = self.session.execute(
                    self.dim_name_by_metric_stmt,
                    [region, tenant_id, metric_name])
                ordered = True
            else:
                rows = self.session.execute(
                    self.dim_name_stmt,
                    [region, tenant_id])
                ordered = False

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

        if not rows:
            return []

        json_dim_name_list = [{u'dimension_name': row.name} for row in rows]

        if not ordered:
            json_dim_name_list.sort(key=lambda x: x[u'dimension_name'])

        return json_dim_name_list

    def list_metrics(self, tenant_id, region, name, dimensions, offset, limit, start_time=None,
                     end_time=None):

        offset_name = None
        offset_dimensions = []
        names = []
        metric_list = []
        offset_futures = []
        non_offset_futures = []

        try:
            if offset:
                offset_metric = self._get_metric_by_id(offset)
                if offset_metric:
                    offset_name = offset_metric.name
                    offset_dimensions = offset_metric.dimensions

            if not name:
                names = self._list_metric_names(tenant_id, region, dimensions, offset=offset_name)
                if names:
                    names = [elem['name'] for elem in names]
            else:
                names.append(name)

            if not names:
                return metric_list

            for name in names:
                if name == offset_name:
                    futures = self._list_metrics_by_name(
                        tenant_id,
                        region,
                        name,
                        dimensions,
                        offset_dimensions,
                        limit,
                        start_time=None,
                        end_time=None)
                    if offset_dimensions and dimensions:
                        offset_futures.extend(futures)
                    else:
                        non_offset_futures.extend(futures)
                else:
                    non_offset_futures.extend(
                        self._list_metrics_by_name(tenant_id, region, name, dimensions, None, limit,
                                                   start_time=None, end_time=None))

            # manually filter out metrics by the offset dimension
            for future in offset_futures:
                rows = future.result()
                for row in rows:
                    if offset_dimensions >= row.dimensions:
                        continue

                    metric_list.append(self._process_metric_row(row))

            for future in non_offset_futures:
                metric_list.extend((self._process_metric_row(row) for row in future.result()))

            return metric_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    @staticmethod
    def _process_metric_row(row):
        dim_map = {}
        for d in row.dimensions:
            pair = d.split('\t')
            dim_map[pair[0]] = pair[1]

        if row.metric_id is None:
            LOG.error(
                'Metric is missing metric_id, using metric_id=None'
                ' name: {}, dimensions: {}'.format(
                    row.metric_name, row.dimensions))
            return {'id': None,
                    'name': row.metric_name,
                    'dimensions': dim_map}

        metric = {'id': binascii.hexlify(bytearray(row.metric_id)),
                  'name': row.metric_name,
                  'dimensions': dim_map}

        return metric

    def _list_metrics_by_name(
            self,
            tenant_id,
            region,
            name,
            dimensions,
            dimension_offset,
            limit,
            start_time=None,
            end_time=None):

        or_dimensions = []
        sub_dimensions = {}
        futures = []

        if not dimensions:
            query = self._build_metrics_by_name_query(
                tenant_id,
                region,
                name,
                dimensions,
                None,
                start_time,
                end_time,
                dimension_offset,
                limit)
            futures.append(self.session.execute_async(query[0], query[1]))
            return futures

        wildcard_dimensions = []
        for dim_name, dim_value in dimensions.items():
            if not dim_value:
                wildcard_dimensions.append(dim_name)

            elif '|' in dim_value:

                def f(val):
                    return {dim_name: val}

                or_dimensions.append(list(map(f, sorted(dim_value.split('|')))))

            else:
                sub_dimensions[dim_name] = dim_value

        if or_dimensions:
            or_dims_list = list(itertools.product(*or_dimensions))

            for or_dims_tuple in or_dims_list:
                extracted_dimensions = sub_dimensions.copy()

                for dims in iter(or_dims_tuple):
                    for k, v in dims.items():
                        extracted_dimensions[k] = v

                query = self._build_metrics_by_name_query(
                    tenant_id,
                    region,
                    name,
                    extracted_dimensions,
                    wildcard_dimensions,
                    start_time,
                    end_time,
                    dimension_offset,
                    limit)

                futures.append(self.session.execute_async(query[0], query[1]))

        else:
            query = self._build_metrics_by_name_query(
                tenant_id,
                region,
                name,
                sub_dimensions,
                wildcard_dimensions,
                start_time,
                end_time,
                dimension_offset,
                limit)
            futures.append(self.session.execute_async(query[0], query[1]))

        return futures

    def _get_metric_by_id(self, metric_id):

        rows = self.session.execute(self.metric_by_id_stmt, [bytearray.fromhex(metric_id)])

        if rows:
            return Metric(id=metric_id, name=rows[0].metric_name, dimensions=rows[0].dimensions)

        return None

    def _build_metrics_by_name_query(
            self,
            tenant_id,
            region,
            name,
            dimensions,
            wildcard_dimensions,
            start_time,
            end_time,
            dim_offset,
            limit):

        conditions = [REGION_EQ, TENANT_EQ]
        params = [region, tenant_id.encode('utf8')]

        if name:
            conditions.append(METRIC_NAME_EQ)
            params.append(name)
        else:
            conditions.append('')

        if dimensions:
            conditions.append(DIMENSIONS_CONTAINS * len(dimensions))
            params.extend(
                [self._create_dimension_value_entry(dim_name, dim_value)
                 for dim_name, dim_value in dimensions.items()])
        else:
            conditions.append('')

        if wildcard_dimensions:
            conditions.append(DIMENSIONS_NAME_CONTAINS * len(wildcard_dimensions))
            params.extend(wildcard_dimensions)
        else:
            conditions.append('')

        if dim_offset and not dimensions:
            # cassandra does not allow using both contains and GT in collection column
            conditions.append(DIMENSIONS_GT)
            params.append(dim_offset)
        else:
            conditions.append('')

        if start_time:
            conditions.append(UPDATED_TIME_GE % start_time)
        else:
            conditions.append('')

        if end_time:
            conditions.append(CREATED_TIME_LE % end_time)
        else:
            conditions.append('')

        if limit:
            conditions.append(LIMIT_CLAUSE)
            params.append(limit)
        else:
            conditions.append('')

        if (not name) or dimensions or wildcard_dimensions or start_time or end_time:
            conditions.append(ALLOW_FILTERING)
        else:
            conditions.append('')

        return METRIC_LIST_CQL % tuple(conditions), params

    @staticmethod
    def _create_dimension_value_entry(name, value):
        return '%s\t%s' % (name, value)

    def list_metric_names(self, tenant_id, region, dimensions):
        return self._list_metric_names(tenant_id, region, dimensions)

    def _list_metric_names(self, tenant_id, region, dimensions, offset=None):

        or_dimensions = []
        single_dimensions = {}

        if dimensions:
            for key, value in dimensions.items():
                if not value:
                    continue

                elif '|' in value:
                    def f(val):
                        return {key: val}

                    or_dimensions.append(list(map(f, sorted(value.split('|')))))

                else:
                    single_dimensions[key] = value

        if or_dimensions:

            names = []
            or_dims_list = list(itertools.product(*or_dimensions))

            for or_dims_tuple in or_dims_list:
                extracted_dimensions = single_dimensions.copy()

                for dims in iter(or_dims_tuple):
                    for k, v in dims.items():
                        extracted_dimensions[k] = v

                names.extend(
                    self._list_metric_names_single_dimension_value(
                        tenant_id, region, extracted_dimensions, offset))

            names.sort(key=lambda x: x[u'name'])
            return names

        else:
            names = self._list_metric_names_single_dimension_value(
                tenant_id, region, single_dimensions, offset)
            names.sort(key=lambda x: x[u'name'])
            return names

    def _list_metric_names_single_dimension_value(self, tenant_id, region, dimensions, offset=None):

        try:
            futures = []
            if dimensions:
                for name, value in dimensions.items():
                    if offset:
                        futures.append(
                            self.session.execute_async(
                                self.metric_name_by_dimension_offset_stmt, [
                                    region, tenant_id, name, value, offset]))
                    else:
                        futures.append(
                            self.session.execute_async(
                                self.metric_name_by_dimension_stmt, [
                                    region, tenant_id, name, value]))

            else:
                if offset:
                    futures.append(
                        self.session.execute_async(
                            self.metric_name_offset_stmt, [
                                region, tenant_id, offset]))
                else:
                    futures.append(
                        self.session.execute_async(
                            self.metric_name_stmt, [
                                region, tenant_id]))

            names_list = []

            for future in futures:
                rows = future.result()
                tmp = set()
                for row in rows:
                    tmp.add(row.metric_name)

                names_list.append(tmp)

            return [{u'name': v} for v in set.intersection(*names_list)]

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def measurement_list(self, tenant_id, region, name, dimensions,
                         start_timestamp, end_timestamp, offset, limit,
                         merge_metrics_flag, group_by):

        metrics = self.list_metrics(tenant_id, region, name, dimensions, None, None)

        if offset:
            tmp = offset.split("_")
            if len(tmp) > 1:
                offset_id = tmp[0]
                offset_timestamp = tmp[1]
            else:
                offset_id = None
                offset_timestamp = offset
        else:
            offset_timestamp = None
            offset_id = None

        if not metrics:
            return None
        elif len(metrics) > 1:
            if not merge_metrics_flag and not group_by:
                raise exceptions.MultipleMetricsException(self.MULTIPLE_METRICS_MESSAGE)

        try:
            if len(metrics) > 1 and not group_by:
                # offset is controlled only by offset_timestamp when the group by option
                # is not enabled
                count, series_list = self._query_merge_measurements(metrics,
                                                                    dimensions,
                                                                    start_timestamp,
                                                                    end_timestamp,
                                                                    offset_timestamp,
                                                                    limit)
                return series_list

            if group_by:
                if not isinstance(group_by, list):
                    group_by = group_by.split(',')
                elif len(group_by) == 1:
                    group_by = group_by[0].split(',')

            if len(metrics) == 1 or group_by[0].startswith('*'):
                if offset_id:
                    for index, metric in enumerate(metrics):
                        if metric['id'] == offset_id:
                            if index > 0:
                                metrics[0:index] = []
                            break

                count, series_list = self._query_measurements(metrics,
                                                              start_timestamp,
                                                              end_timestamp,
                                                              offset_timestamp,
                                                              limit)

                return series_list

            grouped_metrics = self._group_metrics(metrics, group_by, dimensions)

            if not grouped_metrics or len(grouped_metrics) == 0:
                return None

            if offset_id:
                found_offset = False
                for outer_index, sublist in enumerate(grouped_metrics):
                    for inner_index, metric in enumerate(sublist):
                        if metric['id'] == offset_id:
                            found_offset = True
                            if inner_index > 0:
                                sublist[0:inner_index] = []
                            break
                    if found_offset:
                        if outer_index > 0:
                            grouped_metrics[0:outer_index] = []
                        break

            remaining = limit
            series_list = []
            for sublist in grouped_metrics:
                sub_count, results = self._query_merge_measurements(sublist,
                                                                    sublist[0]['dimensions'],
                                                                    start_timestamp,
                                                                    end_timestamp,
                                                                    offset_timestamp,
                                                                    remaining)

                series_list.extend(results)

                if remaining:
                    remaining -= sub_count
                    if remaining <= 0:
                        break

                # offset_timestamp is used only in the first group, reset to None for
                # subsequent groups
                if offset_timestamp:
                    offset_timestamp = None

            return series_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def _query_merge_measurements(self, metrics, dimensions, start_timestamp, end_timestamp,
                                  offset_timestamp, limit):
        results = []
        for metric in metrics:
            if limit and len(metrics) > 1:
                fetch_size = min(limit, max(1000, limit / len(metrics) + 2))
            else:
                fetch_size = None
            query = self._build_measurement_query(metric['id'],
                                                  start_timestamp,
                                                  end_timestamp,
                                                  offset_timestamp,
                                                  limit,
                                                  fetch_size)
            results.append((metric, iter(self.session.execute_async(query[0], query[1]).result())))

        return self._merge_series(results, dimensions, limit)

    def _query_measurements(self, metrics, start_timestamp, end_timestamp,
                            offset_timestamp, limit):
        results = []
        for index, metric in enumerate(metrics):
            if index == 0:
                query = self._build_measurement_query(metric['id'],
                                                      start_timestamp,
                                                      end_timestamp,
                                                      offset_timestamp,
                                                      limit)
            else:
                if limit:
                    fetch_size = min(self.session.default_fetch_size,
                                     max(1000, limit / min(index, 4)))
                else:
                    fetch_size = self.session.default_fetch_size
                query = self._build_measurement_query(metric['id'],
                                                      start_timestamp,
                                                      end_timestamp,
                                                      None,
                                                      limit,
                                                      fetch_size)

            results.append([metric,
                            iter(self.session.execute_async(query[0], query[1]).result())])

        series_list = []
        count = 0
        for result in results:
            measurements = []
            row = next(result[1], None)
            while row:
                measurements.append(
                    [self._isotime_msec(row.time_stamp), row.value,
                     rest_utils.from_json(row.value_meta) if row.value_meta else {}])
                count += 1
                if limit and count >= limit:
                    break

                row = next(result[1], None)

            series_list.append({'name': result[0]['name'],
                                'id': result[0]['id'],
                                'columns': ['timestamp', 'value', 'value_meta'],
                                'measurements': measurements,
                                'dimensions': result[0]['dimensions']})
            if limit and count >= limit:
                break

        return count, series_list

    @staticmethod
    def _build_measurement_query(metric_id, start_timestamp,
                                 end_timestamp, offset_timestamp,
                                 limit=None, fetch_size=FETCH_SIZE_UNSET):
        conditions = [METRIC_ID_EQ]
        decode_metric_id = metric_id if six.PY2 else metric_id.decode('utf-8')
        params = [bytearray.fromhex(decode_metric_id)]

        if offset_timestamp:
            conditions.append(OFFSET_TIME_GT)
            params.append(offset_timestamp)
        elif start_timestamp:
            conditions.append(START_TIME_GE)
            params.append(int(start_timestamp * 1000))
        else:
            conditions.append('')

        if end_timestamp:
            conditions.append(END_TIME_LE)
            params.append(int(end_timestamp * 1000))
        else:
            conditions.append('')

        if limit:
            conditions.append(LIMIT_CLAUSE)
            params.append(limit)
        else:
            conditions.append('')

        return SimpleStatement(MEASUREMENT_LIST_CQL %
                               tuple(conditions), fetch_size=fetch_size), params

    def _merge_series(self, series, dimensions, limit):
        series_list = []

        if not series:
            return series_list

        measurements = []
        top_batch = []
        num_series = len(series)
        for i in range(0, num_series):
            row = next(series[i][1], None)
            if row:
                top_batch.append([i,
                                  row.time_stamp,
                                  row.value,
                                  rest_utils.from_json(row.value_meta) if row.value_meta else {}])
            else:
                num_series -= 1

        top_batch.sort(key=lambda m: m[1], reverse=True)

        count = 0
        while (not limit or count < limit) and top_batch:
            measurements.append([self._isotime_msec(top_batch[num_series - 1][1]),
                                 top_batch[num_series - 1][2],
                                 top_batch[num_series - 1][3]])
            count += 1
            row = next(series[top_batch[num_series - 1][0]][1], None)
            if row:
                top_batch[num_series - 1] = \
                    [top_batch[num_series - 1][0], row.time_stamp,
                     row.value, rest_utils.from_json(row.value_meta) if row.value_meta else {}]

                top_batch.sort(key=lambda m: m[1], reverse=True)
            else:
                num_series -= 1
                top_batch.pop()

        series_list.append({'name': series[0][0]['name'],
                            'id': series[0][0]['id'],
                            'columns': ['timestamp', 'value', 'value_meta'],
                            'measurements': measurements,
                            'dimensions': dimensions})

        return count, series_list

    @staticmethod
    def _group_metrics(metrics, group_by, search_by):

        grouped_metrics = {}
        for metric in metrics:
            key = ''
            display_dimensions = dict(search_by.items())
            for name in group_by:
                # '_' ensures te key with missing dimension is sorted lower
                value = metric['dimensions'].get(name, '_')
                if value != '_':
                    display_dimensions[name] = value
                key = key + '='.join((urllib.quote_plus(name), urllib.quote_plus(value))) + '&'

            metric['dimensions'] = display_dimensions

            if key in grouped_metrics:
                grouped_metrics[key].append(metric)
            else:
                grouped_metrics[key] = [metric]

        grouped_metrics = grouped_metrics.items()
        grouped_metrics.sort(key=lambda k: k[0])
        return [x[1] for x in grouped_metrics]

    @staticmethod
    def _isotime_msec(timestamp):
        """Stringify datetime in ISO 8601 format + millisecond.
        """
        st = timestamp.isoformat()
        if '.' in st:
            st = st[:23] + u'Z'
        else:
            st += u'.000Z'
        return st

    def metrics_statistics(self, tenant_id, region, name, dimensions,
                           start_timestamp, end_timestamp, statistics,
                           period, offset, limit, merge_metrics_flag,
                           group_by):

        if not period:
            period = 300
        else:
            period = int(period)

        series_list = self.measurement_list(tenant_id, region, name, dimensions,
                                            start_timestamp, end_timestamp,
                                            offset, None, merge_metrics_flag, group_by)

        json_statistics_list = []

        if not series_list:
            return json_statistics_list

        statistics = [stat.lower() for stat in statistics]

        columns = [u'timestamp']

        columns.extend([x for x in ['avg', 'min', 'max', 'count', 'sum'] if x in statistics])

        start_time = datetime.utcfromtimestamp(start_timestamp)
        if end_timestamp:
            end_time = datetime.utcfromtimestamp(end_timestamp)
        else:
            end_time = datetime.utcnow()

        for series in series_list:

            if limit <= 0:
                break

            measurements = series['measurements']

            if not measurements:
                continue

            first_measure = measurements[0]
            first_measure_start_time = MetricsRepository._parse_time_string(first_measure[0])

            # skip blank intervals at the beginning, finds the start time of stat
            # period that is not empty
            stat_start_time = start_time + timedelta(
                seconds=((first_measure_start_time - start_time).seconds / period) * period)

            stats_list = []
            stats_count = 0
            stats_sum = 0
            stats_min = stats_max = first_measure[1]

            for measurement in series['measurements']:

                time_stamp = MetricsRepository._parse_time_string(measurement[0])
                value = measurement[1]

                if (time_stamp - stat_start_time).seconds >= period:

                    stat = MetricsRepository._create_stat(statistics, stat_start_time, stats_count,
                                                          stats_sum, stats_min, stats_max)

                    stats_list.append(stat)
                    limit -= 1
                    if limit <= 0:
                        break

                    # initialize the new stat period
                    stats_sum = value
                    stats_count = 1
                    stats_min = value
                    stats_max = value
                    stat_start_time += timedelta(seconds=period)

                else:
                    stats_min = min(stats_min, value)
                    stats_max = max(stats_max, value)
                    stats_count += 1
                    stats_sum += value

            if stats_count:
                stat = MetricsRepository._create_stat(
                    statistics, stat_start_time, stats_count, stats_sum, stats_min, stats_max)
                stats_list.append(stat)
                limit -= 1

            stats_end_time = stat_start_time + timedelta(seconds=period) - timedelta(milliseconds=1)
            if stats_end_time > end_time:
                stats_end_time = end_time

            statistic = {u'name': encodeutils.safe_decode(name, 'utf-8'),
                         u'id': series['id'],
                         u'dimensions': series['dimensions'],
                         u'columns': columns,
                         u'statistics': stats_list,
                         u'end_time': self._isotime_msec(stats_end_time)}

            json_statistics_list.append(statistic)

        return json_statistics_list

    @staticmethod
    def _create_stat(
            statistics,
            timestamp,
            stat_count=None,
            stat_sum=None,
            stat_min=None,
            stat_max=None):

        stat = [MetricsRepository._isotime_msec(timestamp)]

        if not stat_count:
            stat.extend([0] * len(statistics))

        else:
            if 'avg' in statistics:
                stat.append(stat_sum / stat_count)

            if 'min' in statistics:
                stat.append(stat_min)

            if 'max' in statistics:
                stat.append(stat_max)

            if 'count' in statistics:
                stat.append(stat_count)

            if 'sum' in statistics:
                stat.append(stat_sum)

        return stat

    @staticmethod
    def _parse_time_string(timestamp):
        dt = timeutils.parse_isotime(timestamp)
        dt = timeutils.normalize_time(dt)
        return dt

    def alarm_history(self, tenant_id, alarm_id_list,
                      offset, limit, start_timestamp=None,
                      end_timestamp=None):

        try:

            json_alarm_history_list = []

            if not alarm_id_list:
                return json_alarm_history_list

            conditions = [ALARM_TENANT_ID_EQ]
            params = [tenant_id.encode('utf8')]
            if len(alarm_id_list) == 1:
                conditions.append(ALARM_ID_EQ)
                params.append(alarm_id_list[0])
            else:
                conditions.append(
                    ' and alarm_id in ({}) '.format(
                        ','.join(
                            ['%s'] *
                            len(alarm_id_list))))
                for alarm_id in alarm_id_list:
                    params.append(alarm_id)

            if offset:
                conditions.append(OFFSET_TIME_GT)
                params.append(offset)

            elif start_timestamp:
                conditions.append(START_TIME_GE)
                params.append(int(start_timestamp * 1000))
            else:
                conditions.append('')

            if end_timestamp:
                conditions.append(END_TIME_LE)
                params.append(int(end_timestamp * 1000))
            else:
                conditions.append('')

            if limit:
                conditions.append(LIMIT_CLAUSE)
                params.append(limit + 1)
            else:
                conditions.append('')

            rows = self.session.execute(ALARM_HISTORY_CQL % tuple(conditions), params)

            if not rows:
                return json_alarm_history_list

            sorted_rows = sorted(rows, key=lambda row: row.time_stamp)

            for (tenant_id, alarm_id, time_stamp, metrics, new_state, old_state, reason,
                 reason_data, sub_alarms) in sorted_rows:

                alarm = {u'timestamp': self._isotime_msec(time_stamp),
                         u'alarm_id': alarm_id,
                         u'metrics': rest_utils.from_json(metrics),
                         u'new_state': new_state,
                         u'old_state': old_state,
                         u'reason': reason,
                         u'reason_data': reason_data,
                         u'sub_alarms': rest_utils.from_json(sub_alarms),
                         u'id': str(int((time_stamp - self.epoch).total_seconds() * 1000))}

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
    def check_status():
        try:
            cluster = Cluster(
                CONF.cassandra.contact_points
            )
            session = cluster.connect(CONF.cassandra.keyspace)
            session.shutdown()
        except Exception as ex:
            LOG.exception(str(ex))
            return False, str(ex)
        return True, 'OK'
