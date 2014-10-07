# Copyright 2014 Hewlett-Packard
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

import json

import falcon
from oslo.config import cfg

from monasca.openstack.common import log
from monasca.api import monasca_api_v2
from monasca.common import resource_api
from monasca.common.messaging import exceptions as message_queue_exceptions
from monasca.common.messaging.message_formats import metrics_transform_factory
from monasca.v2.common import utils
from monasca.v2.common.schemas import exceptions as schemas_exceptions
from monasca.v2.common.schemas import metrics_request_body_schema as schemas_metrics
from monasca.v2.reference import helpers

from stevedore import driver

LOG = log.getLogger(__name__)


class Metrics(monasca_api_v2.V2API):
    def __init__(self, global_conf):
        super(Metrics, self).__init__(global_conf)
        self._region = cfg.CONF.region
        self._default_authorized_roles = cfg.CONF.security.default_authorized_roles
        self._delegate_authorized_roles = cfg.CONF.security.delegate_authorized_roles
        self._post_metrics_authorized_roles = cfg.CONF.security.default_authorized_roles + \
                                              cfg.CONF.security.agent_authorized_roles
        self._metrics_transform = metrics_transform_factory.create_metrics_transform()
        self._init_message_queue()
        self._init_metrics_repo()

    def _init_message_queue(self):
        mgr = driver.DriverManager(
            namespace = 'monasca.messaging',
            name = cfg.CONF.messaging.driver,
            invoke_on_load=True,
            invoke_args=(['metrics'])
        )
        self._message_queue = mgr.driver

    def _init_metrics_repo(self):
        mgr = driver.DriverManager(
            namespace = 'monasca.repositories',
            name = cfg.CONF.repositories.metrics_driver,
            invoke_on_load=True,
            invoke_args=()
        )
        self._metrics_repo = mgr.driver

    def _read_metrics(self, req):
        '''
        Read the metrics from the http request and return them as JSON.
        :param req: HTTP request object.
        :return: Returns the metrics as a JSON object.
        :raises falcon.HTTPBadRequest:
        '''
        try:
            msg = req.stream.read()
            json_msg = json.loads(msg)
            return json_msg
        except ValueError as ex:
            LOG.debug(ex)
            raise falcon.HTTPBadRequest('Bad request', 'Request body is not valid JSON')

    def _validate_metrics(self, metrics):
        '''
        Validates the metrics
        :param metrics: A metric object or array of metrics objects.
        :raises falcon.HTTPBadRequest:
        '''
        try:
            schemas_metrics.validate(metrics)
        except schemas_exceptions.ValidationException as ex:
            LOG.debug(ex)
            raise falcon.HTTPBadRequest('Bad request', ex.message)

    def _send_metrics(self, metrics):
        '''
        Send the metrics using the message queue.
        :param metrics: A metric object or array of metrics objects.
        :raises: falcon.HTTPServiceUnavailable:
        '''
        def _send_metric(metric):
            try:
                str_msg = json.dumps(metric, default=utils.date_handler)
                self._message_queue.send_message(str_msg)
            except message_queue_exceptions.MessageQueueException as ex:
                LOG.exception(ex)
                raise falcon.HTTPServiceUnavailable('Service unavailable', ex.message)

        if isinstance(metrics, list):
            for metric in metrics:
                _send_metric(metric)
        else:
            _send_metric(metrics)

    def _list_metrics(self, tenant_id, name, dimensions):
        '''
        Query the metric repo for the metrics, format them and return them.
        :param tenant_id:
        :param name:
        :param dimensions:
        :raises falcon.HTTPServiceUnavailable:
        '''
        try:
            result = self._metrics_repo.list_metrics(tenant_id, name, dimensions)
            # TODO: Format response body correctly. Currently just returning what is returned by the metrics repository.
            return result
        except Exception as ex:
            log.exception()
            raise falcon.HTTPServiceUnavailable('Service unavailable', ex.message)

    @resource_api.Restify('/v2.0/metrics/', method='post')
    def do_post_metrics(self, req, res):
        helpers.validate_json_content_type(req)
        helpers.validate_authorization(req, self._post_metrics_authorized_roles)
        metrics = self._read_metrics(req)
        self._validate_metrics(metrics)
        tenant_id = helpers.get_cross_tenant_or_tenant_id(req, self._delegate_authorized_roles)
        transformed_metrics = self._metrics_transform(metrics, tenant_id, self._region)
        self._send_metrics(transformed_metrics)
        res.status = falcon.HTTP_204

    @resource_api.Restify('/v2.0/metrics/', method='get')
    def do_get_metrics(self, req, res):
        helpers.validate_authorization(req, self._default_authorized_roles)
        tenant_id = helpers.get_tenant_id(req)
        name = helpers.get_query_name(req)
        helpers.validate_query_name(name)
        dimensions = helpers.get_query_dimensions(req)
        helpers.validate_query_dimensions(dimensions)
        result = self._list_metrics(tenant_id, name, dimensions)
        res.body =  json.dumps(result)
        res.status = falcon.HTTP_200