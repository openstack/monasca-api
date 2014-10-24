# Copyright 2014 Hewlett-Packard
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

import json

import falcon
from oslo.config import cfg

from monasca.openstack.common import log
from monasca.api import monasca_events_api_v2
from monasca.common import resource_api
from monasca.common.messaging import exceptions as message_queue_exceptions
from monasca.common.messaging.message_formats import events_transform_factory
from monasca.v2.common import utils
from monasca.v2.common.schemas import exceptions as schemas_exceptions
from monasca.v2.common.schemas import events_request_body_schema as schemas_event
from monasca.v2.reference import helpers

from stevedore import driver

LOG = log.getLogger(__name__)


class Events(monasca_events_api_v2.EventsV2API):
    def __init__(self, global_conf):
        super(Events, self).__init__(global_conf)
        self._region = cfg.CONF.region
        self._default_authorized_roles = cfg.CONF.security.default_authorized_roles
        self._delegate_authorized_roles = cfg.CONF.security.delegate_authorized_roles
        self._post_events_authorized_roles = cfg.CONF.security.default_authorized_roles + \
                                             cfg.CONF.security.agent_authorized_roles
        self._event_transform = events_transform_factory.create_events_transform()
        self._message_queue = resource_api.init_driver('monasca.messaging', 
                                        cfg.CONF.messaging.driver, ['raw-events'])

    def _validate_event(self, event):
        """Validates the event
        
        :param event: An event object.
        :raises falcon.HTTPBadRequest
        """
        try:
            schemas_event.validate(event)
        except schemas_exceptions.ValidationException as ex:
            LOG.debug(ex)
            raise falcon.HTTPBadRequest('Bad request', ex.message)

    def _send_event(self, event):
        """Send the event using the message queue.
        
        :param metrics: An event object.
        :raises: falcon.HTTPServiceUnavailable
        """
        try:
            str_msg = json.dumps(event, default=utils.date_handler)
            self._message_queue.send_message(str_msg)
        except message_queue_exceptions.MessageQueueException as ex:
            LOG.exception(ex)
            raise falcon.HTTPInternalServerError('Service unavailable', ex.message)

    @resource_api.Restify('/v2.0/events/', method='post')
    def do_post_events(self, req, res):
        helpers.validate_json_content_type(req)
        helpers.validate_authorization(req, self._post_events_authorized_roles)
        event = helpers.read_http_resource(req)
        self._validate_event(event)
        tenant_id = helpers.get_tenant_id(req)
        transformed_event = self._event_transform(event, tenant_id, self._region)
        self._send_event(transformed_event)
        res.status = falcon.HTTP_204