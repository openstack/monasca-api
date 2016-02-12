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
        super(AlarmDefinitionsRepository, self).__init__()

    @abc.abstractmethod
    def create_alarm_definition(self, tenant_id, name, expression,
                                sub_expr_list, description, severity, match_by,
                                alarm_actions, undetermined_actions,
                                ok_action):
        pass

    @abc.abstractmethod
    def get_sub_alarms(self, tenant_id, alarm_definition_id):
        pass

    @abc.abstractmethod
    def get_alarm_metrics(self, tenant_id, alarm_definition_id):
        pass

    @abc.abstractmethod
    def delete_alarm_definition(self, tenant_id, alarm_definition_id):
        pass

    @abc.abstractmethod
    def get_sub_alarm_definitions(self, alarm_definition_id):
        pass

    @abc.abstractmethod
    def get_alarm_definition(self, tenant_id, id):
        pass

    @abc.abstractmethod
    def get_alarm_definitions(self, tenant_id, name, dimensions, severity, sort_by,
                              offset, limit):
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
        pass
