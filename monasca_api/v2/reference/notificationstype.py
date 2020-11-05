# (C) Copyright 2016-2017 Hewlett Packard Enterprise Development LP
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

from monasca_api.api import notificationstype_api_v2
from monasca_api.v2.reference import helpers
from monasca_api.v2.reference import resource


class NotificationsType(notificationstype_api_v2.NotificationsTypeV2API):
    def __init__(self):
        """
        Initialize notifications

        Args:
            self: (todo): write your description
        """
        super(NotificationsType, self).__init__()
        self._notification_method_type_repo = simport.load(
            cfg.CONF.repositories.notification_method_type_driver)()

    def _list_notifications(self, uri, limit):
        """
        Return a notification.

        Args:
            self: (todo): write your description
            uri: (str): write your description
            limit: (int): write your description
        """
        rows = self._notification_method_type_repo.list_notification_method_types()
        result = [dict(type=row) for row in rows]
        return helpers.paginate(result, uri, limit)

    @resource.resource_try_catch_block
    def on_get(self, req, res):
        """
        Respond to get requests.

        Args:
            self: (todo): write your description
            req: (str): write your description
            res: (list): write your description
        """
        helpers.validate_authorization(req, ['api:notifications:type'])
        # This is to provide consistency. Pagination is not really supported here as there
        # are not that many rows
        result = self._list_notifications(req.uri, req.limit)

        res.body = helpers.to_json(result)
        res.status = falcon.HTTP_200
