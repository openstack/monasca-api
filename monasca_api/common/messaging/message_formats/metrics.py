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

from oslo_utils import timeutils

from monasca_api.common.rest import utils as rest_utils


def transform(metrics, tenant_id, region):
    """
    Transform metrics to json.

    Args:
        metrics: (array): write your description
        tenant_id: (str): write your description
        region: (array): write your description
    """
    transformed_metric = {'metric': {},
                          'meta': {'tenantId': tenant_id, 'region': region},
                          'creation_time': timeutils.utcnow_ts()}

    if isinstance(metrics, list):
        transformed_metrics = []
        for metric in metrics:
            transformed_metric['metric'] = metric
            transformed_metrics.append(rest_utils.as_json(transformed_metric))
        return transformed_metrics
    else:
        transformed_metric['metric'] = metrics
        return [rest_utils.as_json(transformed_metric)]
