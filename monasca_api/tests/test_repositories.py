# Copyright 2015 Cray Inc. All Rights Reserved.
# (C) Copyright 2016-2018 Hewlett Packard Enterprise Development LP
# Copyright 2017 Fujitsu LIMITED
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

import cassandra
from cassandra.auth import PlainTextAuthProvider
from unittest.mock import patch

from oslo_config import cfg
from oslo_utils import timeutils

from monasca_api.common.repositories.cassandra import metrics_repository \
    as cassandra_repo
from monasca_api.common.repositories.influxdb import metrics_repository \
    as influxdb_repo
from monasca_api.tests import base

CONF = cfg.CONF


class TestRepoMetricsInfluxDB(base.BaseTestCase):
    @patch("monasca_api.common.repositories.influxdb."
           "metrics_repository.client.InfluxDBClient")
    def test_measurement_list(self, influxdb_client_mock):
        mock_client = influxdb_client_mock.return_value
        mock_client.query.return_value.raw = {
            "series": [
                {
                    "name": "dummy.series",
                    "values": [
                        ["2015-03-14T09:26:53.59Z", 2, None],
                        ["2015-03-14T09:26:53.591Z", 2.5, ''],
                        ["2015-03-14T09:26:53.6Z", 4.0, '{}'],
                        ["2015-03-14T09:26:54Z", 4, '{"key": "value"}'],
                        ["2015-03-14T09:26:53.1234567Z", 1, '{}']
                    ]
                }
            ]
        }

        repo = influxdb_repo.MetricsRepository()
        repo._version = 'from_0.11.0'
        result = repo.measurement_list(
            "tenant_id",
            "region",
            name=None,
            dimensions=None,
            start_timestamp=1,
            end_timestamp=2,
            offset=None,
            limit=1,
            merge_metrics_flag=True,
            group_by=None)

        self.assertEqual(len(result), 1)
        self.assertIsNone(result[0]['dimensions'])
        self.assertEqual(result[0]['name'], 'dummy.series')
        self.assertEqual(result[0]['columns'],
                         ['timestamp', 'value', 'value_meta'])

        measurements = result[0]['measurements']

        self.assertEqual(
            [["2015-03-14T09:26:53.590Z", 2, {}],
             ["2015-03-14T09:26:53.591Z", 2.5, {}],
             ["2015-03-14T09:26:53.600Z", 4.0, {}],
             ["2015-03-14T09:26:54.000Z", 4, {"key": "value"}],
             ["2015-03-14T09:26:53.123Z", 1, {}]],
            measurements
        )

    @patch("monasca_api.common.repositories.influxdb."
           "metrics_repository.client.InfluxDBClient")
    def test_list_metrics(self, influxdb_client_mock):
        mock_client = influxdb_client_mock.return_value
        mock_client.query.return_value.raw = {
            u'series': [{
                u'values': [[
                    u'disk.space_used_perc,_region=region,_tenant_id='
                    u'0b5e7d8c43f74430add94fba09ffd66e,device=rootfs,'
                    'hostname=host0,hosttype=native,mount_point=/'
                ]],
                u'columns':[u'key']
            }]
        }

        repo = influxdb_repo.MetricsRepository()
        repo._version = 'from_0.11.0'
        result = repo.list_metrics(
            "0b5e7d8c43f74430add94fba09ffd66e",
            "region",
            name="disk.space_user_perc",
            dimensions={
                "hostname": "host0",
                "hosttype": "native",
                "mount_point": "/",
                "device": "rootfs"},
            offset=None,
            limit=1)

        self.assertEqual(result, [{
            u'id': '0',
            u'name': u'disk.space_used_perc',
            u'dimensions': {
                u'device': u'rootfs',
                u'hostname': u'host0',
                u'mount_point': u'/',
                u'hosttype': u'native'
            },
        }])

    @patch('monasca_api.common.repositories.influxdb.'
           'metrics_repository.client.InfluxDBClient')
    def test_metrics_statistics(self, influxdb_client_mock):
        mock_client = influxdb_client_mock.return_value
        mock_client.query.return_value.raw = {
            u'series': [{
                u'values': [[
                    u'1970-01-01T00:00:00Z', 0.047
                ]],
                u'name': u'cpu.utilization',
                u'columns': [u'time', u'mean']}],
            u'statement_id': 0
        }

        tenant_id = '1'
        region = 'USA'
        name = 'cpu.utilization'
        start_timestamp = 1484036107.86
        statistics = [u"avg"]
        limit = 10000
        dimensions = None
        end_timestamp = None
        period = None
        offset = None
        merge_metrics_flag = None
        group_by = None

        repo = influxdb_repo.MetricsRepository()
        repo._version = 'from_0.11.0'
        stats_list = repo.metrics_statistics(tenant_id, region, name,
                                             dimensions, start_timestamp,
                                             end_timestamp, statistics,
                                             period, offset, limit,
                                             merge_metrics_flag, group_by)
        expected_result = [{
            u'columns': [u'timestamp', u'avg'],
            u'dimensions': {},
            u'id': '0',
            u'name': u'cpu.utilization',
            u'statistics': [[u'1970-01-01T00:00:00Z', 0.047]]}]
        self.assertEqual(stats_list, expected_result)

    def test_build_group_by_clause_with_period(self):
        group_by = 'hostname,service'
        period = 300
        expected_clause = ' group by hostname,service,time(300s) fill(none)'

        repo = influxdb_repo.MetricsRepository()
        repo._version = 'from_0.11.0'
        clause = repo._build_group_by_clause(group_by, period)
        self.assertEqual(clause, expected_clause)

    def test_build_group_by_clause_without_period(self):
        group_by = 'hostname,service'
        expected_clause = ' group by hostname,service'

        repo = influxdb_repo.MetricsRepository()
        repo._version = 'from_0.11.0'
        clause = repo._build_group_by_clause(group_by)
        self.assertEqual(clause, expected_clause)

    @patch("monasca_api.common.repositories.influxdb."
           "metrics_repository.client.InfluxDBClient")
    def test_list_dimension_values(self, influxdb_client_mock, timestamp=False):
        mock_client = influxdb_client_mock.return_value

        tenant_id = u'38dc2a2549f94d2e9a4fa1cc45a4970c'
        region = u'useast'
        metric = u'custom_metric'
        column = u'hostname'
        hostname = u'custom_host'
        start_timestamp = 1571917171275
        end_timestamp = 1572917171275
        mock_client.query.return_value.raw = {
            u'series': [{
                u'values': [[column, hostname]],
                u'name': metric,
                u'columns': [u'key', u'value']
            }]
        }
        repo = influxdb_repo.MetricsRepository()
        repo._version = 'from_0.11.0'
        mock_client.query.reset_mock()

        db_per_tenant = repo.conf.influxdb.db_per_tenant
        database = repo.conf.influxdb.database_name
        database += "_%s" % tenant_id if db_per_tenant else ""

        result = (repo.list_dimension_values(tenant_id, region, metric, column,
                                             start_timestamp, end_timestamp)
                  if timestamp else
                  repo.list_dimension_values(tenant_id, region, metric, column))

        self.assertEqual(result, [{u'dimension_value': hostname}])

        query = ('show tag values from "{metric}"'
                 ' with key = "{column}"'
                 ' where _region = \'{region}\''
                 .format(region=region, metric=metric, column=column))
        query += ('' if db_per_tenant else ' and _tenant_id = \'{tenant_id}\''
                  .format(tenant_id=tenant_id))
        query += (' and time >= {start_timestamp}000000u'
                  ' and time < {end_timestamp}000000u'
                  .format(start_timestamp=start_timestamp,
                          end_timestamp=end_timestamp)
                  if timestamp else '')
        mock_client.query.assert_called_once_with(query, database=database)

    def test_list_dimension_values_with_timestamp(self):
        self.test_list_dimension_values(timestamp=True)

    @patch("monasca_api.common.repositories.influxdb."
           "metrics_repository.client.InfluxDBClient")
    def test_list_dimension_names(self, influxdb_client_mock, timestamp=False):
        mock_client = influxdb_client_mock.return_value

        tenant_id = u'38dc2a2549f94d2e9a4fa1cc45a4970c'
        region = u'useast'
        metric = u'custom_metric'
        start_timestamp = 1571917171275
        end_timestamp = 1572917171275
        mock_client.query.return_value.raw = {
            u'series': [{
                u'values': [[u'_region'], [u'_tenant_id'], [u'hostname'],
                            [u'service']],
                u'name': metric,
                u'columns': [u'tagKey']
            }]
        }

        repo = influxdb_repo.MetricsRepository()
        repo._version = 'from_0.11.0'
        mock_client.query.reset_mock()

        db_per_tenant = repo.conf.influxdb.db_per_tenant
        database = repo.conf.influxdb.database_name
        database += "_%s" % tenant_id if db_per_tenant else ""

        result = (repo.list_dimension_names(tenant_id, region, metric,
                                            start_timestamp, end_timestamp)
                  if timestamp else
                  repo.list_dimension_names(tenant_id, region, metric))

        self.assertEqual(result,
                         [
                             {u'dimension_name': u'hostname'},
                             {u'dimension_name': u'service'}
                         ])

        query = ('show tag keys from "{metric}"'
                 ' where _region = \'{region}\''
                 .format(region=region, metric=metric))
        query += ('' if db_per_tenant else ' and _tenant_id = \'{tenant_id}\''
                  .format(tenant_id=tenant_id))
        query += (' and time >= {start_timestamp}000000u'
                  ' and time < {end_timestamp}000000u'
                  .format(start_timestamp=start_timestamp,
                          end_timestamp=end_timestamp)
                  if timestamp else '')

        mock_client.query.assert_called_once_with(query, database=database)

    def test_list_dimension_names_with_timestamp(self):
        self.test_list_dimension_names(timestamp=True)

    @patch("monasca_api.common.repositories.influxdb."
           "metrics_repository.requests.head")
    def test_check_status(self, head_mock):
        head_mock.return_value.ok = True
        head_mock.return_value.status_code = 204

        result = influxdb_repo.MetricsRepository.check_status()

        self.assertEqual(result, (True, 'OK'))

    @patch("monasca_api.common.repositories.influxdb."
           "metrics_repository.requests.head")
    def test_check_status_server_error(self, head_mock):
        head_mock.return_value.status_code = 500
        head_mock.return_value.ok = False

        result = influxdb_repo.MetricsRepository.check_status()

        self.assertEqual(result, (False, 'Error: 500'))


class TestRepoMetricsCassandra(base.BaseTestCase):
    def setUp(self):
        super(TestRepoMetricsCassandra, self).setUp()
        self.conf_default(contact_points='127.0.0.1',
                          group='cassandra')

    @patch("monasca_api.common.repositories.cassandra."
           "metrics_repository.Cluster.connect")
    def test_init(self, cassandra_connect_mock):
        repo = cassandra_repo.MetricsRepository()
        self.assertIsNone(
            repo.cluster.auth_provider,
            'cassandra cluster auth provider is expected to None'
        )

        repo.conf.cassandra.user = 'cassandra'
        repo.conf.cassandra.password = 'cassandra'
        repo = cassandra_repo.MetricsRepository()
        self.assertIsInstance(
            repo.cluster.auth_provider,
            PlainTextAuthProvider,
            'cassandra cluster auth provider is expected to be PlainTextAuthProvider'
        )

    @patch("monasca_api.common.repositories.cassandra."
           "metrics_repository.Cluster.connect")
    def test_list_metrics(self, cassandra_connect_mock):
        cassandra_session_mock = cassandra_connect_mock.return_value
        cassandra_future_mock = cassandra_session_mock.execute_async.return_value

        Metric = namedtuple('Metric', 'metric_id metric_name dimensions')

        cassandra_future_mock.result.return_value = [
            Metric(
                metric_id=binascii.unhexlify(b"01d39f19798ed27bbf458300bf843edd17654614"),
                metric_name='disk.space_used_perc',
                dimensions=[
                    'device\trootfs',
                    'hostname\thost0',
                    'hosttype\tnative',
                    'mount_point\t/']
            )
        ]

        repo = cassandra_repo.MetricsRepository()

        result = repo.list_metrics(
            "0b5e7d8c43f74430add94fba09ffd66e",
            "region",
            name="disk.space_user_perc",
            dimensions={
                "hostname": "host0",
                "hosttype": "native",
                "mount_point": "/",
                "device": "rootfs"},
            offset=None,
            limit=1)

        self.assertEqual([{
            u'id': b'01d39f19798ed27bbf458300bf843edd17654614',
            u'name': u'disk.space_used_perc',
            u'dimensions': {
                u'device': u'rootfs',
                u'hostname': u'host0',
                u'mount_point': u'/',
                u'hosttype': u'native'
            }}], result)

    # As Cassandra allows sparse data, it is possible to have a missing metric_id
    @patch("monasca_api.common.repositories.cassandra."
           "metrics_repository.Cluster.connect")
    def test_list_metrics_empty_metric_id(self, cassandra_connect_mock):
        cassandra_session_mock = cassandra_connect_mock.return_value
        cassandra_future_mock = cassandra_session_mock.execute_async.return_value

        Metric = namedtuple('Metric', 'metric_id metric_name dimensions')

        cassandra_future_mock.result.return_value = [
            Metric(
                metric_id=None,
                metric_name='disk.space_used_perc',
                dimensions=[
                    'device\trootfs',
                    'hostname\thost0',
                    'hosttype\tnative',
                    'mount_point\t/']
            )
        ]

        repo = cassandra_repo.MetricsRepository()

        result = repo.list_metrics(
            "0b5e7d8c43f74430add94fba09ffd66e",
            "region",
            name="disk.space_user_perc",
            dimensions={
                "hostname": "host0",
                "hosttype": "native",
                "mount_point": "/",
                "device": "rootfs"},
            offset=None,
            limit=1)

        self.assertEqual([{
            u'id': None,
            u'name': u'disk.space_used_perc',
            u'dimensions': {
                u'device': u'rootfs',
                u'hostname': u'host0',
                u'mount_point': u'/',
                u'hosttype': u'native'
            }}], result)

    @patch("monasca_api.common.repositories.cassandra."
           "metrics_repository.Cluster.connect")
    def test_list_metric_names(self, cassandra_connect_mock):
        cassandra_session_mock = cassandra_connect_mock.return_value
        cassandra_future_mock = cassandra_session_mock.execute_async.return_value

        Metric = namedtuple('Metric', 'metric_name')

        cassandra_future_mock.result.return_value = [
            Metric('disk.space_used_perc'),
            Metric('cpu.idle_perc')
        ]

        cassandra_session_mock.execute.return_value = [
            Metric('disk.space_used_perc'),
            Metric('cpu.idle_perc')
        ]

        repo = cassandra_repo.MetricsRepository()
        result = repo.list_metric_names(
            "0b5e7d8c43f74430add94fba09ffd66e",
            "region",
            dimensions={
                "hostname": "host0",
                "hosttype": "native",
                "mount_point": "/",
                "device": "rootfs"})

        self.assertEqual([
            {
                u'name': u'cpu.idle_perc'
            },
            {
                u'name': u'disk.space_used_perc'
            }
        ], result)

    @patch("monasca_api.common.repositories.cassandra."
           "metrics_repository.Cluster.connect")
    def test_measurement_list(self, cassandra_connect_mock):
        Measurement = namedtuple('Measurement', 'time_stamp value value_meta')

        cassandra_session_mock = cassandra_connect_mock.return_value
        cassandra_future_mock = cassandra_session_mock.execute_async.return_value

        Metric = namedtuple('Metric', 'metric_id metric_name dimensions')

        cassandra_future_mock.result.side_effect = [
            [
                Metric(
                    metric_id=binascii.unhexlify(b"01d39f19798ed27bbf458300bf843edd17654614"),
                    metric_name='disk.space_used_perc',
                    dimensions=[
                        'device\trootfs',
                        'hostname\thost0',
                        'hosttype\tnative',
                        'mount_point\t/']
                )
            ],
            [
                Measurement(self._convert_time_string("2015-03-14T09:26:53.59Z"), 2, None),
                Measurement(self._convert_time_string("2015-03-14T09:26:53.591Z"), 4,
                            '{"key": "value"}'),
                Measurement(self._convert_time_string("2015-03-14T09:26:53.6Z"), 2.5, ''),
                Measurement(self._convert_time_string("2015-03-14T09:26:54.0Z"), 4.0, '{}'),
            ]
        ]

        repo = cassandra_repo.MetricsRepository()
        result = repo.measurement_list(
            "tenant_id",
            "region",
            name="disk.space_used_perc",
            dimensions=None,
            start_timestamp=1,
            end_timestamp=2,
            offset=None,
            limit=2,
            merge_metrics_flag=True,
            group_by=None)

        self.assertEqual(len(result), 1)
        self.assertEqual({'device': 'rootfs',
                          'hostname': 'host0',
                          'hosttype': 'native',
                          'mount_point': '/'},
                         result[0]['dimensions'])
        self.assertEqual(result[0]['name'], 'disk.space_used_perc')
        self.assertEqual(result[0]['columns'],
                         ['timestamp', 'value', 'value_meta'])

        self.assertEqual(
            [['2015-03-14T09:26:53.590Z', 2, {}],
             ['2015-03-14T09:26:53.591Z', 4, {'key': 'value'}]],
            result[0]['measurements']
        )

    @patch("monasca_api.common.repositories.cassandra."
           "metrics_repository.Cluster.connect")
    def test_metrics_statistics(self, cassandra_connect_mock):
        Measurement = namedtuple('Measurement', 'time_stamp value value_meta')

        cassandra_session_mock = cassandra_connect_mock.return_value
        cassandra_future_mock = cassandra_session_mock.execute_async.return_value

        Metric = namedtuple('Metric', 'metric_id metric_name dimensions')

        cassandra_future_mock.result.side_effect = [
            [
                Metric(
                    metric_id=binascii.unhexlify(b"01d39f19798ed27bbf458300bf843edd17654614"),
                    metric_name='cpu.idle_perc',
                    dimensions=[
                        'device\trootfs',
                        'hostname\thost0',
                        'hosttype\tnative',
                        'mount_point\t/']
                )
            ],
            [
                Measurement(self._convert_time_string("2016-05-19T11:58:24Z"), 95.0, '{}'),
                Measurement(self._convert_time_string("2016-05-19T11:58:25Z"), 97.0, '{}'),
                Measurement(self._convert_time_string("2016-05-19T11:58:26Z"), 94.0, '{}'),
                Measurement(self._convert_time_string("2016-05-19T11:58:27Z"), 96.0, '{}'),
            ]
        ]

        start_timestamp = (self._convert_time_string("2016-05-19T11:58:24Z") -
                           datetime(1970, 1, 1)).total_seconds()
        end_timestamp = (self._convert_time_string("2016-05-19T11:58:27Z") -
                         datetime(1970, 1, 1)).total_seconds()
        print(start_timestamp)

        repo = cassandra_repo.MetricsRepository()
        result = repo.metrics_statistics(
            "tenant_id",
            "region",
            name="cpu.idle_perc",
            dimensions=None,
            start_timestamp=start_timestamp,
            end_timestamp=end_timestamp,
            statistics=['avg', 'min', 'max', 'count', 'sum'],
            period=300,
            offset=None,
            limit=1,
            merge_metrics_flag=True,
            group_by=None)

        self.assertEqual([
            {
                u'dimensions': {'device': 'rootfs',
                                'hostname': 'host0',
                                'hosttype': 'native',
                                'mount_point': '/'},
                u'end_time': u'2016-05-19T11:58:27.000Z',
                u'statistics': [[u'2016-05-19T11:58:24.000Z', 95.5, 94.0, 97.0, 4, 382.0]],
                u'name': u'cpu.idle_perc',
                u'columns': [u'timestamp', 'avg', 'min', 'max', 'count', 'sum'],
                u'id': b'01d39f19798ed27bbf458300bf843edd17654614'
            }
        ], result)

        cassandra_future_mock.result.side_effect = [
            [
                Metric(
                    metric_id=binascii.unhexlify(b"01d39f19798ed27bbf458300bf843edd17654614"),
                    metric_name='cpu.idle_perc',
                    dimensions=[
                        'device\trootfs',
                        'hostname\thost0',
                        'hosttype\tnative',
                        'mount_point\t/']
                )
            ],
            [
                Measurement(self._convert_time_string("2016-05-19T11:58:24Z"), 95.0, '{}'),
                Measurement(self._convert_time_string("2016-05-19T11:58:25Z"), 97.0, '{}'),
                Measurement(self._convert_time_string("2016-05-19T11:58:26Z"), 94.0, '{}'),
                Measurement(self._convert_time_string("2016-05-19T11:58:27Z"), 96.0, '{}'),
            ]
        ]

        result = repo.metrics_statistics(
            "tenant_id",
            "region",
            name="cpu.idle_perc",
            dimensions=None,
            start_timestamp=start_timestamp,
            end_timestamp=None,
            statistics=['avg', 'min', 'max', 'count', 'sum'],
            period=300,
            offset=None,
            limit=1,
            merge_metrics_flag=True,
            group_by=None)

        self.assertEqual([
            {
                u'dimensions': {'device': 'rootfs',
                                'hostname': 'host0',
                                'hosttype': 'native',
                                'mount_point': '/'},
                u'end_time': u'2016-05-19T12:03:23.999Z',
                u'statistics': [[u'2016-05-19T11:58:24.000Z', 95.5, 94.0, 97.0, 4, 382.0]],
                u'name': u'cpu.idle_perc',
                u'columns': [u'timestamp', 'avg', 'min', 'max', 'count', 'sum'],
                u'id': b'01d39f19798ed27bbf458300bf843edd17654614'
            }
        ], result)

    @patch("monasca_api.common.repositories.cassandra."
           "metrics_repository.Cluster.connect")
    def test_alarm_history(self, cassandra_connect_mock):
        AlarmHistory = namedtuple('AlarmHistory', 'alarm_id, time_stamp, metrics, '
                                                  'new_state, old_state, reason, '
                                                  'reason_data, sub_alarms, tenant_id')

        cassandra_session_mock = cassandra_connect_mock.return_value
        cassandra_session_mock.execute.return_value = [
            AlarmHistory('741e1aa149524c0f9887a8d6750f67b1',
                         '09c2f5e7-9245-4b7e-bce1-01ed64a3c63d',
                         self._convert_time_string("2016-05-19T11:58:27Z"),
                         """[{
                             "dimensions": {"hostname": "devstack", "service": "monitoring"},
                             "id": "",
                             "name": "cpu.idle_perc"
                         }]""",
                         'OK',
                         'UNDETERMINED',
                         'The alarm threshold(s) have not been exceeded for the sub-alarms: '
                         'avg(cpu.idle_perc) < 10.0 times 3 with the values: [84.35]',
                         '{}',
                         """[
                             {
                                 "sub_alarm_state": "OK",
                                 "currentValues": [
                                     "84.35"
                                 ],
                                 "sub_alarm_expression": {
                                     "function": "AVG",
                                     "period": 60,
                                     "threshold": 10.0,
                                     "periods": 3,
                                     "operator": "LT",
                                     "metric_definition": {
                                         "dimensions": "{}",
                                         "id": "",
                                         "name": "cpu.idle_perc"
                                     }
                                 }
                             }
                         ]""")
        ]

        repo = cassandra_repo.MetricsRepository()
        result = repo.alarm_history('741e1aa149524c0f9887a8d6750f67b1',
                                    ['09c2f5e7-9245-4b7e-bce1-01ed64a3c63d'],
                                    None, None, None, None)

        # TODO(Cassandra) shorted out temporarily until the api is implemented in Cassandra
        self.assertNotEqual(
            [{
                u'id': u'1463659107000',
                u'time_stamp': u'2016-05-19T11:58:27.000Z',
                u'new_state': u'OK',
                u'old_state': u'UNDETERMINED',
                u'reason_data': u'{}',
                u'reason': u'The alarm threshold(s) have not been exceeded for the sub-alarms: '
                           u'avg(cpu.idle_perc) < 10.0 times 3 with the values: [84.35]',
                u'alarm_id': u'09c2f5e7-9245-4b7e-bce1-01ed64a3c63d',
                u'metrics': [{
                    u'id': u'',
                    u'name': u'cpu.idle_perc',
                    u'dimensions': {
                        u'service': u'monitoring',
                        u'hostname': u'devstack'
                    }
                }],
                u'sub_alarms': [
                    {
                        u'sub_alarm_state': u'OK',
                        u'currentValues': [
                            u'84.35'
                        ],
                        u'sub_alarm_expression': {
                            u'dimensions': u'{}',
                            u'threshold': 10.0,
                            u'periods': 3,
                            u'operator': u'LT',
                            u'period': 60,
                            u'metric_name': u'cpu.idle_perc',
                            u'function': u'AVG'
                        }
                    }
                ]
            }], result)

    @patch("monasca_api.common.repositories.cassandra."
           "metrics_repository.Cluster.connect")
    def test_check_status(self, _):
        repo = cassandra_repo.MetricsRepository()

        result = repo.check_status()

        self.assertEqual(result, (True, 'OK'))

    @patch("monasca_api.common.repositories.cassandra."
           "metrics_repository.Cluster.connect")
    def test_check_status_server_error(self, cassandra_connect_mock):
        repo = cassandra_repo.MetricsRepository()
        cassandra_connect_mock.side_effect = \
            cassandra.DriverException("Cluster is already shut down")

        result = repo.check_status()

        self.assertEqual(result, (False, 'Cluster is already shut down'))

    @staticmethod
    def _convert_time_string(date_time_string):
        dt = timeutils.parse_isotime(date_time_string)
        dt = timeutils.normalize_time(dt)
        return dt
