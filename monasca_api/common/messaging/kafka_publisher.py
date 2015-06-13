# Copyright 2014 Hewlett-Packard
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

import time

from kafka import client
from kafka import common
from kafka import producer
from oslo_config import cfg
from oslo_log import log

from monasca_api.common.messaging import exceptions
from monasca_api.common.messaging import publisher

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

        self._client = None
        self._producer = None

    def _init_client(self, wait_time=None):
        for i in range(self.max_retry):
            try:
                # if there is a client instance, but _init_client is called
                # again, most likely the connection has gone stale, close that
                # connection and reconnect.
                if self._client:
                    self._client.close()

                if not wait_time:
                    wait_time = self.wait_time
                time.sleep(wait_time)

                self._client = client.KafkaClient(self.uri)

                # when a client is re-initialized, existing consumer should be
                # reset as well.
                self._producer = None
                break
            except common.KafkaUnavailableError:
                LOG.error('Kafka server at %s is down.' % self.uri)
            except common.LeaderNotAvailableError:
                LOG.error('Kafka at %s has no leader available.' % self.uri)
            except Exception:
                LOG.error('Kafka at %s initialization failed.' % self.uri)

            # Wait a bit and try again to get a client
            time.sleep(self.wait_time)

    def _init_producer(self):
        try:
            if not self._client:
                self._init_client()
            self._producer = producer.SimpleProducer(
                self._client, async=self.async, ack_timeout=self.ack_time)
            LOG.debug('Kafka SimpleProducer was created successfully.')
        except Exception:
            self._producer = None
            LOG.exception('Kafka (%s) producer can not be created.' % self.uri)

    def close(self):
        if self._client:
            self._producer = None
            self._client.close()

    def send_message(self, message):
        try:
            if not self._producer:
                self._init_producer()
            self._producer.send_messages(self.topic, message)

        except (common.KafkaUnavailableError,
                common.LeaderNotAvailableError):
            self._client = None
            LOG.exception('Error occurred while posting data to Kafka.')
            raise exceptions.MessageQueueException()
        except Exception:
            LOG.exception('Unknown error.')
            raise exceptions.MessageQueueException()

    def send_message_batch(self, messages):
        try:
            if not self._producer:
                self._init_producer()
            self._producer.send_messages(self.topic, *messages)
        except (common.KafkaUnavailableError,
                common.LeaderNotAvailableError):
            self._client = None
            LOG.exception('Error occurred while posting data to Kafka.')
            raise exceptions.MessageQueueException()
        except Exception:
            LOG.exception('Unknown error.')
            raise exceptions.MessageQueueException()
