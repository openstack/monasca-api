# Copyright 2015-2017 FUJITSU LIMITED
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

from monasca_log_api import conf
from oslo_log import log
from oslo_middleware import base as om
from webob import response


CONF = conf.CONF
LOG = log.getLogger(__name__)

_X_IDENTITY_STATUS = 'X-Identity-Status'
_X_ROLES = 'X-Roles'
_X_MONASCA_LOG_AGENT = 'X-MONASCA-LOG-AGENT'
_CONFIRMED_STATUS = 'Confirmed'


def _ensure_lower_roles(roles):
    if not roles:
        return []
    return [role.strip().lower() for role in roles]


def _intersect(a, b):
    return list(set(a) & set(b))


class RoleMiddleware(om.ConfigurableMiddleware):
    """Authorization middleware for X-Roles header.

    RoleMiddleware is responsible for authorizing user's
    access against **X-Roles** header. Middleware
    expects authentication to be completed (i.e. keystone middleware
    has been already called).

    If tenant is authenticated and authorized middleware
    exits silently (that is considered a success). Otherwise
    middleware produces JSON response according to following schema

    .. code-block:: javascript

        {
          'title': u'Unauthorized',
          'message': explanation (str)
        }

    Configuration example

    .. code-block:: cfg

        [roles_middleware]
        path = /v2.0/log
        default_roles = monasca-user
        agent_roles = monasca-log-agent
        delegate_roles = admin

    Configuration explained:

    * path (list) - path (or list of paths) middleware should be applied
    * agent_roles (list) - list of roles that identifies tenant as an agent
    * default_roles (list) - list of roles that should be authorized
    * delegate_roles (list) - list of roles that are allowed to POST logs on
                                behalf of another tenant (project)

    Note:
        Being an agent means that tenant is automatically authorized.
    Note:
        Middleware works only for configured paths and for all
        requests apart from HTTP method **OPTIONS**.

    """

    def __init__(self, application, conf=None):
        super(RoleMiddleware, self).__init__(application, conf)
        middleware = CONF.roles_middleware

        self._path = middleware.path
        self._default_roles = _ensure_lower_roles(middleware.default_roles)
        self._agent_roles = _ensure_lower_roles(middleware.agent_roles)

        LOG.debug('RolesMiddleware initialized for paths=%s', self._path)

    def process_request(self, req):
        if not self._can_apply_middleware(req):
            LOG.debug('%s skipped in role middleware', req.path)
            return None

        is_authenticated = self._is_authenticated(req)
        is_agent = self._is_agent(req)
        tenant_id = req.headers.get('X-Tenant-Id')

        req.environ[_X_MONASCA_LOG_AGENT] = is_agent

        LOG.debug('%s is authenticated=%s, log_agent=%s',
                  tenant_id, is_authenticated, is_agent)

        if is_authenticated:
            LOG.debug('%s has been authenticated', tenant_id)
            return  # do return nothing to enter API internal

        explanation = u'Failed to authenticate request for %s' % tenant_id
        LOG.error(explanation)
        json_body = {u'title': u'Unauthorized', u'message': explanation}
        return response.Response(status=401,
                                 json_body=json_body,
                                 content_type='application/json')

    def _is_agent(self, req):
        headers = req.headers
        roles = headers.get(_X_ROLES)

        if not roles:
            LOG.warning('Couldn\'t locate %s header,or it was empty', _X_ROLES)
            return False
        else:
            roles = _ensure_lower_roles(roles.split(','))

        is_agent = len(_intersect(roles, self._agent_roles)) > 0

        return is_agent

    def _is_authenticated(self, req):
        headers = req.headers
        if _X_IDENTITY_STATUS in headers:
            status = req.headers.get(_X_IDENTITY_STATUS)
            return _CONFIRMED_STATUS == status
        return False

    def _can_apply_middleware(self, req):
        path = req.path
        method = req.method

        if method == 'OPTIONS':
            return False

        if self._path:
            for p in self._path:
                if path.startswith(p):
                    return True
        return False  # if no configured paths, or nothing matches
