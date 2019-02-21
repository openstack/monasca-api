============================
Docker image for Monasca API
============================
The Monasca API image is based on the monasca-base image.


Building monasca-base image
===========================
See https://github.com/openstack/monasca-common/tree/master/docker/README.rst


Building Monasca API image
==========================

Example:
  $ ./build_image.sh <repository_version> <upper_constains_branch> <common_version>


Requirements from monasca-base image
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
health_check.py
  This file will be used for checking the status of the Monasca API
  application.


Scripts
~~~~~~~
start.sh
  In this starting script provide all steps that lead to the proper service
  start. Including usage of wait scripts and templating of configuration
  files. You also could provide the ability to allow running container after
  service died for easier debugging.

build_image.sh
  Please read detailed build description inside the script.


Environment variables
~~~~~~~~~~~~~~~~~~~~~
============================== ======================================================================= ==========================================
Variable                       Default                                                                 Description
============================== ======================================================================= ==========================================
KAFKA_URI                      kafka:9092                                                              URI to Apache Kafka (distributed streaming platform)
KAFKA_WAIT_FOR_TOPICS          alarm-state-transitions,metrics                                         The topics where metric-api streams the metric messages and alarm-states
KAFKA_WAIT_RETRIES 	           24                                                                      Number of kafka connect attempts
KAFKA_WAIT_DELAY               5                                                                       Seconds to wait between attempts
MONASCA_CONTAINER_API_PORT     8070                                                                    The port from the metric pipeline endpoint
INFLUX_HOST                    influxdb                                                                The host for influxdb
INFLUX_PORT                    8086                                                                    The port for influxdb
INFLUX_USER                    mon_api                                                                 The influx username
INFLUX_PASSWORD                password                                                                The influx password
INFLUX_DB                      mon                                                                     The influx database name
MYSQL_DB_HOST                  mysql                                                                   The host for MySQL
MYSQL_DB_PORT                  3306                                                                    The port for MySQL
MYSQL_DB_USERNAME              monapi                                                                  The MySQL username
MYSQL_DB_PASSWORD              password                                                                The MySQL password
MYSQL_DB_DATABASE              mon                                                                     The MySQL database name
MYSQL_WAIT_RETRIES             24                                                                      Number of MySQL connection attempts
MYSQL_WAIT_DELAY               5                                                                       Seconds to wait between attempts
API_MYSQL_DISABLED             unset                                                                   If 'true' do not use a mysql database. Only metric API will work
MEMCACHED_URI                  memcached:11211                                                         URI to Keystone authentication cache
DEFAULT_REGION                 RegionOne                                                               Region that API is running in
AUTHORIZED_ROLES               admin,domainuser,domainadmin,monasca-user                               Roles for Monasca users (full API access)
AGENT_AUTHORIZED_ROLES         monasca-agent                                                           Roles for Monasca agents (sending data only)
READ_ONLY_AUTHORIZED_ROLES     monasca-read-only-user                                                  Roles for read only users
DELEGATE_AUTHORIZED_ROLES      admin                                                                   Roles allow to read/write cross tenant ID
KEYSTONE_IDENTITY_URI          http://keystone:35357                                                   URI to Keystone admin endpoint
KEYSTONE_AUTH_URI              http://keystone:5000                                                    URI to Keystone public endpoint
KEYSTONE_ADMIN_USER            admin                                                                   OpenStack administrator user name
KEYSTONE_ADMIN_PASSWORD        secretadmin                                                             OpenStack administrator user password
KEYSTONE_ADMIN_TENANT          admin                                                                   OpenStack administrator tenant name
KEYSTONE_ADMIN_DOMAIN          default                                                                 OpenStack administrator domain
KEYSTONE_INSECURE              false                                                                   Allow insecure Keystone connection
KEYSTONE_REGION_NAME           undefined                                                               Keystone admin account region
GUNICORN_WORKERS               9                                                                       Number of gunicorn (WSGI-HTTP server) workers
GUNICORN_WORKER_CLASS          gevent                                                                  Used gunicorn worker class
GUNICORN_WORKER_CONNECTIONS    2000                                                                    Number of gunicorn worker connections
GUNICORN_BACKLOG               1000                                                                    Number of gunicorn backlogs
GUNICORN_TIMEOUT               10                                                                      Gunicorn connection timeout
ADD_ACCESS_LOG                 false                                                                   Enable gunicorn request/access logging
ACCESS_LOG_FORMAT              "%(asctime)s [%(process)d] gunicorn.access [%(levelname)s] %(message)s" Define the logging format
ACCESS_LOG_FIELDS              '%(h)s %(l)s %(u)s %(t)s %(r)s %(s)s %(b)s "%(f)s" "%(a)s" %(L)s'       Define the fields to be logged
LOG_LEVEL_ROOT                 WARN                                                                    Log level for root logging
LOG_LEVEL_CONSOLE              INFO                                                                    Log level for console logging
LOG_LEVEL_ACCESS               INFO                                                                    Log level for access logging
STAY_ALIVE_ON_FAILURE          false                                                                   If true, container runs 2 hours after service fail
============================== ======================================================================= ==========================================


Provide Configuration templates
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
* monasca-api.conf.j2
* api-config.ini.j2
* api-logging.conf.j2


Links
~~~~~
https://docs.openstack.org/monasca-api/latest/

https://github.com/openstack/monasca-api/blob/master/README.rst
