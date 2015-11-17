# Copyright 2014, 2015 Hewlett-Packard
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


def list_item_length(v):
    if not isinstance(v, list):
        raise Invalid('Not a list: {}'.format(type(v)))
    for i in v:
        if not isinstance(i, (str, unicode)):
            raise Invalid('list item <{}> -> {} not one of (str, unicode)'
                          .format(i, type(i)))
        if len(i) > MAX_ITEM_LENGTH:
            raise Invalid('length {} > {}'.format(len(i), MAX_ITEM_LENGTH))


alarm_definition_schema = {
    Required('name'): All(Any(str, unicode), Length(max=255)),
    Required('expression'): All(Any(str, unicode)),
    Marker('description'): All(Any(str, unicode), Length(max=255)),
    Marker('severity'): All(Upper, Any('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    Marker('match_by'): Any([unicode], [str]),
    Marker('ok_actions'): list_item_length,
    Marker('alarm_actions'): list_item_length,
    Marker('undetermined_actions'): list_item_length,
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
