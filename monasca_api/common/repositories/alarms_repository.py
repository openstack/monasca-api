# Copyright 2014-2016 Hewlett Packard Enterprise Development Company LP
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
class AlarmsRepository(object):

    def __init__(self):
        """
        Initialize the superclass

        Args:
            self: (todo): write your description
        """

        super(AlarmsRepository, self).__init__()

    @abc.abstractmethod
    def get_alarm_metrics(self, alarm_id):
        """
        Get a list of alarm metrics.

        Args:
            self: (todo): write your description
            alarm_id: (str): write your description
        """
        pass

    @abc.abstractmethod
    def get_sub_alarms(self, tenant_id, alarm_id):
        """
        Retrieves a list of a tenant.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            alarm_id: (str): write your description
        """
        pass

    @abc.abstractmethod
    def update_alarm(self, tenant_id, alarm_id, state, lifecycle_state, link):
        """
        Update the specified alarm.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            alarm_id: (str): write your description
            state: (todo): write your description
            lifecycle_state: (todo): write your description
            link: (todo): write your description
        """
        pass

    @abc.abstractmethod
    def delete_alarm(self, tenant_id, id):
        """
        Delete a tenant.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            id: (str): write your description
        """
        pass

    @abc.abstractmethod
    def get_alarm(self, tenant_id, id):
        """
        Get a specific alarms for a tenant.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            id: (str): write your description
        """
        pass

    @abc.abstractmethod
    def get_alarms(self, tenant_id, query_parms, offset, limit):
        """
        Get a specific alarms.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            query_parms: (str): write your description
            offset: (int): write your description
            limit: (todo): write your description
        """
        pass

    @abc.abstractmethod
    def get_alarms_count(self, tenant_id, query_parms, offset, limit):
        """
        Get the number of alarm count for a tenant.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            query_parms: (str): write your description
            offset: (int): write your description
            limit: (str): write your description
        """
        pass
