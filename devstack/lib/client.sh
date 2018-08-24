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

_XTRACE_MON_CLIENT=$(set +o | grep xtrace)
set +o xtrace

install_monascaclient() {
    if python3_enabled; then
        enable_python3_package python-monascaclient
    fi
    git_clone $MONASCA_CLIENT_REPO $MONASCA_CLIENT_DIR $MONASCA_CLIENT_BRANCH
    setup_dev_lib "python-monascaclient"

    # install completion file
    monasca complete > /tmp/monasca.bash_completion
    sudo install -D -m 0644 -o $STACK_USER /tmp/monasca.bash_completion $MONASCA_COMPLETION_FILE
    rm -rf /tmp/monasca.bash_completion
}

clean_monascaclient() {
    sudo rm -rf $MONASCA_COMPLETION_FILE
}

${_XTRACE_MON_CLIENT}
