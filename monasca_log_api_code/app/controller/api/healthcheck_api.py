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

import collections

import falcon
from oslo_log import log

LOG = log.getLogger(__name__)

HealthCheckResult = collections.namedtuple('HealthCheckResult',
                                           ['status', 'details'])


# TODO(feature) monasca-common candidate
class HealthChecksApi(object):
    """HealthChecks Api

    HealthChecksApi server information regarding health of the API.

    """

    def __init__(self):
        super(HealthChecksApi, self).__init__()
        LOG.info('Initializing HealthChecksApi!')

    def on_get(self, req, res):
        """Complex healthcheck report on GET.

        Returns complex report regarding API well being
        and all dependent services.

        :param falcon.Request req: current request
        :param falcon.Response res: current response
        """
        res.status = falcon.HTTP_501

    def on_head(self, req, res):
        """Simple healthcheck report on HEAD.

        In opposite to :py:meth:`.HealthChecksApi.on_get`, this
        method is supposed to execute ASAP to inform user that
        API is up and running.

        :param falcon.Request req: current request
        :param falcon.Response res: current response

        """
        res.status = falcon.HTTP_501
