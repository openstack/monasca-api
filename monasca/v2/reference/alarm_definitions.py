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
import json

from pyparsing import ParseException
import falcon
from oslo.config import cfg

from monasca.common.repositories import exceptions
from monasca.common import resource_api
from monasca.api.alarm_definitions_api_v2 import AlarmDefinitionsV2API
from monasca.expression_parser.alarm_expr_parser import AlarmExprParser
from monasca.openstack.common import log
from monasca.v2.reference import helpers
from monasca.v2.common.schemas import \
    alarm_definition_request_body_schema as schema_alarms
from monasca.v2.common.schemas import exceptions as schemas_exceptions
from monasca.v2.reference.helpers import read_json_msg_body
from monasca.common.messaging import exceptions as message_queue_exceptions


LOG = log.getLogger(__name__)


class AlarmDefinitions(AlarmDefinitionsV2API):
    def __init__(self, global_conf):
        try:
            super(AlarmDefinitions, self).__init__(global_conf)

            self._region = cfg.CONF.region

            self._default_authorized_roles = \
                cfg.CONF.security.default_authorized_roles
            self._delegate_authorized_roles = \
                cfg.CONF.security.delegate_authorized_roles
            self._post_metrics_authorized_roles = \
                cfg.CONF.security.default_authorized_roles + \
                cfg.CONF.security.agent_authorized_roles

            self._message_queue = resource_api.init_driver('monasca.messaging',
                                                           cfg.CONF.messaging.driver,
                                                           (['events']))

            self._alarm_definitions_repo = resource_api.init_driver(
                'monasca.repositories',
                cfg.CONF.repositories.alarm_definitions_driver)

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    @resource_api.Restify('/v2.0/alarm-definitions', method='post')
    def do_post_alarm_definitions(self, req, res):
        helpers.validate_authorization(req, self._default_authorized_roles)

        alarm_definition = read_json_msg_body(req)

        self._validate_alarm_definition(alarm_definition)

        tenant_id = helpers.get_tenant_id(req)
        name = get_query_alarm_definition_name(alarm_definition)
        expression = get_query_alarm_definition_expression(alarm_definition)
        description = get_query_alarm_definition_description(alarm_definition)
        severity = get_query_alarm_definition_severity(alarm_definition)
        match_by = get_query_alarm_definition_match_by(alarm_definition)
        alarm_actions = get_query_alarm_definition_alarm_actions(
            alarm_definition)
        undetermined_actions = get_query_alarm_definition_undetermined_actions(
            alarm_definition)
        ok_actions = get_query_ok_actions(alarm_definition)

        result = self._alarm_definition_create(tenant_id, name, expression,
                                               description, severity, match_by,
                                               alarm_actions,
                                               undetermined_actions,
                                               ok_actions)

        helpers.add_links_to_resource(result, req.uri)
        res.body = json.dumps(result, ensure_ascii=False).encode('utf8')
        res.status = falcon.HTTP_201

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

    def _validate_alarm_definition(self, alarm_definition):

        try:
            schema_alarms.validate(alarm_definition)
        except schemas_exceptions.ValidationException as ex:
            LOG.debug(ex)
            raise falcon.HTTPBadRequest('Bad reqeust', ex.message)

    def _alarm_definition_create(self, tenant_id, name, expression,
                                 description, severity, match_by,
                                 alarm_actions, undetermined_actions,
                                 ok_actions):
        try:
            sub_expr_list = AlarmExprParser(expression).sub_expr_list

            alarm_definition_id = \
                self._alarm_definitions_repo.create_alarm_definition(
                tenant_id, name, expression, sub_expr_list, description,
                severity, match_by, alarm_actions, undetermined_actions,
                ok_actions)

            self._send_alarm_definition_created_event(tenant_id,
                                                      alarm_definition_id,
                                                      name, expression,
                                                      sub_expr_list,
                                                      description, match_by)
            result = (
                {u'alarm_actions': alarm_actions, u'ok_actions': ok_actions,
                 u'description': description, u'match_by': match_by,
                 u'severity': severity.lower(), u'actions_enabled': u'true',
                 u'undetermined_actions': undetermined_actions,
                 u'expression': expression, u'id': alarm_definition_id,
                 u'name': name})

            return result

        except ParseException as ex:
            LOG.exception(ex)
            title = "Invalid alarm expression".encode('utf8')
            msg = "parser failed on expression '{}' at column {}".format(
                expression.encode('utf8'), str(ex.column).encode('utf'))
            raise falcon.HTTPBadRequest(title, msg)
        except exceptions.RepositoryException as ex:
            LOG.exception(ex)
            msg = "".join(ex.message.args)
            raise falcon.HTTPInternalServerError('Service unavailable', msg)

    def _send_alarm_definition_created_event(self, tenant_id,
                                             alarm_definition_id, name,
                                             expression, sub_expr_list,
                                             description, match_by):

        alarm_definition_created_event_msg = {
            u'alarm-definition-created': {u'tenantId': tenant_id,
                                          u'alarmDefinitionId':
                                              alarm_definition_id,
                                          u'alarmName': name,
                                          u'alarmDescription': description,
                                          u'alarmExpression': expression,
                                          u'matchBy': match_by}}

        sub_expr_event_msg = {}
        for sub_expr in sub_expr_list:
            sub_expr_event_msg[sub_expr.id] = {
                u'function': sub_expr.normalized_func}
            metric_definition = {u'name': sub_expr.normalized_metric_name}
            sub_expr_event_msg[sub_expr.id][
                u'metricDefinition'] = metric_definition
            dimensions = {}
            for dimension in sub_expr.dimensions_as_list:
                parsed_dimension = dimension.split("=")
                dimensions[parsed_dimension[0]] = parsed_dimension[1]
            metric_definition[u'dimensions'] = dimensions
            sub_expr_event_msg[sub_expr.id][
                u'operator'] = sub_expr.normalized_operator
            sub_expr_event_msg[sub_expr.id][
                u'threshold'] = sub_expr.threshold
            sub_expr_event_msg[sub_expr.id][u'period'] = sub_expr.period
            sub_expr_event_msg[sub_expr.id][u'periods'] = sub_expr.periods
            sub_expr_event_msg[sub_expr.id][
                u'expression'] = sub_expr.fmtd_sub_expr_str

        alarm_definition_created_event_msg[u'alarm-definition-created'][
            u'alarmSubExpressions'] = sub_expr_event_msg

        self._send_event(alarm_definition_created_event_msg)

    def _send_event(self, event_msg):
        try:
            self._message_queue.send_message(
                json.dumps(event_msg, ensure_ascii=False).encode('utf8'))
        except message_queue_exceptions.MessageQueueException as ex:
            LOG.exception(ex)
            raise falcon.HTTPInternalServerError(
                'Message queue service unavailable'.encode('utf8'),
                ex.message.encode('utf8'))


def get_query_alarm_definition_name(alarm_definition):
    try:
        if 'name' in alarm_definition:
            name = alarm_definition['name']
            return name
        else:
            raise Exception("Missing name")
    except Exception as ex:
        LOG.debug(ex)
        raise falcon.HTTPBadRequest('Bad request', ex.message)


def get_query_alarm_definition_expression(alarm_definition):
    try:
        if 'expression' in alarm_definition:
            expression = alarm_definition['expression']
            return expression
        else:
            raise Exception("Missing expression")
    except Exception as ex:
        LOG.debug(ex)
        raise falcon.HTTPBadRequest('Bad request', ex.message)


def get_query_alarm_definition_description(alarm_definition):
    if 'description' in alarm_definition:
        return alarm_definition['description']
    else:
        return ''


def get_query_alarm_definition_severity(alarm_definition):
    if 'severity' in alarm_definition:
        severity = alarm_definition['severity']
        severity = severity.decode('utf8').lower()
        if severity not in ['low', 'medium', 'high', 'critical']:
            raise falcon.HTTPBadRequest('Bad request, Invalid severity')
        return severity
    else:
        return ''


def get_query_alarm_definition_match_by(alarm_definition):
    if 'match_by' in alarm_definition:
        match_by = alarm_definition['match_by']
        return match_by
    else:
        return []


def get_query_alarm_definition_alarm_actions(alarm_definition):
    if 'alarm_actions' in alarm_definition:
        alarm_actions = alarm_definition['alarm_actions']
        return alarm_actions
    else:
        return []


def get_query_alarm_definition_undetermined_actions(alarm_definition):
    if 'undetermined_actions' in alarm_definition:
        undetermined_actions = alarm_definition['undetermined_actions']
        return undetermined_actions
    else:
        return []


def get_query_ok_actions(alarm_definition):
    if 'ok_actions' in alarm_definition:
        ok_actions = alarm_definition['ok_actions']
        return ok_actions
    else:
        return []
