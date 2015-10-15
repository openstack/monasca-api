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
from oslo_config import cfg
from oslo_log import log
import simport

from monasca_api.api import notifications_api_v2
from monasca_api.v2.common.schemas import (
    notifications_request_body_schema as schemas_notifications)
from monasca_api.v2.common.schemas import exceptions as schemas_exceptions
from monasca_api.v2.reference import helpers
from monasca_api.v2.reference import resource

LOG = log.getLogger(__name__)


class Notifications(notifications_api_v2.NotificationsV2API):
    def __init__(self):

        super(Notifications, self).__init__()

        self._region = cfg.CONF.region
        self._default_authorized_roles = (
            cfg.CONF.security.default_authorized_roles)
        self._notifications_repo = simport.load(
            cfg.CONF.repositories.notifications_driver)()

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

    @resource.resource_try_catch_block
    def _create_notification(self, tenant_id, notification, uri):

        name = notification['name']
        notification_type = notification['type'].upper()
        address = notification['address']
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

    @resource.resource_try_catch_block
    def _update_notification(self, id, tenant_id, notification, uri):

        name = notification['name']
        notification_type = notification['type'].upper()
        address = notification['address']
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

    @resource.resource_try_catch_block
    def _list_notifications(self, tenant_id, uri, offset, limit):

        rows = self._notifications_repo.list_notifications(tenant_id, offset,
                                                           limit)

        result = [self._build_notification_result(row,
                                                  uri) for row in rows]

        return helpers.paginate(result, uri, limit)

    @resource.resource_try_catch_block
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

    @resource.resource_try_catch_block
    def _delete_notification(self, tenant_id, notification_id):

        self._notifications_repo.delete_notification(tenant_id,
                                                     notification_id)

    def on_post(self, req, res):
        helpers.validate_json_content_type(req)
        helpers.validate_authorization(req, self._default_authorized_roles)
        notification = helpers.read_http_resource(req)
        self._validate_notification(notification)
        tenant_id = helpers.get_tenant_id(req)
        result = self._create_notification(tenant_id, notification, req.uri)
        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_201

    def on_get(self, req, res, notification_method_id=None):
        if notification_method_id is None:
            helpers.validate_authorization(req, self._default_authorized_roles)
            tenant_id = helpers.get_tenant_id(req)
            offset = helpers.get_query_param(req, 'offset')
            limit = helpers.get_limit(req)
            result = self._list_notifications(tenant_id, req.uri, offset,
                                              limit)
            res.body = helpers.dumpit_utf8(result)
            res.status = falcon.HTTP_200
        else:
            helpers.validate_authorization(req,
                                           self._default_authorized_roles)
            tenant_id = helpers.get_tenant_id(req)
            result = self._list_notification(tenant_id,
                                             notification_method_id,
                                             req.uri)
            res.body = helpers.dumpit_utf8(result)
            res.status = falcon.HTTP_200

    def on_delete(self, req, res, notification_method_id):
        helpers.validate_authorization(req, self._default_authorized_roles)
        tenant_id = helpers.get_tenant_id(req)
        self._delete_notification(tenant_id, notification_method_id)
        res.status = falcon.HTTP_204

    def on_put(self, req, res, notification_method_id):
        helpers.validate_json_content_type(req)
        helpers.validate_authorization(req, self._default_authorized_roles)
        notification = helpers.read_http_resource(req)
        self._validate_notification(notification)
        tenant_id = helpers.get_tenant_id(req)
        result = self._update_notification(notification_method_id, tenant_id,
                                           notification, req.uri)
        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_200
