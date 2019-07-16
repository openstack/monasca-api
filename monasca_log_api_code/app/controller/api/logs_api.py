# Copyright 2015 kornicameister@gmail.com
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
from monasca_log_api import conf
from monasca_log_api.monitoring import client
from monasca_log_api.monitoring import metrics
from oslo_log import log


CONF = conf.CONF
LOG = log.getLogger(__name__)


class LogsApi(object):
    """Logs API.

    Logs API acts as RESTful endpoint accepting
    messages contains collected log entries from the system.
    Works as gateway for any further processing for accepted data.

    """
    def __init__(self):
        super(LogsApi, self).__init__()
        if CONF.monitoring.enable:
            self._statsd = client.get_client()

            # create_common counters, gauges etc.
            self._metrics_dimensions = dimensions = {'version': self.version}

            self._logs_in_counter = self._statsd.get_counter(
                name=metrics.LOGS_RECEIVED_METRIC,
                dimensions=dimensions
            )
            self._logs_size_gauge = self._statsd.get_gauge(
                name=metrics.LOGS_RECEIVED_BYTE_SIZE_METRICS,
                dimensions=dimensions
            )
            self._logs_rejected_counter = self._statsd.get_counter(
                name=metrics.LOGS_REJECTED_METRIC,
                dimensions=dimensions
            )
            self._logs_processing_time = self._statsd.get_timer(
                name=metrics.LOGS_PROCESSING_TIME_METRIC,
                dimensions=dimensions
            )

        LOG.info('Initializing LogsApi %s!' % self.version)

    def on_post(self, req, res):
        """Accepts sent logs as text or json.

        Accepts logs sent to resource which should
        be sent to kafka queue.

        :param req: current request
        :param res: current response

        """
        res.status = falcon.HTTP_501  # pragma: no cover

    def on_get(self, req, res):
        """Queries logs matching specified dimension values.

        Performs queries on the underlying log storage
        against a time range and set of dimension values.

        :param req: current request
        :param res: current response

        """
        res.status = falcon.HTTP_501  # pragma: no cover

    @property
    def version(self):
        return getattr(self, 'VERSION')
