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
import six.moves.urllib.parse as urlparse
from validate_email import validate_email
import voluptuous

from monasca_api.v2.common.schemas import exceptions

LOG = log.getLogger(__name__)

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
        raise exceptions.ValidationException("Address {} is not of correct format".format(address))


def _validate_url(address):
    try:
        parsed = urlparse.urlparse(address)
    except Exception:
        raise exceptions.ValidationException("Address {} is not of correct format".format(address))

    if not parsed.scheme:
        raise exceptions.ValidationException("Address {} does not have URL scheme".format(address))
    if not parsed.netloc:
        raise exceptions.ValidationException("Address {} does not have network location"
                                             .format(address))
    if parsed.scheme not in schemes:
        raise exceptions.ValidationException("Address {} scheme is not in {}"
                                             .format(address, schemes))
