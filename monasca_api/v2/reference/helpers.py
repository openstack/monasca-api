# Copyright 2015 Cray Inc. All Rights Reserved.
# (C) Copyright 2014,2016-2017 Hewlett Packard Enterprise Development LP
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

import falcon
from monasca_common.validation import metrics as metric_validation
from oslo_log import log
from oslo_utils import timeutils
import simplejson
import six
import six.moves.urllib.parse as urlparse

from monasca_api.v2.common.exceptions import HTTPUnprocessableEntityError

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


def validate_authorization(req, authorized_roles):
    """Validates whether one or more X-ROLES in the HTTP header is authorized.

    If authorization fails, 401 is thrown with appropriate description.
    Additionally response specifies 'WWW-Authenticate' header with 'Token'
    value challenging the client to use different token (the one with
    different set of roles).

    :param req: HTTP request object. Must contain "X-ROLES" in the HTTP
    request header.
    :param authorized_roles: List of authorized roles to check against.
    :raises falcon.HTTPUnauthorized
    """
    roles = req.roles
    challenge = 'Token'
    if not roles:
        raise falcon.HTTPUnauthorized('Forbidden',
                                      'Tenant does not have any roles',
                                      challenge)
    roles = roles.split(',') if isinstance(roles, six.string_types) else roles
    authorized_roles_lower = [r.lower() for r in authorized_roles]
    for role in roles:
        role = role.lower()
        if role in authorized_roles_lower:
            return
    raise falcon.HTTPUnauthorized('Forbidden',
                                  'Tenant ID is missing a required role to '
                                  'access this service',
                                  challenge)


def get_x_tenant_or_tenant_id(req, delegate_authorized_roles):
    """Evaluates whether the tenant ID or cross tenant ID should be returned.

    :param req: HTTP request object.
    :param delegate_authorized_roles: List of authorized roles that have
    delegate privileges.
    :returns: Returns the cross tenant or tenant ID.
    """
    if any(x in set(delegate_authorized_roles) for x in req.roles):
        params = falcon.uri.parse_query_string(req.query_string)
        if 'tenant_id' in params:
            tenant_id = params['tenant_id']
            return tenant_id
    return req.project_id


def get_query_param(req, param_name, required=False, default_val=None):
    try:
        params = falcon.uri.parse_query_string(req.query_string)
        if param_name in params:
            if isinstance(params[param_name], list):
                param_val = params[param_name][0].decode('utf8')
            else:
                param_val = params[param_name].decode('utf8')

            return param_val
        else:
            if required:
                raise Exception("Missing " + param_name)
            else:
                return default_val
    except Exception as ex:
        LOG.debug(ex)
        raise HTTPUnprocessableEntityError('Unprocessable Entity', ex.message)


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
        raise HTTPUnprocessableEntityError('Unprocessable Entity', ex.message)


def get_query_dimensions(req, param_key='dimensions'):
    """Gets and parses the query param dimensions.

    :param req: HTTP request object.
    :param dimensions_param: param name for dimensions, default='dimensions'
    :return: Returns the dimensions as a JSON object
    :raises falcon.HTTPBadRequest: If dimensions are malformed.
    """
    try:
        params = falcon.uri.parse_query_string(req.query_string)
        dimensions = {}
        if param_key not in params:
            return dimensions

        dimensions_param = params[param_key]
        if isinstance(dimensions_param, basestring):
            dimensions_str_array = dimensions_param.split(',')
        elif isinstance(dimensions_param, list):
            dimensions_str_array = []
            for sublist in dimensions_param:
                dimensions_str_array.extend(sublist.split(","))
        else:
            raise Exception("Error parsing dimensions, unknown format")

        for dimension in dimensions_str_array:
            dimension_name_value = dimension.split(':', 1)
            if len(dimension_name_value) == 2:
                dimensions[dimension_name_value[0]] = dimension_name_value[1]
            elif len(dimension_name_value) == 1:
                dimensions[dimension_name_value[0]] = ""
        return dimensions
    except Exception as ex:
        LOG.debug(ex)
        raise HTTPUnprocessableEntityError('Unprocessable Entity', ex.message)


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
        raise HTTPUnprocessableEntityError('Unprocessable Entity', ex.message)


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
        raise HTTPUnprocessableEntityError('Unprocessable Entity', ex.message)


def validate_start_end_timestamps(start_timestamp, end_timestamp=None):
    if end_timestamp:
        if not start_timestamp < end_timestamp:
            raise falcon.HTTPBadRequest('Bad request',
                                        'start_time must be before end_time')


def _convert_time_string(date_time_string):
    dt = timeutils.parse_isotime(date_time_string)
    dt = timeutils.normalize_time(dt)
    timestamp = (dt - datetime.datetime(1970, 1, 1)).total_seconds()
    return timestamp


def get_query_statistics(req):
    try:
        params = falcon.uri.parse_query_string(req.query_string)
        if 'statistics' in params:
            statistics = []
            # falcon may return this as a list or as a string
            if isinstance(params['statistics'], list):
                statistics.extend(params['statistics'])
            else:
                statistics.extend(params['statistics'].split(','))
            statistics = [statistic.lower() for statistic in statistics]
            if not all(statistic in ['avg', 'min', 'max', 'count', 'sum'] for
                       statistic in statistics):
                raise Exception("Invalid statistic")
            return statistics
        else:
            raise Exception("Missing statistics")
    except Exception as ex:
        LOG.debug(ex)
        raise HTTPUnprocessableEntityError('Unprocessable Entity', ex.message)


def get_query_period(req):
    try:
        params = falcon.uri.parse_query_string(req.query_string)
        if 'period' in params:
            period = params['period']
            try:
                period = int(period)
            except Exception:
                raise Exception("Period must be a valid integer")
            if period < 0:
                raise Exception("Period must be a positive integer")
            return str(period)
        else:
            return None
    except Exception as ex:
        LOG.debug(ex)
        raise HTTPUnprocessableEntityError('Unprocessable Entity', ex.message)


def get_query_group_by(req):
    try:
        params = falcon.uri.parse_query_string(req.query_string)
        if 'group_by' in params:
            group_by = params['group_by']
            if not isinstance(group_by, list):
                group_by = [group_by]
            return group_by
        else:
            return None
    except Exception as ex:
        LOG.debug(ex)
        raise HTTPUnprocessableEntityError('Unprocessable Entity', ex.message)


def validate_query_name(name):
    """Validates the query param name.

    :param name: Query param name.
    :raises falcon.HTTPBadRequest: If name is not valid.
    """
    if not name:
        return
    try:
        metric_validation.validate_name(name)
    except Exception as ex:
        LOG.debug(ex)
        raise HTTPUnprocessableEntityError('Unprocessable Entity', ex.message)


def validate_query_dimensions(dimensions):
    """Validates the query param dimensions.

    :param dimensions: Query param dimensions.
    :raises falcon.HTTPBadRequest: If dimensions are not valid.
    """
    try:

        for key, value in dimensions.items():
            if key.startswith('_'):
                raise Exception("Dimension key {} may not start with '_'".format(key))
            metric_validation.validate_dimension_key(key)
            if value:
                if '|' in value:
                    values = value.split('|')
                    for v in values:
                        metric_validation.validate_dimension_value(key, v)
                else:
                    metric_validation.validate_dimension_value(key, value)
    except Exception as ex:
        LOG.debug(ex)
        raise HTTPUnprocessableEntityError('Unprocessable Entity', ex.message)


def paginate(resource, uri, limit):
    parsed_uri = urlparse.urlparse(uri)

    self_link = build_base_uri(parsed_uri)

    old_query_params = _get_old_query_params(parsed_uri)

    if old_query_params:
        self_link += '?' + '&'.join(old_query_params)

    if resource and len(resource) > limit:

        if 'id' in resource[limit - 1]:
            new_offset = resource[limit - 1]['id']

        next_link = build_base_uri(parsed_uri)

        new_query_params = [u'offset' + '=' + urlparse.quote(
            new_offset.encode('utf8'), safe='')]

        _get_old_query_params_except_offset(new_query_params, parsed_uri)

        if new_query_params:
            next_link += '?' + '&'.join(new_query_params)

        resource = {u'links': ([{u'rel': u'self',
                                 u'href': self_link.decode('utf8')},
                                {u'rel': u'next',
                                 u'href': next_link.decode('utf8')}]),
                    u'elements': resource[:limit]}

    else:

        resource = {u'links': ([{u'rel': u'self',
                                 u'href': self_link.decode('utf8')}]),
                    u'elements': resource}

    return resource


def paginate_with_no_id(dictionary_list, uri, offset, limit):
    """This method is to paginate a list of dictionaries with no id in it.
       For example, metric name list, directory name list and directory
       value list.
    """
    parsed_uri = urlparse.urlparse(uri)
    self_link = build_base_uri(parsed_uri)
    old_query_params = _get_old_query_params(parsed_uri)

    if old_query_params:
        self_link += '?' + '&'.join(old_query_params)

    value_list = []
    for item in dictionary_list:
        value_list.extend(item.values())

    if value_list:
        # Truncate dictionary list with offset first
        truncated_list_offset = _truncate_with_offset(
            dictionary_list, value_list, offset)

        # Then truncate it with limit
        truncated_list_offset_limit = truncated_list_offset[:limit]

        links = [{u'rel': u'self', u'href': self_link.decode('utf8')}]
        if len(truncated_list_offset) > limit:
            new_offset = truncated_list_offset_limit[limit - 1].values()[0]
            next_link = build_base_uri(parsed_uri)
            new_query_params = [u'offset' + '=' + new_offset]

            _get_old_query_params_except_offset(new_query_params, parsed_uri)

            if new_query_params:
                next_link += '?' + '&'.join(new_query_params)

            links.append({u'rel': u'next', u'href': next_link.decode('utf8')})

        resource = {u'links': links,
                    u'elements': truncated_list_offset_limit}
    else:
        resource = {u'links': ([{u'rel': u'self',
                                 u'href': self_link.decode('utf8')}]),
                    u'elements': dictionary_list}

    return resource


def _truncate_with_offset(resource, value_list, offset):
    """Truncate a list of dictionaries with a given offset.
    """
    if not offset:
        return resource

    offset = offset.lower()
    for i, j in enumerate(value_list):
        # if offset matches one of the values in value_list,
        # the truncated list should start with the one after current offset
        if j == offset:
            return resource[i + 1:]
        # if offset does not exist in value_list, find the nearest
        # location and truncate from that location.
        if j > offset:
            return resource[i:]
    return []


def paginate_alarming(resource, uri, limit):
    parsed_uri = urlparse.urlparse(uri)

    self_link = build_base_uri(parsed_uri)

    old_query_params = _get_old_query_params(parsed_uri)

    if old_query_params:
        self_link += '?' + '&'.join(old_query_params)

    if resource and len(resource) > limit:

        old_offset = 0
        for param in old_query_params:
            if param.find('offset') >= 0:
                old_offset = int(param.split('=')[-1])
        new_offset = str(limit + old_offset)

        next_link = build_base_uri(parsed_uri)

        new_query_params = [u'offset' + '=' + urlparse.quote(
            new_offset.encode('utf8'), safe='')]

        _get_old_query_params_except_offset(new_query_params, parsed_uri)

        if new_query_params:
            next_link += '?' + '&'.join(new_query_params)

        resource = {u'links': ([{u'rel': u'self',
                                 u'href': self_link.decode('utf8')},
                                {u'rel': u'next',
                                 u'href': next_link.decode('utf8')}]),
                    u'elements': resource[:limit]}

    else:

        resource = {u'links': ([{u'rel': u'self',
                                 u'href': self_link.decode('utf8')}]),
                    u'elements': resource}

    return resource


def paginate_dimension_values(dimvals, uri, offset, limit):

    parsed_uri = urlparse.urlparse(uri)
    self_link = build_base_uri(parsed_uri)
    old_query_params = _get_old_query_params(parsed_uri)

    if old_query_params:
        self_link += '?' + '&'.join(old_query_params)

    if (dimvals and dimvals[u'values']):
        have_more, truncated_values = _truncate_dimension_values(dimvals[u'values'],
                                                                 limit,
                                                                 offset)

        links = [{u'rel': u'self', u'href': self_link.decode('utf8')}]
        if have_more:
            new_offset = truncated_values[limit - 1]
            next_link = build_base_uri(parsed_uri)
            new_query_params = [u'offset' + '=' + urlparse.quote(
                new_offset.encode('utf8'), safe='')]

            _get_old_query_params_except_offset(new_query_params, parsed_uri)

            if new_query_params:
                next_link += '?' + '&'.join(new_query_params)

            links.append({u'rel': u'next', u'href': next_link.decode('utf8')})

        truncated_dimvals = {u'id': dimvals[u'id'],
                             u'dimension_name': dimvals[u'dimension_name'],
                             u'values': truncated_values}
        #
        # Only return metric name if one was provided
        #
        if u'metric_name' in dimvals:
            truncated_dimvals[u'metric_name'] = dimvals[u'metric_name']

        resource = {u'links': links,
                    u'elements': [truncated_dimvals]}
    else:
        resource = {u'links': ([{u'rel': u'self',
                                 u'href': self_link.decode('utf8')}]),
                    u'elements': [dimvals]}

    return resource


def _truncate_dimension_values(values, limit, offset):
    if offset and offset in values:
        next_value_pos = values.index(offset) + 1
        values = values[next_value_pos:]
    have_more = len(values) > limit
    return have_more, values[:limit]


def paginate_measurements(measurements, uri, limit):
    parsed_uri = urlparse.urlparse(uri)

    self_link = build_base_uri(parsed_uri)

    old_query_params = _get_old_query_params(parsed_uri)

    if old_query_params:
        self_link += '?' + '&'.join(old_query_params)

    if measurements:
        measurement_elements = []
        resource = {u'links': [{u'rel': u'self',
                                u'href': self_link.decode('utf8')},
                               ]}
        for measurement in measurements:
            if len(measurement['measurements']) >= limit:

                new_offset = measurement['measurements'][limit - 1][0]

                next_link = build_base_uri(parsed_uri)

                new_query_params = [u'offset' + '=' + urlparse.quote(
                    new_offset.encode('utf8'), safe='')]

                _get_old_query_params_except_offset(new_query_params, parsed_uri)

                if new_query_params:
                    next_link += '?' + '&'.join(new_query_params)

                resource[u'links'].append({u'rel': u'next',
                                           u'href': next_link.decode('utf8')})

                truncated_measurement = {u'dimensions': measurement['dimensions'],
                                         u'measurements': (measurement
                                                           ['measurements'][:limit]),
                                         u'name': measurement['name'],
                                         u'columns': measurement['columns'],
                                         u'id': measurement['id']}
                measurement_elements.append(truncated_measurement)
                break
            else:
                limit -= len(measurement['measurements'])
                measurement_elements.append(measurement)

        resource[u'elements'] = measurement_elements

    else:

        resource = {u'links': ([{u'rel': u'self',
                                 u'href': self_link.decode('utf8')}]),
                    u'elements': []}

    return resource


def _get_old_query_params(parsed_uri):
    old_query_params = []

    if parsed_uri.query:

        for query_param in parsed_uri.query.split('&'):
            query_param_name, query_param_val = query_param.split('=', 1)

            old_query_params.append(urlparse.quote(
                query_param_name.encode('utf8'), safe='')
                + "="
                + urlparse.quote(query_param_val.encode('utf8'), safe=''))

    return old_query_params


def _get_old_query_params_except_offset(new_query_params, parsed_uri):
    if parsed_uri.query:

        for query_param in parsed_uri.query.split('&'):
            query_param_name, query_param_val = query_param.split('=', 1)
            if query_param_name.lower() != 'offset':
                new_query_params.append(urlparse.quote(
                    query_param_name.encode(
                        'utf8'), safe='') + "=" + urlparse.quote(
                    query_param_val.encode(
                        'utf8'), safe=''))


def paginate_statistics(statistics, uri, limit):
    parsed_uri = urlparse.urlparse(uri)

    self_link = build_base_uri(parsed_uri)

    old_query_params = _get_old_query_params(parsed_uri)

    if old_query_params:
        self_link += '?' + '&'.join(old_query_params)

    if statistics:
        statistic_elements = []
        resource = {u'links': [{u'rel': u'self',
                                u'href': self_link.decode('utf8')}]}

        for statistic in statistics:
            if len(statistic['statistics']) >= limit:

                new_offset = (
                    statistic['statistics'][limit - 1][0])

                next_link = build_base_uri(parsed_uri)

                new_query_params = [u'offset' + '=' + urlparse.quote(
                    new_offset.encode('utf8'), safe='')]

                _get_old_query_params_except_offset(new_query_params, parsed_uri)

                if new_query_params:
                    next_link += '?' + '&'.join(new_query_params)

                resource[u'links'].append({u'rel': u'next',
                                          u'href': next_link.decode('utf8')})

                truncated_statistic = {u'dimensions': statistic['dimensions'],
                                       u'statistics': (statistic['statistics'][:limit]),
                                       u'name': statistic['name'],
                                       u'columns': statistic['columns'],
                                       u'id': statistic['id']}

                statistic_elements.append(truncated_statistic)
                break
            else:
                limit -= len(statistic['statistics'])
                statistic_elements.append(statistic)

        resource[u'elements'] = statistic_elements

    else:

        resource = {u'links': ([{u'rel': u'self',
                                 u'href': self_link.decode('utf8')}]),
                    u'elements': []}

    return resource


def create_alarms_count_next_link(uri, offset, limit):
    if offset is None:
            offset = 0
    parsed_url = urlparse.urlparse(uri)
    base_url = build_base_uri(parsed_url)
    new_query_params = [u'offset=' + urlparse.quote(str(offset + limit))]
    _get_old_query_params_except_offset(new_query_params, parsed_url)

    next_link = base_url
    if new_query_params:
        next_link += '?' + '&'.join(new_query_params)

    return next_link


def build_base_uri(parsed_uri):
    return parsed_uri.scheme + '://' + parsed_uri.netloc + parsed_uri.path


def get_link(uri, resource_id, rel='self'):
    """Returns a link dictionary containing href, and rel.

    :param uri: the http request.uri.
    :param resource_id: the id of the resource
    """
    parsed_uri = urlparse.urlparse(uri)
    href = build_base_uri(parsed_uri)
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
        raise HTTPUnprocessableEntityError('Unprocessable Entity', 'Request body is not valid JSON')


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


def dumpit_utf8(thingy):
    return json.dumps(thingy, ensure_ascii=False).encode('utf8')


def str_2_bool(s):
    return s.lower() in ("true")
