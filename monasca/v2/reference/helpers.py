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
import datetime

import falcon
from falcon.util.uri import parse_query_string

from monasca.openstack.common import log
from monasca.v2.common.schemas import exceptions as schemas_exceptions
from monasca.v2.common.schemas import metric_name_schema
from monasca.v2.common.schemas import dimensions_schema


LOG = log.getLogger(__name__)


def validate_json_content_type(req):
    if req.content_type not in ['application/json']:
        raise falcon.HTTPBadRequest('Bad request', 'Bad content type. Must be '
                                                   'application/json')


def is_in_role(req, authorized_roles):
    '''
    Determines if one or more of the X-ROLES is in the supplied
    authorized_roles.
    :param req: HTTP request object. Must contain "X-ROLES" in the HTTP
    request header.
    :param authorized_roles: List of authorized roles to check against.
    :return: Returns True if in the list of authorized roles, otherwise False.
    '''
    str_roles = req.get_header('X-ROLES')
    if str_roles == None:
        return False
    roles = str_roles.lower().split(',')
    for role in roles:
        if role in authorized_roles:
            return True
    return False


def validate_authorization(req, authorized_roles):
    '''
    Validates whether one or more X-ROLES in the HTTP header is authorized.
    :param req: HTTP request object. Must contain "X-ROLES" in the HTTP
    request header.
    :param authorized_roles: List of authorized roles to check against.
    :raises falcon.HTTPUnauthorized:
    '''
    str_roles = req.get_header('X-ROLES')
    if str_roles == None:
        raise falcon.HTTPUnauthorized('Forbidden',
                                      'Tenant does not have any roles', '')
    roles = str_roles.lower().split(',')
    for role in roles:
        if role in authorized_roles:
            return
    raise falcon.HTTPUnauthorized('Forbidden',
                                  'Tenant ID is missing a required role to '
                                  'access this service', '')


def get_tenant_id(req):
    '''
    Returns the tenant ID in the HTTP request header.
    :param req: HTTP request object.
    '''
    return req.get_header('X-TENANT-ID')


def get_cross_tenant_or_tenant_id(req, delegate_authorized_roles):
    '''
    Evaluates whether the tenant ID or cross tenant ID should be returned.
    :param req: HTTP request object.
    :param delegate_authorized_roles: List of authorized roles that have
    delegate privileges.
    :returns: Returns the cross tenant or tenant ID.
    '''
    if is_in_role(req, delegate_authorized_roles):
        params = parse_query_string(req.query_string)
        if 'tenant_id' in params:
            tenant_id = params['tenant_id']
            return tenant_id
    return get_tenant_id(req)


def get_query_name(req):
    '''
    Returns the query param "name" if supplied.
    :param req: HTTP request object.
    '''
    params = parse_query_string(req.query_string)
    name = ''
    if 'name' in params:
        name = params['name']
    return name


def get_query_dimensions(req):
    '''
    Gets and parses the query param dimensions.
    :param req: HTTP request object.
    :return: Returns the dimensions as a JSON object
    :raises falcon.HTTPBadRequest: If dimensions are malformed.
    '''
    try:
        params = parse_query_string(req.query_string)
        dimensions = {}
        if 'dimensions' in params:
            dimensions_str = params['dimensions']
            dimensions_str_array = dimensions_str.split(',')
            for dimension in dimensions_str_array:
                dimension_name_value = dimension.split(':')
                if len(dimension_name_value) == 2:
                    dimensions[dimension_name_value[0]] = dimension_name_value[
                        1]
                else:
                    raise Exception('Dimensions are malformed')
        return dimensions
    except Exception as ex:
        LOG.debug(ex)
        raise falcon.HTTPBadRequest('Bad request', ex.message)


def get_query_starttime_timestamp(req):
    try:
        params = parse_query_string(req.query_string)
        if 'start_time' in params:
            return _convert_time_string(params['start_time'])
        else:
            raise Exception("Missing start time")
    except Exception as ex:
        LOG.debug(ex)
        raise falcon.HTTPBadRequest('Bad request', ex.message)


def get_query_endtime_timestamp(req):
    try:
        params = parse_query_string(req.query_string)
        if 'end_time' in params:
            return _convert_time_string(params['end_time'])
        else:
            return None
    except Exception as ex:
        LOG.debug(ex)
        raise falcon.HTTPBadRequest('Bad request', ex.message)


def _convert_time_string(date_time_string):
    dt = datetime.datetime.strptime(date_time_string, "%Y-%m-%dT%H:%M:%SZ")
    timestamp = (dt - datetime.datetime(1970, 1, 1)).total_seconds()
    return timestamp


def validate_query_name(name):
    '''
    Validates the query param name.
    :param name: Query param name.
    :raises falcon.HTTPBadRequest: If name is not valid.
    '''
    try:
        metric_name_schema.validate(name)
    except schemas_exceptions.ValidationException as ex:
        LOG.debug(ex)
        raise falcon.HTTPBadRequest('Bad request', ex.message)


def validate_query_dimensions(dimensions):
    '''
    Validates the query param dimensions.
    :param dimensions: Query param dimensions.
    :raises falcon.HTTPBadRequest: If dimensions are not valid.
    '''
    try:
        dimensions_schema.validate(dimensions)
    except schemas_exceptions.ValidationException as ex:
        LOG.debug(ex)
        raise falcon.HTTPBadRequest('Bad request', ex.message)