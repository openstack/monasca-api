# Copyright 2014 IBM Corp
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

from oslo_log import log

LOG = log.getLogger(__name__)


class MetricsV2API(object):
    def __init__(self):
        super(MetricsV2API, self).__init__()
        LOG.info('Initializing MetricsV2API!')

    def on_get(self, req, res):
        res.status = '501 Not Implemented'

    def on_post(self, req, res):
        res.status = '501 Not Implemented'


class MetricsMeasurementsV2API(object):
    def __init__(self):
        super(MetricsMeasurementsV2API, self).__init__()
        LOG.info('Initializing MetricsMeasurementsV2API!')

    def on_get(self, req, res):
        res.status = '501 Not Implemented'


class MetricsStatisticsV2API(object):
    def __init__(self):
        super(MetricsStatisticsV2API, self).__init__()
        LOG.info('Initializing MetricsStatisticsV2API!')

    def on_get(self, req, res):
        res.status = '501 Not Implemented'


class MetricsNamesV2API(object):
    def __init__(self):
        super(MetricsNamesV2API, self).__init__()
        LOG.info('Initializing MetricsNamesV2API!')

    def on_get(self, req, res):
        res.status = '501 Not Implemented'


class DimensionValuesV2API(object):
    def __init__(self):
        super(DimensionValuesV2API, self).__init__()
        LOG.info('Initializing DimensionValuesV2API!')

    def on_get(self, req, res):
        res.status = '501 Not Implemented'
