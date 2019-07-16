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

_DEFAULT_MAX_LOG_SIZE = 1024 * 1024

service_opts = [
    cfg.StrOpt('region',
               default=None,
               help='Region'),
    cfg.IntOpt('max_log_size',
               default=_DEFAULT_MAX_LOG_SIZE,
               help=('Refers to payload/envelope size. If either is exceeded'
                     'API will throw an error'))
]
service_group = cfg.OptGroup(name='service', title='service')


def register_opts(conf):
    conf.register_group(service_group)
    conf.register_opts(service_opts, service_group)


def list_opts():
    return service_group, service_opts
