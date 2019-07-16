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

_DEFAULT_HOST = '127.0.0.1'
_DEFAULT_PORT = 8125
_DEFAULT_BUFFER_SIZE = 50

monitoring_opts = [
    cfg.BoolOpt('enable',
                default=True,
                help='Determine if self monitoring is enabled'),
    cfg.HostAddressOpt('statsd_host',
                       default=_DEFAULT_HOST,
                       help=('IP address or host domain name of statsd server, default to %s'
                             % _DEFAULT_HOST)),
    cfg.PortOpt('statsd_port',
                default=_DEFAULT_PORT,
                help='Port of statsd server, default to %d' % _DEFAULT_PORT),
    cfg.IntOpt('statsd_buffer',
               default=_DEFAULT_BUFFER_SIZE,
               required=True,
               help=('Maximum number of metric to buffer before sending, '
                     'default to %d' % _DEFAULT_BUFFER_SIZE)),
    cfg.DictOpt('dimensions', default={},
                required=False, help='Additional dimensions that can be set')
]
monitoring_group = cfg.OptGroup(name='monitoring', title='monitoring')


def register_opts(conf):
    conf.register_group(monitoring_group)
    conf.register_opts(monitoring_opts, monitoring_group)


def list_opts():
    return monitoring_group, monitoring_opts
