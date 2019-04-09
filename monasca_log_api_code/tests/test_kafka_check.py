# Copyright 2015-2017 FUJITSU LIMITED
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

from monasca_common.kafka_lib import client

from monasca_log_api.healthcheck import kafka_check as kc
from monasca_log_api.tests import base


class KafkaCheckLogicTest(base.BaseTestCase):

    mock_kafka_url = 'localhost:1234'
    mocked_topics = ['test_1', 'test_2']
    mock_config = {
        'kafka_url': mock_kafka_url,
        'kafka_topics': mocked_topics
    }

    def setUp(self):
        super(KafkaCheckLogicTest, self).setUp()
        self.conf_default(group='kafka_healthcheck', **self.mock_config)

    @mock.patch('monasca_log_api.healthcheck.kafka_check.client.KafkaClient')
    def test_should_fail_kafka_unavailable(self, kafka_client):
        kafka_client.side_effect = client.KafkaUnavailableError()
        kafka_health = kc.KafkaHealthCheck()
        result = kafka_health.healthcheck()

        self.assertFalse(result.healthy)

    @mock.patch('monasca_log_api.healthcheck.kafka_check.client.KafkaClient')
    def test_should_fail_topic_missing(self, kafka_client):
        kafka = mock.Mock()
        kafka.topic_partitions = [self.mocked_topics[0]]
        kafka_client.return_value = kafka

        kafka_health = kc.KafkaHealthCheck()
        result = kafka_health.healthcheck()

        # verify result
        self.assertFalse(result.healthy)

        # ensure client was closed
        self.assertTrue(kafka.close.called)

    @mock.patch('monasca_log_api.healthcheck.kafka_check.client.KafkaClient')
    def test_should_pass(self, kafka_client):
        kafka = mock.Mock()
        kafka.topic_partitions = self.mocked_topics
        kafka_client.return_value = kafka

        kafka_health = kc.KafkaHealthCheck()
        result = kafka_health.healthcheck()

        self.assertTrue(result)

        # ensure client was closed
        self.assertTrue(kafka.close.called)
