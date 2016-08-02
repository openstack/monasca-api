#
# (C) Copyright 2015 Hewlett Packard Enterprise Development Company LP
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Environment variables for use with python-monascaclient running via monasca-vagrant

. /opt/monasca/bin/activate
. /usr/local/share/monasca.bash_completion
export OS_USERNAME=mini-mon
export OS_PASSWORD=password
export OS_USER_DOMAIN_NAME=Default
export OS_PROJECT_NAME=mini-mon
export OS_PROJECT_DOMAIN_NAME=Default
export OS_AUTH_URL=http://127.0.0.1:35357/v3/
