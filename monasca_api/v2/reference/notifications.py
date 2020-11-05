# (C) Copyright 2014-2017 Hewlett Packard Enterprise Development LP
# Copyright 2018 OP5 AB
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
from monasca_common.simport import simport
from oslo_config import cfg
from oslo_log import log
import six

from monasca_api.api import notifications_api_v2
from monasca_api.common.repositories import exceptions
from monasca_api.v2.common.exceptions import HTTPUnprocessableEntityError
from monasca_api.v2.common.schemas import (
    notifications_request_body_schema as schemas_notifications)
from monasca_api.v2.common.schemas import exceptions as schemas_exceptions
from monasca_api.v2.common import validation
from monasca_api.v2.reference import helpers
from monasca_api.v2.reference import resource

LOG = log.getLogger(__name__)


class Notifications(notifications_api_v2.NotificationsV2API):
    def __init__(self):
        """
        Initialize notifications.

        Args:
            self: (todo): write your description
        """

        super(Notifications, self).__init__()

        self._region = cfg.CONF.region
        self._notifications_repo = simport.load(
            cfg.CONF.repositories.notifications_driver)()
        self._notification_method_type_repo = simport.load(
            cfg.CONF.repositories.notification_method_type_driver)()
        self.valid_periods = cfg.CONF.valid_notification_periods

    def _parse_and_validate_notification(self, notification, require_all=False):
        """Validates the notification

        :param notification: An event object.
        :raises falcon.HTTPBadRequest
        """
        try:
            schemas_notifications.parse_and_validate(
                notification, self.valid_periods, require_all=require_all)
        except schemas_exceptions.ValidationException as ex:
            LOG.exception(ex)
            raise falcon.HTTPBadRequest('Bad Request', str(ex))

    def _validate_name_not_conflicting(self, tenant_id, name, expected_id=None):
        """
        Ensure the notifications exist.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            name: (str): write your description
            expected_id: (str): write your description
        """
        notification = self._notifications_repo.find_notification_by_name(tenant_id, name)

        if notification:
            if not expected_id:
                LOG.warning(
                    "Found existing notification method for {} with tenant_id {}"
                    .format(name, tenant_id))
                raise exceptions.AlreadyExistsException(
                    "A notification method with the name {} already exists".format(name))

            found_notification_id = notification['id']
            if found_notification_id != expected_id:
                LOG.warning(
                    "Found existing notification method for {} "
                    "with tenant_id {} with unexpected id {}"
                    .format(name, tenant_id, found_notification_id))
                raise exceptions.AlreadyExistsException(
                    "A notification method with name {} already exists with id {}"
                    .format(name, found_notification_id))

    def _validate_notification_method_type_exist(self, nmt):
        """
        Ensure that the nmt notification.

        Args:
            self: (todo): write your description
            nmt: (str): write your description
        """
        notification_methods = self._notification_method_type_repo.list_notification_method_types()
        exists = nmt.upper() in notification_methods

        if not exists:
            LOG.warning(
                "Found no notification method type  {}."
                "Did you install/enable the plugin for that type?"
                .format(nmt))
            raise falcon.HTTPBadRequest('Bad Request', "Not a valid notification method type {} "
                                        .format(nmt))

    def _create_notification(self, tenant_id, notification, uri):
        """
        Create a notification for a given notification.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            notification: (todo): write your description
            uri: (str): write your description
        """

        name = notification['name']
        notification_type = notification['type'].upper()
        address = notification['address']
        period = notification['period']

        self._validate_name_not_conflicting(tenant_id, name)
        self._validate_notification_method_type_exist(notification_type)

        notification_id = self._notifications_repo.create_notification(
            tenant_id,
            name,
            notification_type,
            address,
            period)

        return self._create_notification_response(notification_id,
                                                  name,
                                                  notification_type,
                                                  address,
                                                  period,
                                                  uri)

    def _update_notification(self, notification_id, tenant_id, notification, uri):
        """
        Updates a notification.

        Args:
            self: (todo): write your description
            notification_id: (str): write your description
            tenant_id: (str): write your description
            notification: (todo): write your description
            uri: (str): write your description
        """

        name = notification['name']
        notification_type = notification['type'].upper()
        address = notification['address']
        period = notification['period']

        self._validate_name_not_conflicting(tenant_id, name, expected_id=notification_id)
        self._validate_notification_method_type_exist(notification_type)

        self._notifications_repo.update_notification(notification_id, tenant_id, name,
                                                     notification_type,
                                                     address,
                                                     period)

        return self._create_notification_response(notification_id,
                                                  name,
                                                  notification_type,
                                                  address,
                                                  period,
                                                  uri)

    def _create_notification_response(self, id, name, type,
                                      address, period, uri):
        """
        Create a notification response.

        Args:
            self: (todo): write your description
            id: (todo): write your description
            name: (str): write your description
            type: (str): write your description
            address: (str): write your description
            period: (int): write your description
            uri: (str): write your description
        """

        response = {
            'id': id,
            'name': name,
            'type': type,
            'address': address,
            'period': period
        }

        return helpers.add_links_to_resource(response, uri)

    def _list_notifications(self, tenant_id, uri, sort_by, offset, limit):
        """
        Return a notification notifications for a tenant.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            uri: (str): write your description
            sort_by: (str): write your description
            offset: (int): write your description
            limit: (int): write your description
        """

        rows = self._notifications_repo.list_notifications(tenant_id, sort_by,
                                                           offset, limit)

        result = [self._build_notification_result(row,
                                                  uri) for row in rows]

        return helpers.paginate(result, uri, limit)

    def _list_notification(self, tenant_id, notification_id, uri):
        """
        Gets a list for a tenant.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            notification_id: (str): write your description
            uri: (str): write your description
        """

        row = self._notifications_repo.list_notification(
            tenant_id,
            notification_id)

        return self._build_notification_result(row, uri)

    def _build_notification_result(self, notification_row, uri):
        """
        Parse notification result

        Args:
            self: (todo): write your description
            notification_row: (todo): write your description
            uri: (str): write your description
        """

        result = {
            u'id': notification_row['id'],
            u'name': notification_row['name'],
            u'type': notification_row['type'],
            u'address': notification_row['address'],
            u'period': notification_row['period']
        }

        helpers.add_links_to_resource(result, uri)

        return result

    def _delete_notification(self, tenant_id, notification_id):
        """
        Deletes a notification.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            notification_id: (str): write your description
        """

        self._notifications_repo.delete_notification(tenant_id,
                                                     notification_id)

    def _patch_get_notification(self, tenant_id, notification_id, notification):
        """
        Patch notification for notifications.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            notification_id: (str): write your description
            notification: (str): write your description
        """
        original_notification = self._notifications_repo.list_notification(
            tenant_id, notification_id)
        if 'name' not in notification:
            notification['name'] = original_notification['name']
        if 'type' not in notification:
            notification['type'] = original_notification['type']
        if 'address' not in notification:
            notification['address'] = original_notification['address']
        if 'period' not in notification:
            notification['period'] = original_notification['period']

    @resource.resource_try_catch_block
    def on_post(self, req, res):
        """
        Callback for post request.

        Args:
            self: (todo): write your description
            req: (todo): write your description
            res: (todo): write your description
        """
        helpers.validate_json_content_type(req)
        helpers.validate_authorization(req, ['api:notifications:post'])
        notification = helpers.from_json(req)
        self._parse_and_validate_notification(notification)
        result = self._create_notification(req.project_id, notification, req.uri)
        res.body = helpers.to_json(result)
        res.status = falcon.HTTP_201

    @resource.resource_try_catch_block
    def on_get(self, req, res, notification_method_id=None):
        """
        Respond to get get requests.

        Args:
            self: (todo): write your description
            req: (str): write your description
            res: (list): write your description
            notification_method_id: (str): write your description
        """
        helpers.validate_authorization(req, ['api:notifications:get'])
        if notification_method_id is None:
            sort_by = helpers.get_query_param(req, 'sort_by', default_val=None)
            if sort_by is not None:
                if isinstance(sort_by, six.string_types):
                    sort_by = sort_by.split(',')

                allowed_sort_by = {'id', 'name', 'type', 'address',
                                   'updated_at', 'created_at'}

                validation.validate_sort_by(sort_by, allowed_sort_by)

            offset = helpers.get_query_param(req, 'offset')
            if offset is not None and not isinstance(offset, int):
                try:
                    offset = int(offset)
                except Exception:
                    raise HTTPUnprocessableEntityError('Unprocessable Entity',
                                                       'Offset value {} must be an integer'
                                                       .format(offset))

            result = self._list_notifications(req.project_id, req.uri, sort_by,
                                              offset, req.limit)

        else:

            result = self._list_notification(req.project_id,
                                             notification_method_id,
                                             req.uri)

        res.body = helpers.to_json(result)
        res.status = falcon.HTTP_200

    @resource.resource_try_catch_block
    def on_delete(self, req, res, notification_method_id):
        """
        Delete a notification.

        Args:
            self: (todo): write your description
            req: (str): write your description
            res: (todo): write your description
            notification_method_id: (str): write your description
        """
        helpers.validate_authorization(req, ['api:notifications:delete'])
        self._delete_notification(req.project_id, notification_method_id)
        res.status = falcon.HTTP_204

    @resource.resource_try_catch_block
    def on_put(self, req, res, notification_method_id):
        """
        Respond to put request.

        Args:
            self: (todo): write your description
            req: (todo): write your description
            res: (todo): write your description
            notification_method_id: (str): write your description
        """
        helpers.validate_json_content_type(req)
        helpers.validate_authorization(req, ['api:notifications:put'])
        notification = helpers.from_json(req)
        self._parse_and_validate_notification(notification, require_all=True)
        result = self._update_notification(notification_method_id, req.project_id,
                                           notification, req.uri)
        res.body = helpers.to_json(result)
        res.status = falcon.HTTP_200

    @resource.resource_try_catch_block
    def on_patch(self, req, res, notification_method_id):
        """
        Dispatches a patch request.

        Args:
            self: (todo): write your description
            req: (todo): write your description
            res: (todo): write your description
            notification_method_id: (str): write your description
        """
        helpers.validate_json_content_type(req)
        helpers.validate_authorization(req, ['api:notifications:patch'])
        notification = helpers.from_json(req)
        self._patch_get_notification(req.project_id, notification_method_id, notification)
        self._parse_and_validate_notification(notification, require_all=True)
        result = self._update_notification(notification_method_id, req.project_id,
                                           notification, req.uri)
        res.body = helpers.to_json(result)
        res.status = falcon.HTTP_200
