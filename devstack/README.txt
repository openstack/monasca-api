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

Add the following to the DevStack local.conf file.

# BEGIN DEVSTACK LOCAL.CONF CONTENTS

[[local|localrc]]
ADMIN_PASSWORD=password
DATABASE_PASSWORD=$ADMIN_PASSWORD
RABBIT_PASSWORD=$ADMIN_PASSWORD
SERVICE_PASSWORD=$ADMIN_PASSWORD
SERVICE_TOKEN=$ADMIN_PASSWORD

LOGFILE=$DEST/logs/stack.sh.log
LOGDIR=$DEST/logs
LOG_COLOR=False

# The following two variables allow switching between Java and Python for the implementations
# of the Monasca API and the Monasca Persister. If these variables are not set, then the
# default is to install the Java implementations of both the Monasca API and the Monasca Persister.

# Uncomment one of the following two lines to choose Java or Python for the Monasca API.
MONASCA_API_IMPLEMENTATION_LANG=${MONASCA_API_IMPLEMENTATION_LANG:-java}
#MONASCA_API_IMPLEMENTATION_LANG=${MONASCA_API_IMPLEMENTATION_LANG:-python}

# Uncomment of the following two lines to choose Java or Python for the Monasca Pesister.
MONASCA_PERSISTER_IMPLEMENTATION_LANG=${MONASCA_PERSISTER_IMPLEMENTATION_LANG:-java}
#MONASCA_PERSISTER_IMPLEMENTATION_LANG=${MONASCA_PERSISTER_IMPLEMENTATION_LANG:-python}

# This line will enable all of Monasca.
enable_plugin monasca git://git.openstack.org/openstack/monasca-api

# END DEVSTACK LOCAL.CONF CONTENTS


Using Vagrant:

Vagrant can be used to deploy a VM with Devstack and Monasca running in it
using the Vagrantfile. After installing Vagrant, just "vagrant up".

Known Issues:

1. The smoke tests do not run successfully with the Python implementations.
2. The Python Monasca API has various bugs.
3. The RabbitMQ Check Plugin is not configured correctly.
