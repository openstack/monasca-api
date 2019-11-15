Team and repository tags
========================

.. image:: https://governance.openstack.org/tc/badges/monasca-api.svg
    :target: https://governance.openstack.org/tc/reference/tags/index.html

.. Change things from this point on

Overview
========

``monasca-api`` is a RESTful API server that is designed with a `layered
architecture`_.

Documentation
-------------

The full API Specification can be found in `docs/monasca-api-spec.md`_

Python Monasca API Implementation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To install the python api implementation, git clone the source and run
the following command:

::

   $ sudo python setup.py install

If it installs successfully, you will need to make changes to the
following two files to reflect your system settings, especially where
kafka server is located:

::

   /etc/monasca/api-config.ini
   /etc/monasca/monasca-api.conf
   /etc/monasca/api-logging.conf

Once the configuration files are modified to match your environment, you
can start up the server by following the following instructions.

To start the server, run the following command:

::

   Running the server in foreground mode
   $ gunicorn -k eventlet --worker-connections=2000 --backlog=1000 --paste /etc/monasca/api-config.ini

   Running the server as daemons
   $ gunicorn -k eventlet --worker-connections=2000 --backlog=1000 --paste /etc/monasca/api-config.ini -D

To check if the code follows python coding style, run the following
command from the root directory of this project

::

   $ tox -e pep8

To run all the unit test cases, run the following command from the root
directory of this project

::

   $ tox -e py36

Start the Server - for Apache
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To start the server using Apache: create a modwsgi file, create a
modwsgi configuration file, and enable the wsgi module in Apache.

The modwsgi configuration file may look something like this, and the
site will need to be enabled:

.. code:: apache

       Listen 8070

       <VirtualHost *:8070>

           WSGIDaemonProcess monasca-api processes=4 threads=1 socket-timeout=120 user=mon-api group=monasca python-path=/usr/local/lib/python2.7/site-packages
           WSGIProcessGroup monasca-api
           WSGIApplicationGroup monasca-api
           WSGIScriptAlias / /usr/local/lib/python2.7/site-packages/monasca_api/api/wsgi/monasca_api.py

           WSGIPassAuthorization On

           LogLevel info
           ErrorLog /var/log/monasca-api/wsgi.log
           CustomLog /var/log/monasca-api/wsgi-access.log combined

           <Directory /usr/local/lib/python2.7/site-packages/monasca_api>
             Require all granted
           </Directory>

           SetEnv no-gzip 1

       </VirtualHost>

The wsgi file may look something like this:

.. code:: py


       from monasca_api.api import server

       application = server.get_wsgi_app(config_base_path='/etc/monasca')

Java Implementation
~~~~~~~~~~~~~~~~~~~

Details on usage can be found `here`_

WARNING: The Java implementation of Monasca API is DEPRECATED and will
be removed in future release.

License
=======

Copyright (c) 2014 Hewlett-Packard Development Company, L.P.

Licensed under the Apache License, Version 2.0 (the "License"); you may
not use this file except in compliance with the License. You may obtain
a copy of the License at

::

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

.. _layered architecture: https://en.wikipedia.org/wiki/Multilayered_architecture
.. _docs/monasca-api-spec.md: docs/monasca-api-spec.md
.. _here: /docs/java.md
