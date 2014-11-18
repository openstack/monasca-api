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
import json
import urlparse

import falcon
import simplejson

from monasca.openstack.common import log
from monasca.v2.common.schemas import dimensions_schema
from monasca.v2.common.schemas import exceptions as schemas_exceptions
from monasca.v2.common.schemas import metric_name_schema

LOG = log.getLogger(__name__)


def read_json_msg_body(req):
    """Read the json_msg from the http request body and return them as JSON.

    :param req: HTTP request object.
    :return: Returns the metrics as a JSON object.
    :raises falcon.HTTPBadRequest:
    """
    try:
        msg = req.stream.read()
        json_msg = json.loads(msg)
        return json_msg
    except ValueError as ex:
        LOG.debug(ex)
        raise falcon.HTTPBadRequest('Bad request',
                                    'Request body is not valid JSON')


def validate_json_content_type(req):
    if req.content_type not in ['application/json']:
        raise falcon.HTTPBadRequest('Bad request', 'Bad content type. Must be '
                                                   'application/json')


def is_in_role(req, authorized_roles):
    """Is one or more of the X-ROLES in the supplied authorized_roles.

    :param req: HTTP request object. Must contain "X-ROLES" in the HTTP
    request header.
    :param authorized_roles: List of authorized roles to check against.
    :return: Returns True if in the list of authorized roles, otherwise False.
    """
    str_roles = req.get_header('X-ROLES')
    if str_roles is None:
        return False
    roles = str_roles.lower().split(',')
    for role in roles:
        if role in authorized_roles:
            return True
    return False


def validate_authorization(req, authorized_roles):
    """Validates whether one or more X-ROLES in the HTTP header is authorized.

    :param req: HTTP request object. Must contain "X-ROLES" in the HTTP
    request header.
    :param authorized_roles: List of authorized roles to check against.
    :raises falcon.HTTPUnauthorized
    """
    str_roles = req.get_header('X-ROLES')
    if str_roles is None:
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
    """Returns the tenant ID in the HTTP request header.

    :param req: HTTP request object.
    """
    return req.get_header('X-TENANT-ID')


def get_x_tenant_or_tenant_id(req, delegate_authorized_roles):
    """Evaluates whether the tenant ID or cross tenant ID should be returned.

    :param req: HTTP request object.
    :param delegate_authorized_roles: List of authorized roles that have
    delegate privileges.
    :returns: Returns the cross tenant or tenant ID.
    """
    if is_in_role(req, delegate_authorized_roles):
        params = falcon.uri.parse_query_string(req.query_string)
        if 'tenant_id' in params:
            tenant_id = params['tenant_id']
            return tenant_id
    return get_tenant_id(req)


def get_query_name(req, name_required=False):
    """Returns the query param "name" if supplied.

    :param req: HTTP request object.
    """
    try:
        params = falcon.uri.parse_query_string(req.query_string)
        if 'name' in params:
            name = params['name']
            return name
        else:
            if name_required:
                raise Exception("Missing name")
            else:
                return ''
    except Exception as ex:
        LOG.debug(ex)
        raise falcon.HTTPBadRequest('Bad request', ex.message)


def get_query_dimensions(req):
    """Gets and parses the query param dimensions.

    :param req: HTTP request object.
    :return: Returns the dimensions as a JSON object
    :raises falcon.HTTPBadRequest: If dimensions are malformed.
    """
    try:
        params = falcon.uri.parse_query_string(req.query_string)
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


def get_query_starttime_timestamp(req, required=True):
    try:
        params = falcon.uri.parse_query_string(req.query_string)
        if 'start_time' in params:
            return _convert_time_string(params['start_time'])
        else:
            if required:
                raise Exception("Missing start time")
            else:
                return None
    except Exception as ex:
        LOG.debug(ex)
        raise falcon.HTTPBadRequest('Bad request', ex.message)


def get_query_endtime_timestamp(req, required=True):
    try:
        params = falcon.uri.parse_query_string(req.query_string)
        if 'end_time' in params:
            return _convert_time_string(params['end_time'])
        else:
            if required:
                raise Exception("Missing end time")
            else:
                return None
    except Exception as ex:
        LOG.debug(ex)
        raise falcon.HTTPBadRequest('Bad request', ex.message)


def _convert_time_string(date_time_string):
    dt = datetime.datetime.strptime(date_time_string, "%Y-%m-%dT%H:%M:%SZ")
    timestamp = (dt - datetime.datetime(1970, 1, 1)).total_seconds()
    return timestamp


def get_query_statistics(req):
    try:
        params = falcon.uri.parse_query_string(req.query_string)
        if 'statistics' in params:
            statistics = params['statistics'].split(',')
            statistics = [statistic.lower() for statistic in statistics]
            if not all(statistic in ['avg', 'min', 'max', 'count', 'sum'] for
                       statistic in statistics):
                raise Exception("Invalid statistic")
            return statistics
        else:
            raise Exception("Missing statistics")
    except Exception as ex:
        LOG.debug(ex)
        raise falcon.HTTPBadRequest('Bad request', ex.message)


def get_query_period(req):
    try:
        params = falcon.uri.parse_query_string(req.query_string)
        if 'period' in params:
            return params['period']
        else:
            return None
    except Exception as ex:
        LOG.debug(ex)
        raise falcon.HTTPBadRequest('Bad request', ex.message)


def validate_query_name(name):
    """Validates the query param name.

    :param name: Query param name.
    :raises falcon.HTTPBadRequest: If name is not valid.
    """
    try:
        metric_name_schema.validate(name)
    except schemas_exceptions.ValidationException as ex:
        LOG.debug(ex)
        raise falcon.HTTPBadRequest('Bad request', ex.message)


def validate_query_dimensions(dimensions):
    """Validates the query param dimensions.

    :param dimensions: Query param dimensions.
    :raises falcon.HTTPBadRequest: If dimensions are not valid.
    """
    try:
        dimensions_schema.validate(dimensions)
    except schemas_exceptions.ValidationException as ex:
        LOG.debug(ex)
        raise falcon.HTTPBadRequest('Bad request', ex.message)


def get_link(uri, resource_id, rel='self'):
    """Returns a link dictionary containing href, and rel.

    :param uri: the http request.uri.
    :param resource_id: the id of the resource
    """
    parsed_uri = urlparse.urlparse(uri)
    href = parsed_uri.scheme + '://' + parsed_uri.netloc + parsed_uri.path
    href += '/' + resource_id

    if rel:
        link_dict = dict(href=href, rel=rel)
    else:
        link_dict = dict(href=href)

    return link_dict


def add_links_to_resource(resource, uri, rel='self'):
    """Adds links to the given resource dictionary.

    :param resource: the resource dictionary you wish to add links.
    :param uri: the http request.uri.
    """
    resource['links'] = [get_link(uri, resource['id'], rel)]
    return resource


def add_links_to_resource_list(resourcelist, uri):
    """Adds links to the given resource dictionary list.

    :param resourcelist: the list of resources you wish to add links.
    :param uri: the http request.uri.
    """
    for resource in resourcelist:
        add_links_to_resource(resource, uri)
    return resourcelist


def read_http_resource(req):
    """Read from http request and return json.

    :param req: the http request.
    """
    try:
        msg = req.stream.read()
        json_msg = simplejson.loads(msg)
        return json_msg
    except ValueError as ex:
        LOG.debug(ex)
        raise falcon.HTTPBadRequest(
            'Bad request',
            'Request body is not valid JSON')


def raise_not_found_exception(resource_name, resource_id, tenant_id):
    """Provides exception for not found requests (update, delete, list).

    :param resource_name: the name of the resource.
    :param resource_id: id of the resource.
    :param tenant_id: id of the tenant
    """
    msg = 'No %s method exists for tenant_id = %s id = %s' % (
        resource_name, tenant_id, resource_id)
    raise falcon.HTTPError(
        status='404 Not Found',
        title='Not Found',
        description=msg,
        code=404)
