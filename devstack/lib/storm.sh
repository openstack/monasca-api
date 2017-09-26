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

# call_order:
# - is_storm_enabled
# - install_storm
# - configure_storm
# - clean_storm

_XTRACE_STORM=$(set +o | grep xtrace)
set +o xtrace

STORM_USER="storm"
STORM_GROUP="storm"

STORM_DIR="/opt/storm"
STORM_CURRENT_DIR="${STORM_DIR}/current"
STORM_BIN="${STORM_CURRENT_DIR}/bin/storm"
STORM_WORK_DIR="/var/storm"
STORM_LOG_DIR="/var/log/storm"

STORM_TARBALL="apache-storm-${STORM_VERSION}.tar.gz"
STORM_TARBALL_DEST="${FILES}/${STORM_TARBALL}"

STORM_NIMBUS_CMD="${STORM_BIN} nimbus"
STORM_SUPERVISOR_CMD="${STORM_BIN} supervisor"
STORM_UI_CMD="${STORM_BIN} ui"
STORM_LOGVIEWER_CMD="${STORM_BIN} logviewer"

function is_storm_enabled {
    [[ ,${ENABLED_SERVICES} =~ ,"monasca-storm" ]] && return 0
    return 1
}

function start_storm {
    if is_storm_enabled; then
        echo_summary "Starting storm"

        run_process "monasca-storm-nimbus" "${STORM_NIMBUS_CMD}" "${STORM_GROUP}" "${STORM_USER}"
        run_process "monasca-storm-supervisor" "${STORM_SUPERVISOR_CMD}" "${STORM_GROUP}" "${STORM_USER}"
        run_process "monasca-storm-ui" "${STORM_UI_CMD}" "${STORM_GROUP}" "${STORM_USER}"
        run_process "monasca-storm-logviewer" "${STORM_LOGVIEWER_CMD}" "${STORM_GROUP}" "${STORM_USER}"
    fi
}

function stop_storm {
    if is_storm_enabled; then
        echo_summary "Stopping storm"

        stop_process "monasca-storm-nimbus"
        stop_process "monasca-storm-supervisor"
        stop_process "monasca-storm-ui"
        stop_process "monasca-storm-logviewer"
    fi
}

function clean_storm {
    if is_storm_enabled; then
        echo_summary "Cleaning storm"

        sudo unlink "${DEST}/logs/storm-workers" || true
        sudo unlink "${STORM_CURRENT_DIR}/logs"|| true
        sudo unlink "${STORM_CURRENT_DIR}"|| true

        sudo rm -rf "${DEST}/logs/storm-workers" || true
        sudo rm -rf "${STORM_CURRENT_DIR}"|| true
        sudo rm -rf "${STORM_DIR}" || true
        sudo rm -rf "${STORM_WORK_DIR}" || true
        sudo rm -rf "${STORM_LOG_DIR}" || true

        sudo userdel "${STORM_USER}" || true
        sudo groupdel "${STORM_GROUP}" || true
    fi
}

function configure_storm {
    if is_storm_enabled; then
        echo_summary "Configuring storm"
        sudo cp -f "${MONASCA_API_DIR}"/devstack/files/storm.yaml "${STORM_CURRENT_DIR}/conf/storm.yaml"
        sudo chown "${STORM_USER}":"${STORM_GROUP}" "${STORM_CURRENT_DIR}/conf/storm.yaml"
        sudo chmod 0644 "${STORM_CURRENT_DIR}/conf/storm.yaml"

        sudo sed -e "
            s|%STORM_UI_HOST%|${STORM_UI_HOST}|g;
            s|%STORM_UI_PORT%|${STORM_UI_PORT}|g;
            s|%STORM_LOGVIEWER_PORT%|${STORM_LOGVIEWER_PORT}|g;
        " -i "${STORM_CURRENT_DIR}/conf/storm.yaml"

    fi
}

function install_storm {
    if is_storm_enabled; then
        echo_summary "Installing storm"
        _download_storm
        _setup_user_group
        _create_directories
        _install_storm
    fi
}

function post_storm {
    if is_storm_enabled; then
        echo "Post configuring storm"
        # if inside the gate, make the visible there too
        if [ -n "${LOGDIR}" ]; then
            sudo ln -sfd "${STORM_LOG_DIR}/workers-artifacts" "${LOGDIR}/storm-workers"
        fi
    fi
}

# helpers

function _download_storm {
    local storm_tarball_url="${APACHE_ARCHIVES}storm/apache-storm-${STORM_VERSION}/${STORM_TARBALL}"
    local storm_dest

    storm_dest=`get_extra_file ${storm_tarball_url}`

    if [ "${storm_dest}" != "${STORM_TARBALL_DEST}" ]; then
        mv -f "${storm_dest}" "${STORM_TARBALL_DEST}"
    fi
}

function _setup_user_group {
    sudo groupadd --system "${STORM_GROUP}" || true
    sudo useradd --system -g "${STORM_GROUP}" "${STORM_USER}" || true
}

function _install_storm {
    # unpack (i.e. install) downloaded tarball
    sudo tar -xzf ${STORM_TARBALL_DEST} -C "${STORM_DIR}"

    # link the versioned folder to more suitable one
    sudo ln -sfd "${STORM_DIR}/apache-storm-${STORM_VERSION}" "${STORM_CURRENT_DIR}"

    # make them visible in standard location
    sudo ln -sfd "${STORM_LOG_DIR}" "${STORM_CURRENT_DIR}/logs"
}

function _create_directories {
    for dir in "${STORM_DIR}" "${STORM_WORK_DIR}" "${STORM_LOG_DIR}"; do
        if [ ! -d "${dir}" ]; then
            sudo mkdir -p "${dir}" || true
        fi
        sudo chown "${STORM_USER}":"${STORM_GROUP}" "${dir}"
        sudo chmod 0775 "${dir}"
    done
}

# helpers

$_XTRACE_STORM
