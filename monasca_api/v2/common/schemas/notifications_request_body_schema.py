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

from oslo_log import log
import sys
from validate_email import validate_email
import voluptuous

from monasca_api.v2.common.schemas import exceptions

if sys.version_info >= (3,):
    import urllib.parse as urlparse
else:
    import urlparse

LOG = log.getLogger(__name__)
ADDRESS_ERR_MESSAGE = "Address {} is not of correct format"

schemes = ['http', 'https']

notification_schema = {
    voluptuous.Required('name'): voluptuous.Schema(
        voluptuous.All(voluptuous.Any(str, unicode),
                       voluptuous.Length(max=250))),
    voluptuous.Required('type'): voluptuous.Schema(
        voluptuous.Any("EMAIL", "email", "WEBHOOK", "webhook",
                       "PAGERDUTY", "pagerduty")),
    voluptuous.Required('address'): voluptuous.Schema(
        voluptuous.All(voluptuous.Any(str, unicode),
                       voluptuous.Length(max=512)))}

request_body_schema = voluptuous.Schema(voluptuous.Any(notification_schema))


def validate(msg):
    try:
        request_body_schema(msg)
    except Exception as ex:
        LOG.debug(ex)
        raise exceptions.ValidationException(str(ex))

    notification_type = str(msg['type']).upper()
    if notification_type == 'EMAIL':
        _validate_email(msg['address'])
    elif notification_type == 'WEBHOOK':
        _validate_url(msg['address'])


def _validate_email(address):
    if not validate_email(address):
        raise exceptions.ValidationException(ADDRESS_ERR_MESSAGE.format(address))


def _validate_url(address):
    try:
        parsed = urlparse.urlparse(address)
    except:
        raise exceptions.ValidationException(ADDRESS_ERR_MESSAGE.format(address))

    if not parsed.scheme or not parsed.netloc:
        raise exceptions.ValidationException(ADDRESS_ERR_MESSAGE.format(address))
    if parsed.scheme not in schemes:
        raise exceptions.ValidationException(ADDRESS_ERR_MESSAGE.format(address))
