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

_XTRACE_MON_PROFILE=$(set +o | grep xtrace)
set +o xtrace

function install_monasca_profile {

    echo_summary "Install Monasca Bash Profile"

    touch /tmp/monasca_cli.sh
    cat > /tmp/monasca_cli.sh << EOF
# signalize we're in shape to use monasca here
export PS1='[\u@\h \W(monasca)]\$ '
# set monasca client bash_completion
source ${MONASCA_COMPLETION_FILE}
# set OS_* variables
source $TOP_DIR/openrc mini-mon mini-mon
# override password for mini-mon (guy is not using SERVICE_PASSWORD)
export OS_PASSWORD=password
EOF

    if [[ "${MONASCA_METRICS_DB,,}" == 'cassandra' ]]; then
        cat >> /tmp/monasca_cli.sh << EOF
# allow to use cassandra cli
export CQLSH_NO_BUNDLED=true
export CQLSH_HOST=${SERVICE_HOST}
EOF
    fi

    sudo install -D -m 0644 -o ${STACK_USER} \
        /tmp/monasca_cli.sh ${MONASCA_PROFILE_FILE}
    rm /tmp/monasca_cli.sh
}

function clean_monasca_profile {
    echo_summary "Clean Monasca CLI Creds"
    sudo rm -f ${MONASCA_PROFILE_FILE}
}

${_XTRACE_DASHBOARD}
