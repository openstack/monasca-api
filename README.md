Team and repository tags
========================

[![Team and repository tags](http://governance.openstack.org/badges/monasca-api.svg)](http://governance.openstack.org/reference/tags/index.html)

<!-- Change things from this point on -->

# Overview

`monasca-api` is a RESTful API server that is designed with a layered architecture [layered architecture](http://en.wikipedia.org/wiki/Multilayered_architecture).

The full API Specification can be found in [docs/monasca-api-spec.md](docs/monasca-api-spec.md)

## Java Build

Requires monasca-common from https://git.openstack.org/openstack/monasca-common. First clone this repository and then do mvn install. Then return to monasca-api and:

    $ cd java
    $ mvn clean package

# StackForge Java Build

There is a pom.xml in the base directory that should only be used for the StackForge build. The StackForge build is a rather strange build because of the limitations of the current StackForge java jobs and infrastructure. We have found that the API runs faster if built with maven 3 but the StackForge nodes only have maven 2. This build checks the version of maven and if not maven 3, it downloads a version of maven 3 and uses it. This build depends on jars that are from monasca-common. That StrackForge build uploads the completed jars to http://tarballs.openstack.org/ci/monasca-common, but they are just regular jars, and not in a maven repository and sometimes zuul takes a long time to do the upload. Hence, the first thing the maven build from the base project does is invoke build_common.sh in the common directory. This script clones monasca-common and then invokes maven 3 to build monasca-common in the common directory and install the jars in the local maven repository.

Since this is all rather complex, that part of the build only works on StackForge so follow the simple instruction above if you are building your own monasca-api.

Currently this build is executed on the bare-precise nodes in StackForge and they only have maven 2. So, this build must be kept compatible with Maven 2. If another monasca-common jar is added as a dependency to java/pom.xml, it must also be added to download/download.sh.

Combining monasca-common, monasca-thresh, monasca-api and monasca-persister into one build would vastly simplify the builds but that is a future task.`

## Usage

    $ java -jar target/monasca-api.jar server config-file.yml

## Keystone Configuration

For secure operation of the Monasca API, the API must be configured to use Keystone in the configuration file under the middleware section. Monasca only works with a Keystone v3 server. The important parts of the configuration are explained below:

* serverVIP - This is the hostname or IP Address of the Keystone server
* serverPort - The port for the Keystone server
* useHttps - Whether to use https when making requests of the Keystone API
* truststore - If useHttps is true and the Keystone server is not using a certificate signed by a public CA recognized by Java, the CA certificate can be placed in a truststore so the Monasca API will trust it, otherwise it will reject the https connection. This must be a JKS truststore
* truststorePassword - The password for the above truststore
* connSSLClientAuth - If the Keystone server requires the SSL client used by the Monasca server to have a specific client certificate, this should be true, false otherwise
* keystore - The keystore holding the SSL Client certificate if connSSLClientAuth is true
* keystorePassword - The password for the keystore
* defaultAuthorizedRoles - An array of roles that authorize a user to access the complete Monasca API. User must have at least one of these roles. See below
* readOnlyAuthorizedRoles - An array of roles that authorize a user to only GET (but not POST, PUT...) metrics.  See Keystone Roles below
* agentAuthorizedRoles - An array of roles that authorize only the posting of metrics.  See Keystone Roles below
* adminAuthMethod - "password" if the Monasca API should adminUser and adminPassword to login to the Keystone server to check the user's token, "token" if the Monasca API should use adminToken
* adminUser - Admin user name
* adminPassword - Admin user password
* adminProjectId - Specify the project ID the api should use to request an admin token. Defaults to the admin user's default project. The adminProjectId option takes precedence over adminProjectName.
* adminProjectName - Specify the project name the api should use to request an admin token. Defaults to the admin user's default project. The adminProjectId option takes precedence over adminProjectName.
* adminToken - A valid admin user token if adminAuthMethod is token
* timeToCacheToken - How long the Monasca API should cache the user's token before checking it again

### Keystone Roles

The Monasca API has two levels of access:
# Full access - user can read/write metrics and Alarm Definitions and Alarms
# Agent access - user can only write metrics

The reason for the "Agent access" level is because the Monasca Agent must be configured to use a Keystone user. Since the user and password are configured onto the all of the systems running the Monasca Agent, this user is most in danger of being compromised. If this user is limited to only writing metrics, then the damage can be limited.

To configure the user to have full access, the user must have a role that is listed in defaultAuthorizedRoles. To configure a user to have only "Agent access", the user must have a role in agentAuthorizedRoles and none of the roles in defaultAuthorizedRoles.

If you want to give users the ability to only view data, configure one or more roles in the readOnlyAuthorizedRoles list.

## Design Overview

### Architectural layers

Requests flow through the following architectural layers from top to bottom:

* Resource
  * Serves as the entrypoint into the service.
  * Responsible for handling web service requests, and performing structural request validation.
* Application
  * Responsible for providing application level implementations for specific use cases.
* Domain
  * Contains the technology agnostic core domain model and domain service definitions.
  * Responsible for upholding invariants and defining state transitions.
* Infrastructure
  * Contains technology specific implementations of domain services.

## Documentation

* API Specification: [/docs/monasca-api-spec.md](/docs/monasca-api-spec.md).


python monasca api implementation
=================================

To install the python api implementation, git clone the source and run the
following command::

    $ sudo python setup.py install

If it installs successfully, you will need to make changes to the following
two files to reflect your system settings, especially where kafka server is
located::

    /etc/monasca/api-config.ini
    /etc/monasca/api-config.conf
    /etc/monasca/api-logging.conf

Once the configurations are modified to match your environment, you can start
up the server by following the following instructions.

To start the server, run the following command:

    Running the server in foreground mode
    $ gunicorn -k eventlet --worker-connections=2000 --backlog=1000 --paste /etc/monasca/api-config.ini

    Running the server as daemons
    $ gunicorn -k eventlet --worker-connections=2000 --backlog=1000 --paste /etc/monasca/api-config.ini -D

To check if the code follows python coding style, run the following command
from the root directory of this project

    $ tox -e pep8

To run all the unit test cases, run the following command from the root
directory of this project

    $ tox -e py27

### Start the Server -- for Apache

To start the server using Apache: create a modwsgi file,
create a modwsgi configuration file, and enable the wsgi module
in Apache.

The modwsgi configuration file may look something like this, and the site will need to be enabled:

```apache
    Listen 8070

    <VirtualHost *:8070>

        WSGIDaemonProcess monasca-api processes=4 threads=1 socket-timeout=120 user=mon-api group=monasca python-path=/usr/local/lib/python2.7/site-packages
        WSGIProcessGroup monasca-api
        WSGIApplicationGroup monasca-api
        WSGIScriptAlias / /usr/local/lib/python2.7/site-packages/monasca_log_api/api/wsgi/monasca_api.py

        WSGIPassAuthorization On

        LogLevel info
        ErrorLog /var/log/monasca-api/wsgi.log
        CustomLog /var/log/monasca-api/wsgi-access.log combined

        <Directory /usr/local/lib/python2.7/site-packages/monasca_log_api>
          Require all granted
        </Directory>

        SetEnv no-gzip 1

    </VirtualHost>

```

The wsgi file may look something like this:

```py

    from monasca_log_api.api import server

    application = server.get_wsgi_app(config_base_path='/etc/monasca')

```

# License

Copyright (c) 2014 Hewlett-Packard Development Company, L.P.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied.
See the License for the specific language governing permissions and
limitations under the License.
