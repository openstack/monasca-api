# Copyright 2015 Cray Inc. All Rights Reserved.
# (C) Copyright 2016-2017 Hewlett Packard Enterprise Development LP
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
import unittest

from mock import patch

import monasca_api.common.repositories.cassandra.metrics_repository as cassandra_repo
import monasca_api.common.repositories.influxdb.metrics_repository as influxdb_repo

from oslo_config import cfg
from oslo_config import fixture as fixture_config
from oslo_utils import timeutils
import testtools

CONF = cfg.CONF


class TestRepoMetricsInfluxDB(unittest.TestCase):

    def setUp(self):
        super(TestRepoMetricsInfluxDB, self).setUp()

    @patch("monasca_api.common.repositories.influxdb.metrics_repository.client.InfluxDBClient")
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
                        ["2015-03-14T09:26:54Z", 4, '{"key": "value"}']
                    ]
                }
            ]
        }

        repo = influxdb_repo.MetricsRepository()
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
             ["2015-03-14T09:26:54.000Z", 4, {"key": "value"}]],
            measurements
        )

    @patch("monasca_api.common.repositories.influxdb.metrics_repository.client.InfluxDBClient")
    def test_list_metrics(self, influxdb_client_mock):
        mock_client = influxdb_client_mock.return_value
        mock_client.query.return_value.raw = {
            u'series': [{
                u'values': [[
                    u'disk.space_used_perc,_region=region,_tenant_id='
                    u'0b5e7d8c43f74430add94fba09ffd66e,device=rootfs,'
                    'hostname=host0,hosttype=native,mount_point=/',
                    u'region',
                    u'0b5e7d8c43f74430add94fba09ffd66e',
                    u'rootfs',
                    u'host0',
                    u'native',
                    u'',
                    u'/'
                ]],
                u'name': u'disk.space_used_perc',
                u'columns': [u'_key', u'_region', u'_tenant_id', u'device',
                             u'hostname', u'hosttype', u'extra', u'mount_point']
            }]
        }

        repo = influxdb_repo.MetricsRepository()

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

    @patch("monasca_api.common.repositories.influxdb.metrics_repository.client.InfluxDBClient")
    def test_list_dimension_values(self, influxdb_client_mock):
        mock_client = influxdb_client_mock.return_value
        mock_client.query.return_value.raw = {
            u'series': [
                {
                    u'values': [[u'custom_host']],
                    u'name': u'custom_metric',
                    u'columns': [u'hostname']
                }]
        }

        repo = influxdb_repo.MetricsRepository()

        result = repo.list_dimension_values(
            "38dc2a2549f94d2e9a4fa1cc45a4970c",
            "useast",
            "custom_metric",
            "hostname")

        self.assertEqual(result, [{u'dimension_value': u'custom_host'}])

    @patch("monasca_api.common.repositories.influxdb.metrics_repository.client.InfluxDBClient")
    def test_list_dimension_names(self, influxdb_client_mock):
        mock_client = influxdb_client_mock.return_value
        mock_client.query.return_value.raw = {
            u'series': [{
                u'values': [[u'_region'], [u'_tenant_id'], [u'hostname'],
                            [u'service']],
                u'name': u'custom_metric',
                u'columns': [u'tagKey']
            }]
        }

        repo = influxdb_repo.MetricsRepository()

        result = repo.list_dimension_names(
            "38dc2a2549f94d2e9a4fa1cc45a4970c",
            "useast",
            "custom_metric")

        self.assertEqual(result,
                         [
                             {u'dimension_name': u'hostname'},
                             {u'dimension_name': u'service'}
                         ])


class TestRepoMetricsCassandra(testtools.TestCase):

    def setUp(self):
        super(TestRepoMetricsCassandra, self).setUp()

        self._fixture_config = self.useFixture(
            fixture_config.Config(cfg.CONF))
        self._fixture_config.config(cluster_ip_addresses='127.0.0.1',
                                    group='cassandra')

    @patch("monasca_api.common.repositories.cassandra.metrics_repository.Cluster.connect")
    def test_list_metrics(self, cassandra_connect_mock):
        cassandra_session_mock = cassandra_connect_mock.return_value
        cassandra_session_mock.execute.return_value = [[
            "0b5e7d8c43f74430add94fba09ffd66e",
            "region",
            binascii.unhexlify(b"01d39f19798ed27bbf458300bf843edd17654614"),
            {
                "__name__": "disk.space_used_perc",
                "device": "rootfs",
                "hostname": "host0",
                "hosttype": "native",
                "mount_point": "/",
            }
        ]]

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
            u'id': u'01d39f19798ed27bbf458300bf843edd17654614',
            u'name': u'disk.space_used_perc',
            u'dimensions': {
                u'device': u'rootfs',
                u'hostname': u'host0',
                u'mount_point': u'/',
                u'hosttype': u'native'
            }}], result)

    @patch("monasca_api.common.repositories.cassandra.metrics_repository.Cluster.connect")
    def test_list_metric_names(self, cassandra_connect_mock):

        Metric_map = namedtuple('Metric_map', 'metric_map')

        cassandra_session_mock = cassandra_connect_mock.return_value
        cassandra_session_mock.execute.return_value = [
            Metric_map(
                {
                    "__name__": "disk.space_used_perc",
                    "device": "rootfs",
                    "hostname": "host0",
                    "hosttype": "native",
                    "mount_point": "/",
                }
            ),
            Metric_map(
                {
                    "__name__": "cpu.idle_perc",
                    "hostname": "host0",
                    "service": "monitoring"
                }
            )
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

    @patch("monasca_api.common.repositories.cassandra.metrics_repository.Cluster.connect")
    def test_measurement_list(self, cassandra_connect_mock):

        Measurement = namedtuple('Measurement', 'time_stamp value value_meta')

        cassandra_session_mock = cassandra_connect_mock.return_value
        cassandra_session_mock.execute.side_effect = [
            [[
                "0b5e7d8c43f74430add94fba09ffd66e",
                "region",
                binascii.unhexlify(b"01d39f19798ed27bbf458300bf843edd17654614"),
                {
                    "__name__": "disk.space_used_perc",
                    "device": "rootfs",
                    "hostname": "host0",
                    "hosttype": "native",
                    "mount_point": "/",
                    "service": "monitoring",
                }
            ]],
            [
                Measurement(self._convert_time_string("2015-03-14T09:26:53.59Z"), 2, None),
                Measurement(self._convert_time_string("2015-03-14T09:26:53.591Z"), 2.5, ''),
                Measurement(self._convert_time_string("2015-03-14T09:26:53.6Z"), 4.0, '{}'),
                Measurement(self._convert_time_string("2015-03-14T09:26:54Z"), 4,
                            '{"key": "value"}'),
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
            limit=1,
            merge_metrics_flag=True)

        self.assertEqual(len(result), 1)
        self.assertIsNone(result[0]['dimensions'])
        self.assertEqual(result[0]['name'], 'disk.space_used_perc')
        self.assertEqual(result[0]['columns'],
                         ['timestamp', 'value', 'value_meta'])

        measurements = result[0]['measurements']

        self.assertEqual(
            [["2015-03-14T09:26:53.590Z", 2, {}],
             ["2015-03-14T09:26:53.591Z", 2.5, {}],
             ["2015-03-14T09:26:53.600Z", 4.0, {}],
             ["2015-03-14T09:26:54.000Z", 4, {"key": "value"}]],
            measurements
        )

    @patch("monasca_api.common.repositories.cassandra.metrics_repository.Cluster.connect")
    def test_metrics_statistics(self, cassandra_connect_mock):

        Measurement = namedtuple('Measurement', 'time_stamp value value_meta')

        cassandra_session_mock = cassandra_connect_mock.return_value
        cassandra_session_mock.execute.side_effect = [
            [[
                "0b5e7d8c43f74430add94fba09ffd66e",
                "region",
                binascii.unhexlify(b"01d39f19798ed27bbf458300bf843edd17654614"),
                {
                    "__name__": "cpu.idle_perc",
                    "hostname": "host0",
                    "service": "monitoring",
                }
            ]],
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
            merge_metrics_flag=True)

        self.assertEqual([
            {
                u'dimensions': None,
                u'statistics': [[u'2016-05-19T11:58:24Z', 95.5, 94, 97, 4, 382]],
                u'name': u'cpu.idle_perc',
                u'columns': [u'timestamp', u'avg', u'min', u'max', u'count', u'sum'],
                u'id': u'2016-05-19T11:58:24Z'
            }
        ], result)

    @patch("monasca_api.common.repositories.cassandra.metrics_repository.Cluster.connect")
    def test_alarm_history(self, cassandra_connect_mock):

        AlarmHistory = namedtuple('AlarmHistory', 'alarm_id, time_stamp, metrics, '
                                                  'new_state, old_state, reason, '
                                                  'reason_data, sub_alarms, tenant_id')

        cassandra_session_mock = cassandra_connect_mock.return_value
        cassandra_session_mock.execute.return_value = [
            AlarmHistory('09c2f5e7-9245-4b7e-bce1-01ed64a3c63d',
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
                         ]""",
                         '741e1aa149524c0f9887a8d6750f67b1')
        ]

        repo = cassandra_repo.MetricsRepository()
        result = repo.alarm_history('741e1aa149524c0f9887a8d6750f67b1',
                                    ['09c2f5e7-9245-4b7e-bce1-01ed64a3c63d'],
                                    None, None)
        self.assertEqual(
            [{
                u'id': u'1463659107000',
                u'timestamp': u'2016-05-19T11:58:27.000Z',
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

    @staticmethod
    def _convert_time_string(date_time_string):
        dt = timeutils.parse_isotime(date_time_string)
        dt = timeutils.normalize_time(dt)
        return dt
