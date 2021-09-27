#
# (C) Copyright 2015-2017 Hewlett Packard Enterprise Development LP
# Copyright 2017 FUJITSU LIMITED
# (C) Copyright 2017 SUSE LLC
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

# Monasca DevStack plugin
#
# Install and start Monasca service in devstack
#
# To enable Monasca in devstack add an entry to local.conf that
# looks like
#
# [[local|localrc]]
# enable_plugin monasca https://git.openstack.org/openstack/monasca-api
#
# By default all Monasca services are started (see
# devstack/settings). To disable a specific service use the
# disable_service function. For example to turn off notification:
#
# disable_service monasca-notification
#
# Several variables set in the localrc section adjust common behaviors
# of Monasca (see within for additional settings):
#
# EXAMPLE VARS HERE

# Save trace setting
XTRACE=$(set +o | grep xtrace)
set -o xtrace

ERREXIT=$(set +o | grep errexit)
set -o errexit

# source lib/*
source ${MONASCA_API_DIR}/devstack/lib/constants.sh
source ${MONASCA_API_DIR}/devstack/lib/zookeeper.sh
source ${MONASCA_API_DIR}/devstack/lib/ui.sh
source ${MONASCA_API_DIR}/devstack/lib/notification.sh
source ${MONASCA_API_DIR}/devstack/lib/profile.sh
source ${MONASCA_API_DIR}/devstack/lib/client.sh
source ${MONASCA_API_DIR}/devstack/lib/persister.sh
source ${MONASCA_API_DIR}/devstack/lib/storm.sh
source ${MONASCA_API_DIR}/devstack/lib/monasca-log.sh
# source lib/*

# Set default implementations to python
export MONASCA_API_IMPLEMENTATION_LANG=${MONASCA_API_IMPLEMENTATION_LANG:-python}

# Set default persistent layer settings
export MONASCA_METRICS_DB=${MONASCA_METRICS_DB:-influxdb}
# Make sure we use ORM mapping as default if postgresql is enabled
if is_service_enabled mysql; then
    MONASCA_DATABASE_USE_ORM=${MONASCA_DATABASE_USE_ORM:-false}
elif is_service_enabled postgresql; then
    MONASCA_DATABASE_USE_ORM=true
fi
MONASCA_DATABASE_USE_ORM=$(trueorfalse False MONASCA_DATABASE_USE_ORM)

# Set INFLUXDB_VERSION
if [[ "${MONASCA_API_IMPLEMENTATION_LANG,,}" == 'java' ]]; then

    INFLUXDB_VERSION=${INFLUXDB_VERSION:-${INFLUXDB_JAVA_VERSION}}

elif [[ "${MONASCA_API_IMPLEMENTATION_LANG,,}" == 'python' ]]; then

    INFLUXDB_VERSION=${INFLUXDB_VERSION:-${INFLUXDB_PYTHON_VERSION}}

else

    echo "Found invalid value for variable MONASCA_API_IMPLEMENTATION_LANG: $MONASCA_API_IMPLEMENTATION_LANG"
    echo "Valid values for MONASCA_API_IMPLEMENTATION_LANG are \"java\" and \"python\""
    die "Please set MONASCA_API_IMPLEMENTATION_LANG to either \"java'' or \"python\""

fi

# monasca-api settings
if [[ ${USE_VENV} = True ]]; then
    PROJECT_VENV["monasca-api"]=${MONASCA_API_DIR}.venv
    MONASCA_API_BIN_DIR=${PROJECT_VENV["monasca-api"]}/bin
else
    MONASCA_API_BIN_DIR=$(get_python_exec_prefix)
fi

if [[ "${MONASCA_API_USE_MOD_WSGI}" == 'True' && "${MONASCA_API_IMPLEMENTATION_LANG}" == "python" ]]; then
    MONASCA_API_BASE_URI=${MONASCA_API_SERVICE_PROTOCOL}://${MONASCA_API_SERVICE_HOST}/metrics
else
    MONASCA_API_BASE_URI=${MONASCA_API_SERVICE_PROTOCOL}://${MONASCA_API_SERVICE_HOST}:${MONASCA_API_SERVICE_PORT}
fi


MONASCA_API_URI_V2=${MONASCA_API_BASE_URI}/v2.0

# Files inside this directory will be visible in gates log
MON_API_GATE_CONFIGURATION_DIR=/etc/monasca-api

function pre_install_monasca {
    echo_summary "Pre-Installing Monasca Components"
    find_nearest_apache_mirror
    install_gate_config_holder
    configure_system_encoding_format
    install_kafka
    install_zookeeper
    install_storm

    install_monasca_virtual_env
    install_monasca_$MONASCA_METRICS_DB

    pre_monasca-persister
}

function install_monasca {

    echo_summary "Installing Monasca"

    install_monasca_common_java
    if is_service_enabled monasca-persister; then
        stack_install_service monasca-persister
    fi
    if is_service_enabled monasca-notification; then
        stack_install_service monasca-notification
    fi

    if is_service_enabled monasca-thresh; then
        if ! is_storm_enabled; then
            die "monasca-thresh requires monasca-storm service to be enabled"
        fi
        install_monasca_thresh
    fi

    if is_service_enabled monasca-api; then
        if [ "$MONASCA_API_IMPLEMENTATION_LANG" == "python" ]; then
            stack_install_service monasca-api
        else
            install_monasca_api_java
            sudo systemctl enable monasca-api
        fi
    fi

    install_ui
}

function configure_monasca {
    echo_summary "Configuring Monasca"

    configure_storm
    configure_ui
    configure_monasca_api
    configure_monasca-notification
    configure_monasca-persister
    install_schema
}

function configure_system_encoding_format {
    # This is needed to build monasca-common
    export LANGUAGE=en_US.UTF-8
    export LC_ALL=en_US.UTF-8
    export LANG=en_US.UTF-8
    export LC_TYPE=en_US.UTF-8
}

function extra_monasca {
    echo_summary "Installing additional monasca components"

    create_accounts
    install_monasca_agent
    install_monascaclient
    install_monasca_profile

    if is_service_enabled horizon; then
        install_nodejs
        install_go
        install_monasca_grafana
    fi

    start_monasca_services
    init_collector_service
    post_storm

    if is_service_enabled horizon; then
        init_monasca_grafana
    fi
}

function start_monasca_services {
    start_storm
    if is_service_enabled monasca-api; then
        start_monasca_api
    fi
    start_monasca-notification
    start_monasca-persister
    if is_service_enabled monasca-thresh; then
        start_service monasca-thresh || restart_service monasca-thresh
    fi
    if is_service_enabled horizon; then
        start_service grafana-server || restart_service grafana-server
    fi
    if is_service_enabled monasca-agent; then
        sudo /usr/local/bin/monasca-reconfigure
        if is_service_enabled nova && [ "$VIRT_DRIVER" = "libvirt" ]; then
            sudo /opt/monasca-agent/bin/monasca-setup -d libvirt
        fi
    fi
}

function delete_kafka_topics {

        /opt/kafka/bin/kafka-topics.sh --delete --zookeeper localhost:2181 \
                --topic metrics || true
        /opt/kafka/bin/kafka-topics.sh --delete --zookeeper localhost:2181 \
                --topic events || true
        /opt/kafka/bin/kafka-topics.sh --delete --zookeeper localhost:2181 \
                --topic alarm-state-transitions || true
        /opt/kafka/bin/kafka-topics.sh --delete --zookeeper localhost:2181 \
                --topic alarm-notifications || true
        /opt/kafka/bin/kafka-topics.sh --delete --zookeeper localhost:2181 \
                --topic retry-notifications || true
        /opt/kafka/bin/kafka-topics.sh --delete --zookeeper localhost:2181 \
                --topic 60-seconds-notifications || true
}

function unstack_monasca {
    stop_service grafana-server || true

    [[ -f /etc/systemd/system/monasca-agent.target ]] && stop_service monasca-agent.target || true

    stop_service monasca-thresh || true

    stop_storm
    stop_monasca-notification
    stop_monasca-persister
    stop_monasca_api

    delete_kafka_topics
    stop_service kafka || true

    stop_service influxdb || true

    stop_service verticad || true

    stop_service vertica_agent || true

    stop_service cassandra || true
}

function clean_monasca {

    set +o errexit

    unstack_monasca
    clean_ui

    if is_service_enabled horizon; then
        clean_nodejs
        clean_monasca_grafana
        clean_go
    fi

    if is_service_enabled monasca-agent; then
        clean_monasca_agent
    fi
    if is_service_enabled monasca-thresh; then
        clean_monasca_thresh
    fi
    clean_storm
    if is_service_enabled monasca-api; then
        clean_monasca_api_$MONASCA_API_IMPLEMENTATION_LANG
    fi

    clean_monasca-persister
    clean_monasca-notification
    clean_monasca_common_java

    clean_schema

    clean_monasca_profile
    clean_monascaclient

    clean_monasca_$MONASCA_METRICS_DB

    clean_kafka

    clean_zookeeper

    clean_monasca_virtual_env

    #Restore errexit
    set -o errexit
}

function install_monasca_virtual_env {

    echo_summary "Install Monasca Virtual Environment"

    sudo groupadd --system monasca || true

    sudo mkdir -p /opt/monasca || true

    sudo chown $STACK_USER:monasca /opt/monasca

    (cd /opt/monasca ; virtualenv .)
}

function clean_monasca_virtual_env {

    echo_summary "Clean Monasca Virtual Environment"

    sudo rm -rf /opt/monasca

    sudo groupdel monasca

}

function install_kafka {

    echo_summary "Install Monasca Kafka"

    local kafka_tarball=kafka_${KAFKA_VERSION}.tgz
    local kafka_tarball_url=${APACHE_ARCHIVES}kafka/${BASE_KAFKA_VERSION}/${kafka_tarball}

    local kafka_tarball_dest
    kafka_tarball_dest=`get_extra_file ${kafka_tarball_url}`

    sudo groupadd --system kafka || true

    sudo useradd --system -g kafka kafka || true

    sudo tar -xzf ${kafka_tarball_dest} -C /opt

    sudo ln -sf /opt/kafka_${KAFKA_VERSION} /opt/kafka

    sudo cp -f "${MONASCA_API_DIR}"/devstack/files/kafka/kafka-server-start.sh /opt/kafka_${KAFKA_VERSION}/bin/kafka-server-start.sh

    sudo cp -f "${MONASCA_API_DIR}"/devstack/files/kafka/kafka.service /etc/systemd/system/kafka.service

    sudo chown root:root /etc/systemd/system/kafka.service

    sudo chmod 644 /etc/systemd/system/kafka.service

    sudo mkdir -p /var/kafka || true

    sudo chown kafka:kafka /var/kafka

    sudo chmod 755 /var/kafka

    sudo rm -rf /var/kafka/lost+found

    sudo mkdir -p /var/log/kafka || true

    sudo chown kafka:kafka /var/log/kafka

    sudo chmod 755 /var/log/kafka

    sudo ln -sf /opt/kafka/config /etc/kafka

    sudo cp -f "${MONASCA_API_DIR}"/devstack/files/kafka/server.properties /etc/kafka/server.properties

    sudo chown kafka:kafka /etc/kafka/server.properties

    sudo chmod 644 /etc/kafka/server.properties

    # set kafka listeners address.
    sudo sed -i "s/listeners = PLAINTEXT:\/\/your.host.name:9092/listeners = PLAINTEXT:\/\/your.host.name:9092\nlisteners=PLAINTEXT:\/\/${SERVICE_HOST}:9092/"\
        /etc/kafka/server.properties

    sudo systemctl enable kafka

    sudo systemctl start kafka || sudo systemctl restart kafka

}

function clean_kafka {

    echo_summary "Clean Monasca Kafka"

    sudo rm -rf /var/kafka

    sudo rm -rf /var/log/kafka

    sudo rm -rf /etc/kafka

    sudo rm -rf /opt/kafka

    sudo systemctl disable kafka

    sudo rm -rf /etc/systemd/system/kafka.service

    sudo userdel kafka

    sudo groupdel kafka

    sudo rm -rf /opt/kafka_${KAFKA_VERSION}

    sudo rm -rf ${FILES}/kafka_${KAFKA_VERSION}.tgz

}

function install_monasca_influxdb {

    if is_service_enabled monasca-persister; then
        echo_summary "Install Monasca Influxdb"

        local influxdb_deb=influxdb_${INFLUXDB_VERSION}_amd64.deb
        local influxdb_deb_url=https://dl.influxdata.com/influxdb/releases/${influxdb_deb}

        local influxdb_deb_dest
        influxdb_deb_dest=`get_extra_file ${influxdb_deb_url}`

        sudo dpkg --skip-same-version -i ${influxdb_deb_dest}

        # Validate INFLUXDB_VERSION
        validate_version ${INFLUXDB_VERSION}

        if [[ $? -ne 0 ]]; then
            echo "Found invalid value for variable INFLUXDB_VERSION: $INFLUXDB_VERSION"
            echo "Valid values for INFLUXDB_VERSION must be in the form of 1.0.0"
            die "Please set INFLUXDB_VERSION to a correct value"
        fi

        sudo cp -f "${MONASCA_API_DIR}"/devstack/files/influxdb/influxdb.conf /etc/influxdb/influxdb.conf

        sudo cp -f "${MONASCA_API_DIR}"/devstack/files/influxdb/influxdb /etc/default/influxdb

        sudo systemctl start influxdb || sudo systemctl restart influxdb
    fi

}

function install_monasca_vertica {

    echo_summary "Install Monasca Vertica"

    apt_get install dialog

    sudo dpkg --skip-same-version -i /vagrant_home/vertica_${VERTICA_VERSION}_amd64.deb

    # Download Vertica JDBC driver
    # local vertica_jar=vertica-jdbc-${VERTICA_VERSION}.jar
    # local vertica_jar_url=https://my.vertica.com/client_drivers/7.2.x/${VERTICA_VERSION}/${vertica_jar}

    # local vertica_jar_dest
    # vertica_jar_dest=`get_extra_file ${vertica_jar_url}`

    # Current version of Vertica 8.0.0 doesn't support Ubuntu Xenial, so fake a version
    sudo cp -p /etc/debian_version /etc/debian_version.org
    sudo sh -c "echo 'jessie/sid' > /etc/debian_version"

    sudo /opt/vertica/sbin/install_vertica --hosts "127.0.0.1" --deb /vagrant_home/vertica_${VERTICA_VERSION}_amd64.deb --dba-user-password password --license CE --accept-eula --failure-threshold NONE

    sudo su dbadmin -c '/opt/vertica/bin/admintools -t create_db -s "127.0.0.1" -d mon -p password'

    # Bring back Ubuntu version
    sudo mv /etc/debian_version.org /etc/debian_version

    # Copy Vertica JDBC driver to /opt/monasca
    # sudo cp ${FILES}/vertica-jdbc-${VERTICA_VERSION}.jar /opt/monasca/vertica-jdbc-${VERTICA_VERSION}.jar
    sudo cp /vagrant_home/vertica-jdbc-${VERTICA_VERSION}.jar /opt/monasca/vertica-jdbc-${VERTICA_VERSION}.jar

}

function install_monasca_cassandra {

    if is_service_enabled monasca-persister; then
        echo_summary "Install Monasca Cassandra"

        if [[ "$OFFLINE" != "True" ]]; then
            sudo sh -c "echo 'deb http://www.apache.org/dist/cassandra/debian ${CASSANDRA_VERSION} main' > /etc/apt/sources.list.d/cassandra.sources.list"
            REPOS_UPDATED=False
            curl https://www.apache.org/dist/cassandra/KEYS | sudo apt-key add -
            PUBLIC_KEY=`sudo apt_get update 2>&1 | awk '/NO_PUBKEY/ {print $NF}'`
            if [ -n "${PUBLIC_KEY}" ]; then
                sudo apt-key adv --keyserver pool.sks-keyservers.net --recv-key  ${PUBLIC_KEY}
            fi
        fi

        REPOS_UPDATED=False
        apt_get_update
        apt_get install cassandra

        if [[ ${SERVICE_HOST} ]]; then

            # set cassandra server listening ip address
            sudo sed -i "s/^rpc_address: localhost/rpc_address: ${SERVICE_HOST}/g" /etc/cassandra/cassandra.yaml

        fi

        # set batch size larger
        sudo sed -i "s/^batch_size_warn_threshold_in_kb: 5/batch_size_warn_threshold_in_kb: 50/g" /etc/cassandra/cassandra.yaml

        sudo sed -i "s/^batch_size_fail_threshold_in_kb: 50/batch_size_fail_threshold_in_kb: 500/g" /etc/cassandra/cassandra.yaml

        sudo service cassandra restart

        echo "Sleep for 15 seconds to wait starting up Cassandra"
        sleep 15s

        export CQLSH_NO_BUNDLED=true

        # always needed for Monasca api
        pip_install_gr cassandra-driver
    fi
}

function clean_monasca_influxdb {

    echo_summary "Clean Monasca Influxdb"

    sudo rm -f /etc/default/influxdb

    sudo rm -f /etc/influxdb/influxdb.conf

    sudo dpkg --purge influxdb

    sudo rm -rf /var/log/influxdb

    sudo rm -rf /tmp/influxdb

    sudo rm -rf /var/lib/influxdb

    sudo rm -rf /etc/init.d/influxdb

    sudo rm -rf /opt/staging/influxdb/influxdb-package

    sudo rm -rf /etc/influxdb

    sudo rm -rf /tmp/bootstrap*

    sudo rm -rf /run/influxdb

    sudo rm -f  ${FILES}/influxdb_${INFLUXDB_VERSION}_amd64.deb

    sudo rm -f /etc/init.d/influxdb
}

function clean_monasca_vertica {

    echo_summary "Clean Monasca Vertica"

    sudo rm -rf /opt/vertica

    sudo dpkg --purge vertica

    sudo userdel dbadmin

    sudo groupdel verticadba

    sudo rm -rf /home/dbadmin

    apt_get purge dialog
}

function clean_monasca_cassandra {

    echo_summary "Clean Monasca Cassandra"

    apt_get purge cassandra

    apt_get autoremove

    sudo rm -rf /var/lib/cassandra

    sudo rm -rf /var/log/cassandra

    sudo rm -rf /etc/cassandra

    sudo rm -f /etc/apt/sources.list.d/cassandra.list

    sudo rm -f /etc/apt/trusted.gpg.d/cassandra.gpg
}

function install_schema {
    echo_summary "Install Monasca Schema"

    sudo mkdir -p $MONASCA_SCHEMA_DIR || true
    sudo chmod 0755 $MONASCA_SCHEMA_DIR

    install_schema_metric_database_$MONASCA_METRICS_DB
    install_schema_alarm_database
    install_schema_kafka_topics
}

function install_schema_metric_database_influxdb {
    sudo cp -f "${MONASCA_API_DIR}"/devstack/files/schema/influxdb_setup.py $MONASCA_SCHEMA_DIR/influxdb_setup.py
    sudo chmod 0750 $MONASCA_SCHEMA_DIR/influxdb_setup.py
    sudo chown root:root $MONASCA_SCHEMA_DIR/influxdb_setup.py
    if python3_enabled; then
        sudo python3 $MONASCA_SCHEMA_DIR/influxdb_setup.py
    else
        sudo python $MONASCA_SCHEMA_DIR/influxdb_setup.py
    fi
}

function install_schema_metric_database_vertica {
    /opt/vertica/bin/vsql -U dbadmin -w password < "${MONASCA_API_DIR}"/devstack/files/vertica/mon_metrics.sql
    /opt/vertica/bin/vsql -U dbadmin -w password < "${MONASCA_API_DIR}"/devstack/files/vertica/mon_alarms.sql
    /opt/vertica/bin/vsql -U dbadmin -w password < "${MONASCA_API_DIR}"/devstack/files/vertica/roles.sql
    /opt/vertica/bin/vsql -U dbadmin -w password < "${MONASCA_API_DIR}"/devstack/files/vertica/users.sql
}

function install_schema_metric_database_cassandra {
    if is_service_enabled monasca-persister; then
        local CASSANDRA_CONNECT_TIMEOUT=300
        local CASSANDRA_REQUEST_TIMEOUT=300
        sudo cp -f "${MONASCA_API_DIR}"/devstack/files/cassandra/*.cql $MONASCA_SCHEMA_DIR
        /usr/bin/cqlsh ${SERVICE_HOST} -f $MONASCA_SCHEMA_DIR/monasca_schema.cql \
            --connect-timeout="${CASSANDRA_CONNECT_TIMEOUT}" \
            --request-timeout="${CASSANDRA_REQUEST_TIMEOUT}"
    fi
}

function install_schema_kafka_topics {
    sudo mkdir -p /opt/kafka/logs || true
    sudo chown kafka:kafka /opt/kafka/logs
    sudo chmod 0766 /opt/kafka/logs
    # Right number of partition is crucial for performance optimization,
    # in high load(real world) deployment this number should be increased.
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 \
        --replication-factor 1 --partitions 3 --topic metrics
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 \
        --replication-factor 1 --partitions 2 --topic events
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 \
        --replication-factor 1 --partitions 2 --topic alarm-state-transitions
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 \
        --replication-factor 1 --partitions 2 --topic alarm-notifications
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 \
        --replication-factor 1 --partitions 2 --topic retry-notifications
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 \
        --replication-factor 1 --partitions 2 --topic 60-seconds-notifications
}

function install_schema_alarm_database {
    local databaseName="mon"

    if is_service_enabled mysql postgresql; then
        recreate_database $databaseName
        $MONASCA_API_BIN_DIR/monasca_db upgrade
    fi
}

function clean_schema {

    echo_summary "Clean Monasca Schema"

    if is_service_enabled mysql; then
        sudo echo "drop database mon;" | mysql -u$DATABASE_USER -p$DATABASE_PASSWORD
    elif is_service_enabled postgresql; then
        sudo -u postgres psql -c "DROP DATABASE mon;"
    fi

    sudo rm -rf $MONASCA_SCHEMA_DIR

}

function install_monasca_common_java {
    echo_summary "Install monasca_common Java"

    git_clone $MONASCA_COMMON_REPO $MONASCA_COMMON_DIR $MONASCA_COMMON_BRANCH
    (cd "${MONASCA_COMMON_DIR}"/java ; sudo mvn clean install -DskipTests)
}

function clean_monasca_common_java {
    echo_summary "Clean Monasca monasca_common"

    (cd "${MONASCA_COMMON_DIR}" ; sudo mvn clean)
}

function install_monasca_api_java {

    echo_summary "Install Monasca monasca_api_java"

    (cd "${MONASCA_API_DIR}"/java ; sudo mvn clean package -DskipTests)

    local version=""
    version="$(get_version_from_pom "${MONASCA_API_DIR}"/java)"

    sudo cp -f "${MONASCA_API_DIR}"/java/target/monasca-api-${version}-shaded.jar \
        /opt/monasca/monasca-api.jar

    sudo useradd --system -g monasca mon-api || true

    sudo cp -f "${MONASCA_API_DIR}"/devstack/files/monasca-api/monasca-api.service /etc/systemd/system/monasca-api.service

    if [[ "${MONASCA_METRICS_DB,,}" == 'vertica' ]]; then

        # Add the Vertica JDBC to the class path.
        sudo sed -i "s/-cp \/opt\/monasca\/monasca-api.jar/-cp \/opt\/monasca\/monasca-api.jar:\/opt\/monasca\/vertica-jdbc-${VERTICA_VERSION}.jar/g" /etc/systemd/system/monasca-api.service

        sudo sed -i "s/influxdb.service/vertica.service/g" /etc/systemd/system/monasca-api.service

    fi

    sudo chown root:root /etc/systemd/system/monasca-api.service

    sudo chmod 0644 /etc/systemd/system/monasca-api.service

    sudo mkdir -p /var/log/monasca || true

    sudo chown root:monasca /var/log/monasca

    sudo chmod 0755 /var/log/monasca

    sudo mkdir -p /var/log/monasca/api || true

    sudo chown root:monasca /var/log/monasca/api

    sudo chmod 0775 /var/log/monasca/api

    sudo mkdir -p /etc/monasca || true

    sudo chown root:monasca /etc/monasca

    sudo chmod 0775 /etc/monasca

    local dbEngine="com.mysql.jdbc.jdbc2.optional.MysqlDataSource"
    local dbPort=3306

    if [[ ${MONASCA_DATABASE_USE_ORM} == "True" ]]; then
        if is_service_enabled postgresql; then
            dbEngine="org.postgresql.ds.PGPoolingDataSource"
            dbPort=5432
        fi
    fi

    sudo cp -f "${MONASCA_API_DIR}"/devstack/files/monasca-api/api-config.yml /etc/monasca/api-config.yml
    sudo chown mon-api:root /etc/monasca/api-config.yml
    sudo chmod 0640 /etc/monasca/api-config.yml

    sudo sed -e "
        s|%KAFKA_HOST%|$SERVICE_HOST|g;
        s|%MONASCA_DATABASE_USE_ORM%|$MONASCA_DATABASE_USE_ORM|g;
        s|%MONASCA_API_DATABASE_ENGINE%|$dbEngine|g;
        s|%MONASCA_API_SERVICE_HOST%|$MONASCA_API_SERVICE_HOST|g;
        s|%MONASCA_API_SERVICE_PORT%|$MONASCA_API_SERVICE_PORT|g;
        s|%MONASCA_API_ADMIN_PORT%|$MONASCA_API_ADMIN_PORT|g;
        s|%DATABASE_USER%|$DATABASE_USER|g;
        s|%DATABASE_HOST%|$DATABASE_HOST|g;
        s|%DATABASE_PORT%|$dbPort|g;
        s|%MYSQL_HOST%|$MYSQL_HOST|g;
        s|%MYSQL_PORT%|$dbPort|g;
        s|%DATABASE_PASSWORD%|$DATABASE_PASSWORD|g;
        s|%MONASCA_METRICS_DB%|$MONASCA_METRICS_DB|g;
        s|%INFLUXDB_HOST%|$SERVICE_HOST|g;
        s|%INFLUXDB_PORT%|8086|g;
        s|%VERTICA_HOST%|$SERVICE_HOST|g;
        s|%ADMIN_PASSWORD%|$ADMIN_PASSWORD|g;
        s|%KEYSTONE_SERVICE_PORT%|$KEYSTONE_SERVICE_PORT|g;
        s|%KEYSTONE_SERVICE_HOST%|$KEYSTONE_SERVICE_HOST|g;
    " -i /etc/monasca/api-config.yml

}
function install_monasca-api {
    echo_summary "Install Monasca monasca_api "

    git_clone $MONASCA_API_REPO $MONASCA_API_DIR $MONASCA_API_BRANCH

    if python3_enabled; then
        enable_python3_package monasca-api
    fi
    setup_develop $MONASCA_API_DIR

    install_monasca_common

    if [[ "${MONASCA_API_USE_MOD_WSGI}" == 'True' ]]; then
        pip_install uwsgi
    else
        pip_install_gr gunicorn
    fi

    if [[ "${MONASCA_METRICS_DB,,}" == 'influxdb' ]]; then
        pip_install_gr influxdb
    fi
    if [[ "${MONASCA_METRICS_DB,,}" == 'cassandra' ]]; then
        pip_install_gr cassandra-driver
    fi
    if is_service_enabled postgresql; then
        apt_get install libpq-dev
        pip_install_gr psycopg2
    elif is_service_enabled mysql; then
        apt_get install libmysqlclient-dev
        pip_install_gr PyMySQL
    fi

}

function configure_monasca_api_python {
    if is_service_enabled monasca-api; then
        echo_summary "Configuring monasca-api python"
        sudo install -d -o $STACK_USER $MONASCA_API_CONF_DIR

        sudo mkdir -p /var/log/monasca || true

        sudo chown $STACK_USER:monasca /var/log/monasca

        sudo chmod 0755 /var/log/monasca

        sudo mkdir -p /var/log/monasca/api || true

        sudo chown $STACK_USER:monasca /var/log/monasca/api

        sudo chmod 0775 /var/log/monasca/api

        # create configuration files in target locations
        rm -rf $MONASCA_API_CONF $MONASCA_API_PASTE_INI $MONASCA_API_LOGGING_CONF
        $MONASCA_API_BIN_DIR/oslo-config-generator \
            --config-file $MONASCA_API_DIR/config-generator/monasca-api.conf \
            --output-file /tmp/monasca-api.conf

        install -m 600 /tmp/monasca-api.conf $MONASCA_API_CONF && rm -rf /tmp/monasca-api.conf
        install -m 600 $MONASCA_API_DIR/etc/api-logging.conf $MONASCA_API_LOGGING_CONF
        install -m 600 $MONASCA_API_DIR/etc/api-config.ini $MONASCA_API_PASTE_INI
        # create configuration files in target locations

        local dbAlarmUrl
        local dbMetricDriver
        if [[ "${MONASCA_METRICS_DB,,}" == 'cassandra' ]]; then
            dbMetricDriver="monasca_api.common.repositories.cassandra.metrics_repository:MetricsRepository"
        else
            dbMetricDriver="monasca_api.common.repositories.influxdb.metrics_repository:MetricsRepository"
        fi
        dbAlarmUrl=`database_connection_url mon`

        # default settings
        iniset "$MONASCA_API_CONF" DEFAULT region $REGION_NAME
        iniset "$MONASCA_API_CONF" DEFAULT log_config_append $MONASCA_API_LOGGING_CONF
        if $USE_OLD_LOG_API = true; then
            iniset "$MONASCA_API_CONF" DEFAULT enable_logs_api false
        else
            if is_service_enabled monasca-log; then
                iniset "$MONASCA_API_CONF" DEFAULT enable_logs_api true
            else
                iniset "$MONASCA_API_CONF" DEFAULT enable_logs_api false
            fi
        fi

        # logging
        iniset "$MONASCA_API_LOGGING_CONF" handler_file args "('$MONASCA_API_LOG_DIR/monasca-api.log', 'a', 104857600, 5)"

        # messaging
        iniset "$MONASCA_API_CONF" messaging driver "monasca_api.common.messaging.kafka_publisher:KafkaPublisher"
        iniset "$MONASCA_API_CONF" kafka uri "$SERVICE_HOST:9092"

        # databases
        iniset "$MONASCA_API_CONF" database connection $dbAlarmUrl
        iniset "$MONASCA_API_CONF" repositories metrics_driver $dbMetricDriver
        iniset "$MONASCA_API_CONF" cassandra contact_points $(ipv6_unquote $SERVICE_HOST)
        iniset "$MONASCA_API_CONF" influxdb ip_address $(ipv6_unquote $SERVICE_HOST)
        iniset "$MONASCA_API_CONF" influxdb port 8086

        # keystone & security
        configure_auth_token_middleware $MONASCA_API_CONF "admin"
        iniset "$MONASCA_API_CONF" keystone_authtoken region_name $REGION_NAME
        iniset "$MONASCA_API_CONF" keystone_authtoken project_name "admin"
        iniset "$MONASCA_API_CONF" keystone_authtoken password $ADMIN_PASSWORD

        iniset "$MONASCA_API_CONF" security default_authorized_roles "monasca-user"
        iniset "$MONASCA_API_CONF" security agent_authorized_roles "monasca-agent"
        iniset "$MONASCA_API_CONF" security read_only_authorized_roles "monasca-read-only-user"
        iniset "$MONASCA_API_CONF" security delegate_authorized_roles "monasca-agent"

        # server setup
        iniset "$MONASCA_API_PASTE_INI" server:main host $MONASCA_API_SERVICE_HOST
        iniset "$MONASCA_API_PASTE_INI" server:main port $MONASCA_API_SERVICE_PORT
        iniset "$MONASCA_API_PASTE_INI" server:main workers $API_WORKERS

        # link configuration for the gate
        ln -sf $MONASCA_API_CONF $MON_API_GATE_CONFIGURATION_DIR
        ln -sf $MONASCA_API_PASTE_INI $MON_API_GATE_CONFIGURATION_DIR
        ln -sf $MONASCA_API_LOGGING_CONF $MON_API_GATE_CONFIGURATION_DIR

        if [ "${MONASCA_API_USE_MOD_WSGI}" == 'True' ]; then
            configure_monasca_api_python_uwsgi
        fi

    fi
}

function configure_monasca_api_python_uwsgi {
    rm -rf $MONASCA_API_UWSGI_CONF

    install -m 600 $MONASCA_API_DIR/etc/api-uwsgi.ini $MONASCA_API_UWSGI_CONF
    write_uwsgi_config "$MONASCA_API_UWSGI_CONF" "$MONASCA_API_BIN_DIR/monasca-api-wsgi" "/metrics"
}

function start_monasca_api_python {
    if is_service_enabled monasca-api; then
        echo_summary "Starting monasca-api"

        local service_port=$MONASCA_API_SERVICE_PORT
        local service_protocol=$MONASCA_API_SERVICE_PROTOCOL
        local gunicorn="$MONASCA_API_BIN_DIR/gunicorn"

        restart_service memcached
        if [ "${MONASCA_API_USE_MOD_WSGI}" == 'True' ]; then
            service_uri=$service_protocol://$MONASCA_API_SERVICE_HOST/api/v2.0
            run_process "monasca-api" "$MONASCA_API_BIN_DIR/uwsgi --ini $MONASCA_API_UWSGI_CONF" ""
        else
            service_uri=$service_protocol://$MONASCA_API_SERVICE_HOST:$service_port
            run_process "monasca-api" "$gunicorn --paste $MONASCA_API_PASTE_INI"
        fi

        echo "Waiting for monasca-api to start..."
        if ! wait_for_service $SERVICE_TIMEOUT $service_uri; then
            die $LINENO "monasca-api did not start"
        fi
    fi
}

function stop_monasca_api_python {
    if is_service_enabled monasca-api; then
        stop_process "monasca-api" || true
    fi
}

function clean_monasca_api_java {

    echo_summary "Clean Monasca monasca_api_java"

    (cd "${MONASCA_API_DIR}" ; sudo mvn clean)

    sudo rm /etc/monasca/api-config.yml

    sudo rm -rf /var/log/monasca/api

    sudo systemctl disable monasca-api

    sudo rm /etc/systemd/system/monasca-api.service

    sudo rm /opt/monasca/monasca-api.jar

    sudo rm /var/log/upstart/monasca-api.log*

    sudo userdel mon-api
}

function clean_monasca_api_python {

    echo_summary "Clean Monasca monasca_api_python"

    sudo rm -rf /etc/monasca/monasca-api.conf
    sudo rm -rf /etc/monasca/api-logging.conf
    sudo rm -rf /etc/monasca/api-config.ini
    sudo rm -rf $MON_API_GATE_CONFIGURATION_DIR
    sudo rm -rf $MONASCA_API_LOG_DIR

    if is_service_enabled postgresql; then
        apt_get purge libpq-dev
    elif is_service_enabled mysql; then
        apt_get purge libmysqlclient-dev
    fi

    if [ "$MONASCA_API_USE_MOD_WSGI" == "True" ]; then
        clean_monasca_api_uwsgi
    fi

}

function clean_monasca_api_uwsgi {
    sudo rm -rf $MONASCA_API_UWSGI_CONF
}

function start_monasca_api {
    if [[ "${MONASCA_API_IMPLEMENTATION_LANG,,}" == 'java' ]]; then
        start_service monasca-api || restart_service monasca-api
    elif [[ "${MONASCA_API_IMPLEMENTATION_LANG,,}" == 'python' ]]; then
        start_monasca_api_python
    fi
}

function stop_monasca_api {
    if [[ "${MONASCA_API_IMPLEMENTATION_LANG,,}" == 'java' ]]; then
        stop_service monasca-api || true
    elif [[ "${MONASCA_API_IMPLEMENTATION_LANG,,}" == 'python' ]]; then
        stop_monasca_api_python
    fi
}

function configure_monasca_api {
    if [[ "${MONASCA_API_IMPLEMENTATION_LANG,,}" == 'python' ]]; then
        configure_monasca_api_python
    fi
    #NOTE(basiaka) Refactor of monasca-api in Java version will be handled in another change
}

function install_monasca_thresh {

    echo_summary "Install Monasca monasca_thresh"

    git_clone $MONASCA_THRESH_REPO $MONASCA_THRESH_DIR $MONASCA_THRESH_BRANCH
    (cd "${MONASCA_THRESH_DIR}"/thresh ; sudo mvn clean package -DskipTests)

    local version=""
    version="$(get_version_from_pom "${MONASCA_THRESH_DIR}"/thresh)"

    sudo cp -f "${MONASCA_THRESH_DIR}"/thresh/target/monasca-thresh-${version}-shaded.jar \
        /opt/monasca/monasca-thresh.jar

    sudo useradd --system -g monasca mon-thresh || true

    sudo mkdir -p /etc/monasca || true

    sudo chown root:monasca /etc/monasca

    sudo chmod 0775 /etc/monasca

    sudo cp -f "${MONASCA_API_DIR}"/devstack/files/monasca-thresh/thresh-config.yml /etc/monasca/thresh-config.yml

    sudo chown root:monasca /etc/monasca/thresh-config.yml

    sudo chmod 0640 /etc/monasca/thresh-config.yml

    local dbEngine="org.mariadb.jdbc.Driver"
    local dbPort=3306

    if [[ ${MONASCA_DATABASE_USE_ORM} == "True" ]]; then
        if is_service_enabled postgresql; then
            dbEngine="org.postgresql.ds.PGPoolingDataSource"
            dbPort=5432
        fi
    fi

    sudo sed -e "
        s|%KAFKA_HOST%|$SERVICE_HOST|g;
        s|%MONASCA_THRESH_DATABASE_ENGINE%|$dbEngine|g;
        s|%DATABASE_USER%|$DATABASE_USER|g;
        s|%MONASCA_DATABASE_USE_ORM%|$MONASCA_DATABASE_USE_ORM|g;
        s|%DATABASE_TYPE%|$DATABASE_TYPE|g;
        s|%DATABASE_HOST%|$DATABASE_HOST|g;
        s|%DATABASE_PASSWORD%|$DATABASE_PASSWORD|g;
        s|%DATABASE_PORT%|$dbPort|g;
        s|%MONASCA_STATSD_PORT%|$MONASCA_STATSD_PORT|g;
    " -i /etc/monasca/thresh-config.yml

    sudo cp -f "${MONASCA_API_DIR}"/devstack/files/monasca-thresh/monasca-thresh /etc/init.d/monasca-thresh

    sudo chown root:root /etc/init.d/monasca-thresh

    sudo chmod 0744 /etc/init.d/monasca-thresh

    sudo systemctl enable monasca-thresh

}

function clean_monasca_thresh {

    echo_summary "Clean Monasca monasca_thresh"

    (cd "${MONASCA_THRESH_DIR}"/thresh ; sudo mvn clean)

    sudo systemctl disable monasca-thresh

    sudo rm /etc/init.d/monasca-thresh

    sudo rm /etc/monasca/thresh-config.yml

    sudo userdel mon-thresh || true

    sudo rm /opt/monasca/monasca-thresh.jar

}

function create_accounts {

    local projects=("mini-mon" "admin" "demo")
    declare -A users=(
        ["mini-mon"]="password"
        ["monasca-agent"]="password"
        ["admin"]="${ADMIN_PASSWORD}"
        ["demo"]="${ADMIN_PASSWORD}"
        ["monasca-read-only-user"]="password"
    )
    local roles=("monasca-user" "monasca-agent" "admin" "monasca-read-only-user")

    for project in "${projects[@]}"; do
        get_or_create_project "${project}"
    done
    for user in "${!users[@]}"; do
        local password
        password="${users[$user]}"
        get_or_create_user "${user}" "${password}"
    done
    for role in "${roles[@]}"; do
        get_or_create_role "${role}"
    done

    # create assignments
    # args=> <role> <user> <project>
    get_or_add_user_project_role "monasca-user" "mini-mon" "mini-mon"
    get_or_add_user_project_role "monasca-user" "admin" "admin"
    get_or_add_user_project_role "monasca-user" "demo" "demo"

    get_or_add_user_project_role "admin" "mini-mon" "mini-mon"

    get_or_add_user_project_role "monasca-agent" "monasca-agent" "mini-mon"

    get_or_add_user_project_role "monasca-read-only-user" "monasca-read-only-user" "mini-mon"

    # crate service
    get_or_create_service "monasca" "${MONASCA_SERVICE_TYPE}" "Monasca Monitoring Service"

    # create endpoint
    get_or_create_endpoint \
            "monasca" \
            "${REGION_NAME}" \
            "${MONASCA_API_URI_V2}" \
            "${MONASCA_API_URI_V2}" \
            "${MONASCA_API_URI_V2}"

    if is_service_enabled monasca-log; then
        local log_search_url="http://$KIBANA_SERVICE_HOST:$KIBANA_SERVICE_PORT/"

        get_or_create_service "logs" "logs" "Monasca Log service"
        get_or_create_endpoint \
            "logs" \
            "{$REGION_NAME}" \
            "{$MONASCA_API_URI_V2}" \
            "{$MONASCA_API_URI_V2}" \
            "{$MONASCA_API_URI_V2}"

        get_or_create_service "logs-search" "logs-search" "Monasca Log search service"
        get_or_create_endpoint \
            "logs-search" \
            "$REGION_NAME" \
            "$log_search_url" \
            "$log_search_url" \
            "$log_search_url"

    fi
}

function install_keystone_client {
    PIP_VIRTUAL_ENV=/opt/monasca

    install_keystoneclient
    install_keystoneauth

    unset PIP_VIRTUAL_ENV
}

function install_monasca_agent {

    if is_service_enabled monasca-agent; then
        echo_summary "Install Monasca monasca_agent"

        apt_get install python-yaml libxml2-dev libxslt1-dev

        MONASCA_AGENT_EXTRAS="kafka_plugin"
        if is_service_enabled nova && [ "$VIRT_DRIVER" = "libvirt" ]; then
            apt_get install libvirt-dev
            MONASCA_AGENT_EXTRAS=${MONASCA_AGENT_EXTRAS},libvirt
        fi

        git_clone $MONASCA_CLIENT_REPO $MONASCA_CLIENT_DIR $MONASCA_CLIENT_BRANCH
        git_clone $MONASCA_AGENT_REPO $MONASCA_AGENT_DIR $MONASCA_AGENT_BRANCH

        sudo mkdir -p /opt/monasca-agent || true
        sudo chown $STACK_USER:monasca /opt/monasca-agent

        # TODO: remove the trailing pip version when a proper fix
        # arrives for handling this bug https://github.com/pypa/pip/issues/8210
        # Similar issue: https://bugs.launchpad.net/devstack/+bug/1906322
        if python3_enabled; then
            (cd /opt/monasca-agent ;
            virtualenv -p python3 . ;
            bin/python3 -m pip install --upgrade pip==20.2.3)
            sudo rm -rf /opt/stack/monasca-common/.eggs/
        else
            (cd /opt/monasca-agent ; virtualenv .)
        fi

        PIP_VIRTUAL_ENV=/opt/monasca-agent

        setup_install $MONASCA_AGENT_DIR $MONASCA_AGENT_EXTRAS
        setup_dev_lib "python-monascaclient"

        unset PIP_VIRTUAL_ENV

        sudo mkdir -p /etc/monasca/agent/conf.d || true

        sudo chown root:root /etc/monasca/agent/conf.d

        sudo chmod 0755 /etc/monasca/agent/conf.d

        sudo mkdir -p /usr/lib/monasca/agent/custom_checks.d || true

        sudo chown root:root /usr/lib/monasca/agent/custom_checks.d

        sudo chmod 0755 /usr/lib/monasca/agent/custom_checks.d

        sudo mkdir -p /usr/lib/monasca/agent/custom_detect.d || true

        sudo chown root:root /usr/lib/monasca/agent/custom_detect.d

        sudo chmod 0755 /usr/lib/monasca/agent/custom_detect.d

        sudo cp -f "${MONASCA_API_DIR}"/devstack/files/monasca-agent/host_alive.yaml /etc/monasca/agent/conf.d/host_alive.yaml
        sudo cp -f "${MONASCA_API_DIR}"/devstack/files/monasca-agent/http_check.yaml /etc/monasca/agent/conf.d/http_check.yaml
        sudo cp -f "${MONASCA_API_DIR}"/devstack/files/monasca-agent/kafka_consumer.yaml /etc/monasca/agent/conf.d/kafka_consumer.yaml
        sudo cp -f "${MONASCA_API_DIR}"/devstack/files/monasca-agent/mysql.yaml /etc/monasca/agent/conf.d/mysql.yaml
        sudo cp -f "${MONASCA_API_DIR}"/devstack/files/monasca-agent/process.yaml /etc/monasca/agent/conf.d/process.yaml
        sudo cp -f "${MONASCA_API_DIR}"/devstack/files/monasca-agent/zk.yaml /etc/monasca/agent/conf.d/zk.yaml

        sudo sed -i "s/127\.0\.0\.1/$(hostname)/" /etc/monasca/agent/conf.d/*.yaml

        sudo cp -f "${MONASCA_API_DIR}"/devstack/files/monasca-agent/monasca-reconfigure /usr/local/bin/monasca-reconfigure

        sudo chown root:root /usr/local/bin/monasca-reconfigure

        sudo chmod 0750 /usr/local/bin/monasca-reconfigure

        sudo sed -e "
            s|%MONASCA_STATSD_PORT%|$MONASCA_STATSD_PORT|g;
            s|%MONASCA_SERVICE_TYPE%|$MONASCA_SERVICE_TYPE|g;
            s|%KEYSTONE_AUTH_URI%|$KEYSTONE_AUTH_URI|g;
            s|%SERVICE_DOMAIN_NAME%|$SERVICE_DOMAIN_NAME|g;
            s|%REGION_NAME%|$REGION_NAME|g;
        " -i /usr/local/bin/monasca-reconfigure
    fi
}

function clean_monasca_agent {

    if is_service_enabled monasca-agent; then
        echo_summary "Clean Monasca monasca_agent"

        sudo rm /etc/init.d/monasca-agent

        sudo rm /usr/local/bin/monasca-reconfigure

        sudo rm /etc/monasca/agent/conf.d/host_alive.yaml

        sudo chown root:root /etc/monasca/agent/conf.d/host_alive.yaml

        chmod 0644 /etc/monasca/agent/conf.d/host_alive.yaml

        sudo rm -rf /usr/lib/monasca/agent/custom_detect.d

        sudo rm -rf  /usr/lib/monasca/agent/custom_checks.d

        sudo rm -rf /etc/monasca/agent/conf.d

        sudo rm -rf /etc/monasca/agent

        sudo rm -rf /opt/monasca-agent

        [[ -f /etc/systemd/system/monasca-agent.target ]] && sudo rm /etc/systemd/system/monasca-agent.target
        [[ -f /etc/systemd/system/monasca-collector.service ]] && sudo rm /etc/systemd/system/monasca-collector.service
        [[ -f /etc/systemd/system/monasca-forwarder.service ]] && sudo rm /etc/systemd/system/monasca-forwarder.service
        [[ -f /etc/systemd/system/monasca-statsd.service ]] && sudo rm /etc/systemd/system/monasca-statsd.service

        apt_get purge libxslt1-dev
        apt_get purge libxml2-dev
        apt_get purge python-yaml
    fi
}

# install nodejs and npm packages, works behind corporate proxy
# and does not result in gnutsl_handshake error
function install_nodejs {

    echo_summary "Install Node.js"
    curl -sL https://deb.nodesource.com/setup_10.x | sudo bash -

    apt_get install nodejs
    npm config set registry "http://registry.npmjs.org/"; \
    npm config set proxy "${HTTP_PROXY}"; \
    npm set strict-ssl false;
}

function init_monasca_grafana {
    echo_summary "Init Grafana"

    sudo cp -f -r "${MONASCA_API_DIR}"/devstack/files/grafana/dashboards.d "${DASHBOARDS_DIR}"
    sudo chown -R root:root "${DASHBOARDS_DIR}"
    sudo chmod -R 0644 "${DASHBOARDS_DIR}"


    if python3_enabled; then
        sudo python3 "${MONASCA_API_DIR}"/devstack/files/grafana/grafana-init.py
    else
        sudo python "${MONASCA_API_DIR}"/devstack/files/grafana/grafana-init.py
    fi

    sudo rm -rf "${DASHBOARDS_DIR}"
}

function install_monasca_grafana {

    echo_summary "Install Grafana"

    if [ ! -d "${GRAFANA_DIR}" ]; then
        git_timed clone $GRAFANA_REPO $GRAFANA_DIR --branch $GRAFANA_BRANCH --depth 1
    fi

    npm config set python /usr/bin/python3

    cd "${MONASCA_BASE}"

    mkdir grafana-build || true
    cd grafana-build
    export GOPATH=`pwd`
    mkdir -p $GOPATH/src/github.com/grafana
    cd $GOPATH/src/github.com/grafana
    cp -rf "${GRAFANA_DIR}" .

    cd grafana
    cp "${MONASCA_UI_DIR}"/grafana-dashboards/* ./public/dashboards/

    go run build.go build

    npm config set unsafe-perm true
    npm install
    sudo npm install -g grunt-cli
    grunt --force

    cd "${MONASCA_BASE}"
    sudo rm -r grafana

    sudo useradd grafana || true
    sudo mkdir /etc/grafana || true
    sudo mkdir /var/lib/grafana || true
    sudo mkdir /var/lib/grafana/plugins || true
    sudo mkdir /var/log/grafana || true

    git_clone $MONASCA_GRAFANA_DATASOURCE_REPO $MONASCA_GRAFANA_DATASOURCE_DIR $MONASCA_GRAFANA_DATASOURCE_BRANCH
    sudo ln -sfF "${MONASCA_GRAFANA_DATASOURCE_DIR}" /var/lib/grafana/plugins/monasca-grafana-datasource

    sudo chown -R grafana:grafana /var/lib/grafana /var/log/grafana

    sudo cp -f "${MONASCA_API_DIR}"/devstack/files/grafana/grafana.ini /etc/grafana/grafana.ini
    sudo sed -e "
        s|%KEYSTONE_AUTH_URI%|$KEYSTONE_AUTH_URI|g;
    " -i /etc/grafana/grafana.ini

    sudo cp -f "${MONASCA_API_DIR}"/devstack/files/grafana/grafana-server /etc/init.d/grafana-server
    sudo sed -i "s#/usr/sbin#"${MONASCA_BASE}"/grafana-build/src/github.com/grafana/grafana/bin#g" /etc/init.d/grafana-server
    sudo sed -i "s#/usr/share#"${MONASCA_BASE}"/grafana-build/src/github.com/grafana#g" /etc/init.d/grafana-server

    sudo systemctl enable grafana-server
}

function clean_nodejs {
    apt_get purge nodejs npm
}

function clean_monasca_grafana {

    sudo rm -f "${MONASCA_BASE}"/grafana-build

    sudo systemctl disable grafana-server

    sudo rm /etc/init.d/grafana-server

    sudo rm -r /etc/grafana

    sudo rm -r /var/lib/grafana

    sudo rm -r /var/log/grafana

}

function install_go {
    echo_summary "Install Go ${GO_VERSION}"

    local go_tarball=go${GO_VERSION}.linux-amd64.tar.gz
    local go_tarball_url=https://storage.googleapis.com/golang/${go_tarball}

    local go_tarball_dest
    go_tarball_dest=`get_extra_file ${go_tarball_url}`

    sudo tar -C /usr/local -xzf ${go_tarball_dest}
    export PATH=$PATH:/usr/local/go/bin
}

function clean_go {
    echo_summary "Clean Go ${GO_VERSION}"

    sudo rm -f ${FILES}/go${GO_VERSION}*
    sudo rm -rf /usr/local/go*
    export PATH=$(echo $PATH | sed -e 's|:/usr/local/go/bin||')
}

###### extra functions

# Validate a program version string is of the form 1.0.0.
# Return 0 if a valid program version string, otherwise 1.
function validate_version {
    version_regex="^([0-9]+\.)?([0-9]+\.)?([0-9]+)$"

    if [[ $1 =~ $version_regex ]]; then
        return 0
    else
        return 1
    fi
}

# Prints the version specified in the pom.xml file in the directory given by
# the argument
function get_version_from_pom {
    python -c "import xml.etree.ElementTree as ET; \
        print(ET.parse(open('$1/pom.xml')).getroot().find( \
        '{http://maven.apache.org/POM/4.0.0}version').text)"
}

function install_monasca_common {
    git_clone $MONASCA_COMMON_REPO $MONASCA_COMMON_DIR $MONASCA_COMMON_BRANCH
    setup_dev_lib "monasca-common"
}

function install_monasca_statsd {
    git_clone $MONASCA_STATSD_REPO $MONASCA_STATSD_DIR $MONASCA_STATSD_BRANCH
    setup_dev_lib "monasca-statsd"
}

function install_gate_config_holder {
    sudo install -d -o $STACK_USER $MON_API_GATE_CONFIGURATION_DIR
}

function find_nearest_apache_mirror {
    if [ -z $APACHE_MIRROR ]; then
        local mirror;
        mirror=`curl -s 'https://www.apache.org/dyn/closer.cgi?as_json=1' | jq --raw-output '.preferred'`
        APACHE_MIRROR=$mirror
    fi
}

# This solution fixes problem with privileges for agent
# to gather metrics from services started as root user.
function init_collector_service {
    if is_service_enabled monasca-agent; then
        echo_summary "Init Monasca collector service"
        sudo systemctl stop monasca-collector
        sudo sed -i "s/User=mon-agent/User=root/g" /etc/systemd/system/monasca-collector.service
        sudo sed -i "s/Group=mon-agent/Group=root/g" /etc/systemd/system/monasca-collector.service
        sudo systemctl daemon-reload
        sudo systemctl restart monasca-collector
    fi
}

function configure_tempest_for_monasca {
    iniset $TEMPEST_CONFIG monitoring kibana_version $KIBANA_VERSION
}

# check for service enabled
if is_service_enabled monasca; then

    if [[ "$1" == "stack" && "$2" == "pre-install" ]]; then
        # Set up system services
        echo_summary "Configuring Monasca system services"
        pre_install_monasca

    elif [[ "$1" == "stack" && "$2" == "install" ]]; then
        # Perform installation of service source
        echo_summary "Installing Monasca"
        install_monasca

    elif [[ "$1" == "stack" && "$2" == "test-config" ]]; then
        if is_service_enabled tempest; then
            echo_summary "Configuring Tempest for Monasca"
            configure_tempest_for_monasca
        fi

    elif [[ "$1" == "stack" && "$2" == "post-config" ]]; then
        # Configure after the other layer 1 and 2 services have been configured
        echo_summary "Configuring Monasca"
        configure_monasca

    elif [[ "$1" == "stack" && "$2" == "extra" ]]; then
        # Initialize and start the Monasca service
        echo_summary "Initializing Monasca"
        extra_monasca
    fi

    if [[ "$1" == "unstack" ]]; then
        # Shut down Monasca services
        echo_summary "Unstacking Monasca"
        unstack_monasca
    fi

    if [[ "$1" == "clean" ]]; then
        # Remove state and transient data
        # Remember clean.sh first calls unstack.sh
        echo_summary "Cleaning Monasca"
        clean_monasca
    fi
fi

# check for service enabled
if is_service_enabled monasca-log; then

    if [[ "$1" == "stack" && "$2" == "pre-install" ]]; then
        # Set up system services
        echo_summary "Configuring Monasca Log Management system services"
        pre_install_logs_services

    elif [[ "$1" == "stack" && "$2" == "install" ]]; then
        # Perform installation of service source
        echo_summary "Installing Monasca Log Management"
        install_monasca_log

    elif [[ "$1" == "stack" && "$2" == "post-config" ]]; then
        # Configure after the other layer 1 and 2 services have been configured
        echo_summary "Configuring Monasca Log Management"
        configure_monasca_log

    elif [[ "$1" == "stack" && "$2" == "extra" ]]; then
        # Initialize and start the Monasca service
        echo_summary "Initializing Monasca Log Management"
        init_monasca_log
        init_monasca_grafana_dashboards
        if is_service_enabled monasca-agent; then
            init_agent
        fi
        start_monasca_log
    fi

    if [[ "$1" == "unstack" ]]; then
        # Shut down Monasca services
        echo_summary "Unstacking Monasca Log Management"
        stop_monasca_log
        delete_kafka_topics
    fi

    if [[ "$1" == "clean" ]]; then
        # Remove state and transient data
        # Remember clean.sh first calls unstack.sh
        echo_summary "Cleaning Monasca Log Management"
        clean_monasca_log
    fi
fi

#Restore errexit
$ERREXIT

# Restore xtrace
$XTRACE
