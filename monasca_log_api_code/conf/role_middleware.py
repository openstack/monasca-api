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

role_m_opts = [
    cfg.ListOpt(name='path',
                default='/',
                help='List of paths where middleware applies to'),
    cfg.ListOpt(name='default_roles',
                default=['monasca-user'],
                help='List of roles allowed to enter api'),
    cfg.ListOpt(name='agent_roles',
                default=None,
                help=('List of roles, that if set, mean that request '
                      'comes from agent, thus is authorized in the same '
                      'time')),
    cfg.ListOpt(name='delegate_roles',
                default=['admin'],
                help=('Roles that are allowed to POST logs on '
                      'behalf of another tenant (project)')),
    cfg.ListOpt(name='check_roles',
                default=['@'],
                help=('Roles that are allowed to do check  '
                      'version and health'))
]
role_m_group = cfg.OptGroup(name='roles_middleware', title='roles_middleware')


def register_opts(conf):
    conf.register_group(role_m_group)
    conf.register_opts(role_m_opts, role_m_group)


def list_opts():
    return role_m_group, role_m_opts
