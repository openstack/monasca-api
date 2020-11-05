# (C) Copyright 2014,2016 Hewlett Packard Enterprise Development LP
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

import abc

import six


@six.add_metaclass(abc.ABCMeta)
class AbstractMetricsRepository(object):

    MULTIPLE_METRICS_MESSAGE = ("Found multiple metrics matching metric name" +
                                " and dimensions. Please refine your search" +
                                " criteria using a unique" +
                                " metric name or additional dimensions." +
                                " Alternatively, you may specify" +
                                " 'merge_metrics=True' as a query" +
                                " parameter to combine all metrics" +
                                " matching search criteria into a single" +
                                " series.")

    @abc.abstractmethod
    def list_metrics(self, tenant_id, region, name, dimensions, offset, limit):
        """
        List all the metrics for a tenant.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            region: (str): write your description
            name: (str): write your description
            dimensions: (int): write your description
            offset: (int): write your description
            limit: (int): write your description
        """
        pass

    @abc.abstractmethod
    def list_metric_names(self, tenant_id, region, dimensions):
        """
        List all metric names.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            region: (str): write your description
            dimensions: (int): write your description
        """
        pass

    @abc.abstractmethod
    def measurement_list(self, tenant_id, region, name, dimensions,
                         start_timestamp, end_timestamp, offset, limit,
                         merge_metrics_flag,
                         group_by):
        """
        Merge a list of a list of a tenant.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            region: (str): write your description
            name: (str): write your description
            dimensions: (int): write your description
            start_timestamp: (todo): write your description
            end_timestamp: (todo): write your description
            offset: (int): write your description
            limit: (int): write your description
            merge_metrics_flag: (todo): write your description
            group_by: (todo): write your description
        """
        pass

    @abc.abstractmethod
    def metrics_statistics(self, tenant_id, region, name, dimensions,
                           start_timestamp, end_timestamp, statistics,
                           period, offset, limit, merge_metrics_flag,
                           group_by):
        """
        Merge metrics for a tenant.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            region: (str): write your description
            name: (str): write your description
            dimensions: (int): write your description
            start_timestamp: (todo): write your description
            end_timestamp: (int): write your description
            statistics: (str): write your description
            period: (int): write your description
            offset: (int): write your description
            limit: (int): write your description
            merge_metrics_flag: (str): write your description
            group_by: (todo): write your description
        """
        pass

    @abc.abstractmethod
    def alarm_history(self, tenant_id, alarm_id_list,
                      offset, limit, start_timestamp, end_timestamp):
        """
        Alarm alarm history history.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            alarm_id_list: (list): write your description
            offset: (int): write your description
            limit: (int): write your description
            start_timestamp: (int): write your description
            end_timestamp: (int): write your description
        """
        pass

    @staticmethod
    @abc.abstractmethod
    def check_status():
        """
        Checks if the status is a valid.

        Args:
        """
        pass
