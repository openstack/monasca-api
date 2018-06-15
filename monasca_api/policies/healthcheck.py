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
HEALTHCHECK_ROLES = policies.roles_list_to_check_str(cfg.CONF.security.healthcheck_roles)

rules = [
    policy.DocumentedRuleDefault(
        name='api:healthcheck',
        check_str=HEALTHCHECK_ROLES,
        description='Check healthiness.',
        operations=[
            {'path': '/healthcheck', 'method': 'GET'}
        ]
    ),
]


def list_rules():
    return rules
