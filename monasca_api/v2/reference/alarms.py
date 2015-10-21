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

import re

import falcon
from oslo_config import cfg
from oslo_log import log
import simport

from monasca_api.api import alarms_api_v2
from monasca_api.common.repositories import exceptions
from monasca_api.v2.common.schemas import alarm_update_schema as schema_alarm
from monasca_api.v2.reference import alarming
from monasca_api.v2.reference import helpers
from monasca_api.v2.reference import resource

LOG = log.getLogger(__name__)


class Alarms(alarms_api_v2.AlarmsV2API,
             alarming.Alarming):
    def __init__(self):
        try:
            super(Alarms, self).__init__()
            self._region = cfg.CONF.region
            self._default_authorized_roles = (
                cfg.CONF.security.default_authorized_roles)
            self._alarms_repo = simport.load(
                cfg.CONF.repositories.alarms_driver)()

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def on_put(self, req, res, alarm_id):

        helpers.validate_authorization(req, self._default_authorized_roles)

        tenant_id = helpers.get_tenant_id(req)

        alarm = helpers.read_http_resource(req)
        schema_alarm.validate(alarm)

        # Validator makes state optional, so check it here
        if 'state' not in alarm or not alarm['state']:
            raise falcon.HTTPBadRequest('Bad request',
                                        "Field 'state' is required")
        if 'lifecycle_state' not in alarm or not alarm['lifecycle_state']:
            raise falcon.HTTPBadRequest('Bad Request',
                                        "Field 'lifecycle_state' is required")
        if 'link' not in alarm or not alarm['link']:
            raise falcon.HTTPBadRequest('Bad Request',
                                        "Field 'link' is required")

        self._alarm_update(tenant_id, alarm_id, alarm['state'],
                           alarm['lifecycle_state'], alarm['link'])

        result = self._alarm_show(req.uri, tenant_id, alarm_id)

        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_200

    def on_patch(self, req, res, alarm_id):

        helpers.validate_authorization(req, self._default_authorized_roles)

        tenant_id = helpers.get_tenant_id(req)

        alarm = helpers.read_http_resource(req)
        schema_alarm.validate(alarm)

        old_alarm = self._alarms_repo.get_alarm(tenant_id, alarm_id)[0]

        # if a field is not present or is None, replace it with the old value
        if 'state' not in alarm or not alarm['state']:
            alarm['state'] = old_alarm['state']
        if 'lifecycle_state' not in alarm or not alarm['lifecycle_state']:
            alarm['lifecycle_state'] = old_alarm['lifecycle_state']
        if 'link' not in alarm or not alarm['link']:
            alarm['link'] = old_alarm['link']

        self._alarm_patch(tenant_id, alarm_id, alarm['state'],
                          alarm['lifecycle_state'], alarm['link'])

        result = self._alarm_show(req.uri, tenant_id, alarm_id)

        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_200

    def on_delete(self, req, res, alarm_id):

        helpers.validate_authorization(req, self._default_authorized_roles)

        tenant_id = helpers.get_tenant_id(req)

        self._alarm_delete(tenant_id, alarm_id)

        res.status = falcon.HTTP_204

    def on_get(self, req, res, alarm_id=None):
        if alarm_id is None:
            helpers.validate_authorization(req, self._default_authorized_roles)
            tenant_id = helpers.get_tenant_id(req)

            query_parms = falcon.uri.parse_query_string(req.query_string)

            offset = helpers.get_query_param(req, 'offset')

            limit = helpers.get_limit(req)

            result = self._alarm_list(req.uri, tenant_id, query_parms, offset,
                                      limit)

            res.body = helpers.dumpit_utf8(result)
            res.status = falcon.HTTP_200

        else:
            helpers.validate_authorization(req, self._default_authorized_roles)
            tenant_id = helpers.get_tenant_id(req)

            result = self._alarm_show(req.uri, tenant_id, alarm_id)

            res.body = helpers.dumpit_utf8(result)
            res.status = falcon.HTTP_200

    @resource.resource_try_catch_block
    def _alarm_update(self, tenant_id, alarm_id, new_state, lifecycle_state,
                      link):

        alarm_metric_rows = self._alarms_repo.get_alarm_metrics(alarm_id)
        sub_alarm_rows = self._alarms_repo.get_sub_alarms(tenant_id, alarm_id)

        old_state = self._alarms_repo.update_alarm(tenant_id, alarm_id,
                                                   new_state,
                                                   lifecycle_state, link)

        # alarm_definition_id is the same for all rows.
        alarm_definition_id = sub_alarm_rows[0]['alarm_definition_id']

        state_info = {u'alarmState': new_state, u'oldAlarmState': old_state}

        self._send_alarm_event(u'alarm-updated', tenant_id,
                               alarm_definition_id, alarm_metric_rows,
                               sub_alarm_rows, state_info)

        if old_state != new_state:
            try:
                alarm_definition_row = self._alarms_repo.get_alarm_definition(
                    tenant_id, alarm_id)
            except exceptions.DoesNotExistException:
                # Alarm definition does not exist. May have been deleted
                # in another transaction. In that case, all associated
                # alarms were also deleted, so don't send transition events.
                pass
            else:
                self._send_alarm_transitioned_event(tenant_id, alarm_id,
                                                    alarm_definition_row,
                                                    alarm_metric_rows,
                                                    old_state, new_state)

    @resource.resource_try_catch_block
    def _alarm_patch(self, tenant_id, alarm_id, new_state, lifecycle_state,
                     link):

        alarm_metric_rows = self._alarms_repo.get_alarm_metrics(alarm_id)
        sub_alarm_rows = self._alarms_repo.get_sub_alarms(tenant_id, alarm_id)

        old_state = self._alarms_repo.update_alarm(tenant_id, alarm_id,
                                                   new_state,
                                                   lifecycle_state, link)

        # alarm_definition_id is the same for all rows.
        alarm_definition_id = sub_alarm_rows[0]['alarm_definition_id']

        state_info = {u'alarmState': new_state, u'oldAlarmState': old_state}

        self._send_alarm_event(u'alarm-updated', tenant_id,
                               alarm_definition_id, alarm_metric_rows,
                               sub_alarm_rows, state_info)

        if old_state != new_state:
            try:
                alarm_definition_row = self._alarms_repo.get_alarm_definition(
                    tenant_id, alarm_id)
            except exceptions.DoesNotExistException:
                # Alarm definition does not exist. May have been deleted
                # in another transaction. In that case, all associated
                # alarms were also deleted, so don't send transition events.
                pass
            else:
                self._send_alarm_transitioned_event(tenant_id, alarm_id,
                                                    alarm_definition_row,
                                                    alarm_metric_rows,
                                                    old_state, new_state)

    @resource.resource_try_catch_block
    def _alarm_delete(self, tenant_id, id):

        alarm_metric_rows = self._alarms_repo.get_alarm_metrics(id)
        sub_alarm_rows = self._alarms_repo.get_sub_alarms(tenant_id, id)

        self._alarms_repo.delete_alarm(tenant_id, id)

        # alarm_definition_id is the same for all rows.
        alarm_definition_id = sub_alarm_rows[0]['alarm_definition_id']

        self._send_alarm_event(u'alarm-deleted', tenant_id,
                               alarm_definition_id, alarm_metric_rows,
                               sub_alarm_rows)

    @resource.resource_try_catch_block
    def _alarm_show(self, req_uri, tenant_id, alarm_id):

        alarm_rows = self._alarms_repo.get_alarm(tenant_id, alarm_id)

        first_row = True
        for alarm_row in alarm_rows:
            if first_row:
                ad = {u'id': alarm_row['alarm_definition_id'],
                      u'name': alarm_row['alarm_definition_name'],
                      u'severity': alarm_row['severity'], }
                helpers.add_links_to_resource(ad,
                                              re.sub('alarms',
                                                     'alarm-definitions',
                                                     req_uri))

                metrics = []
                alarm = {u'id': alarm_row['alarm_id'], u'metrics': metrics,
                         u'state': alarm_row['state'],
                         u'lifecycle_state': alarm_row['lifecycle_state'],
                         u'link': alarm_row['link'],
                         u'state_updated_timestamp':
                             alarm_row['state_updated_timestamp'].isoformat() +
                             'Z',
                         u'updated_timestamp':
                             alarm_row['updated_timestamp'].isoformat() + 'Z',
                         u'created_timestamp':
                             alarm_row['created_timestamp'].isoformat() + 'Z',
                         u'alarm_definition': ad}
                helpers.add_links_to_resource(alarm, req_uri)

                first_row = False

            dimensions = {}
            metric = {u'name': alarm_row['metric_name'],
                      u'dimensions': dimensions}

            if alarm_row['metric_dimensions']:
                for dimension in alarm_row['metric_dimensions'].split(','):
                    parsed_dimension = dimension.split('=')
                    dimensions[parsed_dimension[0]] = parsed_dimension[1]

            metrics.append(metric)

        return alarm

    @resource.resource_try_catch_block
    def _alarm_list(self, req_uri, tenant_id, query_parms, offset, limit):

        alarm_rows = self._alarms_repo.get_alarms(tenant_id, query_parms,
                                                  offset, limit)

        result = []

        if not alarm_rows:
            return result

        # Forward declaration
        alarm = {}
        prev_alarm_id = None
        for alarm_row in alarm_rows:
            if prev_alarm_id != alarm_row['alarm_id']:
                if prev_alarm_id is not None:
                    result.append(alarm)

                ad = {u'id': alarm_row['alarm_definition_id'],
                      u'name': alarm_row['alarm_definition_name'],
                      u'severity': alarm_row['severity'], }
                helpers.add_links_to_resource(ad,
                                              re.sub('alarms',
                                                     'alarm-definitions',
                                                     req_uri))

                metrics = []
                alarm = {u'id': alarm_row['alarm_id'], u'metrics': metrics,
                         u'state': alarm_row['state'],
                         u'lifecycle_state': alarm_row['lifecycle_state'],
                         u'link': alarm_row['link'],
                         u'state_updated_timestamp':
                             alarm_row['state_updated_timestamp'].isoformat() +
                             'Z',
                         u'updated_timestamp':
                             alarm_row['updated_timestamp'].isoformat() + 'Z',
                         u'created_timestamp':
                             alarm_row['created_timestamp'].isoformat() + 'Z',
                         u'alarm_definition': ad}
                helpers.add_links_to_resource(alarm, req_uri)

                prev_alarm_id = alarm_row['alarm_id']

            dimensions = {}
            metric = {u'name': alarm_row['metric_name'],
                      u'dimensions': dimensions}

            if alarm_row['metric_dimensions']:
                for dimension in alarm_row['metric_dimensions'].split(','):
                    parsed_dimension = dimension.split('=')
                    dimensions[parsed_dimension[0]] = parsed_dimension[1]

            metrics.append(metric)

        result.append(alarm)

        return helpers.paginate(result, req_uri, limit)


class AlarmsStateHistory(alarms_api_v2.AlarmsStateHistoryV2API,
                         alarming.Alarming):
    def __init__(self):
        try:
            super(AlarmsStateHistory, self).__init__()
            self._region = cfg.CONF.region
            self._default_authorized_roles = (
                cfg.CONF.security.default_authorized_roles)
            self._alarms_repo = simport.load(
                cfg.CONF.repositories.alarms_driver)()
            self._metrics_repo = simport.load(
                cfg.CONF.repositories.metrics_driver)()

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def on_get(self, req, res, alarm_id=None):

        if alarm_id is None:
            helpers.validate_authorization(req, self._default_authorized_roles)
            tenant_id = helpers.get_tenant_id(req)
            start_timestamp = helpers.get_query_starttime_timestamp(req, False)
            end_timestamp = helpers.get_query_endtime_timestamp(req, False)
            query_parms = falcon.uri.parse_query_string(req.query_string)
            offset = helpers.get_query_param(req, 'offset')
            limit = helpers.get_limit(req)

            result = self._alarm_history_list(tenant_id, start_timestamp,
                                              end_timestamp, query_parms,
                                              req.uri, offset, limit)

            res.body = helpers.dumpit_utf8(result)
            res.status = falcon.HTTP_200

        else:
            helpers.validate_authorization(req, self._default_authorized_roles)
            tenant_id = helpers.get_tenant_id(req)
            offset = helpers.get_query_param(req, 'offset')
            limit = helpers.get_limit(req)

            result = self._alarm_history(tenant_id, [alarm_id], req.uri,
                                         offset, limit)

            res.body = helpers.dumpit_utf8(result)
            res.status = falcon.HTTP_200

    @resource.resource_try_catch_block
    def _alarm_history_list(self, tenant_id, start_timestamp,
                            end_timestamp, query_parms, req_uri, offset,
                            limit):

        # get_alarms expects 'metric_dimensions' for dimensions key.
        if 'dimensions' in query_parms:
            new_query_parms = {'metric_dimensions': query_parms['dimensions']}
        else:
            new_query_parms = {}

        alarm_rows = self._alarms_repo.get_alarms(tenant_id, new_query_parms,
                                                  None, None)
        alarm_id_list = [alarm_row['alarm_id'] for alarm_row in alarm_rows]

        result = self._metrics_repo.alarm_history(tenant_id, alarm_id_list,
                                                  offset, limit,
                                                  start_timestamp,
                                                  end_timestamp)

        return helpers.paginate(result, req_uri, limit)

    @resource.resource_try_catch_block
    def _alarm_history(self, tenant_id, alarm_id, req_uri, offset, limit):

        result = self._metrics_repo.alarm_history(tenant_id, alarm_id, offset,
                                                  limit)

        return helpers.paginate(result, req_uri, limit)
