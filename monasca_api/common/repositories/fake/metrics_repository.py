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

from monasca_api.common.repositories import metrics_repository


class MetricsRepository(metrics_repository.AbstractMetricsRepository):
    def __init__(self):
        """
        Initialize a function

        Args:
            self: (todo): write your description
        """
        return

    def list_metrics(self, tenant_id, name, dimensions, offset, limit):
        """
        List the list for a tenant.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            name: (str): write your description
            dimensions: (int): write your description
            offset: (int): write your description
            limit: (int): write your description
        """
        return {}
