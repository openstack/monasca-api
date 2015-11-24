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

The Monasca DevStack plugin currently only works on Ubuntu 14.04 (Trusty).
More Linux Distributions will be supported as soon as is possible.

Running the Monasca DevStack plugin requires a machine with 10GB of RAM.

Directions for installing an running Devstack can be found here:

    http://docs.openstack.org/developer/devstack/

To run Monasca in DevStack, do the following three steps.

1. Clone the DevStack repo.

    git clone https://git.openstack.org/openstack-dev/devstack

2. Add the following to the DevStack local.conf file in the root of the devstack directory. You may
   need to create the local.conf if it does not already exist.

    # BEGIN DEVSTACK LOCAL.CONF CONTENTS

    [[local|localrc]]
    MYSQL_PASSWORD=secretmysql
    DATABASE_PASSWORD=secretdatabase
    RABBIT_PASSWORD=secretrabbit
    ADMIN_PASSWORD=secretadmin
    SERVICE_PASSWORD=secretservice
    SERVICE_TOKEN=111222333444

    LOGFILE=$DEST/logs/stack.sh.log
    LOGDIR=$DEST/logs
    LOG_COLOR=False

    # The following two variables allow switching between Java and Python for the implementations
    # of the Monasca API and the Monasca Persister. If these variables are not set, then the
    # default is to install the Python implementations of both the Monasca API and the Monasca Persister.

    # Uncomment one of the following two lines to choose Java or Python for the Monasca API.
    MONASCA_API_IMPLEMENTATION_LANG=${MONASCA_API_IMPLEMENTATION_LANG:-java}
    #MONASCA_API_IMPLEMENTATION_LANG=${MONASCA_API_IMPLEMENTATION_LANG:-python}

    # Uncomment of the following two lines to choose Java or Python for the Monasca Pesister.
    MONASCA_PERSISTER_IMPLEMENTATION_LANG=${MONASCA_PERSISTER_IMPLEMENTATION_LANG:-java}
    #MONASCA_PERSISTER_IMPLEMENTATION_LANG=${MONASCA_PERSISTER_IMPLEMENTATION_LANG:-python}

    # This line will enable all of Monasca.
    enable_plugin monasca-api git://git.openstack.org/openstack/monasca-api

    # END DEVSTACK LOCAL.CONF CONTENTS

3.   Run './stack.sh' from the root of the devstack directory.


If you want to run Monasca with the bare mininum of OpenStack components
you can add the following two lines to the local.conf file.

  disable_all_services
  enable_service rabbit mysql key tempest


Using Vagrant:

Vagrant can be used to deploy a VM with Devstack and Monasca running in it
using the Vagrantfile. After installing Vagrant, just "vagrant up".


Known Issues:

1. The Python Monasca API has various bugs.
2. The RabbitMQ Check Plugin is not configured correctly.
