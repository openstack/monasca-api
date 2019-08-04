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

influxdb_opts = [
    cfg.StrOpt('database_name', default='mon',
               help='''
Database name where metrics are stored
'''),
    cfg.BoolOpt('db_per_tenant', default=False,
                help='''
Whether to use a separate database per tenant
'''),
    cfg.HostAddressOpt('ip_address', default='127.0.0.1',
                       help='''
IP address to Influxdb server
'''),
    cfg.PortOpt('port', default=8086,
                help='Port to Influxdb server'),
    cfg.StrOpt('user', required=True,
               sample_default='monasca-api', help='''
Influxdb user
'''),
    cfg.StrOpt('password', secret=True, sample_default='password',
               help='''
Influxdb password
''')
]

influxdb_group = cfg.OptGroup(name='influxdb', title='influxdb')


def register_opts(conf):
    conf.register_group(influxdb_group)
    conf.register_opts(influxdb_opts, influxdb_group)


def list_opts():
    return influxdb_group, influxdb_opts
