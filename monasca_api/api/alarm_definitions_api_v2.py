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

from oslo_log import log

LOG = log.getLogger(__name__)


class AlarmDefinitionsV2API(object):
    def __init__(self):
        """
        Method to initialize all of - chain.

        Args:
            self: (todo): write your description
        """
        super(AlarmDefinitionsV2API, self).__init__()
        LOG.info('Initializing AlarmDefinitionsV2API!')

    def on_post(self, req, res):
        """
        Respond to post request.

        Args:
            self: (todo): write your description
            req: (todo): write your description
            res: (todo): write your description
        """
        res.status = '501 Not Implemented'

    def on_get(self, req, res, alarm_definition_id):
        """
        Taobao. fencode

        Args:
            self: (todo): write your description
            req: (str): write your description
            res: (list): write your description
            alarm_definition_id: (str): write your description
        """
        res.status = '501 Not Implemented'

    def on_put(self, req, res, alarm_definition_id):
        """
        Respond to put request.

        Args:
            self: (todo): write your description
            req: (todo): write your description
            res: (todo): write your description
            alarm_definition_id: (str): write your description
        """
        res.status = '501 Not Implemented'

    def on_patch(self, req, res, alarm_definition_id):
        """
        Register a put request.

        Args:
            self: (todo): write your description
            req: (todo): write your description
            res: (todo): write your description
            alarm_definition_id: (str): write your description
        """
        res.status = '501 Not Implemented'

    def on_delete(self, req, res, alarm_definition_id):
        """
        Respond to delete request.

        Args:
            self: (todo): write your description
            req: (str): write your description
            res: (todo): write your description
            alarm_definition_id: (str): write your description
        """
        res.status = '501 Not Implemented'
