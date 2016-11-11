# (C) Copyright 2014-2016 Hewlett Packard Enterprise Development LP
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
from voluptuous import All
from voluptuous import Any
from voluptuous import Invalid
from voluptuous import Length
from voluptuous import Marker
from voluptuous import Required
from voluptuous import Schema
from voluptuous import Upper

from monasca_api.v2.common.schemas import exceptions


LOG = log.getLogger(__name__)

MAX_ITEM_LENGTH = 50


def validate_action_list(notification_ids, action_type):
    if not isinstance(notification_ids, list):
        raise Invalid('Not a list: {}'.format(type(notification_ids)))
    existing = []
    for notification_id in notification_ids:
        if not isinstance(notification_id, (str, unicode)):
            raise Invalid('list item <{}> -> {} not one of (str, unicode)'
                          .format(notification_id, type(notification_id)))
        if len(notification_id) > MAX_ITEM_LENGTH:
            raise Invalid('length {} > {}'.format(len(notification_id),
                                                  MAX_ITEM_LENGTH))
        if notification_id in existing:
            raise Invalid('Duplicate {} notification method {}'
                          .format(action_type, notification_id))
        existing.append(notification_id)


def validate_ok_action_list(v):
    validate_action_list(v, 'OK')


def validate_alarm_action_list(v):
    validate_action_list(v, 'ALARM')


def validate_undetermined_action_list(v):
    validate_action_list(v, 'UNDETERMINED')

alarm_definition_schema = {
    Required('name'): All(Any(str, unicode), Length(max=255)),
    Required('expression'): All(Any(str, unicode)),
    Marker('description'): All(Any(str, unicode), Length(max=255)),
    Marker('severity'): All(Upper, Any('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    Marker('match_by'): Any([unicode], [str]),
    Marker('ok_actions'): validate_ok_action_list,
    Marker('alarm_actions'): validate_alarm_action_list,
    Marker('undetermined_actions'): validate_undetermined_action_list,
    Marker('actions_enabled'): bool}


def validate(msg, require_all=False):
    try:
        request_body_schema = Schema(alarm_definition_schema,
                                     required=require_all,
                                     extra=True)
        request_body_schema(msg)
    except Exception as ex:
        LOG.debug(ex)
        raise exceptions.ValidationException(str(ex))
