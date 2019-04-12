# Copyright 2014 IBM Corp.
# Copyright 2016-2017 FUJITSU LIMITED
# (C) Copyright 2016-2017 Hewlett Packard Enterprise Development LP
# (C) Copyright 2017-2018 SUSE LLC
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
from oslo_config import types

cassandra_opts = [
    cfg.ListOpt('contact_points',
                default=['127.0.0.1'],
                item_type=types.HostAddress(),
                help='''
Comma separated list of Cassandra node IP addresses
'''),
    cfg.PortOpt('port', default=9042,
                help='''
Cassandra port number
'''),
    cfg.StrOpt('keyspace', default='monasca',
               help='''
keyspace where metric are stored
'''),
    cfg.StrOpt('user', default='',
               help='''
Cassandra user for monasca-api service
'''),
    cfg.StrOpt('password', default='', secret=True,
               help='''
Cassandra user password for monasca-api service
'''),
    cfg.IntOpt('connection_timeout', default=5,
               help='''
Cassandra timeout in seconds when creating a new connection
'''),
    cfg.StrOpt('local_data_center', default='',
               help='''
Cassandra local data center name
''')
]

cassandra_group = cfg.OptGroup(name='cassandra')


def register_opts(conf):
    conf.register_group(cassandra_group)
    conf.register_opts(cassandra_opts, cassandra_group)


def list_opts():
    return cassandra_group, cassandra_opts
