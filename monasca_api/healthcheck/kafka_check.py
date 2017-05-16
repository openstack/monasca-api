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

from oslo_config import cfg
from oslo_log import log

from monasca_api.healthcheck import base
from monasca_common.kafka_lib import client

LOG = log.getLogger(__name__)
CONF = cfg.CONF


class KafkaHealthCheck(base.BaseHealthCheck):
    """Evaluates kafka health

    Healthcheck verifies if:

    * kafka server is up and running
    * there is a configured topic in kafka

    If following conditions are met health check returns healthy status.
    Otherwise unhealthy status is returned with message.

    Note:
        Healthcheck checks 3 type of topics given in configuration:
        metrics_topic, events_topic and alarm_state_transition_topic.
    """

    def health_check(self):
        url = CONF.kafka.uri

        try:
            kafka_client = client.KafkaClient(hosts=url)
        except client.KafkaUnavailableError as ex:
            LOG.error(repr(ex))
            error_str = 'Could not connect to Kafka at {0}'.format(url)
            return base.CheckResult(healthy=False, message=error_str)

        status = self._verify_topics(kafka_client)
        self._disconnect_gracefully(kafka_client)

        return base.CheckResult(healthy=status[0],
                                message=status[1])

    @staticmethod
    def _verify_topics(kafka_client):
        topics = (CONF.kafka.metrics_topic,
                  CONF.kafka.events_topic,
                  CONF.kafka.alarm_state_transitions_topic)

        for topic in topics:
            for_topic = topic in kafka_client.topic_partitions
            if not for_topic:
                error_str = 'Kafka: Topic {0} not found'.format(for_topic)
                LOG.error(error_str)
                return False, str(error_str)
        return True, 'OK'

    @staticmethod
    def _disconnect_gracefully(kafka_client):
        try:
            kafka_client.close()
        except Exception:
            LOG.exception('Closing Kafka Connection')
