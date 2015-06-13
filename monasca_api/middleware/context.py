# Copyright (c) 2015 OpenStack Foundation
#
#    Licensed under the Apache License, Version 2.0 (the "License"); you may
#    not use this file except in compliance with the License. You may obtain
#    a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#    License for the specific language governing permissions and limitations
#    under the License.

"""RequestContext: context for requests that persist through monasca."""

import uuid

from oslo_log import log
from oslo_utils import timeutils

LOG = log.getLogger(__name__)


class RequestContext(object):
    """Security context and request information.

    Represents the user taking a given action within the system.

    """

    def __init__(self, user_id, project_id, domain_id=None, domain_name=None,
                 roles=None, timestamp=None, request_id=None,
                 auth_token=None, user_name=None, project_name=None,
                 service_catalog=None, user_auth_plugin=None, **kwargs):
        """Creates the Keystone Context. Supports additional parameters:

        :param user_auth_plugin:
            The auth plugin for the current request's authentication data.
        :param kwargs:
            Extra arguments that might be present
        """
        if kwargs:
            LOG.warning(
                'Arguments dropped when creating context: %s') % str(kwargs)

        self._roles = roles or []
        self.timestamp = timeutils.utcnow()

        if not request_id:
            request_id = self.generate_request_id()
        self._request_id = request_id
        self._auth_token = auth_token

        self._service_catalog = service_catalog

        self._domain_id = domain_id
        self._domain_name = domain_name

        self._user_id = user_id
        self._user_name = user_name

        self._project_id = project_id
        self._project_name = project_name

        self._user_auth_plugin = user_auth_plugin

    def to_dict(self):
        return {'user_id': self._user_id,
                'project_id': self._project_id,
                'domain_id': self._domain_id,
                'domain_name': self._domain_name,
                'roles': self._roles,
                'timestamp': timeutils.strtime(self._timestamp),
                'request_id': self._request_id,
                'auth_token': self._auth_token,
                'user_name': self._user_name,
                'service_catalog': self._service_catalog,
                'project_name': self._project_name,
                'user': self._user}

    def generate_request_id(self):
        return b'req-' + str(uuid.uuid4()).encode('ascii')
