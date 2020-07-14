#!/bin/bash

# Copyright 2017 FUJITSU LIMITED
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless reqmonasca_notificationred by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

_XTRACE_MON_NOTIFICATION=$(set +o | grep xtrace)
set +o xtrace

MONASCA_NOTIFICATION_CONF_DIR=${MONASCA_NOTIFICATION_CONF_DIR:-/etc/monasca}
MONASCA_NOTIFICATION_LOG_DIR=${MONASCA_NOTIFICATION_LOG_DIR:-/var/log/monasca/notification}
MONASCA_NOTIFICATION_CONF=${MONASCA_NOTIFICATION_CONF:-$MONASCA_NOTIFICATION_CONF_DIR/monasca-notification.conf}
MONASCA_NOTIFICATION_GATE_CFG_LINK=/etc/monasca-notification

if [[ ${USE_VENV} = True ]]; then
    PROJECT_VENV["monasca-notification"]=${MONASCA_NOTIFICATION_DIR}.venv
    MONASCA_NOTIFICATION_BIN_DIR=${PROJECT_VENV["monasca-notification"]}/bin
else
    MONASCA_NOTIFICATION_BIN_DIR=$(get_python_exec_prefix)
fi

is_monasca_notification_enabled() {
    is_service_enabled monasca-notification && return 0
    return 1
}

# NOTE(trebskit) ref: stack_install_service from devstack
install_monasca-notification() {
    if ! is_monasca_notification_enabled; then
        return
    fi
    echo_summary "Installing monasca-notification"

    git_clone ${MONASCA_NOTIFICATION_REPO} ${MONASCA_NOTIFICATION_DIR} \
        ${MONASCA_NOTIFICATION_BRANCH}
    if python3_enabled; then
        enable_python3_package monasca-notification
    fi
    setup_develop ${MONASCA_NOTIFICATION_DIR} jira_plugin
    # see devstack/plugin.sh
    install_monasca_common
    install_monasca_statsd
    # see devstack/plugin.sh

    if is_service_enabled postgresql; then
        apt_get -y install libpq-dev
        pip_install_gr psycopg2
    elif is_service_enabled mysql; then
        apt_get -y install python-mysqldb libmysqlclient-dev
        pip_install_gr PyMySQL
    fi

    if [[ ${MONASCA_DATABASE_USE_ORM} == "True" ]]; then
        pip_install_gr sqlalchemy
    fi
}

configure_monasca-notification() {
    if ! is_monasca_notification_enabled; then
        return
    fi

    echo_summary "Configuring monasca-notification"

    sudo install -d -o $STACK_USER ${MONASCA_NOTIFICATION_CONF_DIR}
    sudo install -d -o $STACK_USER ${MONASCA_NOTIFICATION_LOG_DIR}

    local dbDriver
    local dbEngine
    local dbPort
    if is_service_enabled postgresql; then
        dbDriver="monasca_notification.common.repositories.postgres.pgsql_repo:PostgresqlRepo"
        dbEngine="postgres"
        dbPort=5432
    else
        dbDriver="monasca_notification.common.repositories.mysql.mysql_repo:MysqlRepo"
        dbEngine="mysql"
        dbPort=3306
    fi
    if [[ ${MONASCA_DATABASE_USE_ORM} == "True" ]]; then
        dbDriver="monasca_notification.common.repositories.orm.orm_repo:OrmRepo"
    fi

    iniset "${MONASCA_NOTIFICATION_CONF}" kafka url ${SERVICE_HOST}:9092
    iniset "${MONASCA_NOTIFICATION_CONF}" database repo_driver ${dbDriver}
    iniset "${MONASCA_NOTIFICATION_CONF}" email_notifier grafana_url ${SERVICE_HOST}:3000
    iniset "${MONASCA_NOTIFICATION_CONF}" keystone auth_url ${SERVICE_HOST}/identity/v3
    if is_service_enabled postgresql; then
        iniset "${MONASCA_NOTIFICATION_CONF}" postgresql host ${DATABASE_HOST}
        iniset "${MONASCA_NOTIFICATION_CONF}" postgresql port ${dbPort}
        iniset "${MONASCA_NOTIFICATION_CONF}" postgresql user ${DATABASE_USER}
        iniset "${MONASCA_NOTIFICATION_CONF}" postgresql passwd ${DATABASE_PASSWORD}
        iniset "${MONASCA_NOTIFICATION_CONF}" postgresql db mon
    else
        iniset "${MONASCA_NOTIFICATION_CONF}" mysql host ${DATABASE_HOST}
        iniset "${MONASCA_NOTIFICATION_CONF}" mysql user ${DATABASE_USER}
        iniset "${MONASCA_NOTIFICATION_CONF}" mysql passwd ${DATABASE_PASSWORD}
        iniset "${MONASCA_NOTIFICATION_CONF}" mysql db mon
    fi
    if [[ ${MONASCA_DATABASE_USE_ORM} == "True" ]]; then
        iniset "${MONASCA_NOTIFICATION_CONF}" orm url ${dbEngine}://${DATABASE_USER}:${DATABASE_PASSWORD}%${DATABASE_HOST}:${dbPort}/mon
    fi

    sudo install -d -o ${STACK_USER} ${MONASCA_NOTIFICATION_GATE_CFG_LINK}
    ln -sf ${MONASCA_NOTIFICATION_CONF} ${MONASCA_NOTIFICATION_GATE_CFG_LINK}

    echo "postfix postfix/mailname string localhost" | sudo debconf-set-selections -v
    echo "postfix postfix/main_mailer_type string 'Local only'" | sudo debconf-set-selections -v

}

start_monasca-notification(){
    if is_monasca_notification_enabled; then
        echo_summary "Starting monasca-notification"
        run_process "monasca-notification" "$MONASCA_NOTIFICATION_BIN_DIR/monasca-notification"
    fi
}

stop_monasca-notification(){
    if is_monasca_notification_enabled; then
        echo_summary "Stopping monasca-notification"
        stop_process "monasca-notification" || true
    fi
}

clean_monasca-notification() {
    if ! is_monasca_notification_enabled; then
        return
    fi

    echo_summary "Configuring monasca-notification"

    sudo rm -rf ${MONASCA_NOTIFICATION_CONF} ${MONASCA_NOTIFICATION_CONF_DIR} \
        ${MONASCA_NOTIFICATION_LOG_DIR} \
        ${MONASCA_NOTIFICATION_GATE_CFG_LINK}

    if is_service_enabled postgresql; then
        apt_get -y purge libpq-dev
    elif is_service_enabled mysql; then
        apt_get -y purge libmysqlclient-dev
        apt_get -y purge python-mysqldb
    fi
}

${_XTRACE_MON_NOTIFICATION}
