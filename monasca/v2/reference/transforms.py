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

import json

import falcon
from oslo.config import cfg

from monasca.api import monasca_transforms_api_v2
from monasca.common.repositories import exceptions as repository_exceptions
from monasca.common import resource_api
from monasca.openstack.common import log
from monasca.openstack.common import uuidutils
from monasca.v2.common.schemas import (exceptions as schemas_exceptions)
from monasca.v2.common.schemas import (
    transforms_request_body_schema as schemas_transforms)
from monasca.v2.reference import helpers


LOG = log.getLogger(__name__)


class Transforms(monasca_transforms_api_v2.TransformsV2API):
    def __init__(self, global_conf):

        super(Transforms, self).__init__(global_conf)
        self._region = cfg.CONF.region
        self._default_authorized_roles = (
            cfg.CONF.security.default_authorized_roles)
        self._transforms_repo = resource_api.init_driver(
            'monasca.repositories', cfg.CONF.repositories.transforms_driver)

    def _validate_transform(self, transform):
        """Validates the transform

        :param transform: An event object.
        :raises falcon.HTTPBadRequest
        """
        try:
            schemas_transforms.validate(transform)
        except schemas_exceptions.ValidationException as ex:
            LOG.debug(ex)
            raise falcon.HTTPBadRequest('Bad request', ex.message)

    def _create_transform(self, id, tenant_id, transform):
        """Store the transform using the repository.

        :param transform: A transform object.
        :raises: falcon.HTTPServiceUnavailable
        """
        try:
            name = transform['name']
            description = transform['description']
            specification = transform['specification']
            enabled = transform['enabled']
            self._transforms_repo.create_transforms(id, tenant_id, name,
                                                    description, specification,
                                                    enabled)
        except repository_exceptions.RepositoryException as ex:
            LOG.error(ex)
            raise falcon.HTTPInternalServerError('Service unavailable',
                                                 ex.message)

    def _create_transform_response(self, id, transform):
        name = transform['name']
        description = transform['description']
        specification = transform['specification']
        enabled = transform['enabled']
        response = {'id': id, 'name': name, 'description': description,
                    'specification': specification, 'enabled': enabled}
        return json.dumps(response)

    def _list_transforms(self, tenant_id):
        try:
            transforms = self._transforms_repo.list_transforms(tenant_id)
            return json.dumps(transforms)
        except repository_exceptions.RepositoryException as ex:
            LOG.error(ex)
            raise falcon.HTTPInternalServerError('Service unavailable',
                                                 ex.message)

    def _delete_transform(self, tenant_id, transform_id):
        try:
            self._transforms_repo.delete_transform(tenant_id, transform_id)
        except repository_exceptions.DoesNotExistException:
            raise falcon.HTTPNotFound()
        except repository_exceptions.RepositoryException as ex:
            LOG.error(ex)
            raise falcon.HTTPInternalServerError('Service unavailable',
                                                 ex.message)

    @resource_api.Restify('/v2.0/events/transforms', method='post')
    def do_post_transforms(self, req, res):
        helpers.validate_json_content_type(req)
        helpers.validate_authorization(req, self._default_authorized_roles)
        transform = helpers.read_http_resource(req)
        self._validate_transform(transform)
        id = uuidutils.generate_uuid()
        tenant_id = helpers.get_tenant_id(req)
        self._create_transform(id, tenant_id, transform)
        res.body = self._create_transform_response(id, transform)
        res.status = falcon.HTTP_200

    @resource_api.Restify('/v2.0/events/transforms', method='get')
    def do_get_transforms(self, req, res):
        helpers.validate_authorization(req, self._default_authorized_roles)
        tenant_id = helpers.get_tenant_id(req)
        res.body = self._list_transforms(tenant_id)
        res.status = falcon.HTTP_200

    @resource_api.Restify('/v2.0/events/transforms/{transform_id}',
                          method='delete')
    def do_delete_transforms(self, req, res, transform_id):
        helpers.validate_authorization(req, self._default_authorized_roles)
        tenant_id = helpers.get_tenant_id(req)
        self._delete_transform(tenant_id, transform_id)
        res.status = falcon.HTTP_204
