# Copyright 2016-2017 FUJITSU LIMITED
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

import re

import falcon
from oslo_log import log
import six

from monasca_api.api.core.log import exceptions
from monasca_api import conf

LOG = log.getLogger(__name__)
CONF = conf.CONF

APPLICATION_TYPE_CONSTRAINTS = {
    'MAX_LENGTH': 255,
    'PATTERN': re.compile('^[a-zA-Z0-9_.\\-]+$')
}
"""Application type constraint used in validation.

See :py:func:`Validations.validate_application_type`
"""
DIMENSION_NAME_CONSTRAINTS = {
    'MAX_LENGTH': 255,
    'PATTERN': re.compile('[^><={}(), \'";&]+$')
}
"""Constraint for name of single dimension.

See :py:func:`Validations.validate_dimensions`
"""
DIMENSION_VALUE_CONSTRAINTS = {
    'MAX_LENGTH': 255
}
"""Constraint for value of single dimension.

See :py:func:`Validations.validate_dimensions`
"""


def validate_application_type(application_type=None):
    """Validates application type.

       Validation won't take place if application_type is None.
       For details see: :py:data:`APPLICATION_TYPE_CONSTRAINTS`

       :param str application_type: application type
       """

    def validate_length():
        if (len(application_type) >
                APPLICATION_TYPE_CONSTRAINTS['MAX_LENGTH']):
            msg = ('Application type {type} must be '
                   '{length} characters or less')
            raise exceptions.HTTPUnprocessableEntity(
                msg.format(
                    type=application_type,
                    length=APPLICATION_TYPE_CONSTRAINTS[
                        'MAX_LENGTH']
                )
            )

    def validate_match():
        if (not APPLICATION_TYPE_CONSTRAINTS['PATTERN']
                .match(application_type)):
            raise exceptions.HTTPUnprocessableEntity(
                'Application type %s may only contain: "a-z A-Z 0-9 _ - ."'
                % application_type
            )

    if application_type:
        validate_length()
        validate_match()


def _validate_dimension_name(name):
    try:
        if len(name) > DIMENSION_NAME_CONSTRAINTS['MAX_LENGTH']:
            raise exceptions.HTTPUnprocessableEntity(
                'Dimension name %s must be 255 characters or less' %
                name
            )
        if name[0] == '_':
            raise exceptions.HTTPUnprocessableEntity(
                'Dimension name %s cannot start with underscore (_)' %
                name
            )
        if not DIMENSION_NAME_CONSTRAINTS['PATTERN'].match(name):
            raise exceptions.HTTPUnprocessableEntity(
                'Dimension name %s may not contain: %s' %
                (name, '> < = { } ( ) \' " , ; &')
            )
    except (TypeError, IndexError):
        raise exceptions.HTTPUnprocessableEntity(
            'Dimension name cannot be empty'
        )


def _validate_dimension_value(value):
    try:
        value[0]
        if len(value) > DIMENSION_VALUE_CONSTRAINTS['MAX_LENGTH']:
            raise exceptions.HTTPUnprocessableEntity(
                'Dimension value %s must be 255 characters or less' %
                value
            )
    except (TypeError, IndexError):
        raise exceptions.HTTPUnprocessableEntity(
            'Dimension value cannot be empty'
        )


def validate_dimensions(dimensions):
    """Validates dimensions type.

       Empty dimensions are not being validated.
       For details see:

       :param dict dimensions: dimensions to validate

       * :py:data:`DIMENSION_NAME_CONSTRAINTS`
       * :py:data:`DIMENSION_VALUE_CONSTRAINTS`
       """
    try:
        for dim_name, dim_value in dimensions.items():
            _validate_dimension_name(dim_name)
            _validate_dimension_value(dim_value)
    except AttributeError:
        raise exceptions.HTTPUnprocessableEntity(
            'Dimensions %s must be a dictionary (map)' % dimensions)


def validate_content_type(req, allowed):
    """Validates content type.

    Method validates request against correct
    content type.

    If content-type cannot be established (i.e. header is missing),
    :py:class:`falcon.HTTPMissingHeader` is thrown.
    If content-type is not **application/json** or **text/plain**,
    :py:class:`falcon.HTTPUnsupportedMediaType` is thrown.


    :param falcon.Request req: current request
    :param iterable allowed: allowed content type

    :exception: :py:class:`falcon.HTTPMissingHeader`
    :exception: :py:class:`falcon.HTTPUnsupportedMediaType`
    """
    content_type = req.content_type

    LOG.debug('Content-Type is %s', content_type)

    if content_type is None or len(content_type) == 0:
        raise falcon.HTTPMissingHeader('Content-Type')

    if content_type not in allowed:
        sup_types = ', '.join(allowed)
        details = ('Only [%s] are accepted as logs representations'
                   % str(sup_types))
        raise falcon.HTTPUnsupportedMediaType(description=details)


def validate_payload_size(req):
    """Validates payload size.

    Method validates sent payload size.
    It expects that http header **Content-Length** is present.
    If it does not, method raises :py:class:`falcon.HTTPLengthRequired`.
    Otherwise values is being compared with ::

        [service]
        max_log_size = 1048576

    **max_log_size** refers to the maximum allowed content length.
    If it is exceeded :py:class:`falcon.HTTPRequestEntityTooLarge` is
    thrown.

    :param falcon.Request req: current request

    :exception: :py:class:`falcon.HTTPLengthRequired`
    :exception: :py:class:`falcon.HTTPRequestEntityTooLarge`

    """
    payload_size = req.content_length
    max_size = CONF.service.max_log_size

    LOG.debug('Payload (content-length) is %s', str(payload_size))

    if payload_size is None:
        raise falcon.HTTPLengthRequired(
            title='Content length header is missing',
            description='Content length is required to estimate if '
                        'payload can be processed'
        )

    if payload_size >= max_size:
        raise falcon.HTTPPayloadTooLarge(
            title='Log payload size exceeded',
            description='Maximum allowed size is %d bytes' % max_size
        )


def validate_is_delegate(roles):
    delegate_roles = CONF.roles_middleware.delegate_roles
    if roles and delegate_roles:
        roles = roles.split(',') if isinstance(roles, six.string_types) \
            else roles
        return any(x in set(delegate_roles) for x in roles)
    return False


def validate_cross_tenant(tenant_id, cross_tenant_id, roles):

    if not validate_is_delegate(roles):
        if cross_tenant_id:
            raise falcon.HTTPForbidden(
                'Permission denied',
                'Projects %s cannot POST cross tenant logs' % tenant_id
            )


def validate_log_message(log_object):
    """Validates log property.

    Log property should have message property.

    Args:
        log_object (dict): log property
       """
    if 'message' not in log_object:
        raise exceptions.HTTPUnprocessableEntity(
            'Log property should have message'
        )
