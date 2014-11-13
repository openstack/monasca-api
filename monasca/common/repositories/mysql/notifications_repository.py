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

import datetime

import model
import peewee

from monasca.common.repositories import exceptions
from monasca.common.repositories import notifications_repository
from monasca.openstack.common import log


LOG = log.getLogger(__name__)


class Notification_Method(model.Model):
    id = peewee.TextField(36)
    tenant_id = peewee.TextField(36)
    name = peewee.TextField()
    type = peewee.TextField()
    address = peewee.TextField()
    created_at = peewee.DateTimeField()
    updated_at = peewee.DateTimeField()


class NotificationsRepository(
        notifications_repository.NotificationsRepository):

    def notification_from_result(self, result):
        notification = dict(id=result.id,
                            name=result.name,
                            type=result.type,
                            address=result.address)
        return notification

    def exists(self, tenant_id, name):
        try:
            return (Notification_Method.select().where(
                (Notification_Method.tenant_id == tenant_id) & (
                    Notification_Method.name == name)).count() > 0)
        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def create_notification(
            self, id, tenant_id, name, notification_type, address):
        try:
            now = datetime.datetime.utcnow()
            Notification_Method.create(
                id=id,
                tenant_id=tenant_id,
                name=name,
                notification_type=notification_type,
                address=address,
                created_at=now,
                updated_at=now)
        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def list_notifications(self, tenant_id):
        try:
            q = Notification_Method.select().where(
                Notification_Method.tenant_id == tenant_id)
            results = q.execute()

            notifications = [
                self.notification_from_result(result) for result in results]
            return notifications
        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def delete_notification(self, tenant_id, notification_id):

        try:
            q = Notification_Method.delete().where(
                (Notification_Method.tenant_id == tenant_id) & (
                    Notification_Method.id == notification_id))
            num_rows_deleted = q.execute()
        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

        if num_rows_deleted < 1:
            raise exceptions.DoesNotExistException()

        return

    def list_notification(self, tenant_id, notification_id):
        try:
            result = Notification_Method.get(
                (Notification_Method.tenant_id == tenant_id) & (
                    Notification_Method.id == notification_id))
            return (self.notification_from_result(result))
        except Notification_Method.DoesNotExist as e:
            raise exceptions.DoesNotExistException(str(e))
        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)

    def update_notification(
            self, id, tenant_id, name, notification_type, address):
        now = datetime.datetime.utcnow()
        try:
            q = Notification_Method.update(
                name=name,
                type=notification_type,
                address=address,
                created_at=now,
                updated_at=now).where(
                (Notification_Method.tenant_id == tenant_id) & (
                    Notification_Method.id == id))
            # Execute the query, updating the database.
            num_rows_updated = q.execute()
        except Exception as ex:
            LOG.exception(ex)
            raise exceptions.RepositoryException(ex)
        else:
            if num_rows_updated == 0:
                raise exceptions.DoesNotExistException('Not Found')
