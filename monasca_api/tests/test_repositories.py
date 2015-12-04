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

import unittest

from mock import patch
import monasca_api.common.repositories.influxdb.metrics_repository as influxdb_repo

from oslo_config import cfg

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
            merge_metrics_flag=True)

        self.assertEqual(len(result), 1)
        self.assertEqual(result[0]['dimensions'], None)
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
