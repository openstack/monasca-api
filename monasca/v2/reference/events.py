# Copyright 2014 Hewlett-Packard
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

import json

import falcon

import collections

from monasca.api import monasca_events_api_v2
from monasca.common.messaging import exceptions as message_queue_exceptions
from monasca.common.messaging.message_formats import events_transform_factory
from monasca.common import resource_api
from monasca.openstack.common import log
from monasca.v2.common.schemas import (
    events_request_body_schema as schemas_event)
from monasca.v2.common.schemas import exceptions as schemas_exceptions
from monasca.v2.common import utils
from monasca.v2.reference import helpers
from monasca.v2.reference import resource

from oslo.config import cfg

LOG = log.getLogger(__name__)


class Events(monasca_events_api_v2.EventsV2API):

    def __init__(self, global_conf):

        super(Events, self).__init__(global_conf)

        self._region = cfg.CONF.region
        self._default_authorized_roles = (
            cfg.CONF.security.default_authorized_roles)
        self._delegate_authorized_roles = (
            cfg.CONF.security.delegate_authorized_roles)
        self._post_events_authorized_roles = (
            cfg.CONF.security.default_authorized_roles +
            cfg.CONF.security.agent_authorized_roles)
        self._event_transform = (
            events_transform_factory.create_events_transform())
        self._message_queue = (
            resource_api.init_driver('monasca.messaging',
                                     cfg.CONF.messaging.driver,
                                     ['raw-events']))
        self._events_repo = resource_api.init_driver(
            'monasca.repositories', cfg.CONF.repositories.events_driver)

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
            str_msg = json.dumps(event, default=utils.date_handler,
                                 ensure_ascii=False).encode('utf8')
            self._message_queue.send_message(str_msg)
        except message_queue_exceptions.MessageQueueException as ex:
            LOG.exception(ex)
            raise falcon.HTTPInternalServerError('Service unavailable',
                                                 ex.message)

    @resource.resource_try_catch_block
    def _list_events(self, tenant_id, uri, offset, limit):
        rows = self._events_repo.list_events(tenant_id, offset, limit)
        return helpers.paginate(self._build_events(rows), uri, offset)

    @resource.resource_try_catch_block
    def _list_event(self, tenant_id, event_id, uri):
        rows = self._events_repo.list_event(tenant_id, event_id)
        return self._build_events(rows)

    def _build_events(self, rows):
        result = collections.OrderedDict()
        for row in rows:
            event_id, event_data = self._build_event_data(row)

            if event_id['id'] in result:
                result[event_id['id']]['data'].update(event_data)
            else:
                result[event_id['id']] = {'id': event_id['id'],
                                          'description': event_id['desc'],
                                          'generated': event_id['generated'],
                                          'data': event_data}
        return result.values()

    def _build_event_data(self, event_row):
        event_data = {}
        name = event_row['name']

        if event_row['t_string']:
            event_data[name] = event_row['t_string']
        if event_row['t_int']:
            event_data[name] = event_row['t_int']
        if event_row['t_float']:
            event_data[name] = event_row['t_float']
        if event_row['t_datetime']:
            event_data[name] = float(event_row['t_datetime'])

        event_id = {'id': event_row['message_id'],
                    'desc': event_row['desc'],
                    'generated': float(event_row['generated'])}

        return event_id, event_data

    @resource_api.Restify('/v2.0/events', method='post')
    def do_post_events(self, req, res):
        helpers.validate_json_content_type(req)
        helpers.validate_authorization(req, self._post_events_authorized_roles)
        event = helpers.read_http_resource(req)
        self._validate_event(event)
        tenant_id = helpers.get_tenant_id(req)
        transformed_event = self._event_transform(event, tenant_id,
                                                  self._region)
        self._send_event(transformed_event)
        res.status = falcon.HTTP_204

    @resource_api.Restify('/v2.0/events', method='get')
    def do_get_events(self, req, res):
        helpers.validate_authorization(req, self._default_authorized_roles)
        tenant_id = helpers.get_tenant_id(req)
        offset = helpers.normalize_offset(helpers.get_query_param(req,
                                                                  'offset'))
        limit = helpers.get_query_param(req, 'limit')

        result = self._list_events(tenant_id, req.uri, offset, limit)
        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_200

    @resource_api.Restify('/v2.0/events/{id}', method='get')
    def do_get_event(self, req, res, id):
        helpers.validate_authorization(req, self._default_authorized_roles)
        tenant_id = helpers.get_tenant_id(req)
        result = self._list_event(tenant_id, id, req.uri)
        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_200
