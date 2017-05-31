# Copyright 2017 FUJITSU LIMITED
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

from keystonemiddleware import auth_token
from oslo_log import log

LOG = log.getLogger(__name__)

_SKIP_PATH = '/version', '/healthcheck'
"""Tuple of non-application endpoints"""


class SkippingAuthProtocol(auth_token.AuthProtocol):
    """SkippingAuthProtocol to reach healthcheck endpoint

    Because healthcheck endpoints exists as endpoint, it
    is hidden behind keystone filter thus a request
    needs to authenticated before it is reached.

    Note:
        SkippingAuthProtocol is lean customization
        of :py:class:`keystonemiddleware.auth_token.AuthProtocol`
        that disables keystone communication if request
        is meant to reach healthcheck

    """

    def process_request(self, request):
        path = request.path
        for p in _SKIP_PATH:
            if path.startswith(p):
                LOG.debug(
                    ('Request path is %s and it does not require keystone '
                     'communication'), path)
                return None  # return NONE to reach actual logic

        return super(SkippingAuthProtocol, self).process_request(request)


def filter_factory(global_conf, **local_conf):  # pragma: no cover
    """Return factory function for :py:class:`.SkippingAuthProtocol`

    :param global_conf: global configuration
    :param local_conf: local configuration
    :return: factory function
    :rtype: function
    """
    conf = global_conf.copy()
    conf.update(local_conf)

    def auth_filter(app):
        return SkippingAuthProtocol(app, conf)

    return auth_filter
