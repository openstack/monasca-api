# Copyright 2015 kornicameister@gmail.com
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

import collections

LogApiHeader = collections.namedtuple('LogApiHeader', ['name', 'is_required'])
"""Tuple describing a header."""

X_TENANT_ID = LogApiHeader(name='X-Tenant-Id', is_required=False)
X_ROLES = LogApiHeader(name='X-Roles', is_required=False)
X_APPLICATION_TYPE = LogApiHeader(name='X-Application-Type', is_required=False)
X_DIMENSIONS = LogApiHeader(name='X_Dimensions', is_required=False)
