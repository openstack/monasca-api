#!/bin/bash

# Copyright 2017 FUJITSU LIMITED
# (C) Copyright 2017 SUSE LLC

# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless reqmonasca_PERSISTERred by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

_XTRACE_MON_PERSISTER=$(set +o | grep xtrace)
set +o xtrace

MONASCA_PERSISTER_IMPLEMENTATION_LANG=${MONASCA_PERSISTER_IMPLEMENTATION_LANG:-python}

MONASCA_PERSISTER_CONF_DIR=${MONASCA_PERSISTER_CONF_DIR:-/etc/monasca}
MONASCA_PERSISTER_LOG_DIR=${MONASCA_PERSISTER_LOG_DIR:-/var/log/monasca/persister}
MONASCA_PERSISTER_GATE_CONFIG=/etc/monasca-persister

if [ "$MONASCA_PERSISTER_IMPLEMENTATION_LANG" == "python" ]; then
    if [[ ${USE_VENV} = True ]]; then
        PROJECT_VENV["monasca-persister"]=${MONASCA_PERSISTER_DIR}.venv
        MONASCA_PERSISTER_BIN_DIR=${PROJECT_VENV["monasca-persister"]}/bin
    else
        MONASCA_PERSISTER_BIN_DIR=$(get_python_exec_prefix)
    fi
    MONASCA_PERSISTER_CONF=${MONASCA_PERSISTER_CONF:-$MONASCA_PERSISTER_CONF_DIR/monasca-persister.conf}
    MONASCA_PERSISTER_LOGGING_CONF=${MONASCA_PERSISTER_LOGGING_CONF:-$MONASCA_PERSISTER_CONF_DIR/persister-logging.conf}

    M_REPO_DRIVER_BASE=monasca_persister.repositories.${MONASCA_METRICS_DB}.metrics_repository
    M_REPO_DRIVER_INFLUX=$M_REPO_DRIVER_BASE:MetricInfluxdbRepository
    M_REPO_DRIVER_CASSANDRA=$M_REPO_DRIVER_BASE:MetricCassandraRepository

    AH_REPO_DRIVER_BASE=monasca_persister.repositories.${MONASCA_METRICS_DB}.alarm_state_history_repository
    AH_REPO_DRIVER_INFLUX=$AH_REPO_DRIVER_BASE:AlarmStateHistInfluxdbRepository
    AH_REPO_DRIVER_CASSANDRA=$AH_REPO_DRIVER_BASE:AlarmStateHistCassandraRepository

    MONASCA_PERSISTER_CMD="$MONASCA_PERSISTER_BIN_DIR/monasca-persister --config-file=$MONASCA_PERSISTER_CONF"
else
    MONASCA_PERSISTER_APP_PORT=${MONASCA_PERSISTER_APP_PORT:-8090}
    MONASCA_PERSISTER_ADMIN_PORT=${MONASCA_PERSISTER_ADMIN_PORT:-8091}
    MONASCA_PERSISTER_BIND_HOST=${MONASCA_PERSISTER_BIND_HOST:-$SERVICE_HOST}

    MONASCA_PERSISTER_CONF=${MONASCA_PERSISTER_CONF:-$MONASCA_PERSISTER_CONF_DIR/persister.yml}
    MONASCA_PERSISTER_JAVA_OPTS="-Dfile.encoding=UTF-8 -Xmx128m"
    MONASCA_PERSISTER_JAR="/opt/monasca/monasca-persister.jar"
    MONASCA_PERSISTER_CMD="/usr/bin/java ${MONASCA_PERSISTER_JAVA_OPTS} -cp ${MONASCA_PERSISTER_JAR} monasca.persister.PersisterApplication server ${MONASCA_PERSISTER_CONF}"
fi

if [[ "${MONASCA_METRICS_DB,,}" == 'cassandra' ]]; then
    MONASCA_PERSISTER_BATCH_SIZE=100
    MONASCA_PERSISTER_MAX_BATCH_TIME=10
    MONASCA_PERSISTER_METRIC_THREADS=2
    MONASCA_PERSISTER_COMMIT_BATCH_TIME=10000
else
    MONASCA_PERSISTER_BATCH_SIZE=100
    MONASCA_PERSISTER_MAX_BATCH_TIME=15
    MONASCA_PERSISTER_METRIC_THREADS=10
    MONASCA_PERSISTER_COMMIT_BATCH_TIME=0
fi

is_monasca_persister_enabled() {
    is_service_enabled monasca-persister && return 0
    return 1
}

# common
pre_monasca-persister() {
    if ! is_monasca_persister_enabled; then
        return
    fi
    sudo install -d -o ${STACK_USER} ${MONASCA_PERSISTER_GATE_CONFIG}
}

install_monasca-persister() {
    echo_summary "Installing monasca-persister"

    git_clone ${MONASCA_PERSISTER_REPO} ${MONASCA_PERSISTER_DIR} \
        ${MONASCA_PERSISTER_BRANCH}

    install_monasca_persister_$MONASCA_PERSISTER_IMPLEMENTATION_LANG
}
configure_monasca-persister() {
    if ! is_monasca_persister_enabled; then
        return
    fi

    echo_summary "Configuring monasca-persister"

    sudo install -d -o $STACK_USER ${MONASCA_PERSISTER_CONF_DIR}
    sudo install -d -o $STACK_USER ${MONASCA_PERSISTER_LOG_DIR}

    configure_monasca_persister_$MONASCA_PERSISTER_IMPLEMENTATION_LANG
}
start_monasca-persister() {
    if ! is_monasca_persister_enabled; then
        return
    fi

    local cmd=${MONASCA_PERSISTER_CMD}
    local systemd_service="devstack@monasca-persister.service"
    local unitfile="$SYSTEMD_DIR/$systemd_service"

    # sanity check the command
    _common_systemd_pitfalls "$cmd"

    # Restart monasca-persister when exited with error code
    iniset -sudo $unitfile "Service" "Restart" "on-failure"
    write_user_unit_file $systemd_service "$cmd" "" "$STACK_USER"

    echo_summary "Starting monasca-persister"
    $SYSTEMCTL enable $systemd_service
    $SYSTEMCTL start $systemd_service
}
stop_monasca-persister() {
    if ! is_monasca_persister_enabled; then
        return
    fi
    echo_summary "Stopping monasca-persister"
    stop_process "monasca-persister"
}
clean_monasca-persister() {
    if ! is_monasca_persister_enabled; then
        return
    fi
    echo_summary "Cleaning monasca-persister"
    clean_monasca_persister_$MONASCA_PERSISTER_IMPLEMENTATION_LANG
    rm -rf ${MONASCA_PERSISTER_GATE_CONFIG}
}
# common

# python
install_monasca_persister_python() {
    echo_summary "Installing monasca-persister"
    if python3_enabled; then
        enable_python3_package monasca-persister
    fi
    setup_develop ${MONASCA_PERSISTER_DIR}

    install_monasca_common
    if [[ "${MONASCA_METRICS_DB,,}" == 'influxdb' ]]; then
        pip_install_gr influxdb
    elif [[ "${MONASCA_METRICS_DB,,}" == 'cassandra' ]]; then
        pip_install_gr cassandra-driver
    fi
}

configure_monasca_persister_python() {
    # ensure fresh installation of configuration files
    rm -rf ${MONASCA_PERSISTER_CONF} ${MONASCA_PERSISTER_LOGGING_CONF}

    $MONASCA_PERSISTER_BIN_DIR/oslo-config-generator \
            --config-file $MONASCA_PERSISTER_DIR/config-generator/persister.conf \
            --output-file /tmp/monasca-persister.conf

    install -m 600 /tmp/monasca-persister.conf ${MONASCA_PERSISTER_CONF} && rm -rf /tmp/monasca-persister.conf

    # Set up logging
    iniset $MONASCA_PERSISTER_CONF DEFAULT use_syslog $SYSLOG

    # Format logging
    setup_logging $MONASCA_PERSISTER_CONF
    iniset $MONASCA_PERSISTER_CONF DEFAULT default_log_levels \
        "monasca_common.kafka_lib.client=INFO"
    iniset $MONASCA_PERSISTER_CONF DEFAULT debug $ENABLE_DEBUG_LOG_LEVEL

    iniset "$MONASCA_PERSISTER_CONF" kafka num_processors 1

    iniset "$MONASCA_PERSISTER_CONF" kafka_metrics uri $SERVICE_HOST:9092
    iniset "$MONASCA_PERSISTER_CONF" kafka_metrics group_id 1_metrics
    iniset "$MONASCA_PERSISTER_CONF" kafka_metrics topic metrics
    iniset "$MONASCA_PERSISTER_CONF" kafka_metrics batch_size 30

    iniset "$MONASCA_PERSISTER_CONF" kafka_alarm_history uri $SERVICE_HOST:9092
    iniset "$MONASCA_PERSISTER_CONF" kafka_alarm_history group_id 1_alarm-state-transitions
    iniset "$MONASCA_PERSISTER_CONF" kafka_alarm_history topic alarm-state-transitions
    iniset "$MONASCA_PERSISTER_CONF" kafka_alarm_history batch_size 1

    iniset "$MONASCA_PERSISTER_CONF" zookeeper uri $SERVICE_HOST:2181

    if [[ "${MONASCA_METRICS_DB,,}" == 'influxdb' ]]; then
        iniset "$MONASCA_PERSISTER_CONF" influxdb database_name mon
        iniset "$MONASCA_PERSISTER_CONF" influxdb ip_address ${SERVICE_HOST}
        iniset "$MONASCA_PERSISTER_CONF" influxdb port 8086
        iniset "$MONASCA_PERSISTER_CONF" influxdb password password
        iniset "$MONASCA_PERSISTER_CONF" repositories metrics_driver ${M_REPO_DRIVER_INFLUX}
        iniset "$MONASCA_PERSISTER_CONF" repositories alarm_state_history_driver ${AH_REPO_DRIVER_INFLUX}
    elif [[ "${MONASCA_METRICS_DB,,}" == 'cassandra' ]]; then
        iniset "$MONASCA_PERSISTER_CONF" cassandra contact_points ${SERVICE_HOST}
        iniset "$MONASCA_PERSISTER_CONF" cassandra port 9042
        # iniset "$MONASCA_PERSISTER_CONF" cassandra user monasca
        # iniset "$MONASCA_PERSISTER_CONF" cassandra password password
        iniset "$MONASCA_PERSISTER_CONF" cassandra keyspace monasca
        iniset "$MONASCA_PERSISTER_CONF" cassandra local_data_center datacenter1
        iniset "$MONASCA_PERSISTER_CONF" cassandra connection_timeout 5
        iniset "$MONASCA_PERSISTER_CONF" cassandra read_timeout 60
        iniset "$MONASCA_PERSISTER_CONF" cassandra max_write_retries 5
        iniset "$MONASCA_PERSISTER_CONF" cassandra max_batches 250
        iniset "$MONASCA_PERSISTER_CONF" cassandra max_definition_cache_size 1000000
        # consistency level names:
        # ANY(0),
        # ONE(1),
        # TWO(2),
        # THREE(3),
        # QUORUM(4),
        # ALL(5),
        # LOCAL_QUORUM(6),
        # EACH_QUORUM(7),
        # SERIAL(8),
        # LOCAL_SERIAL(9),
        # LOCAL_ONE(10);
        iniset "$MONASCA_PERSISTER_CONF" cassandra consistency_level ONE
        iniset "$MONASCA_PERSISTER_CONF" cassandra retention_policy 45
        iniset "$MONASCA_PERSISTER_CONF" repositories metrics_driver ${M_REPO_DRIVER_CASSANDRA}
        iniset "$MONASCA_PERSISTER_CONF" repositories alarm_state_history_driver ${AH_REPO_DRIVER_CASSANDRA}
    fi

    ln -sf ${MONASCA_PERSISTER_CONF} ${MONASCA_PERSISTER_GATE_CONFIG}
}

clean_monasca_persister_python() {
    rm -rf ${MONASCA_PERSISTER_CONF} ${MONASCA_PERSISTER_LOGGING_CONF}
}
# python

# java
install_monasca_persister_java() {
    (cd "${MONASCA_PERSISTER_DIR}"/java ; sudo mvn clean package -DskipTests)

    local version=""
    version="$(get_version_from_pom "${MONASCA_PERSISTER_DIR}"/java)"
    sudo cp -f "${MONASCA_PERSISTER_DIR}"/java/target/monasca-persister-${version}-shaded.jar \
        ${MONASCA_PERSISTER_JAR}
}

configure_monasca_persister_java() {
    # ensure fresh installation of configuration file
    rm -rf $MONASCA_PERSISTER_CONF

    install -m 600 "${MONASCA_API_DIR}"/devstack/files/monasca-persister/persister.yml ${MONASCA_PERSISTER_CONF}
    sudo sed -e "
        s|%ZOOKEEPER_HOST%|${SERVICE_HOST}|g;
        s|%VERTICA_HOST%|${SERVICE_HOST}|g;
        s|%INFLUXDB_HOST%|${SERVICE_HOST}|g;
        s|%CASSANDRADB_HOST%|${SERVICE_HOST}|g;
        s|%MONASCA_PERSISTER_DB_TYPE%|${MONASCA_METRICS_DB}|g;
        s|%MONASCA_PERSISTER_BIND_HOST%|${MONASCA_PERSISTER_BIND_HOST}|g;
        s|%MONASCA_PERSISTER_APP_PORT%|${MONASCA_PERSISTER_APP_PORT}|g;
        s|%MONASCA_PERSISTER_ADMIN_PORT%|${MONASCA_PERSISTER_ADMIN_PORT}|g;
        s|%MONASCA_PERSISTER_LOG_DIR%|${MONASCA_PERSISTER_LOG_DIR}|g;
        s|%MONASCA_PERSISTER_BATCH_SIZE%|${MONASCA_PERSISTER_BATCH_SIZE}|g;
        s|%MONASCA_PERSISTER_MAX_BATCH_TIME%|${MONASCA_PERSISTER_MAX_BATCH_TIME}|g;
        s|%MONASCA_PERSISTER_COMMIT_BATCH_TIME%|${MONASCA_PERSISTER_COMMIT_BATCH_TIME}|g;
        s|%MONASCA_PERSISTER_METRIC_THREADS%|${MONASCA_PERSISTER_METRIC_THREADS}|g;
    " -i ${MONASCA_PERSISTER_CONF}

    ln -sf ${MONASCA_PERSISTER_CONF} ${MONASCA_PERSISTER_GATE_CONFIG}
}

clean_monasca_persister_java() {
    rm -rf ${MONASCA_PERSISTER_CONF} ${MONASCA_PERSISTER_LOGGING_CONF} \
        ${MONASCA_PERSISTER_JAR}
}
# java

${_XTRACE_MON_PERSISTER}
