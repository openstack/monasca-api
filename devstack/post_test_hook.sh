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

(cd $BASE/new/tempest/; sudo virtualenv .venv)
source $BASE/new/tempest/.venv/bin/activate

(cd $BASE/new/tempest/; sudo pip install -r requirements.txt -r test-requirements.txt)
sudo pip install nose
sudo pip install numpy

(cd $BASE/new/tempest/; sudo oslo-config-generator --config-file  tempest/cmd/config-generator.tempest.conf  --output-file etc/tempest.conf)
(cd $BASE/new/; sudo sh -c 'cat monasca-api/devstack/files/tempest/tempest.conf >> tempest/etc/tempest.conf')

sudo cp $BASE/new/tempest/etc/logging.conf.sample $BASE/new/tempest/etc/logging.conf

(cd $BASE/new/monasca-api/; sudo pip install -r requirements.txt -r test-requirements.txt)
(cd $BASE/new/monasca-api/; sudo python setup.py install)

(cd $BASE/new/tempest/; sudo testr init)

(cd $BASE/new/tempest/; sudo sh -c 'testr list-tests monasca_tempest_tests > monasca_tempest_tests')
(cd $BASE/new/tempest/; sudo sh -c 'cat monasca_tempest_tests')
(cd $BASE/new/tempest/; sudo sh -c 'cat monasca_tempest_tests | grep gate > monasca_tempest_tests_gate')
(cd $BASE/new/tempest/; sudo sh -c 'testr run --subunit --load-list=monasca_tempest_tests_gate | subunit-trace --fails')
