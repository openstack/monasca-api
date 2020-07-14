# Copyright 2014 IBM Corp.
# Copyright 2016-2017 FUJITSU LIMITED
# (C) Copyright 2016-2017 Hewlett Packard Enterprise Development LP
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from oslo_config import cfg

from monasca_api.conf import types

kafka_opts = [
    cfg.ListOpt('uri',
                default=['127.0.0.1:9092'],
                item_type=types.HostAddressPortType(),
                help='Comma separated list of Kafka broker host:port'),
    cfg.StrOpt('metrics_topic', default='metrics',
               help='The topic that metrics will be published to'),
    cfg.StrOpt('events_topic', default='events',
               help='The topic that events will be published to'),
    cfg.MultiStrOpt('logs_topics', default=['log'],
                    help='The topic that logs will be published to'),
    cfg.StrOpt('alarm_state_transitions_topic',
               default='alarm-state-transitions',
               help='The topic that alarm state will be published to'),
    cfg.StrOpt('group', default='api',
               help='The group name that this service belongs to'),
    cfg.IntOpt('wait_time', default=1,
               advanced=True, min=1,
               help='The wait time when no messages on kafka queue (NOT USED)'),
    cfg.IntOpt('ack_time', default=20,
               help='The ack time back to kafka. (NOT USED)'),
    cfg.IntOpt('max_retry', default=3,
               help='Number of retries in case of connection error (NOT USED)'),
    cfg.BoolOpt('auto_commit', default=False,
                advanced=True,
                help='Whether the message is automatically committed '
                     '(NOT USED)'),
    cfg.BoolOpt('is_async', default=True,
                help='Whether posting is asynchronous or not (NOT USED)'),
    cfg.BoolOpt('compact', default=True,
                help='Specify if the message received should be parsed. If '
                     'True, message will not be parsed, otherwise messages '
                     'will be parsed (NOT USED)'),
    cfg.ListOpt('partitions', item_type=int,
                default=[0],
                help='The partitions this connection should listen for '
                     'messages on. (NOT USED)'),
    cfg.BoolOpt('drop_data', default=False,
                help='Specify if received data should be simply dropped. '
                     'This parameter is only for testing purposes. (NOT USED)'),
    cfg.BoolOpt(name='legacy_kafka_client_enabled', default=False,
                required=True, advanced=True,
                help='Enable legacy Kafka client. When set old version of '
                     'kafka-python library is used. Message format version '
                     'for the brokers should be set to 0.9.0.0 to avoid '
                     'performance issues until all consumers are upgraded.'),
    cfg.IntOpt('queue_buffering_max_messages', default=1000,
               help='The maximum number of metrics per payload sent to '
                    'Kafka. Posts to the Monasca API which exceed this will '
                    'be chunked into batches not exceeding this number.')
]

kafka_group = cfg.OptGroup(name='kafka', title='kafka')


def register_opts(conf):
    conf.register_group(kafka_group)
    conf.register_opts(kafka_opts, kafka_group)


def list_opts():
    return kafka_group, kafka_opts
