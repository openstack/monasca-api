# Copyright 2017 FUJITSU LIMITED
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

from oslo_context import context

from monasca_api.common.policy import policy_engine as policy
from monasca_api import policies

policy.POLICIES = policies


class RequestContext(context.RequestContext):
    """RequestContext.

    RequestContext is customized version of
    :py:class:oslo_context.context.RequestContext.
    """

    def can(self, action, target=None):
        if target is None:
            target = {'project_id': self.project_id,
                      'user_id': self.user_id}

        return policy.authorize(self, action=action, target=target)
