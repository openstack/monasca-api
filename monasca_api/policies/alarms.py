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
        name='api:alarms:definition:post',
        check_str=DEFAULT_AUTHORIZED_ROLES,
        description='Create an alarm definition.',
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
        description='List or get the details of the specified alarm definition.',
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
        description='Update/Replace the specified alarm definition.',
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
        description='Update selected parameters of the specified alarm definition, '
                    'and enable/disable its actions.',
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
        description='Delete the specified alarm definition.',
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
        description='Update/Replace the entire state of the specified alarm.',
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
        description='Update selected parameters of a specified alarm,'
                    ' set the alarm state and enable/disable it.',
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
        description='Delete the specified alarm.',
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
        description='List or get the details of the specified alarm.',
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
        description='Get the number of alarms that match the criteria.',
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
        description='List alarm state history for alarms.',
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
