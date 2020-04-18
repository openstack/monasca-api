# Copyright 2018 Fujitsu LIMITED
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
from unittest import mock

from monasca_api.common.messaging.message_formats.metrics import transform
from monasca_api.tests import base


class TestMessageFormats(base.BaseTestCase):
    @mock.patch('oslo_utils.timeutils.utcnow_ts')
    def test_single_metrics(self, time):
        time.return_value = 514862580
        tenant_id = 222
        region = 'default'
        metrics = 'example.test'
        expected_metrics = {'metric': 'example.test',
                            'creation_time': 514862580,
                            'meta':
                                {'region': 'default',
                                 'tenantId': 222}}
        transformed_metric = transform(metrics, tenant_id, region)
        self.assertIsInstance(transformed_metric, list)
        self.assertEqual(len(transformed_metric), 1)
        self.assertEqual(expected_metrics, json.loads(transformed_metric[0]))

    @mock.patch('oslo_utils.timeutils.utcnow_ts')
    def test_multiple_metrics(self, time):
        time.return_value = 514862580
        tenant_id = 222
        region = 'default'
        metrics = ['example.test1', 'example.test2']
        expected_metrics = []
        for metric in metrics:
            expected_metrics.append({'metric': metric,
                                     'creation_time': 514862580,
                                     'meta':
                                         {'region': 'default',
                                          'tenantId': 222}})
        transformed_metrics = transform(metrics, tenant_id, region)
        self.assertIsInstance(transformed_metrics, list)
        self.assertEqual(len(transformed_metrics), len(metrics))
        for transformed_metric in transformed_metrics:
            self.assertIn(json.loads(transformed_metric), expected_metrics)
