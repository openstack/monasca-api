# -*- coding: utf-8 -*-
# Copyright 2015 Hewlett-Packard
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

from oslo_config import cfg
from oslo_log import log

from monasca_api.common.repositories import exceptions
from monasca_api.common.repositories import metrics_repository

LOG = log.getLogger(__name__)


class MetricsRepository(metrics_repository.MetricsRepository):

    def __init__(self):

        try:

            self.conf = cfg.CONF

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def list_metrics(self, tenant_id, region, name, dimensions, offset,
                     limit):

        try:

            json_metric_list = []
            return json_metric_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def measurement_list(self, tenant_id, region, name, dimensions,
                         start_timestamp, end_timestamp, offset,
                         limit, merge_metrics_flag):

        try:
            json_measurement_list = []
            return json_measurement_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def list_metric_names(self, tenant_id, region, dimensions, offset, limit):

        try:
            json_name_list = []
            return json_name_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def metrics_statistics(self, tenant_id, region, name, dimensions,
                           start_timestamp,
                           end_timestamp, statistics, period, offset, limit,
                           merge_metrics_flag):

        try:

            json_statistics_list = []
            return json_statistics_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def alarm_history(self, tenant_id, alarm_id_list,
                      offset, limit, start_timestamp=None,
                      end_timestamp=None):

        try:

            json_alarm_history_list = []
            return json_alarm_history_list

        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)
