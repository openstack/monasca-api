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

}

function post_config_monasca {
:
}

function extra_monasca {
:
}

function unstack_monasca {
:
}

function clean_monasca {

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

    sudo stop zookeeper

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

    sudo apt-get -y autoremove

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

    sudo stop kafka

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

# Allows this script to be called directly outside of
# the devstack infrastructure code.
if [[ $(type -t) != 'function' ]]; then

    function is_service_enabled {

        return 0;

     }
fi

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