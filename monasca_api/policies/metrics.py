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

from monasca_api.policies import AGENT_AUTHORIZED_ROLES
from monasca_api.policies import DEFAULT_AUTHORIZED_ROLES
from monasca_api.policies import READ_ONLY_AUTHORIZED_ROLES


rules = [
    policy.DocumentedRuleDefault(
        name='api:metrics:get',
        check_str=DEFAULT_AUTHORIZED_ROLES + ' or ' + READ_ONLY_AUTHORIZED_ROLES,
        description='Get metrics role',
        operations=[
            {'path': '/v2.0/metrics', 'method': 'GET'},
            {'path': '/v2.0/metrics/measurements', 'method': 'GET'},
            {'path': '/v2.0/metrics/statistics', 'method': 'GET'},
            {'path': '/v2.0/metrics/names', 'method': 'GET'}
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:metrics:post',
        check_str=DEFAULT_AUTHORIZED_ROLES + ' or ' + AGENT_AUTHORIZED_ROLES,
        description='Post metrics role',
        operations=[
            {'path': '/v2.0/metrics', 'method': 'POST'}
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:metrics:dimension:values',
        check_str=DEFAULT_AUTHORIZED_ROLES + ' or ' + READ_ONLY_AUTHORIZED_ROLES,
        description='Get metrics dimension values role',
        operations=[
            {'path': '/v2.0/metrics/dimensions/names/values', 'method': 'GET'}
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:metrics:dimension:names',
        check_str=DEFAULT_AUTHORIZED_ROLES + ' or ' + READ_ONLY_AUTHORIZED_ROLES,
        description='Get metrics dimension names role',
        operations=[
            {'path': '/v2.0/metrics/dimensions/names', 'method': 'GET'}
        ]
    ),
]


def list_rules():
    return rules
