# Copyright 2016 FUJITSU LIMITED
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

import falcon
from monasca_common.rest import utils as rest_utils

from monasca_log_api.app.base.validation import validate_authorization
from monasca_log_api.app.controller.api import healthcheck_api
from monasca_log_api.healthcheck import kafka_check


class HealthChecks(healthcheck_api.HealthChecksApi):
    # response configuration
    CACHE_CONTROL = ['must-revalidate', 'no-cache', 'no-store']

    # response codes
    HEALTHY_CODE_GET = falcon.HTTP_OK
    HEALTHY_CODE_HEAD = falcon.HTTP_NO_CONTENT
    NOT_HEALTHY_CODE = falcon.HTTP_SERVICE_UNAVAILABLE

    def __init__(self):
        self._kafka_check = kafka_check.KafkaHealthCheck()
        super(HealthChecks, self).__init__()

    def on_head(self, req, res):
        validate_authorization(req, ['log_api:healthcheck:head'])
        res.status = self.HEALTHY_CODE_HEAD
        res.cache_control = self.CACHE_CONTROL

    def on_get(self, req, res):
        # at this point we know API is alive, so
        # keep up good work and verify kafka status
        validate_authorization(req, ['log_api:healthcheck:get'])
        kafka_result = self._kafka_check.healthcheck()

        # in case it'd be unhealthy,
        # message will contain error string
        status_data = {
            'kafka': kafka_result.message
        }

        # Really simple approach, ideally that should be
        # part of monasca-common with some sort of registration of
        # healthchecks concept

        res.status = (self.HEALTHY_CODE_GET
                      if kafka_result.healthy else self.NOT_HEALTHY_CODE)
        res.cache_control = self.CACHE_CONTROL
        res.body = rest_utils.as_json(status_data)
