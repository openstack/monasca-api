# (C) Copyright 2016 Hewlett Packard Enterprise Development LP
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


class NotificationsTypeV2API(object):
    def __init__(self):
        """
        Initializes all the properties.

        Args:
            self: (todo): write your description
        """
        super(NotificationsTypeV2API, self).__init__()
        LOG.info('Initializing NotificationsTypeV2API!')

    def on_post(self, req, res):
        """
        Respond to post request.

        Args:
            self: (todo): write your description
            req: (todo): write your description
            res: (todo): write your description
        """
        res.status = '501 Not Implemented'

    def on_delete(self, req, res):
        """
        Delete a resource delete

        Args:
            self: (todo): write your description
            req: (str): write your description
            res: (todo): write your description
        """
        res.status = '501 Not Implemented'

    def on_get(self, req, res):
        """
        Respond to get request.

        Args:
            self: (todo): write your description
            req: (str): write your description
            res: (list): write your description
        """
        res.status = '501 Not Implemented'

    def on_put(self, req, res):
        """
        Callback for put request

        Args:
            self: (todo): write your description
            req: (todo): write your description
            res: (todo): write your description
        """
        res.status = '501 Not Implemented'

    def on_patch(self, req, res):
        """
        Hook for patch.

        Args:
            self: (todo): write your description
            req: (todo): write your description
            res: (todo): write your description
        """
        res.status = '501 Not Implemented'
