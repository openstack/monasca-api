# Copyright 2015 kornicameister@gmail.com
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

import copy
import datetime
import random
from unittest import mock

from oslo_config import cfg
from oslo_log import log
import simplejson as json
import six
import unittest

from monasca_api.api.core.log import log_publisher
from monasca_api.api.core.log import model
from monasca_api.tests import base


LOG = log.getLogger(__name__)
EPOCH_START = datetime.datetime(1970, 1, 1)


class TestSendMessage(base.BaseTestCase):

    @mock.patch('monasca_api.api.core.log.log_publisher.client_factory'
                '.get_kafka_producer')
    def test_should_not_send_empty_message(self, _):
        instance = log_publisher.LogPublisher()

        instance._kafka_publisher = mock.Mock()
        instance.send_message({})

        self.assertFalse(instance._kafka_publisher.publish.called)

    @unittest.expectedFailure
    def test_should_not_send_message_not_dict(self):
        instance = log_publisher.LogPublisher()
        not_dict_value = 123
        instance.send_message(not_dict_value)

    @mock.patch('monasca_api.api.core.log.log_publisher.client_factory'
                '.get_kafka_producer')
    def test_should_not_send_message_missing_keys(self, _):
        # checks every combination of missing keys
        # test does not rely on those keys having a value or not,
        # it simply assumes that values are set but important
        # message (i.e. envelope) properties are missing entirely
        # that's why there are two loops instead of three

        instance = log_publisher.LogPublisher()
        keys = ['log', 'creation_time', 'meta']

        for key_1 in keys:
            diff = keys[:]
            diff.remove(key_1)
            for key_2 in diff:
                message = {
                    key_1: random.randint(10, 20),
                    key_2: random.randint(30, 50)
                }
                self.assertRaises(log_publisher.InvalidMessageException,
                                  instance.send_message,
                                  message)

    @mock.patch('monasca_api.api.core.log.log_publisher.client_factory'
                '.get_kafka_producer')
    def test_should_not_send_message_missing_values(self, _):
        # original message assumes that every property has value
        # test modify each property one by one by removing that value
        # (i.e. creating false-like value)
        instance = log_publisher.LogPublisher()
        message = {
            'log': {
                'message': '11'
            },
            'creation_time': 123456,
            'meta': {
                'region': 'pl'
            }
        }

        for key in message:
            tmp_message = message
            tmp_message[key] = None
            self.assertRaises(log_publisher.InvalidMessageException,
                              instance.send_message,
                              tmp_message)

    @mock.patch('monasca_api.api.core.log.log_publisher.client_factory'
                '.get_kafka_producer')
    def test_should_send_message(self, kafka_producer):
        instance = log_publisher.LogPublisher()
        instance._kafka_publisher = kafka_producer
        instance.send_message({})

        creation_time = ((datetime.datetime.utcnow() - EPOCH_START)
                         .total_seconds())
        application_type = 'monasca-log-api'
        dimension_1_name = 'disk_usage'
        dimension_1_value = '50'
        dimension_2_name = 'cpu_time'
        dimension_2_value = '60'

        msg = model.Envelope(
            log={
                'message': '1',
                'application_type': application_type,
                'dimensions': {
                    dimension_1_name: dimension_1_value,
                    dimension_2_name: dimension_2_value
                }
            },
            meta={
                'tenantId': '1'
            }
        )
        msg['creation_time'] = creation_time
        instance.send_message(msg)

        instance._kafka_publisher.publish.assert_called_once_with(
            cfg.CONF.kafka.logs_topics[0],
            [json.dumps(msg, ensure_ascii=False).encode('utf-8')])

    @mock.patch('monasca_api.api.core.log.log_publisher.client_factory'
                '.get_kafka_producer')
    def test_should_send_message_multiple_topics(self, _):
        topics = ['logs_topics', 'analyzer', 'tester']
        self.conf_override(logs_topics=topics,
                           group='kafka')
        self.conf_override(max_message_size=5000,
                           group='log_publisher')

        instance = log_publisher.LogPublisher()
        instance._kafka_publisher = mock.Mock()
        instance.send_message({})

        creation_time = ((datetime.datetime.utcnow() - EPOCH_START)
                         .total_seconds())
        dimension_1_name = 'disk_usage'
        dimension_1_value = '50'
        dimension_2_name = 'cpu_time'
        dimension_2_value = '60'
        application_type = 'monasca-log-api'
        msg = model.Envelope(
            log={
                'message': '1',
                'application_type': application_type,
                'dimensions': {
                    dimension_1_name: dimension_1_value,
                    dimension_2_name: dimension_2_value
                }
            },
            meta={
                'tenantId': '1'
            }
        )
        msg['creation_time'] = creation_time
        json_msg = json.dumps(msg, ensure_ascii=False)

        instance.send_message(msg)

        self.assertEqual(len(topics),
                         instance._kafka_publisher.publish.call_count)
        for topic in topics:
            instance._kafka_publisher.publish.assert_any_call(
                topic,
                [json_msg.encode('utf-8')])

    @mock.patch('monasca_api.api.core.log.log_publisher.client_factory'
                '.get_kafka_producer')
    def test_should_send_unicode_message(self, kp):
        instance = log_publisher.LogPublisher()
        instance._kafka_publisher = kp

        for um in base.UNICODE_MESSAGES:
            case, msg = um.values()
            try:
                envelope = model.Envelope(
                    log={
                        'message': msg,
                        'application_type': 'test',
                        'dimensions': {
                            'test': 'test_log_publisher',
                            'case': 'test_should_send_unicode_message'
                        }
                    },
                    meta={
                        'tenantId': 1
                    }
                )
                instance.send_message(envelope)

                expected_message = json.dumps(envelope, ensure_ascii=False)

                if six.PY3:
                    expected_message = expected_message.encode('utf-8')

                instance._kafka_publisher.publish.assert_called_with(
                    cfg.CONF.kafka.logs_topics[0],
                    [expected_message]
                )
            except Exception:
                LOG.exception('Failed to evaluate unicode case %s', case)
                raise


@mock.patch('monasca_api.api.core.log.log_publisher.client_factory'
            '.get_kafka_producer')
class TestTruncation(base.BaseTestCase):
    EXTRA_CHARS_SIZE = len(bytearray(json.dumps({
        'log': {
            'message': None
        }
    }), 'utf8')) - 2

    def test_should_not_truncate_message_if_size_is_smaller(self, _):
        diff_size = random.randint(1, 100)
        self._run_truncate_test(log_size_factor=-diff_size,
                                truncate_by=0)

    def test_should_not_truncate_message_if_size_equal_to_max(self, _):
        self._run_truncate_test(log_size_factor=0,
                                truncate_by=0)

    def test_should_truncate_too_big_message(self, _):
        diff_size = random.randint(1, 100)
        max_size = 1000
        truncate_by = ((max_size -
                        (max_size - log_publisher._TRUNCATED_PROPERTY_SIZE)) +
                       log_publisher._TRUNCATION_SAFE_OFFSET + diff_size)
        self._run_truncate_test(max_message_size=1000,
                                log_size_factor=diff_size,
                                truncate_by=truncate_by)

    def _run_truncate_test(self,
                           max_message_size=1000,
                           log_size_factor=0,
                           truncate_by=0,
                           gen_fn=base.generate_unique_message):

        log_size = (max_message_size -
                    TestTruncation.EXTRA_CHARS_SIZE -
                    log_publisher._KAFKA_META_DATA_SIZE -
                    log_publisher._TIMESTAMP_KEY_SIZE +
                    log_size_factor)

        expected_log_message_size = log_size - truncate_by

        self.conf_override(
            group='log_publisher',
            max_message_size=max_message_size
        )

        log_msg = gen_fn(log_size)
        envelope = {
            'log': {
                'message': log_msg
            }
        }

        instance = log_publisher.LogPublisher()

        envelope_copy = copy.deepcopy(envelope)
        json_envelope = instance._truncate(envelope_copy)

        parsed_envelope = json.loads(json_envelope)

        parsed_log_message = parsed_envelope['log']['message']
        parsed_log_message_len = len(parsed_log_message)

        if truncate_by > 0:
            self.assertNotEqual(envelope['log']['message'],
                                parsed_log_message)
        else:
            self.assertEqual(envelope['log']['message'],
                             parsed_log_message)

        self.assertEqual(expected_log_message_size, parsed_log_message_len)
