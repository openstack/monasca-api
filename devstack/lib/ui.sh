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

        cp $MONASCA_UI_DIR/monitoring/config/local_settings.py \
            $HORIZON_DIR/openstack_dashboard/local/local_settings.d/_50_monasca_ui_settings.py

        local localSettings=$HORIZON_DIR/openstack_dashboard/local/local_settings.d/_50_monasca_ui_settings.py

        sed -e "
            s#getattr(settings, 'GRAFANA_URL', None)#{'RegionOne': \"http:\/\/${SERVICE_HOST}:3000\", }#g;
        " -i ${localSettings}

        if is_service_enabled horizon && is_service_enabled kibana && is_service_enabled monasca-log; then
            echo_summary "Configure Horizon with Kibana access"
            sudo sed -e "
                s|KIBANA_HOST = getattr(settings, 'KIBANA_HOST', 'http://192.168.10.6:5601/')|KIBANA_HOST = getattr(settings, 'KIBANA_HOST', 'http://${KIBANA_SERVICE_HOST}:${KIBANA_SERVICE_PORT}/')|g;
            " -i ${localSettings}

            sudo sed -e "
                s|'ENABLE_LOG_MANAGEMENT_BUTTON', False|'ENABLE_LOG_MANAGEMENT_BUTTON', True|g;
            " -i ${localSettings}
        fi
        if python3_enabled; then
            DJANGO_SETTINGS_MODULE=openstack_dashboard.settings python3 "${MONASCA_BASE}"/horizon/manage.py collectstatic --noinput
            DJANGO_SETTINGS_MODULE=openstack_dashboard.settings python3 "${MONASCA_BASE}"/horizon/manage.py compress --force
        else
            DJANGO_SETTINGS_MODULE=openstack_dashboard.settings python "${MONASCA_BASE}"/horizon/manage.py collectstatic --noinput
            DJANGO_SETTINGS_MODULE=openstack_dashboard.settings python "${MONASCA_BASE}"/horizon/manage.py compress --force
        fi
        restart_service apache2 || true
    fi
}

function install_ui {
    if is_ui_enabled; then
        git_clone $MONASCA_UI_REPO $MONASCA_UI_DIR $MONASCA_UI_BRANCH
        git_clone $MONASCA_CLIENT_REPO $MONASCA_CLIENT_DIR $MONASCA_CLIENT_BRANCH
        if python3_enabled; then
            enable_python3_package monasca-ui
        fi

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
