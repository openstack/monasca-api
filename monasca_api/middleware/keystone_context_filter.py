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

import falcon
from oslo_log import log
from oslo_middleware import request_id
from oslo_serialization import jsonutils

from monasca_api.middleware import context

LOG = log.getLogger(__name__)


def filter_factory(global_conf, **local_conf):
    def validator_filter(app):
        return KeystoneContextFilter(app, local_conf)

    return validator_filter


class KeystoneContextFilter(object):
    """Make a request context from keystone headers."""

    def __init__(self, app, conf):
        self._app = app
        self._conf = conf

    def __call__(self, env, start_response):

        LOG.debug("Creating Keystone Context Object.")

        user_id = env.get('HTTP_X_USER_ID', env.get('HTTP_X_USER'))
        if user_id is None:
            msg = "Neither X_USER_ID nor X_USER found in request"
            LOG.error(msg)
            raise falcon.HTTPUnauthorized(title='Forbidden', description=msg)

        roles = self._get_roles(env)

        project_id = env.get('HTTP_X_PROJECT_ID')
        project_name = env.get('HTTP_X_PROJECT_NAME')

        domain_id = env.get('HTTP_X_DOMAIN_ID')
        domain_name = env.get('HTTP_X_DOMAIN_NAME')

        user_name = env.get('HTTP_X_USER_NAME')

        req_id = env.get(request_id.ENV_REQUEST_ID)

        # Get the auth token
        auth_token = env.get('HTTP_X_AUTH_TOKEN',
                             env.get('HTTP_X_STORAGE_TOKEN'))

        service_catalog = None
        if env.get('HTTP_X_SERVICE_CATALOG') is not None:
            try:
                catalog_header = env.get('HTTP_X_SERVICE_CATALOG')
                service_catalog = jsonutils.loads(catalog_header)
            except ValueError:
                msg = "Invalid service catalog json."
                LOG.error(msg)
                raise falcon.HTTPInternalServerError(msg)

        # NOTE(jamielennox): This is a full auth plugin set by auth_token
        # middleware in newer versions.
        user_auth_plugin = env.get('keystone.token_auth')

        # Build a context
        ctx = context.RequestContext(user_id,
                                     project_id,
                                     user_name=user_name,
                                     project_name=project_name,
                                     domain_id=domain_id,
                                     domain_name=domain_name,
                                     roles=roles,
                                     auth_token=auth_token,
                                     service_catalog=service_catalog,
                                     request_id=req_id,
                                     user_auth_plugin=user_auth_plugin)

        env['monasca.context'] = ctx

        LOG.debug("Keystone Context successfully created.")

        return self._app(env, start_response)

    def _get_roles(self, env):
        """Get the list of roles."""

        if 'HTTP_X_ROLES' in env:
            roles = env.get('HTTP_X_ROLES', '')
        else:
            # Fallback to deprecated role header:
            roles = env.get('HTTP_X_ROLE', '')
            if roles:
                LOG.warning(
                    'Sourcing roles from deprecated X-Role HTTP header')
        return [r.strip() for r in roles.split(',')]
