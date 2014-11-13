# Copyright 2014 Hewlett-Packard
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

import model
import peewee

from monasca.common.repositories import exceptions
from monasca.common.repositories import transforms_repository
from monasca.openstack.common import log


LOG = log.getLogger(__name__)


class Transform(model.Model):
    id = peewee.TextField(36)
    tenant_id = peewee.TextField(36)
    name = peewee.TextField()
    description = peewee.TextField()
    specification = peewee.TextField()
    enabled = peewee.BooleanField()
    created_at = peewee.DateTimeField()
    updated_at = peewee.DateTimeField()
    deleted_at = peewee.DateTimeField()


class TransformsRepository(transforms_repository.TransformsRepository):
    def create_transforms(self, id, tenant_id, name, description,
                          specification, enabled):
        try:
            q = Transform.create(id=id, tenant_id=tenant_id, name=name,
                                 description=description,
                                 specification=specification, enabled=enabled)
            q.save()
        except Exception as ex:
            LOG.exception(str(ex))
            raise exceptions.RepositoryException(str(ex))

    def list_transforms(self, tenant_id):
        try:
            q = Transform.select().where(Transform.tenant_id == tenant_id)
            results = q.execute()

            transforms = []
            for result in results:
                transform = {'id': result.id, 'name': result.name,
                             'description': result.description,
                             'specification': result.specification,
                             'enabled': result.enabled}
                transforms.append(transform)
            return transforms
        except Exception as ex:
            LOG.exception(str(ex))
            raise exceptions.RepositoryException(str(ex))

    def delete_transform(self, tenant_id, transform_id):
        num_rows_deleted = 0

        try:
            q = Transform.delete().where((Transform.tenant_id == tenant_id) & (
                Transform.id == transform_id))
            num_rows_deleted = q.execute()
        except Exception as ex:
            LOG.exception(str(ex))
            raise exceptions.RepositoryException(str(ex))

        if num_rows_deleted < 1:
            raise exceptions.DoesNotExistException()

        return
