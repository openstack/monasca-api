#
# (C) Copyright 2015 Hewlett Packard Enterprise Development Company LP
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
# enable_plugin monasca git://git.openstack.org/openstack/monasca-api
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

function pre_install_monasca {
:
}

function install_monasca {

    install_monasca_virtual_env

    install_openjdk_7_jdk

    install_zookeeper

    install_kafka

    install_influxdb

    install_cli_creds

    install_schema

    install_maven

    install_git

    install_monasca_common

    if [[ "${MONASCA_API_IMPLEMENTATION_LANG,,}" == 'java' ]]; then

        install_monasca_api_java

    elif [[ "${MONASCA_API_IMPLEMENTATION_LANG,,}" == 'python' ]]; then

        install_monasca_api_python

    else

        echo "Found invalid value for variable MONASCA_API_IMPLEMENTATION_LANG: $MONASCA_API_IMPLEMENTATION_LANG"
        echo "Valid values for MONASCA_API_IMPLEMENTATION_LANG are 'java' and 'python'"
        die "Please set MONASCA_API_IMPLEMENTATION_LANG to either 'java' or 'python'"

    fi

    if [[ "${MONASCA_PERSISTER_IMPLEMENTATION_LANG,,}" == 'java' ]]; then

        install_monasca_persister_java

    elif [[ "${MONASCA_PERSISTER_IMPLEMENTATION_LANG,,}" == 'python' ]]; then

        install_monasca_persister_python

    else

        echo "Found invalid value for varible MONASCA_PERSISTER_IMPLEMENTATION_LANG: $MONASCA_PERSISTER_IMPLEMENTATION_LANG"
        echo "Valid values for MONASCA_PERSISTER_IMPLEMENTATION_LANG are 'java' and 'python'"
        die "Please set MONASCA_PERSISTER_IMPLEMENTATION_LANG to either 'java' or 'python'"

    fi

    install_monasca_notification

    install_storm

    install_monasca_thresh

}

function post_config_monasca {
:
}

function extra_monasca {

    install_monasca_keystone_client

    install_monasca_agent

    install_monasca_default_alarms

    install_monasca_horizon_ui

    install_monasca_smoke_test
}

function unstack_monasca {


    sudo service monasca-agent stop || true

    sudo service monasca-thresh stop || true

    sudo stop storm-supervisor || true

    sudo stop storm-nimbus || true

    sudo stop monasca-notification || true

    sudo stop monasca-persister || true

    sudo stop monasca-api || true

    sudo stop kafka || true

    sudo stop zookeeper || true

    sudo /etc/init.d/influxdb stop || true
}

function clean_monasca {

    set +o errexit

    unstack_monasca

    clean_monasca_smoke_test

    clean_monasca_horizon_ui

    clean_monasca_default_alarms

    clean_monasca_agent

    clean_monasca_keystone_client

    clean_monasca_thresh

    clean_storm

    clean_monasca_notification

    if [[ "${MONASCA_PERSISTER_IMPLEMENTATION_LANG,,}" == 'java' ]]; then

        clean_monasca_persister_java

    elif [[ "${MONASCA_PERSISTER_IMPLEMENTATION_LANG,,}" == 'python' ]]; then

        clean_monasca_persister_python

    else

        echo "Found invalid value for varible MONASCA_PERSISTER_IMPLEMENTATION_LANG: $MONASCA_PERSISTER_IMPLEMENTATION_LANG"
        echo "Valid values for MONASCA_PERSISTER_IMPLEMENTATION_LANG are 'java' and 'python'"
        die "Please set MONASCA_PERSISTER_IMPLEMENTATION_LANG to either 'java' or 'python'"

    fi

    if [[ "${MONASCA_API_IMPLEMENTATION_LANG,,}" == 'java' ]]; then

        clean_monasca_api_java

    elif [[ "${MONASCA_API_IMPLEMENTATION_LANG,,}" == 'python' ]]; then

        clean_monasca_api_python

    else

        echo "Found invalid value for variable MONASCA_API_IMPLEMENTATION_LANG: $MONASCA_API_IMPLEMENTATION_LANG"
        echo "Valid values for MONASCA_API_IMPLEMENTATION_LANG are 'java' and 'python'"
        die "Please set MONASCA_API_IMPLEMENTATION_LANG to either 'java' or 'python'"

    fi

    clean_monasca_common

    clean_maven

    clean_schema

    clean_cli_creds

    clean_influxdb

    clean_kafka

    clean_zookeeper

    clean_storm

    clean_openjdk_7_jdk

    clean_monasca_virtual_env

    #Restore errexit
    set -o errexit
}

function install_monasca_virtual_env {

    echo_summary "Install Monasca Virtual Environment"

    sudo groupadd --system monasca || true

    sudo mkdir -p /opt/monasca || true

    (cd /opt/monasca ; sudo virtualenv .)
}

function clean_monasca_virtual_env {

    echo_summary "Clean Monasca Virtual Environment"

    sudo rm -rf /opt/monasca

    sudo groupdel monasca

}

function install_zookeeper {

    echo_summary "Install Monasca Zookeeper"

    sudo apt-get -y install zookeeperd

    sudo cp /opt/stack/monasca/devstack/files/zookeeper/zoo.cfg /etc/zookeeper/conf/zoo.cfg

    sudo cp /opt/stack/monasca/devstack/files/zookeeper/myid /etc/zookeeper/conf/myid

    sudo cp /opt/stack/monasca/devstack/files/zookeeper/environment /etc/zookeeper/conf/environment

    sudo mkdir -p /var/log/zookeeper || true

    sudo chmod 755 /var/log/zookeeper

    sudo cp /opt/stack/monasca/devstack/files/zookeeper/log4j.properties /etc/zookeeper/conf/log4j.properties

    sudo start zookeeper || sudo restart zookeeper

}

function clean_zookeeper {

    echo_summary "Clean Monasca Zookeeper"

    sudo apt-get -y purge zookeeperd

    sudo apt-get -y purge zookeeper

    sudo rm -rf /etc/zookeeper

    sudo rm -rf  /var/log/zookeeper

    sudo rm -rf /var/lib/zookeeper

}

function install_kafka {

    echo_summary "Install Monasca Kafka"

    sudo curl http://apache.mirrors.tds.net/kafka/0.8.1.1/kafka_2.9.2-0.8.1.1.tgz -o /root/kafka_2.9.2-0.8.1.1.tgz

    sudo groupadd --system kafka || true

    sudo useradd --system -g kafka kafka || true

    sudo tar -xzf /root/kafka_2.9.2-0.8.1.1.tgz -C /opt

    sudo ln -s /opt/kafka_2.9.2-0.8.1.1 /opt/kafka

    sudo cp -f /opt/stack/monasca/devstack/files/kafka/kafka-server-start.sh /opt/kafka_2.9.2-0.8.1.1/bin/kafka-server-start.sh

    sudo cp -f /opt/stack/monasca/devstack/files/kafka/kafka.conf /etc/init/kafka.conf

    sudo chown root:root /etc/init/kafka.conf

    sudo chmod 644 /etc/init/kafka.conf

    sudo mkdir -p /var/kafka || true

    sudo chown kafka:kafka /var/kafka

    sudo chmod 755 /var/kafka

    sudo rm -rf /var/kafka/lost+found

    sudo mkdir -p /var/log/kafka || true

    sudo chown kafka:kafka /var/log/kafka

    sudo chmod 755 /var/log/kafka

    sudo ln -s /opt/kafka/config /etc/kafka

    sudo cp -f /opt/stack/monasca/devstack/files/kafka/log4j.properties /etc/kafka/log4j.properties

    sudo chown kafka:kafka /etc/kafka/log4j.properties

    sudo chmod 644 /etc/kafka/log4j.properties

    sudo cp -f /opt/stack/monasca/devstack/files/kafka/server.properties /etc/kafka/server.properties

    sudo chown kafka:kafka /etc/kafka/server.properties

    sudo chmod 644 /etc/kafka/server.properties

    sudo start kafka || sudo restart kafka

}

function clean_kafka {

    echo_summary "Clean Monasca Kafka"

    sudo rm -rf /var/kafka

    sudo rm -rf /var/log/kafka

    sudo rm -rf /etc/kafka

    sudo rm -rf /opt/kafka

    sudo rm -rf /etc/init/kafka.conf

    sudo userdel kafka

    sudo groupdel kafka

    sudo rm -rf /opt/kafka_2.9.2-0.8.1.1

    sudo rm -rf /root/kafka_2.9.2-0.8.1.1.tgz

}

function install_influxdb {

    echo_summary "Install Monasca Influxdb"

    sudo mkdir -p /opt/monasca_download_dir || true

    sudo curl http://s3.amazonaws.com/influxdb/influxdb_0.9.1_amd64.deb -o /opt/monasca_download_dir/influxdb_0.9.1_amd64.deb

    sudo dpkg --skip-same-version -i /opt/monasca_download_dir/influxdb_0.9.1_amd64.deb

    sudo cp -f /opt/stack/monasca/devstack/files/influxdb/influxdb.conf /etc/opt/influxdb/influxdb.conf

    sudo cp -f /opt/stack/monasca/devstack/files/influxdb/influxdb /etc/default/influxdb

    sudo /etc/init.d/influxdb start || sudo /etc/init.d/influxdb restart

    echo "Sleep for 60 seconds to let Influxdb elect a leader and start listening for connections"

    sleep 60s

}

function clean_influxdb {

    echo_summary "Clean Monasca Influxdb"

    sudo rm -f /etc/default/influxdb

    sudo rm -f /etc/opt/influxdb/influxdb.conf

    sudo dpkg --purge influxdb

    sudo rm -rf /opt/influxdb

    sudo rm -rf /var/log/influxdb

    sudo rm -rf /tmp/influxdb

    sudo rm -rf /var/opt/influxdb

    sudo rm -rf /etc/init.d/influxdb

    sudo rm -rf /opt/staging/influxdb/influxdb-package

    sudo rm -rf /etc/opt/influxdb/influxdb.conf

    sudo rm -rf /etc/opt/influxdb

    sudo rm -rf /tmp/bootstrap*

    sudo rm -rf /run/influxdb

    sudo rm -rf /opt/influxdb

    sudo rm -f  /opt/monasca_download_dir/influxdb_0.9.1_amd64.deb

    sudo rm -rf /opt/monasca_download_dir

    sudo rm -f /etc/init.d/influxdb
}

function install_cli_creds {

    echo_summary "Install Monasca CLI Creds"

    sudo cp -f /opt/stack/monasca/devstack/files/env.sh /etc/profile.d/monasca_cli.sh

    sudo chown root:root /etc/profile.d/monasca_cli.sh

    sudo chmod 0644 /etc/profile.d/monasca_cli.sh

}

function clean_cli_creds {

    echo_summary "Clean Monasca CLI Creds"

    sudo rm -f /etc/profile.d/monasca_cli.sh

}

function install_schema {

    echo_summary "Install Monasca Schema"

    sudo mkdir -p /opt/monasca/sqls || true

    sudo chmod 0755 /opt/monasca/sqls

    sudo cp -f /opt/stack/monasca/devstack/files/schema/influxdb_setup.py /opt/influxdb/influxdb_setup.py

    sudo chmod 0750 /opt/influxdb/influxdb_setup.py

    sudo chown root:root /opt/influxdb/influxdb_setup.py

    sudo /opt/influxdb/influxdb_setup.py

    sudo cp -f /opt/stack/monasca/devstack/files/schema/mon_mysql.sql /opt/monasca/sqls/mon.sql

    sudo chmod 0644 /opt/monasca/sqls/mon.sql

    sudo chown root:root /opt/monasca/sqls/mon.sql

    sudo mysql -uroot -ppassword < /opt/monasca/sqls/mon.sql || echo "Did the schema change? This process will fail on schema changes."

    sudo cp -f /opt/stack/monasca/devstack/files/schema/winchester.sql /opt/monasca/sqls/winchester.sql

    sudo chmod 0644 /opt/monasca/sqls/winchester.sql

    sudo chown root:root /opt/monasca/sqls/winchester.sql

    sudo mysql -uroot -ppassword < /opt/monasca/sqls/winchester.sql || echo "Did the schema change? This process will fail on schema changes."

    sudo mkdir -p /opt/kafka/logs || true

    sudo chown kafka:kafka /opt/kafka/logs

    sudo chmod 0766 /opt/kafka/logs

    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 64 --topic metrics
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 12 --topic events
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 12 --topic raw-events
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 12 --topic transformed-events
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 12 --topic stream-definitions
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 12 --topic transform-definitions
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 12 --topic alarm-state-transitions
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 12 --topic alarm-notifications
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 12 --topic stream-notifications
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 3 --topic retry-notifications

}

function clean_schema {

    echo_summary "Clean Monasca Schema"

    sudo echo "drop database winchester;" | mysql -uroot -ppassword

    sudo echo "drop database mon;" | mysql -uroot -ppassword

    sudo rm -f /opt/monasca/sqls/winchester.sql

    sudo rm -f /opt/monasca/sqls/mon.sql

    sudo rm -f /opt/influxdb/influxdb_setup.py

    sudo rm -rf /opt/monasca/sqls

}

function install_openjdk_7_jdk {

    echo_summary "Install Monasca openjdk_7_jdk"

    sudo apt-get -y install openjdk-7-jdk

}

function clean_openjdk_7_jdk {

    echo_summary "Clean Monasca openjdk_7_jdk"

    sudo apt-get -y purge openjdk-7-jdk

    sudo apt-get -y autoremove

}

function install_maven {

    echo_summary "Install Monasca Maven"

    sudo apt-get -y install maven

}

function clean_maven {

    echo_summary "Clean Monasca Maven"

    sudo apt-get -y purge maven
}

function install_git {

    echo_summary "Install git"

    sudo apt-get -y install git

}

function install_monasca_common {

    echo_summary "Install Monasca monasca_common"

    if [[ ! -d /opt/stack/monasca-common ]]; then

        sudo git clone https://git.openstack.org/openstack/monasca-common.git /opt/stack/monasca-common

    fi

    (cd /opt/stack/monasca-common ; sudo mvn clean install -DskipTests)

}

function clean_monasca_common {

    echo_summary "Clean Monasca monasca_common"

    (cd /opt/stack/monasca-common ; sudo mvn clean)

}

function install_monasca_api_java {

    echo_summary "Install Monasca monasca_api_java"

    (cd /opt/stack/monasca/java ; sudo mvn clean package -DskipTests)

    sudo cp -f /opt/stack/monasca/java/target/monasca-api-1.1.0-SNAPSHOT-shaded.jar /opt/monasca/monasca-api.jar

    sudo useradd --system -g monasca mon-api || true

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-api/monasca-api.conf /etc/init/monasca-api.conf

    sudo chown root:root /etc/init/monasca-api.conf

    sudo chmod 0744 /etc/init/monasca-api.conf

    sudo mkdir -p /var/log/monasca || true

    sudo chown root:monasca /var/log/monasca

    sudo chmod 0755 /var/log/monasca

    sudo mkdir -p /var/log/monasca/api || true

    sudo chown root:monasca /var/log/monasca/api

    sudo chmod 0755 /var/log/monasca/api

    sudo mkdir -p /etc/monasca || true

    sudo chown root:monasca /etc/monasca

    sudo chmod 0775 /etc/monasca

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-api/api-config.yml /etc/monasca/api-config.yml

    sudo chown mon-api:root /etc/monasca/api-config.yml

    sudo chmod 0640 /etc/monasca/api-config.yml

    sudo start monasca-api || sudo restart monasca-api

}

function install_monasca_api_python {

    echo_summary "Install Monasca monasca_api_python"

    sudo apt-get -y install python-dev

    (cd /opt/monasca; sudo -H ./bin/pip install gunicorn)

    (cd /opt/stack/monasca ; sudo python setup.py sdist)

    MONASCA_API_SRC_DIST=$(ls -td /opt/stack/monasca/dist/monasca-api-*.tar.gz)

    (cd /opt/monasca ; sudo -H ./bin/pip  install --pre --allow-all-external --allow-unverified simport $MONASCA_API_SRC_DIST)

    sudo useradd --system -g monasca mon-api || true

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-api/python/monasca-api.conf /etc/init/monasca-api.conf

    sudo chown root:root /etc/init/monasca-api.conf

    sudo chmod 0744 /etc/init/monasca-api.conf

    sudo mkdir -p /var/log/monasca || true

    sudo chown root:monasca /var/log/monasca

    sudo chmod 0755 /var/log/monasca

    sudo mkdir -p /var/log/monasca/api || true

    sudo chown root:monasca /var/log/monasca/api

    sudo chmod 0775 /var/log/monasca/api

    sudo mkdir -p /etc/monasca || true

    sudo chown root:monasca /etc/monasca

    sudo chmod 0775 /etc/monasca

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-api/python/api-config.conf /etc/monasca/api-config.conf

    sudo chown mon-api:root /etc/monasca/api-config.conf

    sudo chmod 0660 /etc/monasca/api-config.conf

    sudo ln -s /etc/monasca/api-config.conf /etc/api-config.conf

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-api/python/api-config.ini /etc/monasca/api-config.ini

    sudo chown mon-api:root /etc/monasca/api-config.ini

    sudo chmod 0660 /etc/monasca/api-config.ini

    sudo ln -s /etc/monasca/api-config.ini /etc/api-config.ini

    sudo start monasca-api || sudo restart monasca-api
}

function clean_monasca_api_java {

    echo_summary "Clean Monasca monasca_api_java"

    (cd /opt/stack/monasca ; sudo mvn clean)

    sudo rm /etc/monasca/api-config.yml

    sudo rm -rf /var/log/monasca/api

    sudo rm /etc/init/monasca-api.conf

    sudo rm /opt/monasca/monasca-api.jar

    sudo rm /var/log/upstart/monasca-api.log*

    sudo userdel mon-api
}

function clean_monasca_api_python {

    echo_summary "Clean Monasca monasca_api_python"

    sudo rm /etc/init/monasca-api.conf

    sudo rm /etc/api-config.conf

    sudo rm /etc/monasca/api-config.conf

    sudo rm /etc/api-config.ini

    sudo rm /etc/monasca/api-config.ini

    sudo rm -rf /var/log/monasca/api

    sudo rm /var/log/upstart/monasca-api.log*

    sudo userdel mon-api

}

function install_monasca_persister_java {

    echo_summary "Install Monasca monasca_persister_java"

    if [[ ! -d /opt/stack/monasca-persister ]]; then

        sudo git clone https://git.openstack.org/openstack/monasca-persister /opt/stack/monasca-persister

    fi

    (cd /opt/stack/monasca-persister/java ; sudo mvn clean package -DskipTests)

    sudo cp -f /opt/stack/monasca-persister/java/target/monasca-persister-1.1.0-SNAPSHOT-shaded.jar /opt/monasca/monasca-persister.jar

    sudo useradd --system -g monasca mon-persister || true

    sudo mkdir -p /var/log/monasca || true

    sudo chown root:monasca /var/log/monasca

    sudo chmod 0755 /var/log/monasca

    sudo mkdir -p /var/log/monasca/persister || true

    sudo chown root:monasca /var/log/monasca/persister

    sudo chmod 0755 /var/log/monasca/persister

    sudo mkdir -p /etc/monasca || true

    sudo chown root:monasca /etc/monasca

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-persister/persister-config.yml /etc/monasca/persister-config.yml

    sudo chown mon-persister:monasca /etc/monasca/persister-config.yml

    sudo chmod 0640 /etc/monasca/persister-config.yml

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-persister/monasca-persister.conf /etc/init/monasca-persister.conf

    sudo chown root:root /etc/init/monasca-persister.conf

    sudo chmod 0744 /etc/init/monasca-persister.conf

    sudo start monasca-persister || sudo restart monasca-persister

}

function install_monasca_persister_python {

    echo_summary "Install Monasca monasca_persister_python"

    if [[ ! -d /opt/stack/monasca-persister ]]; then

        sudo git clone https://git.openstack.org/openstack/monasca-persister /opt/stack/monasca-persister

    fi

    (cd /opt/stack/monasca-persister ; sudo python setup.py sdist)

    MONASCA_PERSISTER_SRC_DIST=$(ls -td /opt/stack/monasca-persister/dist/monasca-persister-*.tar.gz | head -1)

    sudo mkdir -p /opt/monasca-persister || true

    (cd /opt/monasca-persister ; sudo virtualenv .)

    (cd /opt/monasca-persister ; sudo -H ./bin/pip  install --pre --allow-all-external --allow-unverified simport $MONASCA_PERSISTER_SRC_DIST)

    sudo useradd --system -g monasca mon-persister || true

    sudo mkdir -p /var/log/monasca || true

    sudo chown root:monasca /var/log/monasca

    sudo chmod 0755 /var/log/monasca

    sudo mkdir -p /var/log/monasca/persister || true

    sudo chown root:monasca /var/log/monasca/persister

    sudo chmod 0775 /var/log/monasca/persister

    sudo mkdir -p /etc/monasca || true

    sudo chown root:monasca /etc/monasca

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-persister/python/persister.conf /etc/monasca/persister.conf

    sudo chown mon-persister:monasca /etc/monasca/persister.conf

    sudo chmod 0640 /etc/monasca/persister.conf

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-persister/python/monasca-persister.conf /etc/init/monasca-persister.conf

    sudo chown root:root /etc/init/monasca-persister.conf

    sudo chmod 0744 /etc/init/monasca-persister.conf

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-persister/persister-config.yml /etc/monasca/persister-config.yml

    sudo chown mon-persister:monasca /etc/monasca/persister-config.yml

    sudo chmod 0660 /etc/monasca/persister-config.yml

    sudo start monasca-persister || sudo restart monasca-persister

}

function clean_monasca_persister_java {

    echo_summary "Clean Monasca monasca_persister_java"

    (cd /opt/stack/monasca-persister ; sudo mvn clean)

    sudo rm /etc/init/monasca-persister.conf

    sudo rm /etc/monasca/persister-config.yml

    sudo rm -rf /var/log/monasca/persister

    sudo rm /opt/monasca/monasca-persister.jar

    sudo rm /var/log/upstart/monasca-persister.log*

    sudo userdel mon-persister
}

function clean_monasca_persister_python {

    echo_summary "Clean Monasca monasca_persister_python"

    sudo rm /etc/init/monasca-persister.conf

    sudo rm /etc/monasca/persister.conf

    sudo rm /etc/monasca/persister-config.yml

    sudo rm -rf /var/log/monasca/persister

    sudo rm /var/log/upstart/monasca-persister.log*

    sudo rm -rf /opt/monasca-persister

    sudo userdel mon-persister
}

function install_monasca_notification {

    echo_summary "Install Monasca monasca_notification"

    sudo apt-get -y install python-dev
    sudo apt-get -y install build-essential
    sudo apt-get -y install python-mysqldb
    sudo apt-get -y install libmysqlclient-dev

    if [[ ! -d /opt/stack/monasca-notification ]]; then

        sudo git clone https://git.openstack.org/openstack/monasca-notification /opt/stack/monasca-notification

    fi

    (cd /opt/stack/monasca-notification ; sudo python setup.py sdist)

    MONASCA_NOTIFICATION_SRC_DIST=$(ls -td /opt/stack/monasca-notification/dist/monasca-notification-*.tar.gz | head -1)

    (cd /opt/monasca ; sudo -H ./bin/pip  install --pre --allow-all-external --allow-unverified simport  $MONASCA_NOTIFICATION_SRC_DIST)

    sudo useradd --system -g monasca mon-notification || true

    sudo mkdir -p /var/log/monasca/notification || true

    sudo chown root:monasca /var/log/monasca/notification

    sudo chmod 0775 /var/log/monasca/notification

    sudo mkdir -p /etc/monasca || true

    sudo chown root:monasca /etc/monasca

    sudo chmod 0775 /etc/monasca

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-notification/notification.yaml /etc/monasca/notification.yaml

    sudo chown mon-notification:monasca /etc/monasca/notification.yaml

    sudo chmod 0660 /etc/monasca/notification.yaml

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-notification/monasca-notification.conf /etc/init/monasca-notification.conf

    sudo chown root:root /etc/init/monasca-notification.conf

    sudo chmod 0744 /etc/init/monasca-notification.conf

    sudo debconf-set-selections <<< "postfix postfix/mailname string localhost"

    sudo debconf-set-selections <<< "postfix postfix/main_mailer_type string 'Local only'"

    sudo apt-get -y install mailutils

    sudo start monasca-notification || sudo restart monasca-notification

}

function clean_monasca_notification {

    echo_summary "Clean Monasca monasca_notification"

    sudo rm /etc/init/monasca-notification.conf

    sudo rm /etc/monasca/notification.yaml

    sudo rm -rf /var/log/monasca/notification

    sudo userdel mon-notification

    sudo rm -rf /opt/monasca/monasca-notification

    sudo rm /var/log/upstart/monasca-notification.log*

    sudo apt-get -y purge libmysqlclient-dev
    sudo apt-get -y purge python-mysqldb
    sudo apt-get -y purge build-essential
    sudo apt-get -y purge python-dev

    sudo apt-get -y purge mailutils

}

function install_storm {

    echo_summary "Install Monasca Storm"

    sudo curl http://apache.mirrors.tds.net/storm/apache-storm-0.9.5/apache-storm-0.9.5.tar.gz -o /root/apache-storm-0.9.5.tar.gz

    sudo groupadd --system storm || true

    sudo useradd --system -g storm storm || true

    sudo mkdir -p /opt/storm || true

    sudo chown storm:storm /opt/storm

    sudo chmod 0755 /opt/storm

    sudo tar -xzf /root/apache-storm-0.9.5.tar.gz -C /opt/storm

    sudo ln -s /opt/storm/apache-storm-0.9.5 /opt/storm/current

    sudo mkdir /var/storm || true

    sudo chown storm:storm /var/storm

    sudo chmod 0775 /var/storm

    sudo mkdir /var/log/storm || true

    sudo chown storm:storm /var/log/storm

    sudo chmod 0775 /var/log/storm

    sudo ln -s /var/log/storm /opt/storm/current/logs

    sudo cp -f /opt/stack/monasca/devstack/files/storm/cluster.xml /opt/storm/current/logback/cluster.xml

    sudo chown storm:storm /opt/storm/current/logback/cluster.xml

    sudo chmod 0644 /opt/storm/current/logback/cluster.xml

    sudo cp -f /opt/stack/monasca/devstack/files/storm/storm.yaml /opt/storm/apache-storm-0.9.5/conf/storm.yaml

    sudo chown storm:storm /opt/storm/apache-storm-0.9.5/conf/storm.yaml

    sudo chmod 0644 /opt/storm/apache-storm-0.9.5/conf/storm.yaml

    sudo cp -f /opt/stack/monasca/devstack/files/storm/storm-nimbus.conf /etc/init/storm-nimbus.conf

    sudo chown root:root /etc/init/storm-nimbus.conf

    sudo chmod 0644 /etc/init/storm-nimbus.conf

    sudo cp -f /opt/stack/monasca/devstack/files/storm/storm-supervisor.conf /etc/init/storm-supervisor.conf

    sudo chown root:root /etc/init/storm-supervisor.conf

    sudo chmod 0644 /etc/init/storm-supervisor.conf

    sudo start storm-nimbus || sudo restart storm-nimbus

    sudo start storm-supervisor || sudo restart storm-supervisor

}

function clean_storm {

    echo_summary "Clean Monasca Storm"

    sudo rm /etc/init/storm-supervisor.conf

    sudo rm /etc/init/storm-nimbus.conf

    sudo rm /opt/storm/apache-storm-0.9.5/conf/storm.yaml

    sudo rm /opt/storm/current/logback/cluster.xml

    sudo unlink /opt/storm/current/logs

    sudo rm -rf /var/storm

    sudo rm -rf /var/log/storm

    sudo userdel storm || true

    sudo groupdel storm || true

    sudo unlink /opt/storm/current

    sudo rm -rf /opt/storm

    sudo rm /root/apache-storm-0.9.5.tar.gz

}

function install_monasca_thresh {

    echo_summary "Install Monasca monasca_thresh"

    if [[ ! -d /opt/stack/monasca-thresh ]]; then

      sudo git clone https://git.openstack.org/openstack/monasca-thresh.git /opt/stack/monasca-thresh

    fi

    (cd /opt/stack/monasca-thresh/thresh ; sudo mvn clean package -DskipTests)

    sudo cp -f /opt/stack/monasca-thresh/thresh/target/monasca-thresh-1.1.0-SNAPSHOT-shaded.jar /opt/monasca/monasca-thresh.jar

    sudo useradd --system -g monasca mon-thresh

    sudo mkdir -p /etc/monasca || true

    sudo chown root:monasca /etc/monasca

    sudo chmod 0775 /etc/monasca

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-thresh/thresh-config.yml /etc/monasca/thresh-config.yml

    sudo chown root:monasca /etc/monasca/thresh-config.yml

    sudo chmod 0640 /etc/monasca/thresh-config.yml

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-thresh/monasca-thresh /etc/init.d/monasca-thresh

    sudo chown root:root /etc/init.d/monasca-thresh

    sudo chmod 0744 /etc/init.d/monasca-thresh

    sudo service monasca-thresh start || sudo service monasca-thresh restart


}

function clean_monasca_thresh {

    echo_summary "Clean Monasca monasca_thresh"

    (cd /opt/stack/monasca-thresh/thresh ; sudo mvn clean)

    sudo rm /etc/init.d/monasca-thresh

    sudo rm /etc/monasca/thresh-config.yml

    sudo userdel mon-thresh || true

    sudo rm /opt/monasca/monasca-thresh.jar

}

function install_monasca_keystone_client {

    echo_summary "Install Monasca Keystone Client"

    sudo apt-get -y install python-dev

     if [[ ! -d /opt/stack/python-keystoneclient ]]; then

        sudo git clone https://git.openstack.org/openstack/python-keystoneclient /opt/stack/python-keystoneclient

    fi

    (cd /opt/stack/python-keystoneclient ; sudo python setup.py sdist)

    MONASCA_KEYSTONE_SRC_DIST=$(ls -td /opt/stack/python-keystoneclient/dist/python-keystoneclient-*.tar.gz | head -1)

    (cd /opt/monasca ; sudo -H ./bin/pip  install --pre --allow-all-external --allow-unverified simport $MONASCA_KEYSTONE_SRC_DIST)

    sudo cp -f /opt/stack/monasca/devstack/files/keystone/create_monasca_service.py /usr/local/bin/create_monasca_service.py

    sudo chmod 0700 /usr/local/bin/create_monasca_service.py

    sudo /opt/monasca/bin/python /usr/local/bin/create_monasca_service.py

}

function clean_monasca_keystone_client {

    echo_summary "Clean Monasca Keystone Client"

    sudo rm /usr/local/bin/create_monasca_service.py

    sudo apt-get -y purge python-dev

}

function install_monasca_agent {

    echo_summary "Install Monasca monasca_agent"

    sudo apt-get -y install python-dev
    sudo apt-get -y install python-yaml
    sudo apt-get -y install build-essential
    sudo apt-get -y install libxml2-dev
    sudo apt-get -y install libxslt1-dev

    if [[ ! -d /opt/stack/monasca-agent ]]; then

        sudo git clone https://git.openstack.org/openstack/monasca-agent /opt/stack/monasca-agent

    fi

    (cd /opt/stack/monasca-agent ; sudo python setup.py sdist)

    MONASCA_AGENT_SRC_DIST=$(ls -td /opt/stack/monasca-agent/dist/monasca-agent-*.tar.gz | head -1)

    (cd /opt/monasca ; sudo -H ./bin/pip  install --pre --allow-all-external --allow-unverified simport $MONASCA_AGENT_SRC_DIST)

    sudo mkdir -p /etc/monasca/agent/conf.d || true

    sudo chown root:root /etc/monasca/agent/conf.d

    sudo chmod 0755 /etc/monasca/agent/conf.d

    sudo mkdir -p /usr/lib/monasca/agent/custom_checks.d || true

    sudo chown root:root /usr/lib/monasca/agent/custom_checks.d

    sudo chmod 0755 /usr/lib/monasca/agent/custom_checks.d

    sudo mkdir -p /usr/lib/monasca/agent/custom_detect.d || true

    sudo chown root:root /usr/lib/monasca/agent/custom_detect.d

    sudo chmod 0755 /usr/lib/monasca/agent/custom_detect.d

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-agent/host_alive.yaml /etc/monasca/agent/conf.d/host_alive.yaml

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-agent/monasca-reconfigure /usr/local/bin/monasca-reconfigure

    sudo chown root:root /usr/local/bin/monasca-reconfigure

    sudo chmod 0750 /usr/local/bin/monasca-reconfigure

    sudo /usr/local/bin/monasca-reconfigure

    sudo service monasca-agent start || sudo service monasca-agent restart

}

function clean_monasca_agent {

    echo_summary "Clean Monasca monasca_agent"

    sudo rm /etc/init.d/monasca-agent

    sudo rm /usr/local/bin/monasca-reconfigure

    sudo rm /etc/monasca/agent/conf.d/host_alive.yaml

    sudo chown root:root /etc/monasca/agent/conf.d/host_alive.yaml

    chmod 0644 /etc/monasca/agent/conf.d/host_alive.yaml

    sudo rm -rf /usr/lib/monasca/agent/custom_detect.d

    sudo rm -rf  /usr/lib/monasca/agent/custom_checks.d

    sudo rm -rf /etc/monasca/agent/conf.d

    sudo apt-get -y purge libxslt1-dev
    sudo apt-get -y purge libxml2-dev
    sudo apt-get -y purge build-essential
    sudo apt-get -y purge python-yaml
    sudo apt-get -y purge python-dev

}

function install_monasca_smoke_test {

    echo_summary "Install Monasca Smoke Test"

    (cd /opt/monasca ; sudo -H ./bin/pip install mySQL-python)

    sudo curl -L https://api.github.com/repos/hpcloud-mon/monasca-ci/tarball/master -o /opt/monasca/monasca-ci.tar.gz

    sudo tar -xzf /opt/monasca/monasca-ci.tar.gz -C /opt/monasca

    HPCLOUD_MON_MONASCA_CI_DIR=$(ls -td /opt/monasca/hpcloud-mon-monasca-ci-* | head -1)

    sudo sed -i s/192\.168\.10\.4/127\.0\.0\.1/g ${HPCLOUD_MON_MONASCA_CI_DIR}/tests/smoke/utils.py
    sudo sed -i s/192\.168\.10\.5/127\.0\.0\.1/g ${HPCLOUD_MON_MONASCA_CI_DIR}/tests/smoke/utils.py
    sudo sed -i "s/'hostname', '-f'/'hostname'/g" ${HPCLOUD_MON_MONASCA_CI_DIR}/tests/smoke/smoke_configs.py

    (cd /opt/monasca ; sudo -H ./bin/pip install influxdb)

    sudo cp -f /opt/stack/monasca/devstack/files/monasca-smoke-test/smoke2_configs.py ${HPCLOUD_MON_MONASCA_CI_DIR}/tests/smoke/smoke2_configs.py

    sudo /opt/monasca/bin/python ${HPCLOUD_MON_MONASCA_CI_DIR}/tests/smoke/smoke2.py || true

    (cd /opt/monasca ; LC_ALL=en_US.UTF-8 LANG=en_US.UTF-8 OS_USERNAME=test OS_PASSWORD=password OS_PROJECT_NAME=test OS_AUTH_URL=http://127.0.0.1:35357/v3 bash -c "sudo /opt/monasca/bin/python ${HPCLOUD_MON_MONASCA_CI_DIR}/tests/smoke/smoke.py")
}

function clean_monasca_smoke_test {

    echo_summary "Clean Monasca Smoke Test"

    sudo rm  /opt/monasca/monasca-ci.tar.gz

    sudo rm -rf /opt/monasca/hpcloud-mon-monasca-ci-7a45d29
}


function install_monasca_default_alarms {
:

}

function clean_monasca_default_alarms {
:

}

function install_monasca_horizon_ui {

    echo_summary "Install Monasca Horizon UI"

    sudo mkdir -p /opt/monasca-horizon-ui || true

    (cd /opt/monasca-horizon-ui ; sudo virtualenv .)

    (cd /opt/monasca-horizon-ui ; sudo -H ./bin/pip  install --pre --allow-all-external --allow-unverified simport monasca-ui)

    sudo ln -s /opt/monasca-horizon-ui/lib/python2.7/site-packages/monitoring/enabled/_50_admin_add_monitoring_panel.py /opt/stack/horizon/openstack_dashboard/local/enabled/_50_admin_add_monitoring_panel.py

    sudo ln -s opt/monasca-horizon-ui/lib/python2.7/site-packages/monitoring/static/monitoring /opt/stack/horizon/monitoring

    sudo python /opt/stack/horizon/manage.py compress --force

    sudo service apache2 restart

}

function clean_monasca_horizon_ui {

    echo_summary "Clean Monasca Horizon UI"

    sudo rm -f /opt/stack/horizon/openstack_dashboard/local/enabled/_50_admin_add_monitoring_panel.py

    sudo rm -f /opt/stack/horizon

    sudo rm -rf /opt/monasca-horizon-ui

}

# Allows this script to be called directly outside of
# the devstack infrastructure code. Uncomment to use.
#if [[ $(type -t is_service_enabled) != 'function' ]]; then
#
#    function is_service_enabled {
#
#        return 0
#
#     }
#fi
#if [[ $(type -t echo_summary) != 'function' ]]; then
#
#    function echo_summary {
#
#        echo "$*"
#
#    }
#
#fi

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

    elif [[ "$1" == "stack" && "$2" == "post-config" ]]; then
        # Configure after the other layer 1 and 2 services have been configured
        echo_summary "Configuring Monasca"
        post_config_monasca

    elif [[ "$1" == "stack" && "$2" == "extra" ]]; then
        # Initialize and start the Monasca service
        echo_summary "Initializing Monasca"
        extra_monasca
    fi

    if [[ "$1" == "unstack" ]]; then
        # Shut down Monasca services
        # no-op
        echo_summary "Unstacking Monasca"
        unstack_monasca
    fi

    if [[ "$1" == "clean" ]]; then
        # Remove state and transient data
        # Remember clean.sh first calls unstack.sh
        # no-op
        echo_summary "Cleaning Monasca"
        clean_monasca
    fi
fi

#Restore errexit
$ERREXIT

# Restore xtrace
$XTRACE
