# Copyright 2018 OP5 AB
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
from oslo_policy import policy

from monasca_api import policies

CONF = cfg.CONF
DEFAULT_AUTHORIZED_ROLES = policies.roles_list_to_check_str(
    cfg.CONF.security.default_authorized_roles)
READ_ONLY_AUTHORIZED_ROLES = policies.roles_list_to_check_str(
    cfg.CONF.security.read_only_authorized_roles)

rules = [
    policy.DocumentedRuleDefault(
        name='api:notifications:put',
        check_str=DEFAULT_AUTHORIZED_ROLES,
        description='Update the specified notification method.',
        operations=[
            {
                'path': '/v2.0/notification-methods/{notification_method_id}',
                'method': 'PUT'
            },
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:notifications:patch',
        check_str=DEFAULT_AUTHORIZED_ROLES,
        description='Update selected parameters of the specified notification method.',
        operations=[
            {
                'path': '/v2.0/notification-methods/{notification_method_id}',
                'method': 'PATCH'
            },
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:notifications:delete',
        check_str=DEFAULT_AUTHORIZED_ROLES,
        description='Delete the specified notification method.',
        operations=[
            {
                'path': '/v2.0/notification-methods/{notification_method_id}',
                'method': 'DELETE'
            },
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:notifications:get',
        check_str=DEFAULT_AUTHORIZED_ROLES + ' or ' + READ_ONLY_AUTHORIZED_ROLES,
        description='List or get the details of the specified notification method.',
        operations=[
            {
                'path': '/v2.0/notification-methods',
                'method': 'GET'
            },
            {
                'path': '/v2.0/notification-methods/{notification_method_id}',
                'method': 'GET'
            },
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:notifications:post',
        check_str=DEFAULT_AUTHORIZED_ROLES,
        description='Create a notification method.',
        operations=[
            {
                'path': '/v2.0/notification-methods',
                'method': 'POST'
            }
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:notifications:type',
        check_str=DEFAULT_AUTHORIZED_ROLES + ' or ' + READ_ONLY_AUTHORIZED_ROLES,
        description='List supported notification method types.',
        operations=[
            {
                'path': '/v2.0/notification-methods/types',
                'method': 'GET'
            }
        ]
    )
]


def list_rules():
    return rules
