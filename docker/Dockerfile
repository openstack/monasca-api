ARG DOCKER_IMAGE=monasca/api
ARG APP_REPO=https://review.opendev.org/openstack/monasca-api

# Branch, tag or git hash to build from.
ARG REPO_VERSION=master
ARG CONSTRAINTS_BRANCH=master

# Extra Python3 dependencies.
# gevent is not in upper constrains and v1.3.6 is not working with
# older greenlet.
ARG EXTRA_DEPS="gunicorn gevent==1.5.0 python-memcached influxdb"

# Always start from `monasca-base` image and use specific tag of it.
ARG BASE_TAG=master
FROM monasca/base:$BASE_TAG

# Environment variables used for our service or wait scripts.
ENV \
    KAFKA_URI=kafka:9092 \
    KAFKA_WAIT_FOR_TOPICS=alarm-state-transitions,metrics \
    KAFKA_LEGACY_CLIENT_ENABLED=false \
    MONASCA_CONTAINER_API_PORT=8070 \
    DATABASE_BACKEND=influxdb \
    INFLUX_HOST=influxdb \
    INFLUX_PORT=8086 \
    INFLUX_USER=mon_api \
    INFLUX_PASSWORD=password \
    INFLUX_DB=mon \
    CASSANDRA_CONTACT_POINTS=cassandra \
    CASSANDRA_PORT=9042 \
    CASSANDRA_KEY_SPACE=monasca \
    CASSANDRA_USER=mon_persister \
    CASSANDRA_PASSWORD=password \
    CASSANDRA_CONNECTION_TIMEOUT=5 \
    MYSQL_HOST=mysql \
    MYSQL_PORT=3306 \
    MYSQL_USER=monapi \
    MYSQL_PASSWORD=password \
    MYSQL_DB=mon \
    MEMCACHED_URI=memcached:11211 \
    DEFAULT_REGION=RegionOne \
    KEYSTONE_IDENTITY_URI=http://keystone:35357 \
    KEYSTONE_AUTH_URI=http://keystone:5000 \
    KEYSTONE_ADMIN_USER=admin \
    KEYSTONE_ADMIN_PASSWORD=secretadmin \
    KEYSTONE_ADMIN_TENANT=admin \
    KEYSTONE_ADMIN_DOMAIN=default \
    KEYSTONE_INSECURE=false \
    GUNICORN_WORKERS=9 \
    GUNICORN_WORKER_CLASS=gevent \
    GUNICORN_WORKER_CONNECTIONS=2000 \
    GUNICORN_BACKLOG=1000 \
    GUNICORN_TIMEOUT=10 \
    ADD_ACCESS_LOG=true \
    ACCESS_LOG_FORMAT="%(asctime)s [%(process)d] gunicorn.access [%(levelname)s] %(message)s" \
    ACCESS_LOG_FIELDS='%(h)s %(l)s %(u)s %(t)s %(r)s %(s)s %(b)s "%(f)s" "%(a)s" %(L)s' \
    LOG_LEVEL_ROOT=WARN \
    LOG_LEVEL_CONSOLE=INFO \
    LOG_LEVEL_ACCESS=INFO \
    STAY_ALIVE_ON_FAILURE=false \
    ENABLE_METRICS_API=true \
    ENABLE_LOGS_API=false

# Copy all neccessary files to proper locations.
COPY api-* /etc/monasca/
COPY monasca-api.conf.j2 /etc/monasca/

# Run here all additionals steps your service need post installation.
# Stay with only one `RUN` and use `&& \` for next steps to don't create
# unnecessary image layers. Clean at the end to conserve space.
#RUN \
#    echo "Some steps to do after main installation." && \
#    echo "Hello when building."

# Expose port for specific service.
EXPOSE ${MONASCA_CONTAINER_API_PORT}

# Implement start script in `start.sh` file.
CMD ["/start.sh"]
