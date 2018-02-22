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

from oslo_policy import policy

from monasca_api.policies import DEFAULT_AUTHORIZED_ROLES
from monasca_api.policies import READ_ONLY_AUTHORIZED_ROLES


rules = [
    policy.DocumentedRuleDefault(
        name='api:notifications:put',
        check_str=DEFAULT_AUTHORIZED_ROLES,
        description='Put notifications role',
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
        description='Patch notifications role',
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
        description='Delete notifications role',
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
        description='Get notifications role',
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
        description='Post notifications role',
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
        description='Get notifications type role',
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
