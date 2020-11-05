# Copyright 2015 Hewlett-Packard
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


class VersionsAPI(object):
    def __init__(self):
        """
        Initialize the api.

        Args:
            self: (todo): write your description
        """
        super(VersionsAPI, self).__init__()
        LOG.info('Initializing VersionsAPI!')

    def on_get(self, req, res, id):
        """
        Respond to get request

        Args:
            self: (todo): write your description
            req: (str): write your description
            res: (list): write your description
            id: (str): write your description
        """
        res.status = '501 Not Implemented'
