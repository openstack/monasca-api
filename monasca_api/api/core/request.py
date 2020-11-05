# Copyright 2016 FUJITSU LIMITED
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

import falcon

from monasca_api.api.core import request_context
from monasca_api.common.policy import policy_engine as policy
from monasca_api.common.repositories import constants
from monasca_api import policies
from monasca_api.v2.common import exceptions


policy.POLICIES = policies

_TENANT_ID_PARAM = 'tenant_id'
"""Name of the query-param pointing at project-id (tenant-id)"""


class Request(falcon.Request):
    """Variation of falcon.Request with context

    Following class enhances :py:class:`falcon.Request` with
    :py:class:`context.RequestContext`.

    """

    def __init__(self, env, options=None):
        """
        Initialize the context.

        Args:
            self: (todo): write your description
            env: (todo): write your description
            options: (dict): write your description
        """
        super(Request, self).__init__(env, options)
        self.context = request_context.RequestContext.from_environ(self.env)

    @property
    def project_id(self):
        """Returns project-id (tenant-id)

        :return: project-id
        :rtype: str

        """
        return self.context.tenant

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

    @property
    def limit(self):
        """Returns LIMIT query param value.

        'limit' is not required query param.
        In case it is not found, py:data:'.constants.PAGE_LIMIT'
        value is returned.

        :return: value of 'limit' query param or default value
        :rtype: int
        :raise exceptions.HTTPUnprocessableEntityError: if limit is not valid integer

        """
        limit = self.get_param('limit', required=False, default=None)
        if limit is not None:
            if limit.isdigit():
                limit = int(limit)
                if limit > constants.PAGE_LIMIT:
                    return constants.PAGE_LIMIT
                else:
                    return limit
            else:
                err_msg = 'Limit parameter must be a positive integer'
                raise exceptions.HTTPUnprocessableEntityError('Invalid limit', err_msg)
        else:
            return constants.PAGE_LIMIT

    def can(self, action, target=None):
        """
        Return true if the target can be triggered.

        Args:
            self: (todo): write your description
            action: (str): write your description
            target: (todo): write your description
        """
        return self.context.can(action, target)

    def __repr__(self):
        """
        Return a human - readable representation.

        Args:
            self: (todo): write your description
        """
        return '%s, context=%s' % (self.path, self.context.to_dict())
