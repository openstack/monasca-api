# Copyright 2014,2016 Hewlett Packard Enterprise Development Company, L.P.
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
class AlarmDefinitionsRepository(object):
    def __init__(self):
        """
        Initialize the superclassinitions

        Args:
            self: (todo): write your description
        """
        super(AlarmDefinitionsRepository, self).__init__()

    @abc.abstractmethod
    def create_alarm_definition(self, tenant_id, name, expression,
                                sub_expr_list, description, severity, match_by,
                                alarm_actions, undetermined_actions,
                                ok_action):
        """
        Create a new alarm definition.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            name: (str): write your description
            expression: (str): write your description
            sub_expr_list: (list): write your description
            description: (str): write your description
            severity: (bool): write your description
            match_by: (todo): write your description
            alarm_actions: (todo): write your description
            undetermined_actions: (todo): write your description
            ok_action: (todo): write your description
        """
        pass

    @abc.abstractmethod
    def get_sub_alarms(self, tenant_id, alarm_definition_id):
        """
        Retrieves alarms for a tenant.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            alarm_definition_id: (str): write your description
        """
        pass

    @abc.abstractmethod
    def get_alarm_metrics(self, tenant_id, alarm_definition_id):
        """
        Retrieves the alarm metrics.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            alarm_definition_id: (str): write your description
        """
        pass

    @abc.abstractmethod
    def delete_alarm_definition(self, tenant_id, alarm_definition_id):
        """
        Delete an alarm definition.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            alarm_definition_id: (str): write your description
        """
        pass

    @abc.abstractmethod
    def get_sub_alarm_definitions(self, alarm_definition_id):
        """
        Gets definition definition definition for a specific alarm.

        Args:
            self: (todo): write your description
            alarm_definition_id: (str): write your description
        """
        pass

    @abc.abstractmethod
    def get_alarm_definition(self, tenant_id, id):
        """
        Retrieves definition.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            id: (int): write your description
        """
        pass

    @abc.abstractmethod
    def get_alarm_definitions(self, tenant_id, name, dimensions, severity, sort_by,
                              offset, limit):
        """
        Get a list of comparm.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            name: (str): write your description
            dimensions: (int): write your description
            severity: (str): write your description
            sort_by: (str): write your description
            offset: (todo): write your description
            limit: (todo): write your description
        """
        pass

    @abc.abstractmethod
    def update_or_patch_alarm_definition(self, tenant_id, id,
                                         name,
                                         expression,
                                         sub_expr_list,
                                         actions_enabled,
                                         description,
                                         alarm_actions,
                                         ok_actions,
                                         undetermined_actions,
                                         match_by, severity, patch):
        """
        Updates actions for a mongodb update.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            id: (int): write your description
            name: (str): write your description
            expression: (todo): write your description
            sub_expr_list: (list): write your description
            actions_enabled: (bool): write your description
            description: (str): write your description
            alarm_actions: (todo): write your description
            ok_actions: (todo): write your description
            undetermined_actions: (todo): write your description
            match_by: (todo): write your description
            severity: (bool): write your description
            patch: (todo): write your description
        """
        pass
