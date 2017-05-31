# Copyright 2017 FUJITSU LIMITED
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

from monasca_api.api import healthcheck_api
from monasca_api.healthcheck import alarms_db_check
from monasca_api.healthcheck import kafka_check
from monasca_api.healthcheck import metrics_db_check
from monasca_api.v2.reference import helpers


class HealthChecks(healthcheck_api.HealthCheckApi):
    CACHE_CONTROL = ['must-revalidate', 'no-cache', 'no-store']

    HEALTHY_CODE_GET = falcon.HTTP_OK
    HEALTHY_CODE_HEAD = falcon.HTTP_NO_CONTENT
    NOT_HEALTHY_CODE = falcon.HTTP_SERVICE_UNAVAILABLE

    def __init__(self):
        super(HealthChecks, self).__init__()
        self._kafka_check = kafka_check.KafkaHealthCheck()
        self._alarm_db_check = alarms_db_check.AlarmsDbHealthCheck()
        self._metrics_db_check = metrics_db_check.MetricsDbCheck()

    def on_head(self, req, res):
        res.status = self.HEALTHY_CODE_HEAD
        res.cache_control = self.CACHE_CONTROL

    def on_get(self, req, res):
        kafka_result = self._kafka_check.health_check()
        alarms_db_result = self._alarm_db_check.health_check()
        metrics_db_result = self._metrics_db_check.health_check()

        status_data = {
            'kafka': kafka_result.message,
            'alarms_database': alarms_db_result.message,
            'metrics_database': metrics_db_result.message
        }
        health = (kafka_result.healthy and alarms_db_result.healthy and
                  metrics_db_result.healthy)
        res.status = (self.HEALTHY_CODE_GET
                      if health else self.NOT_HEALTHY_CODE)
        res.cache_control = self.CACHE_CONTROL
        res.body = helpers.to_json(status_data)
