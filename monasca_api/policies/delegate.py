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

DELEGATE_AUTHORIZED_ROLES = policies.roles_list_to_check_str(
    cfg.CONF.security.delegate_authorized_roles)

rules = [
    policy.RuleDefault(
        name='api:delegate',
        check_str=DELEGATE_AUTHORIZED_ROLES,
        description='The rules which allow to access the API on'
                    ' behalf of another project (tenant).',

    )
]


def list_rules():
    return rules
