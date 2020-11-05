# Copyright 2014-2016 Hewlett Packard Enterprise Development Company LP
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


class AlarmsV2API(object):
    def __init__(self):
        """
        Initialize the device initialization.

        Args:
            self: (todo): write your description
        """
        super(AlarmsV2API, self).__init__()
        LOG.info('Initializing AlarmsV2API!')

    def on_put(self, req, res, alarm_id):
        """
        Respond to a request.

        Args:
            self: (todo): write your description
            req: (todo): write your description
            res: (todo): write your description
            alarm_id: (str): write your description
        """
        res.status = '501 Not Implemented'

    def on_patch(self, req, res, alarm_id):
        """
        Send put request to update.

        Args:
            self: (todo): write your description
            req: (todo): write your description
            res: (todo): write your description
            alarm_id: (str): write your description
        """
        res.status = '501 Not Implemented'

    def on_delete(self, req, res, alarm_id):
        """
        Delete an update request.

        Args:
            self: (todo): write your description
            req: (str): write your description
            res: (todo): write your description
            alarm_id: (str): write your description
        """
        res.status = '501 Not Implemented'

    def on_get(self, req, res, alarm_id):
        """
        Respond to get request.

        Args:
            self: (todo): write your description
            req: (str): write your description
            res: (list): write your description
            alarm_id: (str): write your description
        """
        res.status = '501 Not Implemented'


class AlarmsCountV2API(object):
    def __init__(self):
        """
        Initialize non - v2 classes

        Args:
            self: (todo): write your description
        """
        super(AlarmsCountV2API, self).__init__()

    def on_get(self, req, res):
        """
        Respond to get request.

        Args:
            self: (todo): write your description
            req: (str): write your description
            res: (list): write your description
        """
        res.status = '501 Not Implemented'


class AlarmsStateHistoryV2API(object):
    def __init__(self):
        """
        Initialize the device state.

        Args:
            self: (todo): write your description
        """
        super(AlarmsStateHistoryV2API, self).__init__()
        LOG.info('Initializing AlarmsStateHistoryV2API!')

    def on_get(self, req, res, alarm_id):
        """
        Respond to get request.

        Args:
            self: (todo): write your description
            req: (str): write your description
            res: (list): write your description
            alarm_id: (str): write your description
        """
        res.status = '501 Not Implemented'
