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
# - is_zookeeper_enabled
# - install_zookeeper
# - configure_zookeeper
# - clean_zookeeper

_XTRACE_ZOOKEEPER=$(set +o | grep xtrace)
set +o xtrace

# Set up default directories
ZOOKEEPER_DATA_DIR=$DEST/data/zookeeper
ZOOKEEPER_CONF_DIR=/etc/zookeeper

function is_zookeeper_enabled {
    is_service_enabled monasca-zookeeper && return 0
    return 1
}

function clean_zookeeper {
    sudo rm -rf $ZOOKEEPER_DATA_DIR
    apt_get -y purge zookeeper
}

function configure_zookeeper {
    if is_zookeeper_enabled; then
        sudo cp $PLUGIN_FILES/zookeeper/* $ZOOKEEPER_CONF_DIR
        sudo sed -i -e 's|.*dataDir.*|dataDir='$ZOOKEEPER_DATA_DIR'|' $ZOOKEEPER_CONF_DIR/zoo.cfg
        sudo rm -rf $ZOOKEEPER_DATA_DIR || true
        sudo mkdir -p $ZOOKEEPER_DATA_DIR || true
        restart_service zookeeper
    fi
}

function install_zookeeper {
    if is_zookeeper_enabled; then
        if is_ubuntu; then
            install_package zookeeperd
        else
            die $LINENO "Don't know how to install zookeeper on this platform"
        fi
    fi

    # NOTE(trebskit) it shouldn't really be done here
    # but monasca devstack cannot allow it do be done properly
    # we'd have to first refactor parts where services are:
    # installed, configured and started in single phase
    configure_zookeeper
}

$_XTRACE_ZOOKEEPER
