# Copyright 2016-2017 FUJITSU LIMITED
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

import mock

from monasca_log_api.monitoring import client
from monasca_log_api.tests import base


class TestMonitoring(base.BaseTestCase):

    @mock.patch('monasca_log_api.monitoring.client.monascastatsd')
    def test_should_use_default_dimensions_if_none_specified(self,
                                                             monascastatsd):
        client.get_client()

        statsd_client = monascastatsd.Client

        expected_dimensions = client._DEFAULT_DIMENSIONS
        actual_dimensions = statsd_client.call_args[1]['dimensions']

        self.assertEqual(1, statsd_client.call_count)
        self.assertEqual(expected_dimensions, actual_dimensions)

    @mock.patch('monasca_log_api.monitoring.client.monascastatsd')
    def test_should_not_override_fixed_dimensions(self,
                                                  monascastatsd):
        dims = {
            'service': 'foo',
            'component': 'bar'
        }

        client.get_client(dims)

        statsd_client = monascastatsd.Client

        expected_dimensions = client._DEFAULT_DIMENSIONS
        actual_dimensions = statsd_client.call_args[1]['dimensions']

        self.assertEqual(1, statsd_client.call_count)
        self.assertEqual(expected_dimensions, actual_dimensions)
