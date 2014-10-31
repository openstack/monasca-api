# Copyright 2013 IBM Corp
#
# Author: Tong Li <litong01@us.ibm.com>
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

from monasca.api import monasca_api_v2
from monasca.common import kafka_conn
from monasca.common import resource_api
from monasca.openstack.common import log


LOG = log.getLogger(__name__)


class KafkaDispatcher(monasca_api_v2.V2API):
    def __init__(self, global_conf):
        LOG.debug('initializing KafkaDispatcher!')
        super(KafkaDispatcher, self).__init__(global_conf)

        self._kafka_conn = kafka_conn.KafkaConnection()

    @resource_api.Restify('/v2.0/metrics/', method='post')
    def do_post_metrics(self, req, res):
        LOG.debug('Getting the call.')
        msg = req.stream.read()

        code = self._kafka_conn.send_messages(msg)
        res.status = getattr(falcon, 'HTTP_' + str(code))
