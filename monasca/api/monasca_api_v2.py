# Copyright 2013 IBM Corp
#
# Author: Tong Li <litong01@us.ibm.com>
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

from monasca.common import resource_api
from monasca.openstack.common import log


LOG = log.getLogger(__name__)


class V2API(object):
    def __init__(self, global_conf):
        LOG.debug('initializing V2API!')
        self.global_conf = global_conf

    @resource_api.Restify('/v2.0/metrics', method='get')
    def do_get_metrics(self, req, res):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/metrics/', method='post')
    def do_post_metrics(self, req, res):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/{version_id}', method='get')
    def do_get_version(self, req, res, version_id):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/metrics/measurements', method='get')
    def do_get_measurements(self, req, res):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/metrics/statistics', method='get')
    def do_get_statistics(self, req, res):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarm-definitions', method='post')
    def do_post_alarm_definitions(self, req, res):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarm-definitions/{id}', method='get')
    def do_get_alarm_definition(self, req, res, id):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarm-definitions/{id}', method='put')
    def do_put_alarm_definitions(self, req, res, id):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarm-definitions', method='get')
    def do_get_alarm_definitions(self, req, res):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarm-definitions/{id}', method='patch')
    def do_patch_alarm_definitions(self, req, res, id):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarm-definitions/{id}', method='delete')
    def do_delete_alarm_definitions(self, req, res, id):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarms/{id}', method='put')
    def do_put_alarms(self, req, res, id):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarms/{id}', method='patch')
    def do_patch_alarms(self, req, res, id):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarms/{id}', method='delete')
    def do_delete_alarms(self, req, res, id):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarms/', method='get')
    def do_get_alarms(self, req, res, id):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarms/{id}', method='get')
    def do_get_alarm_by_id(self, req, res, id):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarms/state-history', method='get')
    def do_get_alarms_state_history(self, req, res, id):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarms/{id}/state-history', method='get')
    def do_get_alarm_state_history(self, req, res, id):
        res.status = '501 Not Implemented'
