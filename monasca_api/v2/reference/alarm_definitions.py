# (C) Copyright 2014-2017 Hewlett Packard Enterprise Development LP
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
from monasca_common.simport import simport
from monasca_common.validation import metrics as metric_validation
from oslo_config import cfg
from oslo_log import log
import pyparsing

from monasca_api.api import alarm_definitions_api_v2
from monasca_api.common.repositories import exceptions
import monasca_api.expression_parser.alarm_expr_parser
from monasca_api.v2.common.exceptions import HTTPUnprocessableEntityError
from monasca_api.v2.common.schemas import (
    alarm_definition_request_body_schema as schema_alarms)
from monasca_api.v2.common import validation
from monasca_api.v2.reference import alarming
from monasca_api.v2.reference import helpers
from monasca_api.v2.reference import resource

LOG = log.getLogger(__name__)


class AlarmDefinitions(alarm_definitions_api_v2.AlarmDefinitionsV2API,
                       alarming.Alarming):

    def __init__(self):
        try:
            super(AlarmDefinitions, self).__init__()
            self._region = cfg.CONF.region
            self._default_authorized_roles = (
                cfg.CONF.security.default_authorized_roles)
            self._get_alarmdefs_authorized_roles = (
                cfg.CONF.security.default_authorized_roles +
                cfg.CONF.security.read_only_authorized_roles)
            self._alarm_definitions_repo = simport.load(
                cfg.CONF.repositories.alarm_definitions_driver)()

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    @resource.resource_try_catch_block
    def on_post(self, req, res):
        helpers.validate_authorization(req, self._default_authorized_roles)

        alarm_definition = helpers.read_json_msg_body(req)

        self._validate_alarm_definition(alarm_definition)

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

        result = self._alarm_definition_create(req.project_id, name, expression,
                                               description, severity, match_by,
                                               alarm_actions,
                                               undetermined_actions,
                                               ok_actions)

        helpers.add_links_to_resource(result, req.uri)
        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_201

    @resource.resource_try_catch_block
    def on_get(self, req, res, alarm_definition_id=None):
        if alarm_definition_id is None:
            helpers.validate_authorization(req, self._get_alarmdefs_authorized_roles)
            name = helpers.get_query_name(req)
            dimensions = helpers.get_query_dimensions(req)
            severity = helpers.get_query_param(req, "severity", default_val=None)
            if severity is not None:
                validation.validate_severity_query(severity)
                severity = severity.upper()
            sort_by = helpers.get_query_param(req, 'sort_by', default_val=None)
            if sort_by is not None:
                if isinstance(sort_by, basestring):
                    sort_by = sort_by.split(',')

                allowed_sort_by = {'id', 'name', 'severity',
                                   'updated_at', 'created_at'}

                validation.validate_sort_by(sort_by, allowed_sort_by)

            offset = helpers.get_query_param(req, 'offset')
            if offset is not None and not isinstance(offset, int):
                try:
                    offset = int(offset)
                except Exception:
                    raise HTTPUnprocessableEntityError('Unprocessable Entity',
                                                       'Offset value {} must be an integer'.format(offset))
            result = self._alarm_definition_list(req.project_id, name,
                                                 dimensions, severity,
                                                 req.uri, sort_by,
                                                 offset, req.limit)

            res.body = helpers.dumpit_utf8(result)
            res.status = falcon.HTTP_200

        else:
            helpers.validate_authorization(req, self._get_alarmdefs_authorized_roles)

            result = self._alarm_definition_show(req.project_id,
                                                 alarm_definition_id)

            helpers.add_links_to_resource(result,
                                          re.sub('/' + alarm_definition_id, '',
                                                 req.uri))
            res.body = helpers.dumpit_utf8(result)
            res.status = falcon.HTTP_200

    @resource.resource_try_catch_block
    def on_put(self, req, res, alarm_definition_id):

        helpers.validate_authorization(req, self._default_authorized_roles)

        alarm_definition = helpers.read_json_msg_body(req)

        self._validate_alarm_definition(alarm_definition, require_all=True)

        name = get_query_alarm_definition_name(alarm_definition)
        expression = get_query_alarm_definition_expression(alarm_definition)
        actions_enabled = (
            get_query_alarm_definition_actions_enabled(alarm_definition))
        description = get_query_alarm_definition_description(alarm_definition)
        alarm_actions = get_query_alarm_definition_alarm_actions(alarm_definition)
        ok_actions = get_query_ok_actions(alarm_definition)
        undetermined_actions = get_query_alarm_definition_undetermined_actions(
            alarm_definition)
        match_by = get_query_alarm_definition_match_by(alarm_definition)
        severity = get_query_alarm_definition_severity(alarm_definition)

        result = self._alarm_definition_update_or_patch(req.project_id,
                                                        alarm_definition_id,
                                                        name,
                                                        expression,
                                                        actions_enabled,
                                                        description,
                                                        alarm_actions,
                                                        ok_actions,
                                                        undetermined_actions,
                                                        match_by,
                                                        severity,
                                                        patch=False)

        helpers.add_links_to_resource(result, req.uri)
        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_200

    @resource.resource_try_catch_block
    def on_patch(self, req, res, alarm_definition_id):

        helpers.validate_authorization(req, self._default_authorized_roles)

        alarm_definition = helpers.read_json_msg_body(req)

        # Optional args
        name = get_query_alarm_definition_name(alarm_definition,
                                               return_none=True)
        expression = get_query_alarm_definition_expression(alarm_definition,
                                                           return_none=True)
        actions_enabled = (
            get_query_alarm_definition_actions_enabled(alarm_definition,
                                                       return_none=True))

        description = get_query_alarm_definition_description(alarm_definition,
                                                             return_none=True)
        alarm_actions = get_query_alarm_definition_alarm_actions(
            alarm_definition, return_none=True)
        ok_actions = get_query_ok_actions(alarm_definition, return_none=True)
        undetermined_actions = get_query_alarm_definition_undetermined_actions(
            alarm_definition, return_none=True)
        match_by = get_query_alarm_definition_match_by(alarm_definition,
                                                       return_none=True)
        severity = get_query_alarm_definition_severity(alarm_definition,
                                                       return_none=True)

        result = self._alarm_definition_update_or_patch(req.project_id,
                                                        alarm_definition_id,
                                                        name,
                                                        expression,
                                                        actions_enabled,
                                                        description,
                                                        alarm_actions,
                                                        ok_actions,
                                                        undetermined_actions,
                                                        match_by,
                                                        severity,
                                                        patch=True)

        helpers.add_links_to_resource(result, req.uri)
        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_200

    @resource.resource_try_catch_block
    def on_delete(self, req, res, alarm_definition_id):

        helpers.validate_authorization(req, self._default_authorized_roles)
        self._alarm_definition_delete(req.project_id, alarm_definition_id)
        res.status = falcon.HTTP_204

    def _validate_name_not_conflicting(self, tenant_id, name, expected_id=None):
        definitions = self._alarm_definitions_repo.get_alarm_definitions(tenant_id=tenant_id,
                                                                         name=name,
                                                                         dimensions=None,
                                                                         severity=None,
                                                                         sort_by=None,
                                                                         offset=None,
                                                                         limit=0)
        if definitions:
            if not expected_id:
                LOG.warning("Found existing definition for {} with tenant_id {}".format(name, tenant_id))
                raise exceptions.AlreadyExistsException("An alarm definition with the name {} already exists"
                                                        .format(name))

            found_definition_id = definitions[0]['id']
            if found_definition_id != expected_id:
                LOG.warning("Found existing alarm definition for {} with tenant_id {} with unexpected id {}"
                            .format(name, tenant_id, found_definition_id))
                raise exceptions.AlreadyExistsException(
                    "An alarm definition with the name {} already exists with id {}"
                    .format(name, found_definition_id))

    def _alarm_definition_show(self, tenant_id, id):

        alarm_definition_row = (
            self._alarm_definitions_repo.get_alarm_definition(tenant_id, id))

        return self._build_alarm_definition_show_result(alarm_definition_row)

    def _build_alarm_definition_show_result(self, alarm_definition_row):

        match_by = get_comma_separated_str_as_list(
            alarm_definition_row['match_by'])

        alarm_actions_list = get_comma_separated_str_as_list(
            alarm_definition_row['alarm_actions'])

        ok_actions_list = get_comma_separated_str_as_list(
            alarm_definition_row['ok_actions'])

        undetermined_actions_list = get_comma_separated_str_as_list(
            alarm_definition_row['undetermined_actions'])

        description = (alarm_definition_row['description']
                       if alarm_definition_row['description'] is not None else None)

        expression = alarm_definition_row['expression'].decode('utf8')
        is_deterministic = is_definition_deterministic(expression)

        result = {
            u'actions_enabled': alarm_definition_row['actions_enabled'] == 1,
            u'alarm_actions': alarm_actions_list,
            u'undetermined_actions': undetermined_actions_list,
            u'ok_actions': ok_actions_list,
            u'description': description,
            u'expression': expression,
            u'deterministic': is_deterministic,
            u'id': alarm_definition_row['id'].decode('utf8'),
            u'match_by': match_by,
            u'name': alarm_definition_row['name'].decode('utf8'),
            u'severity': alarm_definition_row['severity'].decode(
                'utf8').upper()}

        return result

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
                               alarm_metric_rows, sub_alarm_rows, None, None)

    def _alarm_definition_list(self, tenant_id, name, dimensions, severity, req_uri, sort_by,
                               offset, limit):

        alarm_definition_rows = (
            self._alarm_definitions_repo.get_alarm_definitions(tenant_id, name,
                                                               dimensions, severity, sort_by,
                                                               offset, limit))

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

            expression = alarm_definition_row['expression']
            is_deterministic = is_definition_deterministic(expression)
            ad = {u'id': alarm_definition_row['id'],
                  u'name': alarm_definition_row['name'],
                  u'description': alarm_definition_row['description'] if (
                      alarm_definition_row['description']) else u'',
                  u'expression': alarm_definition_row['expression'],
                  u'deterministic': is_deterministic,
                  u'match_by': match_by,
                  u'severity': alarm_definition_row['severity'].upper(),
                  u'actions_enabled':
                      alarm_definition_row['actions_enabled'] == 1,
                  u'alarm_actions': alarm_actions_list,
                  u'ok_actions': ok_actions_list,
                  u'undetermined_actions': undetermined_actions_list}

            helpers.add_links_to_resource(ad, req_uri)
            result.append(ad)

        result = helpers.paginate_alarming(result, req_uri, limit)

        return result

    def _validate_alarm_definition(self, alarm_definition, require_all=False):

        try:
            schema_alarms.validate(alarm_definition, require_all=require_all)
            if 'match_by' in alarm_definition:
                for name in alarm_definition['match_by']:
                    metric_validation.validate_dimension_key(name)

        except Exception as ex:
            LOG.debug(ex)
            raise HTTPUnprocessableEntityError('Unprocessable Entity', ex.message)

    def _alarm_definition_update_or_patch(self, tenant_id,
                                          definition_id,
                                          name,
                                          expression,
                                          actions_enabled,
                                          description,
                                          alarm_actions,
                                          ok_actions,
                                          undetermined_actions,
                                          match_by,
                                          severity,
                                          patch):

        if expression:
            try:
                sub_expr_list = (
                    monasca_api.expression_parser.alarm_expr_parser.
                    AlarmExprParser(expression).sub_expr_list)

            except (pyparsing.ParseException,
                    pyparsing.ParseFatalException) as ex:
                LOG.exception(ex)
                title = "Invalid alarm expression".encode('utf8')
                msg = "parser failed on expression '{}' at column {}: {}".format(
                      expression.encode('utf8'), str(ex.column).encode('utf8'),
                      ex.msg.encode('utf8'))
                raise HTTPUnprocessableEntityError(title, msg)
        else:
            sub_expr_list = None

        if name:
            self._validate_name_not_conflicting(tenant_id, name, expected_id=definition_id)

        alarm_def_row, sub_alarm_def_dicts = (
            self._alarm_definitions_repo.update_or_patch_alarm_definition(
                tenant_id,
                definition_id,
                name,
                expression,
                sub_expr_list,
                actions_enabled,
                description,
                alarm_actions,
                ok_actions,
                undetermined_actions,
                match_by,
                severity,
                patch))

        old_sub_alarm_def_event_dict = (
            self._build_sub_alarm_def_update_dict(
                sub_alarm_def_dicts['old']))

        new_sub_alarm_def_event_dict = (
            self._build_sub_alarm_def_update_dict(sub_alarm_def_dicts[
                'new']))

        changed_sub_alarm_def_event_dict = (
            self._build_sub_alarm_def_update_dict(sub_alarm_def_dicts[
                'changed']))

        unchanged_sub_alarm_def_event_dict = (
            self._build_sub_alarm_def_update_dict(sub_alarm_def_dicts[
                'unchanged']))

        result = self._build_alarm_definition_show_result(alarm_def_row)
        # Not all of the passed in parameters will be set if this called
        # from on_patch vs on_update. The alarm-definition-updated event
        # MUST have all of the fields set so use the dict built from the
        # data returned from the database
        alarm_def_event_dict = (
            {u'tenantId': tenant_id,
             u'alarmDefinitionId': definition_id,
             u'alarmName': result['name'],
             u'alarmDescription': result['description'],
             u'alarmExpression': result['expression'],
             u'severity': result['severity'],
             u'matchBy': result['match_by'],
             u'alarmActionsEnabled': result['actions_enabled'],
             u'oldAlarmSubExpressions': old_sub_alarm_def_event_dict,
             u'changedSubExpressions': changed_sub_alarm_def_event_dict,
             u'unchangedSubExpressions': unchanged_sub_alarm_def_event_dict,
             u'newAlarmSubExpressions': new_sub_alarm_def_event_dict})

        alarm_definition_updated_event = (
            {u'alarm-definition-updated': alarm_def_event_dict})

        self.send_event(self.events_message_queue,
                        alarm_definition_updated_event)

        return result

    def _build_sub_alarm_def_update_dict(self, sub_alarm_def_dict):

        sub_alarm_def_update_dict = {}
        for id, sub_alarm_def in sub_alarm_def_dict.items():
            dimensions = {}
            for name, value in sub_alarm_def.dimensions.items():
                dimensions[u'uname'] = value
            sub_alarm_def_update_dict[sub_alarm_def.id] = {}
            sub_alarm_def_update_dict[sub_alarm_def.id][u'function'] = (
                sub_alarm_def.function)
            sub_alarm_def_update_dict[sub_alarm_def.id][
                u'metricDefinition'] = (
                {u'name': sub_alarm_def.metric_name,
                 u'dimensions': dimensions})
            sub_alarm_def_update_dict[sub_alarm_def.id][u'operator'] = (
                sub_alarm_def.operator)
            sub_alarm_def_update_dict[sub_alarm_def.id][u'threshold'] = (
                sub_alarm_def.threshold)
            sub_alarm_def_update_dict[sub_alarm_def.id][u'period'] = (
                sub_alarm_def.period)
            sub_alarm_def_update_dict[sub_alarm_def.id][u'periods'] = (
                sub_alarm_def.periods)
            sub_alarm_def_update_dict[sub_alarm_def.id][u'expression'] = (
                sub_alarm_def.expression)

        return sub_alarm_def_update_dict

    def _alarm_definition_create(self, tenant_id, name, expression,
                                 description, severity, match_by,
                                 alarm_actions, undetermined_actions,
                                 ok_actions):
        try:

            sub_expr_list = (
                monasca_api.expression_parser.alarm_expr_parser.
                AlarmExprParser(expression).sub_expr_list)

        except (pyparsing.ParseException,
                pyparsing.ParseFatalException) as ex:
            LOG.exception(ex)
            title = "Invalid alarm expression".encode('utf8')
            msg = "parser failed on expression '{}' at column {}: {}".format(
                  expression.encode('utf8'), str(ex.column).encode('utf8'),
                  ex.msg.encode('utf8'))
            raise HTTPUnprocessableEntityError(title, msg)

        self._validate_name_not_conflicting(tenant_id, name)

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
             u'severity': severity, u'actions_enabled': u'true',
             u'undetermined_actions': undetermined_actions,
             u'expression': expression, u'id': alarm_definition_id,
             u'deterministic': is_definition_deterministic(expression),
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

        self.send_event(self.events_message_queue,
                        alarm_definition_deleted_event_msg)

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

        self.send_event(self.events_message_queue,
                        alarm_definition_created_event_msg)


def get_query_alarm_definition_name(alarm_definition, return_none=False):
    try:
        if 'name' in alarm_definition:
            name = alarm_definition['name']
            return name
        else:
            if return_none:
                return None
            else:
                raise Exception("Missing name")
    except Exception as ex:
        LOG.debug(ex)
        raise HTTPUnprocessableEntityError('Unprocessable Entity', ex.message)


def get_query_alarm_definition_expression(alarm_definition,
                                          return_none=False):
    try:
        if 'expression' in alarm_definition:
            expression = alarm_definition['expression']
            return expression
        else:
            if return_none:
                return None
            else:
                raise Exception("Missing expression")
    except Exception as ex:
        LOG.debug(ex)
        raise HTTPUnprocessableEntityError('Unprocessable Entity', ex.message)


def get_query_alarm_definition_description(alarm_definition,
                                           return_none=False):
    if 'description' in alarm_definition:
        return alarm_definition['description'].decode('utf8')
    else:
        if return_none:
            return None
        else:
            return ''


def get_query_alarm_definition_severity(alarm_definition, return_none=False):
    if 'severity' in alarm_definition:
        severity = alarm_definition['severity']
        severity = severity.decode('utf8').upper()
        if severity not in ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']:
            raise HTTPUnprocessableEntityError('Unprocessable Entity', 'Invalid severity')
        return severity
    else:
        if return_none:
            return None
        else:
            return 'LOW'


def get_query_alarm_definition_match_by(alarm_definition, return_none=False):
    if 'match_by' in alarm_definition:
        match_by = alarm_definition['match_by']
        return match_by
    else:
        if return_none:
            return None
        else:
            return []


def get_query_alarm_definition_alarm_actions(alarm_definition,
                                             return_none=False):
    if 'alarm_actions' in alarm_definition:
        alarm_actions = alarm_definition['alarm_actions']
        return alarm_actions
    else:
        if return_none:
            return None
        else:
            return []


def get_query_alarm_definition_undetermined_actions(alarm_definition,
                                                    return_none=False):
    if 'undetermined_actions' in alarm_definition:
        undetermined_actions = alarm_definition['undetermined_actions']
        return undetermined_actions
    else:
        if return_none:
            return None
        else:
            return []


def get_query_ok_actions(alarm_definition, return_none=False):
    if 'ok_actions' in alarm_definition:
        ok_actions = alarm_definition['ok_actions']
        return ok_actions
    else:
        if return_none:
            return None
        else:
            return []


def get_query_alarm_definition_actions_enabled(alarm_definition,
                                               required=False,
                                               return_none=False):
    try:
        if 'actions_enabled' in alarm_definition:
            enabled_actions = alarm_definition['actions_enabled']
            return enabled_actions
        else:
            if return_none:
                return None
            elif required:
                raise Exception("Missing actions-enabled")
            else:
                return ''
    except Exception as ex:
        LOG.debug(ex)
        raise HTTPUnprocessableEntityError('Unprocessable Entity', ex.message)


def get_comma_separated_str_as_list(comma_separated_str):
    if not comma_separated_str:
        return []
    else:
        return comma_separated_str.decode('utf8').split(',')


def is_definition_deterministic(expression):
    """Evaluates if found expression is deterministic or not.

    In order to do that expression is parsed into sub expressions.
    Each sub expression needs to be deterministic in order for
    entity expression to be such.

    Otherwise expression is non-deterministic.

    :param str expression: expression to be evaluated
    :return: true/false
    :rtype: bool
    """
    expr_parser = (monasca_api.expression_parser
                   .alarm_expr_parser.AlarmExprParser(expression))
    sub_expressions = expr_parser.sub_expr_list

    for sub_expr in sub_expressions:
        if not sub_expr.deterministic:
            return False

    return True
