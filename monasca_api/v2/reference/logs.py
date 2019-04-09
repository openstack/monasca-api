# Copyright 2016 Hewlett Packard Enterprise Development Company, L.P.
# Copyright 2016-2017 FUJITSU LIMITED
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
from oslo_log import log

from monasca_api.api.core.log import exceptions
from monasca_api.api.core.log import validation
from monasca_api.api import logs_api
from monasca_api import conf
from monasca_api.v2.common import bulk_processor
from monasca_api.v2.reference import helpers


CONF = conf.CONF
LOG = log.getLogger(__name__)


class Logs(logs_api.LogsApi):

    VERSION = 'v2.0'
    SUPPORTED_CONTENT_TYPES = {'application/json'}

    def __init__(self):

        super(Logs, self).__init__()
        self._processor = bulk_processor.BulkProcessor()

    def on_post(self, req, res):
        helpers.validate_json_content_type(req)
        helpers.validate_authorization(req, ['api:logs:post'])
        helpers.validate_payload_size(req.content_length)
        self.process_on_post_request(req, res)

    def process_on_post_request(self, req, res):
        try:
            request_body = helpers.from_json(req)
            log_list = self._get_logs(request_body)
            global_dimensions = self._get_global_dimensions(request_body)

        except Exception as ex:
            LOG.error('Entire bulk package has been rejected')
            LOG.exception(ex)

            raise ex
        tenant_id = (req.cross_project_id if req.cross_project_id
                     else req.project_id)

        try:
            self._processor.send_message(
                logs=log_list,
                global_dimensions=global_dimensions,
                log_tenant_id=tenant_id
            )
        except Exception as ex:
            res.status = getattr(ex, 'status', falcon.HTTP_500)
            return

        res.status = falcon.HTTP_204

    @staticmethod
    def _get_global_dimensions(request_body):
        """Get the top level dimensions in the HTTP request body."""
        global_dims = request_body.get('dimensions', {})
        validation.validate_dimensions(global_dims)
        return global_dims

    @staticmethod
    def _get_logs(request_body):
        """Get the logs in the HTTP request body."""
        if request_body is None:
            raise falcon.HTTPBadRequest('Bad request',
                                        'Request body is Empty')
        if 'logs' not in request_body:
            raise exceptions.HTTPUnprocessableEntity(
                'Unprocessable Entity Logs not found')
        return request_body['logs']
