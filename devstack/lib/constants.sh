#!/bin/bash

# Copyright 2017 FUJITSU LIMITED
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

_XTRACE_MON_CONST=$(set +o | grep xtrace)
set +o xtrace

# Location of python-monascaclient completion file
MONASCA_COMPLETION_FILE=/etc/bash_completion.d/monasca.bash_completion

# Location of monasca-profile
MONASCA_PROFILE_FILE=/etc/profile.d/monasca.sh

# monasca_service_type, used in:
# keystone endpoint creation
# configuration files
MONASCA_SERVICE_TYPE=monitoring

${_XTRACE_MON_CONST}
