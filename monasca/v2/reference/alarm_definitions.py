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
import re

import falcon
from oslo.config import cfg
import pyparsing

from monasca.api.alarm_definitions_api_v2 import AlarmDefinitionsV2API
from monasca.common.repositories import exceptions
from monasca.common import resource_api
import monasca.expression_parser.alarm_expr_parser
from monasca.openstack.common import log
from monasca.v2.common.schemas import (alarm_definition_request_body_schema
                                       as schema_alarms)
from monasca.v2.common.schemas import exceptions as schemas_exceptions
from monasca.v2.reference.alarming import Alarming
from monasca.v2.reference import helpers
from monasca.v2.reference.helpers import read_json_msg_body
from monasca.v2.reference.resource import resource_try_catch_block


LOG = log.getLogger(__name__)


class AlarmDefinitions(AlarmDefinitionsV2API, Alarming):

    def __init__(self, global_conf):

        try:

            super(AlarmDefinitions, self).__init__(global_conf)

            self._region = cfg.CONF.region

            self._default_authorized_roles = (
                cfg.CONF.security.default_authorized_roles)
            self._delegate_authorized_roles = (
                cfg.CONF.security.delegate_authorized_roles)
            self._post_metrics_authorized_roles = (
                cfg.CONF.security.default_authorized_roles +
                cfg.CONF.security.agent_authorized_roles)

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

        helpers.validate_authorization(req, self._default_authorized_roles)
        tenant_id = helpers.get_tenant_id(req)

        result = self._alarm_definition_show(tenant_id, id)

        helpers.add_links_to_resource(result, re.sub('/' + id, '', req.uri))
        res.body = json.dumps(result, ensure_ascii=False).encode('utf8')
        res.status = falcon.HTTP_200

    @resource_api.Restify('/v2.0/alarm-definitions/{id}', method='put')
    def do_put_alarm_definitions(self, req, res, id):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarm-definitions', method='get')
    def do_get_alarm_definitions(self, req, res):

        helpers.validate_authorization(req, self._default_authorized_roles)
        tenant_id = helpers.get_tenant_id(req)
        name = helpers.get_query_name(req)
        dimensions = helpers.get_query_dimensions(req)

        result = self._alarm_definition_list(tenant_id, name, dimensions,
                                             req.uri)

        res.body = json.dumps(result, ensure_ascii=False).encode('utf8')
        res.status = falcon.HTTP_200

    @resource_api.Restify('/v2.0/alarm-definitions/{id}', method='patch')
    def do_patch_alarm_definitions(self, req, res, id):
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarm-definitions/{id}', method='delete')
    def do_delete_alarm_definitions(self, req, res, id):

        helpers.validate_authorization(req, self._default_authorized_roles)
        tenant_id = helpers.get_tenant_id(req)
        self._alarm_definition_delete(tenant_id, id)
        res.status = falcon.HTTP_204

    @resource_try_catch_block
    def _alarm_definition_show(self, tenant_id, id):

        alarm_definition_row = (
            self._alarm_definitions_repo.get_alarm_definition(tenant_id, id))

        match_by = get_comma_separated_str_as_list(
            alarm_definition_row['match_by'])

        alarm_actions_list = get_comma_separated_str_as_list(
            alarm_definition_row['alarm_actions'])

        ok_actions_list = get_comma_separated_str_as_list(
            alarm_definition_row['ok_actions'])

        undetermined_actions_list = get_comma_separated_str_as_list(
            alarm_definition_row['undetermined_actions'])

        result = {
            u'actions_enabled': alarm_definition_row['actions_enabled'] == 1,
            u'alarm_actions': alarm_actions_list,
            u'undetermined_actions': undetermined_actions_list,
            u'ok_actions': ok_actions_list,
            u'description': alarm_definition_row['description'].decode(
                'utf8'),
            u'expression': alarm_definition_row['expression'].decode('utf8'),
            u'id': alarm_definition_row['id'].decode('utf8'),
            u'match_by': match_by,
            u'name': alarm_definition_row['name'].decode('utf8'),
            u'severity': alarm_definition_row['severity'].decode('utf8')}

        return result

    @resource_try_catch_block
    def _alarm_definition_delete(self, tenant_id, id):

        sub_alarm_definition_rows = (
            self._alarm_definitions_repo.get_sub_alarm_definitions(id))
        alarm_metric_rows = self._alarm_definitions_repo.get_alarm_metrics(
            tenant_id, id)
        sub_alarm_rows = self._alarm_definitions_repo.get_sub_alarms(
            tenant_id, id)

        if not self._alarm_definitions_repo.delete_alarm_definition(
                tenant_id, id):
            raise falcon.HTTPNotFound

        self._send_alarm_definition_deleted_event(id,
                                                  sub_alarm_definition_rows)

        self._send_alarm_event(u'alarm-deleted', tenant_id, id,
                               alarm_metric_rows, sub_alarm_rows)

    @resource_try_catch_block
    def _alarm_definition_list(self, tenant_id, name, dimensions, req_uri):

        alarm_definition_rows = (
            self._alarm_definitions_repo.get_alarm_definitions(tenant_id, name,
                                                               dimensions))

        result = []
        for alarm_definition_row in alarm_definition_rows:

            match_by = get_comma_separated_str_as_list(
                alarm_definition_row['match_by'])

            alarm_actions_list = get_comma_separated_str_as_list(
                alarm_definition_row['alarm_actions'])

            ok_actions_list = get_comma_separated_str_as_list(
                alarm_definition_row['ok_actions'])

            undetermined_actions_list = get_comma_separated_str_as_list(
                alarm_definition_row['undetermined_actions'])

            ad = {u'id': alarm_definition_row['id'].decode('utf8'),
                  u'name': alarm_definition_row['name'].decode("utf8"),
                  u'description': alarm_definition_row['description'].decode(
                      'utf8'),
                  u'expression': alarm_definition_row['expression'].decode(
                      'utf8'), u'match_by': match_by,
                  u'severity': alarm_definition_row['severity'].decode(
                      'utf8'),
                  u'actions_enabled':
                      alarm_definition_row['actions_enabled'] == 1,
                  u'alarm_actions': alarm_actions_list,
                  u'ok_actions': ok_actions_list,
                  u'undetermined_actions': undetermined_actions_list}

            helpers.add_links_to_resource(ad, req_uri)
            result.append(ad)

        return result

    def _validate_alarm_definition(self, alarm_definition):

        try:
            schema_alarms.validate(alarm_definition)
        except schemas_exceptions.ValidationException as ex:
            LOG.debug(ex)
            raise falcon.HTTPBadRequest('Bad request', ex.message)

    @resource_try_catch_block
    def _alarm_definition_create(self, tenant_id, name, expression,
                                 description, severity, match_by,
                                 alarm_actions, undetermined_actions,
                                 ok_actions):
        try:

            sub_expr_list = (
                monasca.expression_parser.alarm_expr_parser.
                AlarmExprParser(expression).sub_expr_list)

        except pyparsing.ParseException as ex:
            LOG.exception(ex)
            title = "Invalid alarm expression".encode('utf8')
            msg = "parser failed on expression '{}' at column {}".format(
                expression.encode('utf8'), str(ex.column).encode('utf8'))
            raise falcon.HTTPBadRequest(title, msg)

        alarm_definition_id = (
            self._alarm_definitions_repo.
            create_alarm_definition(tenant_id,
                                    name,
                                    expression,
                                    sub_expr_list,
                                    description,
                                    severity,
                                    match_by,
                                    alarm_actions,
                                    undetermined_actions,
                                    ok_actions))

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

    def _send_alarm_definition_deleted_event(self, alarm_definition_id,
                                             sub_alarm_definition_rows):

        sub_alarm_definition_deleted_event_msg = {}
        alarm_definition_deleted_event_msg = {u"alarm-definition-deleted": {
            u"alarmDefinitionId": alarm_definition_id,
            u'subAlarmMetricDefinitions':
                sub_alarm_definition_deleted_event_msg}}

        for sub_alarm_definition in sub_alarm_definition_rows:
            sub_alarm_definition_deleted_event_msg[
                sub_alarm_definition['id']] = {
                u'name': sub_alarm_definition['metric_name']}
            dimensions = {}
            sub_alarm_definition_deleted_event_msg[sub_alarm_definition['id']][
                u'dimensions'] = dimensions
            if sub_alarm_definition['dimensions']:
                for dimension in sub_alarm_definition['dimensions'].split(','):
                    parsed_dimension = dimension.split('=')
                    dimensions[parsed_dimension[0]] = parsed_dimension[1]

        self._send_event(alarm_definition_deleted_event_msg)

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
            sub_expr_event_msg[sub_expr.id][u'threshold'] = sub_expr.threshold
            sub_expr_event_msg[sub_expr.id][u'period'] = sub_expr.period
            sub_expr_event_msg[sub_expr.id][u'periods'] = sub_expr.periods
            sub_expr_event_msg[sub_expr.id][
                u'expression'] = sub_expr.fmtd_sub_expr_str

        alarm_definition_created_event_msg[u'alarm-definition-created'][
            u'alarmSubExpressions'] = sub_expr_event_msg

        self._send_event(alarm_definition_created_event_msg)


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
            raise falcon.HTTPBadRequest('Bad request', 'Invalid severity')
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


def get_comma_separated_str_as_list(comma_separated_str):

    if not comma_separated_str:
        return []
    else:
        return comma_separated_str.decode('utf8').split(',')