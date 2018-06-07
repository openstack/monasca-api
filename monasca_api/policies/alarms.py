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
        name='api:alarms:definition:post',
        check_str=DEFAULT_AUTHORIZED_ROLES,
        description='Post alarm definition role',
        operations=[
            {
                'path': '/v2.0/alarm-definitions/',
                'method': 'POST'
            }
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:alarms:definition:get',
        check_str=DEFAULT_AUTHORIZED_ROLES + ' or ' + READ_ONLY_AUTHORIZED_ROLES,
        description='Get alarm definition role',
        operations=[
            {
                'path': '/v2.0/alarm-definitions/{alarm_definition_id}',
                'method': 'GET'
            },
            {
                'path': '/v2.0/alarm-definitions',
                'method': 'GET'
            }
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:alarms:definition:put',
        check_str=DEFAULT_AUTHORIZED_ROLES,
        description='Put alarm definition role',
        operations=[
            {
                'path': '/v2.0/alarm-definitions/{alarm_definition_id}',
                'method': 'PUT'
            },
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:alarms:definition:patch',
        check_str=DEFAULT_AUTHORIZED_ROLES,
        description='Patch alarm definition role',
        operations=[
            {
                'path': '/v2.0/alarm-definitions/{alarm_definition_id}',
                'method': 'PATCH'
            },
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:alarms:definition:delete',
        check_str=DEFAULT_AUTHORIZED_ROLES,
        description='Delete alarm definition role',
        operations=[
            {
                'path': '/v2.0/alarm-definitions/{alarm_definition_id}',
                'method': 'DELETE'
            },
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:alarms:put',
        check_str=DEFAULT_AUTHORIZED_ROLES,
        description='Put alarm role',
        operations=[
            {
                'path': '/v2.0/alarms/{alarm_id}',
                'method': 'PUT'
            },
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:alarms:patch',
        check_str=DEFAULT_AUTHORIZED_ROLES,
        description='Patch alarm role',
        operations=[
            {
                'path': '/v2.0/alarms/{alarm_id}',
                'method': 'PATCH'
            },
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:alarms:delete',
        check_str=DEFAULT_AUTHORIZED_ROLES,
        description='Delete alarm role',
        operations=[
            {
                'path': '/v2.0/alarms/{alarm_id}',
                'method': 'DELETE'
            },
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:alarms:get',
        check_str=DEFAULT_AUTHORIZED_ROLES + ' or ' + READ_ONLY_AUTHORIZED_ROLES,
        description='Get alarm role',
        operations=[
            {
                'path': '/v2.0/alarms/',
                'method': 'GET'
            },
            {
                'path': '/v2.0/alarms/{alarm_id}',
                'method': 'GET'
            },
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:alarms:count',
        check_str=DEFAULT_AUTHORIZED_ROLES + ' or ' + READ_ONLY_AUTHORIZED_ROLES,
        description='Count alarm role',
        operations=[
            {
                'path': '/v2.0/alarms/count/',
                'method': 'GET'
            }
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:alarms:state_history',
        check_str=DEFAULT_AUTHORIZED_ROLES + ' or ' + READ_ONLY_AUTHORIZED_ROLES,
        description='Alarm state history role',
        operations=[
            {
                'path': '/v2.0/alarms/state-history',
                'method': 'GET'
            },
            {
                'path': '/v2.0/alarms/{alarm_id}/state-history',
                'method': 'GET'
            }
        ]
    )
]


def list_rules():
    return rules
