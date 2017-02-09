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

import falcon
from monasca_common.simport import simport
from monasca_common.validation import metrics as metric_validation
from oslo_config import cfg
from oslo_log import log

from monasca_api.api import metrics_api_v2
from monasca_api.common.messaging import (
    exceptions as message_queue_exceptions)
from monasca_api.common.messaging.message_formats import (
    metrics as metrics_message)
from monasca_api.v2.common.exceptions import HTTPUnprocessableEntityError
from monasca_api.v2.reference import helpers
from monasca_api.v2.reference import resource

LOG = log.getLogger(__name__)


def get_merge_metrics_flag(req):
    '''Return the value of the optional metrics_flag

    Returns False if merge_metrics parameter is not supplied or is not a
    string that evaluates to True, otherwise True
    '''

    merge_metrics_flag = helpers.get_query_param(req,
                                                 'merge_metrics',
                                                 False,
                                                 False)
    if merge_metrics_flag is not False:
        return helpers.str_2_bool(merge_metrics_flag)
    else:
        return False


class Metrics(metrics_api_v2.MetricsV2API):
    def __init__(self):
        try:
            super(Metrics, self).__init__()
            self._region = cfg.CONF.region
            self._delegate_authorized_roles = (
                cfg.CONF.security.delegate_authorized_roles)
            self._get_metrics_authorized_roles = (
                cfg.CONF.security.default_authorized_roles +
                cfg.CONF.security.read_only_authorized_roles)
            self._post_metrics_authorized_roles = (
                cfg.CONF.security.default_authorized_roles +
                cfg.CONF.security.agent_authorized_roles)
            self._message_queue = simport.load(cfg.CONF.messaging.driver)(
                'metrics')
            self._metrics_repo = simport.load(
                cfg.CONF.repositories.metrics_driver)()

        except Exception as ex:
            LOG.exception(ex)
            raise falcon.HTTPInternalServerError('Service unavailable',
                                                 ex.message)

    def _send_metrics(self, metrics):
        try:
            self._message_queue.send_message(metrics)
        except message_queue_exceptions.MessageQueueException as ex:
            LOG.exception(ex)
            raise falcon.HTTPServiceUnavailable('Service unavailable',
                                                ex.message, 60)

    def _list_metrics(self, tenant_id, name, dimensions, req_uri, offset,
                      limit, start_timestamp, end_timestamp):

        result = self._metrics_repo.list_metrics(tenant_id,
                                                 self._region,
                                                 name,
                                                 dimensions,
                                                 offset, limit,
                                                 start_timestamp,
                                                 end_timestamp)

        return helpers.paginate(result, req_uri, limit)

    @resource.resource_try_catch_block
    def on_post(self, req, res):
        helpers.validate_json_content_type(req)
        helpers.validate_authorization(req,
                                       self._post_metrics_authorized_roles)
        metrics = helpers.read_http_resource(req)
        try:
            metric_validation.validate(metrics)
        except Exception as ex:
            LOG.exception(ex)
            raise HTTPUnprocessableEntityError("Unprocessable Entity", ex.message)

        tenant_id = (
            helpers.get_x_tenant_or_tenant_id(req,
                                              self._delegate_authorized_roles))
        transformed_metrics = metrics_message.transform(
            metrics, tenant_id, self._region)
        self._send_metrics(transformed_metrics)
        res.status = falcon.HTTP_204

    @resource.resource_try_catch_block
    def on_get(self, req, res):
        helpers.validate_authorization(req, self._get_metrics_authorized_roles)
        tenant_id = (
            helpers.get_x_tenant_or_tenant_id(req,
                                              self._delegate_authorized_roles))
        name = helpers.get_query_name(req)
        helpers.validate_query_name(name)
        dimensions = helpers.get_query_dimensions(req)
        helpers.validate_query_dimensions(dimensions)
        offset = helpers.get_query_param(req, 'offset')
        start_timestamp = helpers.get_query_starttime_timestamp(req, False)
        end_timestamp = helpers.get_query_endtime_timestamp(req, False)
        helpers.validate_start_end_timestamps(start_timestamp, end_timestamp)
        result = self._list_metrics(tenant_id, name,
                                    dimensions, req.uri,
                                    offset, req.limit,
                                    start_timestamp, end_timestamp)
        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_200


class MetricsMeasurements(metrics_api_v2.MetricsMeasurementsV2API):
    def __init__(self):
        try:
            super(MetricsMeasurements, self).__init__()
            self._region = cfg.CONF.region
            self._delegate_authorized_roles = (
                cfg.CONF.security.delegate_authorized_roles)
            self._get_metrics_authorized_roles = (
                cfg.CONF.security.default_authorized_roles +
                cfg.CONF.security.read_only_authorized_roles)
            self._post_metrics_authorized_roles = (
                cfg.CONF.security.default_authorized_roles +
                cfg.CONF.security.agent_authorized_roles)
            self._metrics_repo = simport.load(
                cfg.CONF.repositories.metrics_driver)()

        except Exception as ex:
            LOG.exception(ex)
            raise falcon.HTTPInternalServerError('Service unavailable',
                                                 ex.message)

    @resource.resource_try_catch_block
    def on_get(self, req, res):
        helpers.validate_authorization(req, self._get_metrics_authorized_roles)
        tenant_id = (
            helpers.get_x_tenant_or_tenant_id(req,
                                              self._delegate_authorized_roles))
        name = helpers.get_query_name(req, True)
        helpers.validate_query_name(name)
        dimensions = helpers.get_query_dimensions(req)
        helpers.validate_query_dimensions(dimensions)
        start_timestamp = helpers.get_query_starttime_timestamp(req)
        end_timestamp = helpers.get_query_endtime_timestamp(req, False)
        helpers.validate_start_end_timestamps(start_timestamp, end_timestamp)
        offset = helpers.get_query_param(req, 'offset')
        merge_metrics_flag = get_merge_metrics_flag(req)
        group_by = helpers.get_query_group_by(req)

        result = self._measurement_list(tenant_id, name, dimensions,
                                        start_timestamp, end_timestamp,
                                        req.uri, offset,
                                        req.limit, merge_metrics_flag,
                                        group_by)

        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_200

    def _measurement_list(self, tenant_id, name, dimensions, start_timestamp,
                          end_timestamp, req_uri, offset,
                          limit, merge_metrics_flag, group_by):

        result = self._metrics_repo.measurement_list(tenant_id,
                                                     self._region,
                                                     name,
                                                     dimensions,
                                                     start_timestamp,
                                                     end_timestamp,
                                                     offset,
                                                     limit,
                                                     merge_metrics_flag,
                                                     group_by)

        return helpers.paginate_measurements(result, req_uri, limit)


class MetricsStatistics(metrics_api_v2.MetricsStatisticsV2API):
    def __init__(self):
        try:
            super(MetricsStatistics, self).__init__()
            self._region = cfg.CONF.region
            self._delegate_authorized_roles = (
                cfg.CONF.security.delegate_authorized_roles)
            self._get_metrics_authorized_roles = (
                cfg.CONF.security.default_authorized_roles +
                cfg.CONF.security.read_only_authorized_roles)
            self._metrics_repo = simport.load(
                cfg.CONF.repositories.metrics_driver)()

        except Exception as ex:
            LOG.exception(ex)
            raise falcon.HTTPInternalServerError('Service unavailable',
                                                 ex.message)

    @resource.resource_try_catch_block
    def on_get(self, req, res):
        helpers.validate_authorization(req, self._get_metrics_authorized_roles)
        tenant_id = (
            helpers.get_x_tenant_or_tenant_id(req,
                                              self._delegate_authorized_roles))
        name = helpers.get_query_name(req, True)
        helpers.validate_query_name(name)
        dimensions = helpers.get_query_dimensions(req)
        helpers.validate_query_dimensions(dimensions)
        start_timestamp = helpers.get_query_starttime_timestamp(req)
        end_timestamp = helpers.get_query_endtime_timestamp(req, False)
        helpers.validate_start_end_timestamps(start_timestamp, end_timestamp)
        statistics = helpers.get_query_statistics(req)
        period = helpers.get_query_period(req)
        offset = helpers.get_query_param(req, 'offset')
        merge_metrics_flag = get_merge_metrics_flag(req)
        group_by = helpers.get_query_group_by(req)

        result = self._metric_statistics(tenant_id, name, dimensions,
                                         start_timestamp, end_timestamp,
                                         statistics, period, req.uri,
                                         offset, req.limit, merge_metrics_flag,
                                         group_by)

        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_200

    def _metric_statistics(self, tenant_id, name, dimensions, start_timestamp,
                           end_timestamp, statistics, period, req_uri,
                           offset, limit, merge_metrics_flag, group_by):

        result = self._metrics_repo.metrics_statistics(tenant_id,
                                                       self._region,
                                                       name,
                                                       dimensions,
                                                       start_timestamp,
                                                       end_timestamp,
                                                       statistics, period,
                                                       offset,
                                                       limit,
                                                       merge_metrics_flag,
                                                       group_by)

        return helpers.paginate_statistics(result, req_uri, limit)


class MetricsNames(metrics_api_v2.MetricsNamesV2API):
    def __init__(self):
        try:
            super(MetricsNames, self).__init__()
            self._region = cfg.CONF.region
            self._delegate_authorized_roles = (
                cfg.CONF.security.delegate_authorized_roles)
            self._get_metrics_authorized_roles = (
                cfg.CONF.security.default_authorized_roles +
                cfg.CONF.security.read_only_authorized_roles)
            self._metrics_repo = simport.load(
                cfg.CONF.repositories.metrics_driver)()

        except Exception as ex:
            LOG.exception(ex)
            raise falcon.HTTPInternalServerError('Service unavailable',
                                                 ex.message)

    @resource.resource_try_catch_block
    def on_get(self, req, res):
        helpers.validate_authorization(req, self._get_metrics_authorized_roles)
        tenant_id = (
            helpers.get_x_tenant_or_tenant_id(req,
                                              self._delegate_authorized_roles))
        dimensions = helpers.get_query_dimensions(req)
        helpers.validate_query_dimensions(dimensions)
        offset = helpers.get_query_param(req, 'offset')
        result = self._list_metric_names(tenant_id, dimensions,
                                         req.uri, offset, req.limit)
        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_200

    def _list_metric_names(self, tenant_id, dimensions, req_uri, offset,
                           limit):

        result = self._metrics_repo.list_metric_names(tenant_id,
                                                      self._region,
                                                      dimensions)

        return helpers.paginate_with_no_id(result, req_uri, offset, limit)


class DimensionValues(metrics_api_v2.DimensionValuesV2API):
    def __init__(self):
        try:
            super(DimensionValues, self).__init__()
            self._region = cfg.CONF.region
            self._delegate_authorized_roles = (
                cfg.CONF.security.delegate_authorized_roles)
            self._get_metrics_authorized_roles = (
                cfg.CONF.security.default_authorized_roles +
                cfg.CONF.security.read_only_authorized_roles)
            self._metrics_repo = simport.load(
                cfg.CONF.repositories.metrics_driver)()

        except Exception as ex:
            LOG.exception(ex)
            raise falcon.HTTPInternalServerError('Service unavailable',
                                                 ex.message)

    @resource.resource_try_catch_block
    def on_get(self, req, res):
        helpers.validate_authorization(req, self._get_metrics_authorized_roles)
        tenant_id = (
            helpers.get_x_tenant_or_tenant_id(req,
                                              self._delegate_authorized_roles))
        metric_name = helpers.get_query_param(req, 'metric_name')
        dimension_name = helpers.get_query_param(req, 'dimension_name',
                                                 required=True)
        offset = helpers.get_query_param(req, 'offset')
        result = self._dimension_values(tenant_id, req.uri, metric_name,
                                        dimension_name, offset, req.limit)
        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_200

    def _dimension_values(self, tenant_id, req_uri, metric_name,
                          dimension_name, offset, limit):

        result = self._metrics_repo.list_dimension_values(tenant_id,
                                                          self._region,
                                                          metric_name,
                                                          dimension_name)

        return helpers.paginate_with_no_id(result, req_uri, offset, limit)


class DimensionNames(metrics_api_v2.DimensionNamesV2API):
    def __init__(self):
        try:
            super(DimensionNames, self).__init__()
            self._region = cfg.CONF.region
            self._delegate_authorized_roles = (
                cfg.CONF.security.delegate_authorized_roles)
            self._get_metrics_authorized_roles = (
                cfg.CONF.security.default_authorized_roles +
                cfg.CONF.security.read_only_authorized_roles)
            self._metrics_repo = simport.load(
                cfg.CONF.repositories.metrics_driver)()

        except Exception as ex:
            LOG.exception(ex)
            raise falcon.HTTPInternalServerError('Service unavailable',
                                                 ex.message)

    @resource.resource_try_catch_block
    def on_get(self, req, res):
        helpers.validate_authorization(req, self._get_metrics_authorized_roles)
        tenant_id = (
            helpers.get_x_tenant_or_tenant_id(req,
                                              self._delegate_authorized_roles))
        metric_name = helpers.get_query_param(req, 'metric_name')
        offset = helpers.get_query_param(req, 'offset')
        result = self._dimension_names(tenant_id, req.uri, metric_name,
                                       offset, req.limit)
        res.body = helpers.dumpit_utf8(result)
        res.status = falcon.HTTP_200

    def _dimension_names(self, tenant_id, req_uri, metric_name, offset, limit):

        result = self._metrics_repo.list_dimension_names(tenant_id,
                                                         self._region,
                                                         metric_name)

        return helpers.paginate_with_no_id(result, req_uri, offset, limit)
