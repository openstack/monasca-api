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

_MAX_MESSAGE_SIZE = 1048576
_DEFAULT_MAX_LOG_SIZE = 1024 * 1024

log_publisher_opts = [
    cfg.IntOpt('max_message_size',
               default=_MAX_MESSAGE_SIZE,
               required=True,
               help='''
Message max size that can be sent to kafka, default to %d bytes
''' % _MAX_MESSAGE_SIZE),
    cfg.StrOpt('region',
               default='Region;',
               help='''
Region
'''),
    cfg.IntOpt('max_log_size',
               default=_DEFAULT_MAX_LOG_SIZE,
               help='''
Refers to payload/envelope size. If either is exceeded API will throw an error
''')
]

log_publisher_group = cfg.OptGroup(name='log_publisher', title='log_publisher')


def register_opts(conf):
    conf.register_group(log_publisher_group)
    conf.register_opts(log_publisher_opts, log_publisher_group)


def list_opts():
    return log_publisher_group, log_publisher_opts
