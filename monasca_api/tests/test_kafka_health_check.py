# Copyright 2017 FUJITSU LIMITED
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
from oslo_config import fixture as oo_cfg
from oslotest import base

from monasca_common.kafka_lib import client

from monasca_api.healthcheck import kafka_check as kc
from monasca_api.v2.reference import cfg

CONF = cfg.CONF


class TestKafkaHealthCheckLogic(base.BaseTestCase):

    mock_kafka_url = 'localhost:1234'
    mocked_topics = 'test1'
    mocked_event_topic = 'test2'
    mocked_alarm_state_topic = 'test3'
    mocked_config = {
        'uri': mock_kafka_url,
        'metrics_topic': mocked_topics,
        'events_topic': mocked_event_topic,
        'alarm_state_transitions_topic': mocked_alarm_state_topic
    }

    def __init__(self, *args, **kwargs):
        super(TestKafkaHealthCheckLogic, self).__init__(*args, **kwargs)
        self._conf = None

    def setUp(self):
        super(TestKafkaHealthCheckLogic, self).setUp()
        self._conf = self.useFixture(oo_cfg.Config(CONF))
        self._conf.config(group='kafka', **self.mocked_config)

    @mock.patch('monasca_api.healthcheck.kafka_check.client.KafkaClient')
    def test_should_fail_kafka_unavailable(self, kafka_client):
        kafka = mock.Mock()
        kafka_client.side_effect = client.KafkaUnavailableError()
        kafka_client.return_value = kafka

        kafka_health = kc.KafkaHealthCheck()
        result = kafka_health.health_check()

        self.assertFalse(result.healthy)
        kafka.close.assert_not_called()

    @mock.patch('monasca_api.healthcheck.kafka_check.client.KafkaClient')
    def test_should_fail_missing_topic(self, kafka_client):
        kafka = mock.Mock()
        kafka.topic_partitions = ['topic1']
        kafka_client.return_value = kafka

        kafka_health = kc.KafkaHealthCheck()
        result = kafka_health.health_check()

        self.assertFalse(result.healthy)
        kafka.close.assert_called_once()

    @mock.patch('monasca_api.healthcheck.kafka_check.client.KafkaClient')
    def test_should_pass(self, kafka_client):
        kafka = mock.Mock()
        kafka.topic_partitions = (self.mocked_topics,
                                  self.mocked_event_topic,
                                  self.mocked_alarm_state_topic)
        kafka_client.return_value = kafka

        kafka_health = kc.KafkaHealthCheck()
        result = kafka_health.health_check()

        self.assertTrue(result.healthy)
        kafka.close.assert_called_once()
