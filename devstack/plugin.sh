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
# enable_plugin monasca git://git.openstack.org/stackforge/monasca-api
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


function pre_install_monasca {
:
}

function install_monasca {

    install_zookeeper

    install_kafka

    install_influxdb

    install_cli_creds

    install_schema



}

function post_config_monasca {
:
}

function extra_monasca {
:
}

function unstack_monasca {

    sudo stop kafka

    sudo stop zookeeper

    sudo /etc/init.d/influxdb stop
}

function clean_monasca {

    unstack_monasca

    clean_schema

    clean_cli_creds

    clean_influxdb

    clean_kafka

    clean_zookeeper

}

function install_zookeeper {

    install_openjdk-7

    sudo apt-get -y install zookeeperd

    sudo cp /opt/stack/monasca/devstack/files/zookeeper/zoo.cfg /etc/zookeeper/conf/zoo.cfg

    sudo cp /opt/stack/monasca/devstack/files/zookeeper/myid /etc/zookeeper/conf/myid

    sudo cp /opt/stack/monasca/devstack/files/zookeeper/environment /etc/zookeeper/conf/environment

    sudo mkdir -p /var/log/zookeeper

    sudo chmod 755 /var/log/zookeeper

    sudo cp /opt/stack/monasca/devstack/files/zookeeper/log4j.properties /etc/zookeeper/conf/log4j.properties

    sudo restart zookeeper

}

function clean_zookeeper {

    clean_openjdk-7

    sudo apt-get -y purge zookeeperd

    sudo rm -rf /etc/zookeeper

    sudo rm -rf  /var/log/zookeeper
}

function install_openjdk-7 {

    sudo apt-get -y install openjdk-7-jre-headless

}

function clean_openjdk-7 {

    sudo apt-get -y purge openjdk-7-jre-headless

}

function install_kafka {

    install_openjdk-7

    sudo curl http://apache.mirrors.tds.net/kafka/0.8.1.1/kafka_2.9.2-0.8.1.1.tgz -o /root/kafka_2.9.2-0.8.1.1.tgz

    sudo groupadd --system kafka

    sudo useradd --system -g kafka kafka

    sudo tar -xzf /root/kafka_2.9.2-0.8.1.1.tgz -C /opt

    sudo ln -s /opt/kafka_2.9.2-0.8.1.1 /opt/kafka

    sudo cp -f /opt/stack/monasca/devstack/files/kafka/kafka-server-start.sh /opt/kafka_2.9.2-0.8.1.1/bin/kafka-server-start.sh

    sudo cp -f /opt/stack/monasca/devstack/files/kafka/kafka.conf /etc/init/kafka.conf

    sudo chown root:root /etc/init/kafka.conf

    sudo chmod 644 /etc/init/kafka.conf

    sudo mkdir -p /var/kafka

    sudo chown kafka:kafka /var/kafka

    sudo chmod 755 /var/kafka

    sudo rm -rf /var/kafka/lost+found

    sudo mkdir -p /var/log/kafka

    sudo chown kafka:kafka /var/log/kafka

    sudo chmod 755 /var/log/kafka

    sudo ln -s /opt/kafka/config /etc/kafka

    sudo cp -f /opt/stack/monasca/devstack/files/kafka/log4j.properties /etc/kafka/log4j.properties

    sudo chown kafka:kafka /etc/kafka/log4j.properties

    sudo chmod 644 /etc/kafka/log4j.properties

    sudo cp -f /opt/stack/monasca/devstack/files/kafka/server.properties /etc/kafka/server.properties

    sudo chown kafka:kafka /etc/kafka/server.properties

    sudo chmod 644 /etc/kafka/server.properties

    sudo start kafka

}

function clean_kafka {

    sudo rm -rf /var/kafka

    sudo rm -rf /var/log/kafka

    sudo rm -rf /etc/kafka

    sudo rm -f /opt/kafka

    sudo rm -rf /etc/init/kafka.conf

    sudo userdel kafka

    sudo groupdel kafka

    sudo rm -rf /opt/kafka_2.9.2-0.8.1.1

    sudo rm -rf /root/kafka_2.9.2-0.8.1.1.tgz

    clean_openjdk-7

}

function install_influxdb {

    sudo mkdir -p /opt/monasca_download_dir

    sudo curl http://s3.amazonaws.com/influxdb/influxdb_0.9.1_amd64.deb -o /opt/monasca_download_dir/influxdb_0.9.1_amd64.deb

    sudo dpkg --skip-same-version -i /opt/monasca_download_dir/influxdb_0.9.1_amd64.deb

    sudo cp -f /opt/stack/monasca/devstack/files/influxdb/influxdb.conf /etc/opt/influxdb/influxdb.conf

    sudo cp -f /opt/stack/monasca/devstack/files/influxdb/influxdb /etc/default/influxdb

    sudo /etc/init.d/influxdb start

    echo "sleep for 5 seconds to let influxdb elect a leader"

    sleep 5s

}

function clean_influxdb {

    sudo rm -f /etc/default/influxdb

    sudo rm -f /etc/opt/influxdb/influxdb.conf

    sudo dpkg --purge influxdb

    sudo rm -rf /opt/influxdb

    sudo rm -f  /opt/monasca_download_dir/influxdb_0.9.1_amd64.deb

    sudo rm -rf /opt/monasca_download_dir

    sudo rm -f /etc/init.d/influxdb
}

function install_cli_creds {

    sudo cp -f /opt/stack/monasca/devstack/files/env.sh /etc/profile.d/monasca_cli.sh

    sudo chown root:root /etc/profile.d/monasca_cli.sh

    sudo chmod 0644 /etc/profile.d/monasca_cli.sh

}

function clean_cli_creds {

    sudo rm -f /etc/profile.d/monasca_cli.sh

}

function install_schema {

    sudo mkdir -p /opt/monasca/sqls

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

    sudo mkdir -p /opt/kafka/logs

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

    sudo echo "drop database winchester;" | mysql -uroot -ppassword

    sudo echo "drop database mon;" | mysql -uroot -ppassword

    sudo rm -f /opt/monasca/sqls/winchester.sql

    sudo rm -f /opt/monasca/sqls/mon.sql

    sudo rm -f /opt/influxdb/influxdb_setup.py

    sudo rm -rf /opt/monasca/sqls

}

# Allows this script to be called directly outside of
# the devstack infrastructure code. Uncomment to use.
#if [[ $(type -t) != 'function' ]]; then
#
#    function is_service_enabled {
#
#        return 0;
#
#     }
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

# Restore xtrace
$XTRACE