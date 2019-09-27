# Copyright 2014 IBM Corp.
# Copyright 2016-2017 FUJITSU LIMITED
# (C) Copyright 2016-2017 Hewlett Packard Enterprise Development LP
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

security_opts = [
    cfg.ListOpt('healthcheck_roles', default=['@'],
                help='''
Roles that are allowed to check the health
'''),
    cfg.ListOpt('versions_roles', default=['@'],
                help='''
Roles that are allowed to check the versions
'''),
    cfg.ListOpt('default_authorized_roles', default=['monasca-user'],
                help='''
Roles that are allowed full access to the API
'''),
    cfg.ListOpt('agent_authorized_roles', default=['monasca-agent'],
                help='''
Roles that are only allowed to POST to the API
'''),
    cfg.ListOpt('read_only_authorized_roles',
                default=['monasca-read-only-user'],
                help='''
Roles that are only allowed to GET from the API
'''),
    cfg.ListOpt('delegate_authorized_roles', default=['admin'],
                help='''
Roles that are allowed to POST metrics on behalf of another tenant
''')
]

security_group = cfg.OptGroup(name='security', title='security')


def register_opts(conf):
    conf.register_group(security_group)
    conf.register_opts(security_opts, security_group)


def list_opts():
    return security_group, security_opts
