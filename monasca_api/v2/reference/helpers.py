# Copyright 2015 Cray Inc. All Rights Reserved.
# (C) Copyright 2014,2016-2017 Hewlett Packard Enterprise Development LP
# (C) Copyright 2017 SUSE LLC
# Copyright 2018 OP5 AB
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
from oslo_log import log
from oslo_utils import encodeutils
from oslo_utils import timeutils
import six
import six.moves.urllib.parse as urlparse

from monasca_api.common.rest import utils as rest_utils
from monasca_api import conf
from monasca_api.v2.common.exceptions import HTTPUnprocessableEntityError
from monasca_common.validation import metrics as metric_validation


LOG = log.getLogger(__name__)
CONF = conf.CONF


def from_json(req):
    """Read the json_msg from the http request body and return them as JSON.

    :param req: HTTP request object.
    :return: Returns the metrics as a JSON object.
    :raises falcon.HTTPBadRequest:
    """
    try:
        return req.media
    except Exception as ex:
        LOG.exception(ex)
        raise falcon.HTTPBadRequest('Bad request',
                                    'Request body is not valid JSON')


def to_json(data):
    """Converts data to JSON string.

    :param dict data: data to be transformed to JSON
    :return: JSON string
    :rtype: str
    :raises: Exception
    """
    try:
        # NOTE(trebskit) ensure_ascii => UTF-8
        return rest_utils.as_json(data, ensure_ascii=False)
    except Exception as ex:
        LOG.exception(ex)
        raise


def validate_json_content_type(req):
    if req.content_type not in ['application/json']:
        raise falcon.HTTPBadRequest('Bad request', 'Bad content type. Must be '
                                                   'application/json')


def validate_authorization(http_request, authorized_rules_list):
    """Validates whether is authorized according to provided policy rules list.

    If authorization fails, 401 is thrown with appropriate description.
    Additionally response specifies 'WWW-Authenticate' header with 'Token'
    value challenging the client to use different token (the one with
    different set of roles which can access the service).
    """

    challenge = 'Token'
    for rule in authorized_rules_list:
        try:
            http_request.can(rule)
            return
        except Exception as ex:
            LOG.debug(ex)

    raise falcon.HTTPUnauthorized(title='Forbidden',
                                  description='The request does not have access to this service',
                                  challenges=challenge)


def validate_payload_size(content_length):
    """Validates payload size.

    Method validates payload size, this method used req.content_length to determinate
    payload size

        [service]
        max_log_size = 1048576

    **max_log_size** refers to the maximum allowed content length.
    If it is exceeded :py:class:`falcon.HTTPRequestEntityTooLarge` is
    thrown.

    :param  content_length: size of payload

    :exception: :py:class:`falcon.HTTPLengthRequired`
    :exception: :py:class:`falcon.HTTPRequestEntityTooLarge`

    """
    max_size = CONF.log_publisher.max_log_size

    LOG.debug('Payload (content-length) is %s', str(content_length))

    if content_length >= max_size:
        raise falcon.HTTPPayloadTooLarge(
            title='Log payload size exceeded',
            description='Maximum allowed size is %d bytes' % max_size
        )


def get_x_tenant_or_tenant_id(http_request, delegate_authorized_rules_list):
    params = falcon.uri.parse_query_string(http_request.query_string)
    if 'tenant_id' in params:
        tenant_id = params['tenant_id']

        for rule in delegate_authorized_rules_list:
            try:
                http_request.can(rule)
                return tenant_id
            except Exception as ex:
                LOG.debug(ex)

    return http_request.project_id


def get_query_param(req, param_name, required=False, default_val=None):
    try:
        params = falcon.uri.parse_query_string(req.query_string)
        if param_name in params:
            if isinstance(params[param_name], list):
                param_val = encodeutils.safe_decode(params[param_name][0], 'utf8')
            else:
                param_val = encodeutils.safe_decode(params[param_name], 'utf8')

            return param_val
        else:
            if required:
                raise Exception("Missing " + param_name)
            else:
                return default_val
    except Exception as ex:
        LOG.debug(ex)
        raise HTTPUnprocessableEntityError('Unprocessable Entity', str(ex))


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
        raise HTTPUnprocessableEntityError('Unprocessable Entity', str(ex))


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
        if isinstance(dimensions_param, six.string_types):
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
        raise HTTPUnprocessableEntityError('Unprocessable Entity', str(ex))


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
        raise HTTPUnprocessableEntityError('Unprocessable Entity', str(ex))


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
        raise HTTPUnprocessableEntityError('Unprocessable Entity', str(ex))


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
        raise HTTPUnprocessableEntityError('Unprocessable Entity', str(ex))


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
        raise HTTPUnprocessableEntityError('Unprocessable Entity', str(ex))


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
        raise HTTPUnprocessableEntityError('Unprocessable Entity', str(ex))


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
        raise HTTPUnprocessableEntityError('Unprocessable Entity', str(ex))


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
        raise HTTPUnprocessableEntityError('Unprocessable Entity', str(ex))


def paginate(resource, uri, limit):
    parsed_uri = urlparse.urlparse(uri)

    self_link = encodeutils.safe_decode(build_base_uri(parsed_uri), 'utf8')

    old_query_params = _get_old_query_params(parsed_uri)

    if old_query_params:
        self_link += '?' + '&'.join(old_query_params)

    if resource and len(resource) > limit:

        if 'id' in resource[limit - 1]:
            new_offset = resource[limit - 1]['id']

        next_link = encodeutils.safe_decode(build_base_uri(parsed_uri), 'utf8')

        new_query_params = [u'offset' + '=' + urlparse.quote(
            new_offset.encode('utf8'), safe='')]

        _get_old_query_params_except_offset(new_query_params, parsed_uri)

        if new_query_params:
            next_link += '?' + '&'.join(new_query_params)

        resource = {u'links': ([{u'rel': u'self',
                                 u'href': self_link},
                                {u'rel': u'next',
                                 u'href': next_link}]),
                    u'elements': resource[:limit]}

    else:

        resource = {u'links': ([{u'rel': u'self',
                                 u'href': encodeutils.safe_decode(self_link, 'utf-8')}]),
                    u'elements': resource}

    return resource


def paginate_with_no_id(dictionary_list, uri, offset, limit):
    """This method is to paginate a list of dictionaries with no id in it.
       For example, metric name list, directory name list and directory
       value list.
    """
    parsed_uri = urlparse.urlparse(uri)
    self_link = encodeutils.safe_decode(build_base_uri(parsed_uri), 'utf-8')
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
        links = [{u'rel': u'self', u'href': self_link}]
        if len(truncated_list_offset) > limit:
            new_offset = list(truncated_list_offset_limit[limit - 1].values())[0]
            next_link = encodeutils.safe_decode(build_base_uri(parsed_uri), 'utf-8')
            new_query_params = [u'offset' + '=' + new_offset]

            _get_old_query_params_except_offset(new_query_params, parsed_uri)

            if new_query_params:
                next_link += '?' + '&'.join(new_query_params)

            links.append({u'rel': u'next', u'href': next_link})

        resource = {u'links': links,
                    u'elements': truncated_list_offset_limit}
    else:
        resource = {u'links': ([{u'rel': u'self',
                                 u'href': self_link}]),
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
                                 u'href': encodeutils.safe_decode(self_link, 'utf8')},
                                {u'rel': u'next',
                                 u'href': encodeutils.safe_decode(next_link, 'utf8')}]),
                    u'elements': resource[:limit]}

    else:

        resource = {u'links': ([{u'rel': u'self',
                                 u'href': encodeutils.safe_decode(self_link, 'utf8')}]),
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
    self_link = encodeutils.safe_decode(self_link, 'utf-8')

    old_query_params = _get_old_query_params(parsed_uri)

    if old_query_params:
        self_link += '?' + '&'.join(old_query_params)

    if measurements:
        measurement_elements = []
        resource = {u'links': [{u'rel': u'self',
                                u'href': self_link},
                               ]}
        for measurement in measurements:
            if len(measurement['measurements']) >= limit:

                new_offset = ('_').join([measurement['id'],
                                         measurement['measurements'][limit - 1][0]])

                next_link = build_base_uri(parsed_uri)
                next_link = encodeutils.safe_decode(next_link, 'utf-8')

                new_query_params = [u'offset' + '=' + urlparse.quote(
                    new_offset.encode('utf8'), safe='')]

                _get_old_query_params_except_offset(new_query_params, parsed_uri)

                if new_query_params:
                    next_link += '?' + '&'.join(new_query_params)

                resource[u'links'].append({u'rel': u'next',
                                           u'href': next_link})

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
                                 u'href': self_link}]),
                    u'elements': []}

    return resource


def _get_old_query_params(parsed_uri):
    old_query_params = []

    if parsed_uri.query:

        for query_param in parsed_uri.query.split('&'):
            query_param_name, query_param_val = query_param.split('=', 1)

            old_query_params.append(urlparse.quote(
                query_param_name.encode('utf8'), safe='') +
                "=" +
                urlparse.quote(query_param_val.encode('utf8'), safe=''))

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

    self_link = encodeutils.safe_decode(self_link, 'utf-8')

    if statistics:
        statistic_elements = []
        resource = {u'links': [{u'rel': u'self',
                                u'href': self_link}]}

        for statistic in statistics:
            stat_id = statistic['id']
            if len(statistic['statistics']) >= limit:

                # cassadra impl use both id and timestamp to paginate in group by
                if 'end_time' in statistic:
                    new_offset = '_'.join([stat_id, statistic['end_time']])
                    del statistic['end_time']
                else:
                    new_offset = (
                        statistic['statistics'][limit - 1][0])

                next_link = build_base_uri(parsed_uri)

                new_query_params = [u'offset' + '=' + urlparse.quote(
                    new_offset.encode('utf8'), safe='')]

                _get_old_query_params_except_offset(new_query_params, parsed_uri)

                if new_query_params:
                    next_link += '?' + '&'.join(new_query_params)

                next_link = encodeutils.safe_decode(next_link, 'utf-8')
                resource[u'links'].append({u'rel': u'next',
                                          u'href': next_link})

                truncated_statistic = {u'dimensions': statistic['dimensions'],
                                       u'statistics': (statistic['statistics'][:limit]),
                                       u'name': statistic['name'],
                                       u'columns': statistic['columns'],
                                       u'id': statistic['id']}

                statistic_elements.append(truncated_statistic)
                break
            else:
                limit -= len(statistic['statistics'])
                if 'end_time' in statistic:
                    del statistic['end_time']
                statistic_elements.append(statistic)

        resource[u'elements'] = statistic_elements

    else:

        resource = {u'links': ([{u'rel': u'self',
                                 u'href': self_link}]),
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


def str_2_bool(s):
    return s.lower() in ("true")
