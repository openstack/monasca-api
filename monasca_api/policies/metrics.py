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
AGENT_AUTHORIZED_ROLES = policies.roles_list_to_check_str(cfg.CONF.security.agent_authorized_roles)

rules = [
    policy.DocumentedRuleDefault(
        name='api:metrics:get',
        check_str=DEFAULT_AUTHORIZED_ROLES + ' or ' + READ_ONLY_AUTHORIZED_ROLES,
        description='List metrics, measurements, metric statistics or metric names.',
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
        description='Create metrics.',
        operations=[
            {'path': '/v2.0/metrics', 'method': 'POST'}
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:metrics:dimension:values',
        check_str=DEFAULT_AUTHORIZED_ROLES + ' or ' + READ_ONLY_AUTHORIZED_ROLES,
        description='List dimension values.',
        operations=[
            {'path': '/v2.0/metrics/dimensions/names/values', 'method': 'GET'}
        ]
    ),
    policy.DocumentedRuleDefault(
        name='api:metrics:dimension:names',
        check_str=DEFAULT_AUTHORIZED_ROLES + ' or ' + READ_ONLY_AUTHORIZED_ROLES,
        description='List dimension names.',
        operations=[
            {'path': '/v2.0/metrics/dimensions/names', 'method': 'GET'}
        ]
    ),
]


def list_rules():
    return rules
