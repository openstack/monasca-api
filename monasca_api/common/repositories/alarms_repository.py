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

        super(AlarmsRepository, self).__init__()

    @abc.abstractmethod
    def get_alarm_metrics(self, alarm_id):
        pass

    @abc.abstractmethod
    def get_sub_alarms(self, tenant_id, alarm_id):
        pass

    @abc.abstractmethod
    def update_alarm(self, tenant_id, alarm_id, state, lifecycle_state, link):
        pass

    @abc.abstractmethod
    def delete_alarm(self, tenant_id, id):
        pass

    @abc.abstractmethod
    def get_alarm(self, tenant_id, id):
        pass

    @abc.abstractmethod
    def get_alarms(self, tenant_id, query_parms, offset, limit):
        pass

    @abc.abstractmethod
    def get_alarms_count(self, tenant_id, query_parms, offset, limit):
        pass
