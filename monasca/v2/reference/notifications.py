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
from oslo.config import cfg

from monasca.api import monasca_notifications_api_v2
from monasca.common.repositories import exceptions as repository_exceptions
from monasca.common import resource_api
from monasca.openstack.common import log
from monasca.openstack.common import uuidutils
from monasca.v2.common.schemas import (exceptions as schemas_exceptions)
from monasca.v2.common.schemas import (
    notifications_request_body_schema as schemas_notifications)
from monasca.v2.reference import helpers

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

    def _create_notification(self, id, tenant_id, notification):
        """Store the notification using the repository.

        :param notification: A notification object.
        :raises: falcon.HTTPServiceUnavailable,falcon.HTTPConflict
        """
        try:
            name = notification['name']
            notification_type = notification['type'].upper()
            address = notification['address']
            if self._notifications_repo.exists(tenant_id, name):
                raise falcon.HTTPConflict('Conflict', (
                    'Notification Method already exists: tenant_id=%s '
                    'name=%s' % (
                        tenant_id, name)), code=409)
            self._notifications_repo.create_notification(id, tenant_id, name,
                                                         notification_type,
                                                         address)
        except repository_exceptions.RepositoryException as ex:
            LOG.error(ex)
            raise falcon.HTTPInternalServerError('Service unavailable',
                                                 ex.message)

    def _update_notification(self, id, tenant_id, notification):
        """Update the notification using the repository.

        :param notification: A notification object.
        :raises: falcon.HTTPServiceUnavailable,falcon.HTTPError (404)
        """
        try:
            name = notification['name']
            notification_type = notification['type'].upper()
            address = notification['address']
            self._notifications_repo.update_notification(id, tenant_id, name,
                                                         notification_type,
                                                         address)
        except repository_exceptions.DoesNotExistException:
            helpers.raise_not_found_exception('notification', id, tenant_id)
        except repository_exceptions.RepositoryException as ex:
            LOG.error(ex)
            raise falcon.HTTPInternalServerError('Service unavailable',
                                                 ex.message)

    def _create_notification_response(self, id, notification, uri):
        name = notification['name']
        notification_type = notification['type'].upper()
        address = notification['address']
        response = {'id': id, 'name': name, 'type': notification_type,
                    'address': address}
        return json.dumps(helpers.add_links_to_resource(response, uri),
                          ensure_ascii=False).encode('utf8')

    def _list_notifications(self, tenant_id, uri):
        """Lists all notifications for this tenant id.

        :param tenant_id: The tenant id.
        :raises: falcon.HTTPServiceUnavailable
        """
        try:
            notifications = self._notifications_repo.list_notifications(
                tenant_id)
            return json.dumps(
                helpers.add_links_to_resource_list(notifications, uri))
        except repository_exceptions.RepositoryException as ex:
            LOG.error(ex)
            raise falcon.HTTPInternalServerError('Service unavailable',
                                                 ex.message)

    def _list_notification(self, tenant_id, notification_id, uri):
        """Lists the notification by id.

        :param tenant_id: The tenant id.
        :param notification_id: The notification id
        :raises: falcon.HTTPServiceUnavailable,falcon.HTTPError (404):
        """
        try:
            notifications = self._notifications_repo.list_notification(
                tenant_id, notification_id)
            return json.dumps(
                helpers.add_links_to_resource(notifications, uri))
        except repository_exceptions.DoesNotExistException:
            helpers.raise_not_found_exception('notification', notification_id,
                                              tenant_id)
        except repository_exceptions.RepositoryException as ex:
            LOG.error(ex)
            raise falcon.HTTPInternalServerError('Service unavailable',
                                                 ex.message)

    def _delete_notification(self, tenant_id, notification_id):
        """Deletes the notification using the repository.

        :param tenant_id: The tenant id.
        :param notification_id: The notification id
        :raises: falcon.HTTPServiceUnavailable,falcon.HTTPError (404)
        """
        try:
            self._notifications_repo.delete_notification(tenant_id,
                                                         notification_id)
        except repository_exceptions.DoesNotExistException:
            helpers.raise_not_found_exception('notification', notification_id,
                                              tenant_id)
        except repository_exceptions.RepositoryException as ex:
            LOG.error(ex)
            raise falcon.HTTPInternalServerError('Service unavailable',
                                                 ex.message)

    @resource_api.Restify('/v2.0/notification-methods', method='post')
    def do_post_notification_methods(self, req, res):
        helpers.validate_json_content_type(req)
        helpers.validate_authorization(req, self._default_authorized_roles)
        notification = helpers.read_http_resource(req)
        self._validate_notification(notification)
        id = uuidutils.generate_uuid()
        tenant_id = helpers.get_tenant_id(req)
        self._create_notification(id, tenant_id, notification)
        res.body = self._create_notification_response(id, notification,
                                                      req.uri)
        res.status = falcon.HTTP_200

    @resource_api.Restify('/v2.0/notification-methods', method='get')
    def do_get_notification_methods(self, req, res):
        helpers.validate_authorization(req, self._default_authorized_roles)
        tenant_id = helpers.get_tenant_id(req)
        res.body = self._list_notifications(tenant_id, req.uri)
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
        res.body = self._list_notification(tenant_id, id, req.uri)
        res.status = falcon.HTTP_200

    @resource_api.Restify('/v2.0/notification-methods/{id}', method='put')
    def do_put_notification_methods(self, req, res, id):
        helpers.validate_json_content_type(req)
        helpers.validate_authorization(req, self._default_authorized_roles)
        notification = helpers.read_http_resource(req)
        self._validate_notification(notification)
        tenant_id = helpers.get_tenant_id(req)
        self._update_notification(id, tenant_id, notification)
        res.body = self._create_notification_response(id, notification,
                                                      req.uri)
        res.status = falcon.HTTP_200
