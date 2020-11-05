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
        """
        Create a notification.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            name: (str): write your description
            notification_type: (str): write your description
            address: (str): write your description
            period: (todo): write your description
        """
        return

    @abc.abstractmethod
    def list_notifications(self, tenant_id, sort_by, offset, limit):
        """
        List notifications for a tenant.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            sort_by: (str): write your description
            offset: (int): write your description
            limit: (int): write your description
        """
        return

    @abc.abstractmethod
    def delete_notification(self, tenant_id, notification_id):
        """
        Deletes a notification.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            notification_id: (str): write your description
        """
        return

    @abc.abstractmethod
    def list_notification(self, tenant_id, notification_id):
        """
        Get a list of notifications.

        Args:
            self: (todo): write your description
            tenant_id: (str): write your description
            notification_id: (str): write your description
        """
        return

    @abc.abstractmethod
    def update_notification(self, notification_id, tenant_id, name, notification_type,
                            address, period):
        """
        Update a notification.

        Args:
            self: (todo): write your description
            notification_id: (str): write your description
            tenant_id: (str): write your description
            name: (str): write your description
            notification_type: (str): write your description
            address: (str): write your description
            period: (todo): write your description
        """
        return
