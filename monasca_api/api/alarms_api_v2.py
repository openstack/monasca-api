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
        super(AlarmsV2API, self).__init__()
        LOG.info('Initializing AlarmsV2API!')

    def on_put(self, req, res, alarm_id):
        res.status = '501 Not Implemented'

    def on_patch(self, req, res, alarm_id):
        res.status = '501 Not Implemented'

    def on_delete(self, req, res, alarm_id):
        res.status = '501 Not Implemented'

    def on_get(self, req, res, alarm_id):
        res.status = '501 Not Implemented'


class AlarmsCountV2API(object):
    def __init__(self):
        super(AlarmsCountV2API, self).__init__()

    def on_get(self, req, res):
        res.status = "501 Not Implemented"


class AlarmsStateHistoryV2API(object):
    def __init__(self):
        super(AlarmsStateHistoryV2API, self).__init__()
        LOG.info('Initializing AlarmsStateHistoryV2API!')

    def on_get(self, req, res, alarm_id):
        res.status = '501 Not Implemented'
