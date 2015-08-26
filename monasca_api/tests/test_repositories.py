# Copyright 2015 Cray Ltd.
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
    def test_summat(self, influxdb_client_mock):
        mock_client = influxdb_client_mock.return_value
        mock_client.query.return_value.raw = {
            "results": [{
                "series": [
                    {
                        "name": "dummy.series",
                        "values": [
                            [1, 2, None],
                            [2, 2.5, ''],
                            [3, 4.0, '{}'],
                            [4, 4, '{"key": "value"}']
                        ]
                    }
                ]
            }]
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
            [[1, 2, {}], [2, 2.5, {}], [3, 4.0, {}], [4, 4, {"key": "value"}]],
            measurements
        )
