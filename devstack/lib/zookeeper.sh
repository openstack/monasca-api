#!/bin/bash

# Copyright 2020 FUJITSU LIMITED
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

_XTRACE_ZOOKEEPER=$(set +o | grep xtrace)
set +o xtrace

function is_zookeeper_enabled {
    is_service_enabled monasca-zookeeper && return 0
    return 1
}

function clean_zookeeper {

    if is_zookeeper_enabled; then
        echo_summary "Cleaning Monasca Zookeeper"

        sudo systemctl disable zookeeper
        sudo systemctl stop zookeeper
        sudo rm -rf /var/log/zookeeper
        sudo rm -rf /var/lib/zookeeper
        sudo rm -rf /opt/zookeeper-${ZOOKEEPER_VERSION}
        sudo rm -rf /opt/zookeeper
        sudo rm -rf /etc/systemd/system/zookeeper.service
        sudo systemctl daemon-reload
    fi
}

function install_zookeeper {

    if is_zookeeper_enabled; then
        echo_summary "Install Monasca Zookeeper"

        local zookeeper_tarball=zookeeper-${ZOOKEEPER_VERSION}.tar.gz
        local zookeeper_tarball_url=${APACHE_ARCHIVES}zookeeper/zookeeper-${ZOOKEEPER_VERSION}/${zookeeper_tarball}
        local zookeeper_tarball_dest
        zookeeper_tarball_dest=`get_extra_file ${zookeeper_tarball_url}`

        sudo groupadd --system zookeeper || true
        sudo useradd --system -g zookeeper zookeeper || true
        sudo tar -xzf ${zookeeper_tarball_dest} -C /opt
        sudo ln -sf /opt/zookeeper-${ZOOKEEPER_VERSION} /opt/zookeeper
        sudo cp $PLUGIN_FILES/zookeeper/* /opt/zookeeper/conf
        sudo chown -R zookeeper:zookeeper /opt/zookeeper/

        sudo mkdir /var/log/zookeeper
        sudo chown -R zookeeper:zookeeper /var/log/zookeeper

        sudo mkdir /var/lib/zookeeper
        sudo chown -R zookeeper:zookeeper /var/lib/zookeeper

        sudo cp -f "${MONASCA_API_DIR}"/devstack/files/zookeeper/zookeeper.service /etc/systemd/system/zookeeper.service
        sudo chown root:root /etc/systemd/system/kafka.service
        sudo chmod 644 /etc/systemd/system/zookeeper.service

        sudo systemctl daemon-reload
        sudo systemctl enable zookeeper
        sudo systemctl start zookeeper || sudo systemctl restart zookeeper
    fi
}

$_XTRACE_ZOOKEEPER
