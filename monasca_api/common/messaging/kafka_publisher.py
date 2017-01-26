# Copyright 2014,2017 Hewlett-Packard
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

from monasca_api.common.messaging import exceptions
from monasca_api.common.messaging import publisher

import monasca_common.kafka.producer as kafka_producer
import monasca_common.kafka_lib.common as kafka_common

LOG = log.getLogger(__name__)


class KafkaPublisher(publisher.Publisher):
    def __init__(self, topic):
        if not cfg.CONF.kafka.uri:
            raise Exception('Kafka is not configured correctly! '
                            'Use configuration file to specify Kafka '
                            'uri, for example: '
                            'uri=192.168.1.191:9092')

        self.uri = cfg.CONF.kafka.uri
        self.topic = topic
        self.group = cfg.CONF.kafka.group
        self.wait_time = cfg.CONF.kafka.wait_time
        self.async = cfg.CONF.kafka.async
        self.ack_time = cfg.CONF.kafka.ack_time
        self.max_retry = cfg.CONF.kafka.max_retry
        self.auto_commit = cfg.CONF.kafka.auto_commit
        self.compact = cfg.CONF.kafka.compact
        self.partitions = cfg.CONF.kafka.partitions
        self.drop_data = cfg.CONF.kafka.drop_data

        self._producer = kafka_producer.KafkaProducer(self.uri)

    def close(self):
        pass

    def send_message(self, message):
        try:
            self._producer.publish(self.topic, message)

        except (kafka_common.KafkaUnavailableError,
                kafka_common.LeaderNotAvailableError):
            LOG.exception('Error occurred while posting data to Kafka.')
            raise exceptions.MessageQueueException()
        except Exception:
            LOG.exception('Unknown error.')
            raise exceptions.MessageQueueException()
