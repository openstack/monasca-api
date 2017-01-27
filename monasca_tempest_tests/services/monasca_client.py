# (C) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
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
from oslo_serialization import jsonutils as json

from tempest import config
from tempest.lib.common import rest_client

CONF = config.CONF


class MonascaClient(rest_client.RestClient):

    def __init__(self, auth_provider):
        super(MonascaClient, self).__init__(
            auth_provider,
            CONF.monitoring.catalog_type,
            CONF.monitoring.region or CONF.identity.region,
            endpoint_type=CONF.monitoring.endpoint_type)

    def get_version(self):
        resp, response_body = self.get('')
        return resp, response_body

    def create_metrics(self, metrics, tenant_id=None):
        uri = 'metrics'
        if tenant_id:
            uri = uri + '?tenant_id=%s' % tenant_id
        request_body = json.dumps(metrics)
        resp, response_body = self.post(uri, request_body)
        return resp, response_body

    def list_metrics(self, query_params=None):
        uri = 'metrics'
        if query_params is not None:
            uri = uri + query_params
        resp, response_body = self.get(uri)
        return resp, json.loads(response_body)

    def list_metrics_names(self, query_params=None):
        uri = 'metrics/names'
        if query_params is not None:
            uri = uri + query_params
        resp, response_body = self.get(uri)
        return resp, json.loads(response_body)

    def list_dimension_names(self, query_params=None):
        uri = 'metrics/dimensions/names'
        if query_params is not None:
            uri = uri + query_params
        resp, response_body = self.get(uri)
        return resp, json.loads(response_body)

    def list_dimension_values(self, query_params=None):
        uri = 'metrics/dimensions/names/values'
        if query_params is not None:
            uri = uri + query_params
        resp, response_body = self.get(uri)
        return resp, json.loads(response_body)

    def list_measurements(self, query_params=None):
        uri = 'metrics/measurements'
        if query_params is not None:
            uri = uri + query_params
        resp, response_body = self.get(uri)
        return resp, json.loads(response_body)

    def list_statistics(self, query_params=None):
        uri = 'metrics/statistics'
        if query_params is not None:
            uri = uri + query_params
        resp, response_body = self.get(uri)
        return resp, json.loads(response_body)

    def create_notifications(self, notification):
        uri = 'notification-methods'
        request_body = json.dumps(notification)
        resp, response_body = self.post(uri, request_body)
        return resp, json.loads(response_body)

    def create_notification_method(self,
                                   name=None,
                                   type=None,
                                   address=None,
                                   period=None):
        uri = 'notification-methods'
        request_body = {}
        if name is not None:
            request_body['name'] = name
        if type is not None:
            request_body['type'] = type
        if address is not None:
            request_body['address'] = address
        if period is not None:
            request_body['period'] = period
        resp, response_body = self.post(uri, json.dumps(request_body))
        return resp, json.loads(response_body)

    def delete_notification_method(self, id):
        uri = 'notification-methods/' + id
        resp, response_body = self.delete(uri)
        return resp, response_body

    def get_notification_method(self, id):
        uri = 'notification-methods/' + id
        resp, response_body = self.get(uri)
        return resp, json.loads(response_body)

    def list_notification_methods(self, query_params=None):
        uri = 'notification-methods'
        if query_params is not None:
            uri = uri + query_params
        resp, response_body = self.get(uri)
        return resp, json.loads(response_body)

    def update_notification_method(self, id, name, type, address, period=None):
        uri = 'notification-methods/' + id
        request_body = {}
        request_body['name'] = name
        request_body['type'] = type
        request_body['address'] = address
        if period is not None:
            request_body['period'] = period
        resp, response_body = self.put(uri, json.dumps(request_body))
        return resp, json.loads(response_body)

    def patch_notification_method(self, id,
                                  name=None, type=None,
                                  address=None, period=None):
        uri = 'notification-methods/' + id
        request_body = {}
        if name is not None:
            request_body['name'] = name
        if type is not None:
            request_body['type'] = type
        if address is not None:
            request_body['address'] = address
        if period is not None:
            request_body['period'] = period
        resp, response_body = self.patch(uri, json.dumps(request_body))
        return resp, json.loads(response_body)

    def list_notification_method_types(self, query_params=None):
        uri = 'notification-methods/types'
        if query_params is not None:
            uri = uri + query_params
        resp, response_body = self.get(uri)
        return resp, json.loads(response_body)

    def create_alarm_definitions(self, alarm_definitions):
        uri = 'alarm-definitions'
        request_body = json.dumps(alarm_definitions)
        resp, response_body = self.post(uri, request_body)
        return resp, json.loads(response_body)

    def list_alarm_definitions(self, query_params=None):
        uri = 'alarm-definitions'
        if query_params is not None:
            uri = uri + query_params
        resp, response_body = self.get(uri)
        return resp, json.loads(response_body)

    def get_alarm_definition(self, id):
        uri = 'alarm-definitions/' + id
        resp, response_body = self.get(uri)
        return resp, json.loads(response_body)

    def delete_alarm_definition(self, id):
        uri = 'alarm-definitions/' + id
        resp, response_body = self.delete(uri)
        return resp, response_body

    def update_alarm_definition(self, id, name, expression, description,
                                actions_enabled, match_by,
                                severity, alarm_actions,
                                ok_actions, undetermined_actions,
                                **kwargs):
        uri = 'alarm-definitions/' + id
        request_body = {}
        request_body['name'] = name
        request_body['expression'] = expression
        request_body['description'] = description
        request_body['actions_enabled'] = actions_enabled
        request_body['match_by'] = match_by
        request_body['severity'] = severity
        request_body['alarm_actions'] = alarm_actions
        request_body['ok_actions'] = ok_actions
        request_body['undetermined_actions'] = undetermined_actions

        for key, value in kwargs.iteritems():
            request_body[key] = value

        resp, response_body = self.put(uri, json.dumps(request_body))
        return resp, json.loads(response_body)

    def patch_alarm_definition(self,
                               id,
                               name=None,
                               description=None,
                               expression=None,
                               actions_enabled=None,
                               match_by=None,
                               severity=None,
                               alarm_actions=None,
                               ok_actions=None,
                               undetermined_actions=None,
                               **kwargs):
        uri = 'alarm-definitions/' + id
        request_body = {}
        if name is not None:
            request_body['name'] = name
        if description is not None:
            request_body['description'] = description
        if expression is not None:
            request_body['expression'] = expression
        if actions_enabled is not None:
            request_body['actions_enabled'] = actions_enabled
        if match_by is not None:
            request_body['match_by'] = match_by
        if severity is not None:
            request_body['severity'] = severity
        if alarm_actions is not None:
            request_body['alarm_actions'] = alarm_actions
        if ok_actions is not None:
            request_body['ok_actions'] = ok_actions
        if undetermined_actions is not None:
            request_body['undetermined_actions'] = undetermined_actions

        for key, value in kwargs.iteritems():
            request_body[key] = value

        resp, response_body = self.patch(uri, json.dumps(request_body))
        return resp, json.loads(response_body)

    def list_alarms(self, query_params=None):
        uri = 'alarms'
        if query_params is not None:
            uri = uri + query_params
        resp, response_body = self.get(uri)
        return resp, json.loads(response_body)

    def get_alarm(self, id):
        uri = 'alarms/' + id
        resp, response_body = self.get(uri)
        return resp, json.loads(response_body)

    def delete_alarm(self, id):
        uri = 'alarms/' + id
        resp, response_body = self.delete(uri)
        return resp, response_body

    def update_alarm(self, id, state, lifecycle_state, link, **kwargs):
        uri = 'alarms/' + id
        request_body = {}
        request_body['state'] = state
        request_body['lifecycle_state'] = lifecycle_state
        request_body['link'] = link

        for key, value in kwargs.iteritems():
            request_body[key] = value

        resp, response_body = self.put(uri, json.dumps(request_body))
        return resp, json.loads(response_body)

    def patch_alarm(self, id, state=None, lifecycle_state=None, link=None,
                    **kwargs):
        uri = 'alarms/' + id
        request_body = {}
        if state is not None:
            request_body['state'] = state
        if lifecycle_state is not None:
            request_body['lifecycle_state'] = lifecycle_state
        if link is not None:
            request_body['link'] = link

        for key, value in kwargs.iteritems():
            request_body[key] = value

        resp, response_body = self.patch(uri, json.dumps(request_body))
        return resp, json.loads(response_body)

    def count_alarms(self, query_params=None):
        uri = 'alarms/count'
        if query_params is not None:
            uri += query_params
        resp, response_body = self.get(uri)
        return resp, json.loads(response_body)

    def list_alarms_state_history(self, query_params=None):
        uri = 'alarms/state-history'
        if query_params is not None:
            uri = uri + query_params
        resp, response_body = self.get(uri)
        return resp, json.loads(response_body)

    def list_alarm_state_history(self, id, query_params=None):
        uri = 'alarms/' + id + '/state-history'
        if query_params is not None:
            uri = uri + query_params
        resp, response_body = self.get(uri)
        return resp, json.loads(response_body)

    # For Negative Tests
    def update_alarm_definition_with_no_ok_actions(self, id, name,
                                                   expression, description,
                                                   actions_enabled, match_by,
                                                   severity, alarm_actions,
                                                   undetermined_actions,
                                                   **kwargs):
        uri = 'alarm-definitions/' + id
        request_body = {}
        request_body['name'] = name
        request_body['expression'] = expression
        request_body['description'] = description
        request_body['actions_enabled'] = actions_enabled
        request_body['match_by'] = match_by
        request_body['severity'] = severity
        request_body['alarm_actions'] = alarm_actions
        request_body['undetermined_actions'] = undetermined_actions

        for key, value in kwargs.iteritems():
            request_body[key] = value

        resp, response_body = self.put(uri, json.dumps(request_body))
        return resp, json.loads(response_body)

    def update_notification_method_with_no_address(self, id, name, type,
                                                   period=None):
        uri = 'notification-methods/' + id
        request_body = {}
        request_body['name'] = name
        request_body['type'] = type
        if period is not None:
            request_body['period'] = period
        resp, response_body = self.put(uri, json.dumps(request_body))
        return resp, json.loads(response_body)
