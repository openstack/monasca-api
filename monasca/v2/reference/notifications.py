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

import falcon
from oslo.config import cfg

from monasca.api import monasca_notifications_api_v2
from monasca.common import resource_api
from monasca.openstack.common import log
from monasca.v2.common.schemas import (exceptions as schemas_exceptions)
from monasca.v2.common.schemas import (
    notifications_request_body_schema as schemas_notifications)
from monasca.v2.reference import helpers
from monasca.v2.reference.resource import resource_try_catch_block


LOG = log.getLogger(__name__)


class Notifications(monasca_notifications_api_v2.NotificationsV2API):

    def __init__(self, global_conf):

        super(Notifications, self).__init__(global_conf)

        self._region = cfg.CONF.region
        self._default_authorized_roles = (
            cfg.CONF.security.default_authorized_roles)
        self._notifications_repo = resource_api.init_driver(
            'monasca.repositories', cfg.CONF.repositories.notifications_driver)

    def _validate_notification(self, notification):
        """Validates the notification

        :param notification: An event object.
        :raises falcon.HTTPBadRequest
        """
        try:
            schemas_notifications.validate(notification)
        except schemas_exceptions.ValidationException as ex:
            LOG.debug(ex)
            raise falcon.HTTPBadRequest('Bad request', ex.message)

    @resource_try_catch_block
    def _create_notification(self, tenant_id, notification, uri):

        name = notification['name'].decode('utf8')
        notification_type = notification['type'].upper().decode('utf8')
        address = notification['address'].decode('utf8')
        notification_id = self._notifications_repo.create_notification(
            tenant_id,
            name,
            notification_type,
            address)

        return self._create_notification_response(notification_id,
                                                  name,
                                                  notification_type,
                                                  address,
                                                  uri)

    @resource_try_catch_block
    def _update_notification(self, id, tenant_id, notification, uri):

        name = notification['name'].decode('utf8')
        notification_type = notification['type'].upper().decode('utf8')
        address = notification['address'].decode('utf8')
        self._notifications_repo.update_notification(id, tenant_id, name,
                                                     notification_type,
                                                     address)

        return self._create_notification_response(id,
                                                  name,
                                                  notification_type,
                                                  address,
                                                  uri)

    def _create_notification_response(self, id, name, type,
                                      address, uri):

        response = {
            'id': id,
            'name': name,
            'type': type,
            'address': address
        }

        return helpers.add_links_to_resource(response, uri)

    @resource_try_catch_block
    def _list_notifications(self, tenant_id, uri, offset):

        rows = self._notifications_repo.list_notifications(tenant_id, offset)

        result = [self._build_notification_result(row,
                                                  uri) for row in rows]

        return helpers.paginate(result, uri, offset)

    @resource_try_catch_block
    def _list_notification(self, tenant_id, notification_id, uri):

        row = self._notifications_repo.list_notification(
            tenant_id,
            notification_id)

        return self._build_notification_result(row, uri)

    def _build_notification_result(self, notification_row, uri):

        result = {
            u'id': notification_row['id'],
            u'name': notification_row['name'],
            u'type': notification_row['type'],
            u'address': notification_row['address']
        }

        helpers.add_links_to_resource(result, uri)

        return result

    @resource_try_catch_block
    def _delete_notification(self, tenant_id, notification_id):

        self._notifications_repo.delete_notification(tenant_id,
                                                     notification_id)

    @resource_api.Restify('/v2.0/notification-methods', method='post')
    def do_post_notification_methods(self, req, res):
        helpers.validate_json_content_type(req)
        helpers.validate_authorization(req, self._default_authorized_roles)
        notification = helpers.read_http_resource(req)
        self._validate_notification(notification)
        tenant_id = helpers.get_tenant_id(req)
        result = self._create_notification(tenant_id, notification, req.uri)
        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_201

    @resource_api.Restify('/v2.0/notification-methods', method='get')
    def do_get_notification_methods(self, req, res):
        helpers.validate_authorization(req, self._default_authorized_roles)
        tenant_id = helpers.get_tenant_id(req)
        offset = helpers.normalize_offset(helpers.get_query_param(req,
                                                                  'offset'))
        result = self._list_notifications(tenant_id, req.uri, offset)
        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_200

    @resource_api.Restify('/v2.0/notification-methods/{id}', method='delete')
    def do_delete_notification_methods(self, req, res, id):
        helpers.validate_authorization(req, self._default_authorized_roles)
        tenant_id = helpers.get_tenant_id(req)
        self._delete_notification(tenant_id, id)
        res.status = falcon.HTTP_204

    @resource_api.Restify('/v2.0/notification-methods/{id}', method='get')
    def do_get_notification_method(self, req, res, id):
        helpers.validate_authorization(req, self._default_authorized_roles)
        tenant_id = helpers.get_tenant_id(req)
        result = self._list_notification(tenant_id, id, req.uri)
        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_200

    @resource_api.Restify('/v2.0/notification-methods/{id}', method='put')
    def do_put_notification_methods(self, req, res, id):
        helpers.validate_json_content_type(req)
        helpers.validate_authorization(req, self._default_authorized_roles)
        notification = helpers.read_http_resource(req)
        self._validate_notification(notification)
        tenant_id = helpers.get_tenant_id(req)
        result = self._update_notification(id, tenant_id, notification,
                                           req.uri)
        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_200
