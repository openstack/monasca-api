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
        super(AlarmDefinitionsV2API, self).__init__()
        LOG.info('Initializing AlarmDefinitionsV2API!')

    def on_post(self, req, res):
        res.status = '501 Not Implemented'

    def on_get(self, req, res, alarm_definition_id):
        res.status = '501 Not Implemented'

    def on_put(self, req, res, alarm_definition_id):
        res.status = '501 Not Implemented'

    def on_patch(self, req, res, alarm_definition_id):
        res.status = '501 Not Implemented'

    def on_delete(self, req, res, alarm_definition_id):
        res.status = '501 Not Implemented'
