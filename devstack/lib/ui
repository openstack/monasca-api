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

_XTRACE_DASHBOARD=$(set +o | grep xtrace)
set +o xtrace

function is_ui_enabled {
    is_service_enabled horizon && return 0
    return 1
}

function clean_ui {
    if is_ui_enabled; then
        rm -rf "${HORIZON_DIR}/monitoring" \
            "${HORIZON_DIR}/openstack_dashboard/local/enabled/_50_admin_add_monitoring_panel.py" \
            "${HORIZON_DIR}/openstack_dashboard/conf/monitoring_policy.json"
    fi
}

function configure_ui {
    if is_ui_enabled; then
        _link_ui_files

        sed -e "
            s#getattr(settings, 'GRAFANA_URL', None)#{'RegionOne': \"http:\/\/${SERVICE_HOST}:3000\", }#g;
        " -i "${MONASCA_BASE}"/monasca-ui/monitoring/config/local_settings.py

        DJANGO_SETTINGS_MODULE=openstack_dashboard.settings python "${MONASCA_BASE}"/horizon/manage.py collectstatic --noinput
        DJANGO_SETTINGS_MODULE=openstack_dashboard.settings python "${MONASCA_BASE}"/horizon/manage.py compress --force

        restart_service apache2 || true
    fi
}

function install_ui {
    if is_ui_enabled; then
        git_clone $MONASCA_UI_REPO $MONASCA_UI_DIR $MONASCA_UI_BRANCH
        git_clone $MONASCA_CLIENT_REPO $MONASCA_CLIENT_DIR $MONASCA_CLIENT_BRANCH

        setup_develop $MONASCA_UI_DIR
        setup_dev_lib "python-monascaclient"

    fi
}

function _link_ui_files {
    ln -f "${MONASCA_UI_DIR}/monitoring/enabled/_50_admin_add_monitoring_panel.py" \
        "${HORIZON_DIR}/openstack_dashboard/local/enabled/_50_admin_add_monitoring_panel.py"
    ln -f "${MONASCA_UI_DIR}/monitoring/conf/monitoring_policy.json" \
        "${HORIZON_DIR}/openstack_dashboard/conf/monitoring_policy.json"
    ln -sfF "${MONASCA_UI_DIR}"/monitoring "${HORIZON_DIR}/monitoring"
}

$_XTRACE_DASHBOARD
