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

from oslo.config import cfg

import monasca.common.messaging.message_formats.cadf.metrics as cadf_metrics
import monasca.common.messaging.message_formats.identity.metrics as id_metrics
import monasca.common.messaging.message_formats.reference.metrics as r_metrics


def create_metrics_transform():
    metrics_message_format = cfg.CONF.messaging.metrics_message_format
    if metrics_message_format == 'reference':
        return r_metrics.transform
    elif metrics_message_format == 'cadf':
        return cadf_metrics.transform
    else:
        return id_metrics.transform