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

sudo $BASE/new/tempest/virtualenv .venv
source $BASE/new/tempest/.venv/bin/activate

(cd $BASE/new/tempest/; sudo pip install -r requirements.txt -r test-requirements.txt)
sudo pip install nose

(cd $BASE/new/tempest/; oslo-config-generator --config-file  etc/config-generator.tempest.conf  --output-file etc/tempest.conf)
cat $BASE/new/monasca-api/devstack/files/tempest/tempest.conf >> $BASE/new/tempest/etc/tempest.conf

cp $BASE/new/tempest/etc/logging.conf.sample $BASE/new/tempest/etc/logging.conf

(cd $BASE/new/monasca-api/; pip install -r requirements.txt -r test-requirements.txt)
(cd $BASE/new/monasca-api/; python setup.py install)

(cd $BASE/new/tempest/; ostestr --serial --regex monasca_tempest_tests)