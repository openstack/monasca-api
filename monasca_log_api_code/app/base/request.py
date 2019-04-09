# Copyright 2016 FUJITSU LIMITED
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

import falcon
from monasca_common.policy import policy_engine as policy
from monasca_log_api import policies
from monasca_log_api.app.base import request_context
from monasca_log_api.app.base import validation

policy.POLICIES = policies


_TENANT_ID_PARAM = 'tenant_id'
"""Name of the query-param pointing at project-id (tenant-id)"""


class Request(falcon.Request):
    """Variation of falcon.Request with context

    Following class enhances :py:class:`falcon.Request` with
    :py:class:`context.RequestContext`.

    """

    def __init__(self, env, options=None):
        super(Request, self).__init__(env, options)
        self.context = request_context.RequestContext.from_environ(self.env)

    def validate(self, content_types):
        """Performs common request validation

        Validation checklist (in that order):

        * :py:func:`validation.validate_content_type`
        * :py:func:`validation.validate_payload_size`
        * :py:func:`validation.validate_cross_tenant`

        :param content_types: allowed content-types handler supports
        :type content_types: list
        :raises Exception: if any of the validation fails

        """
        validation.validate_content_type(self, content_types)
        validation.validate_payload_size(self)
        validation.validate_cross_tenant(
            tenant_id=self.project_id,
            roles=self.roles,
            cross_tenant_id=self.cross_project_id
        )

    @property
    def project_id(self):
        """Returns project-id (tenant-id)

        :return: project-id
        :rtype: str

        """
        return self.context.project_id

    @property
    def cross_project_id(self):
        """Returns project-id (tenant-id) found in query params.

        This particular project-id is later on identified as
        cross-project-id

        :return: project-id
        :rtype: str

        """
        return self.get_param(_TENANT_ID_PARAM, required=False)

    @property
    def user_id(self):
        """Returns user-id

        :return: user-id
        :rtype: str

        """
        return self.context.user

    @property
    def roles(self):
        """Returns roles associated with user

        :return: user's roles
        :rtype: list

        """
        return self.context.roles

    def can(self, action, target=None):
        return self.context.can(action, target)

    def __repr__(self):
        return '%s, context=%s' % (self.path, self.context)
