# (C) Copyright 2014,2016 Hewlett Packard Enterprise Development LP
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

from oslo_log import log

LOG = log.getLogger(__name__)


class NotificationsV2API(object):
    def __init__(self):
        """
        Initialize the device info.

        Args:
            self: (todo): write your description
        """
        super(NotificationsV2API, self).__init__()
        LOG.info('Initializing NotificationsV2API!')

    def on_post(self, req, res):
        """
        Respond to post request.

        Args:
            self: (todo): write your description
            req: (todo): write your description
            res: (todo): write your description
        """
        res.status = '501 Not Implemented'

    def on_delete(self, req, res, notification_method_id):
        """
        Delete a notification.

        Args:
            self: (todo): write your description
            req: (str): write your description
            res: (todo): write your description
            notification_method_id: (str): write your description
        """
        res.status = '501 Not Implemented'

    def on_get(self, req, res, notification_method_id):
        """
        Request a notification.

        Args:
            self: (todo): write your description
            req: (str): write your description
            res: (list): write your description
            notification_method_id: (str): write your description
        """
        res.status = '501 Not Implemented'

    def on_put(self, req, res, notification_method_id):
        """
        Respond to put request.

        Args:
            self: (todo): write your description
            req: (todo): write your description
            res: (todo): write your description
            notification_method_id: (str): write your description
        """
        res.status = '501 Not Implemented'

    def on_patch(self, req, res, notification_method_id):
        """
        Handle patch request.

        Args:
            self: (todo): write your description
            req: (todo): write your description
            res: (todo): write your description
            notification_method_id: (str): write your description
        """
        res.status = '501 Not Implemented'
