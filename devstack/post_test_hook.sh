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

source $BASE/new/devstack/stackrc
CONSTRAINTS="-c $REQUIREMENTS_DIR/upper-constraints.txt"
REQS="$CONSTRAINTS -r requirements.txt -r test-requirements.txt"

(cd $BASE/new/tempest/; sudo -EH pip install $REQS)

(cd $BASE/new/tempest/; sudo -EH oslo-config-generator --config-file  tempest/cmd/config-generator.tempest.conf  --output-file etc/tempest.conf)
(cd $BASE/new/; sudo -EH sh -c 'cat monasca-api/devstack/files/tempest/tempest.conf >> tempest/etc/tempest.conf')

sudo cp $BASE/new/tempest/etc/logging.conf.sample $BASE/new/tempest/etc/logging.conf

(cd $BASE/new/monasca-api/; sudo -EH pip install $REQS)
(cd $BASE/new/monasca-api/; sudo -EH python setup.py install)

(cd $BASE/new/tempest/; sudo -EH testr init)

(cd $BASE/new/tempest/; sudo -EH sh -c 'testr list-tests monasca_tempest_tests > monasca_tempest_tests')
(cd $BASE/new/tempest/; sudo -EH sh -c 'cat monasca_tempest_tests')
(cd $BASE/new/tempest/; sudo -EH sh -c 'cat monasca_tempest_tests | grep gate > monasca_tempest_tests_gate')
(cd $BASE/new/tempest/; sudo -EH sh -c 'testr run --subunit --load-list=monasca_tempest_tests_gate | subunit-trace --fails')
