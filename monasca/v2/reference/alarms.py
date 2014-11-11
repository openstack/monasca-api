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
from falcon.util.uri import parse_query_string

from pyparsing import ParseException
import falcon
from oslo.config import cfg
from monasca.api.alarms_api_v2 import AlarmsV2API

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


class Alarms(AlarmsV2API):

    def __init__(self, global_conf):

        try:
            super(Alarms, self).__init__(global_conf)

            self._region = cfg.CONF.region

            self._default_authorized_roles = \
                cfg.CONF.security.default_authorized_roles
            self._delegate_authorized_roles = \
                cfg.CONF.security.delegate_authorized_roles
            self._post_metrics_authorized_roles = \
                cfg.CONF.security.default_authorized_roles + \
                cfg.CONF.security.agent_authorized_roles

            self._message_queue \
                = resource_api.init_driver('monasca.messaging',
                                           cfg.CONF.messaging.driver,
                                           (['events']))

            self._alarms_repo = resource_api.init_driver(
                'monasca.repositories', cfg.CONF.repositories.alarms_driver)

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    @resource_api.Restify('/v2.0/alarms/{id}', method='put')
    def do_put_alarms(self, req, res, id):

        helpers.validate_authorization(req, self._default_authorized_roles)

        result = ''

        res.body = json.dumps(result, ensure_ascii=False).encode('utf8')
        res.status = falcon.HTTP_200
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarms/{id}', method='patch')
    def do_patch_alarms(self, req, res, id):

        helpers.validate_authorization(req, self._default_authorized_roles)

        result = ''
        res.body = json.dumps(result, ensure_ascii=False).encode('utf8')
        res.status = falcon.HTTP_200
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarms/{id}', method='delete')
    def do_delete_alarms(self, req, res, id):

        helpers.validate_authorization(req, self._default_authorized_roles)

        result = ''
        res.body = json.dumps(result, ensure_ascii=False).encode('utf8')
        res.status = falcon.HTTP_200
        res.status = '501 Not Implemented'

    @resource_api.Restify('/v2.0/alarms', method='get')
    def do_get_alarms(self, req, res):

        helpers.validate_authorization(req, self._default_authorized_roles)
        tenant_id = helpers.get_tenant_id(req)

        query_parms = parse_query_string(req.query_string)

        result = self._alarm_list(req.uri, tenant_id, query_parms)

        res.body = json.dumps(result, ensure_ascii=False).encode('utf8')
        res.status = falcon.HTTP_200

    @resource_api.Restify('/v2.0/alarms/{id}', method='get')
    def do_get_alarm_by_id(self, req, res, id):

        helpers.validate_authorization(req, self._default_authorized_roles)

        result = ''
        res.body = json.dumps(result, ensure_ascii=False).encode('utf8')
        res.status = falcon.HTTP_200
        res.status = '501 Not Implemented'

    def _alarm_list(self, req_uri, tenant_id, query_parms):

        try:

            alarm_rows = self._alarms_repo.get_alarms(tenant_id, query_parms)

            result = []

            if not alarm_rows:
                return result

            prev_alarm_id = None
            for alarm_row in alarm_rows:
                if prev_alarm_id != alarm_row.alarm_id:
                    if prev_alarm_id is not None:
                        result.append(a)

                    ad = {u'id': alarm_row.alarm_definition_id,
                          u'name': alarm_row.alarm_definition_name,
                          u'severity': alarm_row.severity,
                          }
                    helpers.add_links_to_resource(ad, req_uri)

                    metrics = []
                    a = {
                        u'id': alarm_row.alarm_id,
                        u'metrics': metrics,
                        u'state': alarm_row.state,
                        u'alarm_definition': ad
                    }
                    helpers.add_links_to_resource(a, req_uri)

                    prev_alarm_id = alarm_row.alarm_id

                dimensions = {}
                metric = {u'name': alarm_row.metric_name,
                          u'dimensions': dimensions}

                if alarm_row.metric_dimensions:
                    for dimension in alarm_row.metric_dimensions.split(','):
                        parsed_dimension = dimension.split('=')
                        dimensions[parsed_dimension[0]] = parsed_dimension[1]

                metrics.append(metric)

            result.append(a)

            return result

        except exceptions.RepositoryException as ex:
            LOG.exception(ex)
            msg = "".join(ex.message.args)
            raise falcon.HTTPInternalServerError('Service unavailable', msg)
        except Exception as ex:
            LOG.exception(ex)
            raise falcon.HTTPInternalServerError('Service unavailable', ex)
