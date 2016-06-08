# (C) Copyright 2014-2016 Hewlett Packard Enterprise Development Company LP
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

import abc

import six


@six.add_metaclass(abc.ABCMeta)
class NotificationsRepository(object):

    @abc.abstractmethod
    def create_notification(self, tenant_id, name, notification_type,
                            address, period):
        return

    @abc.abstractmethod
    def list_notifications(self, tenant_id, sort_by, offset, limit):
        return

    @abc.abstractmethod
    def delete_notification(self, tenant_id, notification_id):
        return

    @abc.abstractmethod
    def list_notification(self, tenant_id, notification_id):
        return

    @abc.abstractmethod
    def update_notification(self, notification_id, tenant_id, name, notification_type,
                            address, period):
        return
