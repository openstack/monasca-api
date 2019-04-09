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

import collections
from monasca_common.kafka_lib import client
from monasca_log_api import conf
from oslo_log import log
from six import PY3


LOG = log.getLogger(__name__)
CONF = conf.CONF


CheckResult = collections.namedtuple('CheckResult', ['healthy', 'message'])
"""Result from the healthcheck, contains healthy(boolean) and message"""


# TODO(feature) monasca-common candidate
class KafkaHealthCheck(object):
    """Evaluates kafka health

    Healthcheck verifies if:

    * kafka server is up and running
    * there is a configured topic in kafka

    If following conditions are met healthcheck returns healthy status.
    Otherwise unhealthy status is returned with explanation.

     Example of middleware configuration:

    .. code-block:: ini

      [kafka_healthcheck]
      kafka_url = localhost:8900
      kafka_topics = log

    Note:
        It is possible to specify multiple topics if necessary.
        Just separate them with ,

    """

    def healthcheck(self):
        url = CONF.kafka_healthcheck.kafka_url

        try:
            kafka_client = client.KafkaClient(hosts=url)
        except client.KafkaUnavailableError as ex:
            LOG.error(repr(ex))
            error_str = 'Could not connect to kafka at %s' % url
            return CheckResult(healthy=False, message=error_str)

        result = self._verify_topics(kafka_client)
        self._disconnect_gracefully(kafka_client)

        return result

    # noinspection PyMethodMayBeStatic
    def _verify_topics(self, kafka_client):
        topics = CONF.kafka_healthcheck.kafka_topics

        if PY3:
            topics = tuple(topic.encode('utf-8') for topic in topics)

        for t in topics:
            # kafka client loads metadata for topics as fast
            # as possible (happens in __init__), therefore this
            # topic_partitions is sure to be filled
            for_topic = t in kafka_client.topic_partitions
            if not for_topic:
                error_str = 'Kafka: Topic %s not found' % t
                LOG.error(error_str)
                return CheckResult(healthy=False, message=error_str)

        return CheckResult(healthy=True, message='OK')

    # noinspection PyMethodMayBeStatic
    def _disconnect_gracefully(self, kafka_client):
        # at this point, client is connected so it must be closed
        # regardless of topic existence
        try:
            kafka_client.close()
        except Exception as ex:
            # log that something went wrong and move on
            LOG.error(repr(ex))
