# Copyright 2015 FUJITSU LIMITED
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

import simplejson as json
import six

from monasca_api.common.rest import exceptions

ENCODING = 'utf8'

TEXT_CONTENT_TYPE = 'text/plain'
JSON_CONTENT_TYPE = 'application/json'


def _try_catch(fun):

    @six.wraps(fun)
    def wrapper(*args, **kwargs):
        try:
            return fun(*args, **kwargs)
        except Exception as ex:
            raise exceptions.DataConversionException(str(ex))

    return wrapper


@_try_catch
def as_json(data, **kwargs):
    """Writes data as json.

    :param dict data: data to convert to json
    :param kwargs kwargs: kwargs for json dumps
    :return: json string
    :rtype: str
    """

    if 'sort_keys' not in kwargs:
        kwargs['sort_keys'] = False
    if 'ensure_ascii' not in kwargs:
        kwargs['ensure_ascii'] = False

    data = json.dumps(data, **kwargs)

    return data


@_try_catch
def from_json(data, **kwargs):
    """Reads data from json str.

    :param str data: data to read
    :param kwargs kwargs: kwargs for json loads
    :return: read data
    :rtype: dict
    """
    return json.loads(data, **kwargs)


_READABLE_CONTENT_TYPES = {
    TEXT_CONTENT_TYPE: lambda content: content,
    JSON_CONTENT_TYPE: from_json
}


def read_body(payload, content_type=JSON_CONTENT_TYPE):
    """Reads HTTP payload according to given content_type.

    Function is capable of reading from payload stream.
    Read data is then processed according to content_type.

    Note:
        Content-Type is validated. It means that if read_body
        body is not capable of reading data in requested type,
        it will throw an exception.

    If read data was empty method will return false boolean
    value to indicate that.

    Note:
        There is no transformation if content type is equal to
        'text/plain'. What has been read is returned.

    :param stream payload: payload to read, payload should have read method
    :param str content_type: payload content type, default to application/json
    :return: read data, returned type depends on content_type or False
             if empty

    :exception: :py:class:`.UnreadableBody` - in case of any failure when
                                              reading data

    """
    if content_type not in _READABLE_CONTENT_TYPES:
        msg = ('Cannot read %s, not in %s' %
               (content_type, _READABLE_CONTENT_TYPES))
        raise exceptions.UnsupportedContentTypeException(msg)

    try:
        content = payload.read()
        if not content:
            return None
    except Exception as ex:
        raise exceptions.UnreadableContentError(str(ex))

    return _READABLE_CONTENT_TYPES[content_type](content)
