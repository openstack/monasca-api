#!/bin/bash

#
# Copyright 2016-2017 FUJITSU LIMITED
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

_XTRACE_MON_LOG=$(set +o | grep xtrace)
set +o xtrace

_ERREXIT_MON_LOG=$(set +o | grep errexit)
set -o errexit

# configuration bits of various services
LOG_PERSISTER_DIR=$DEST/monasca-log-persister
LOG_TRANSFORMER_DIR=$DEST/monasca-log-transformer
LOG_METRICS_DIR=$DEST/monasca-log-metrics
LOG_AGENT_DIR=$DEST/monasca-log-agent

ELASTICSEARCH_DIR=$DEST/elasticsearch
ELASTICSEARCH_CFG_DIR=$ELASTICSEARCH_DIR/config
ELASTICSEARCH_LOG_DIR=$LOGDIR/elasticsearch
ELASTICSEARCH_DATA_DIR=$DATA_DIR/elasticsearch

KIBANA_DIR=$DEST/kibana
KIBANA_CFG_DIR=$KIBANA_DIR/config
KIBANA_DEV_DIR=$DEST/kibana_dev
KIBANA_DEV_NODE_JS_VERSION=${KIBANA_DEV_NODE_JS_VERSION:-"10.15.2"}

LOGSTASH_DIR=$DEST/logstash
LOGSTASH_DATA_DIR=$DEST/logstash-data

ES_SERVICE_BIND_HOST=${ES_SERVICE_BIND_HOST:-${SERVICE_HOST}}
ES_SERVICE_BIND_PORT=${ES_SERVICE_BIND_PORT:-9200}
ES_SERVICE_PUBLISH_HOST=${ES_SERVICE_PUBLISH_HOST:-${SERVICE_HOST}}
ES_SERVICE_PUBLISH_PORT=${ES_SERVICE_PUBLISH_PORT:-9300}

KIBANA_SERVICE_HOST=${KIBANA_SERVICE_HOST:-${SERVICE_HOST}}
KIBANA_SERVICE_PORT=${KIBANA_SERVICE_PORT:-5601}
KIBANA_SERVER_BASE_PATH=${KIBANA_SERVER_BASE_PATH:-"/dashboard/monitoring/logs_proxy"}

# Settings needed for Elasticsearch
# Elasticsearch uses a lot of file descriptors or file handles.
# Increase the limit on the number of open files descriptors for the user running Elasticsearch to 65,536 or higher.
LIMIT_NOFILE=${LIMIT_NOFILE:-65535}
# Elasticsearch uses a mmapfs directory by default to store its indices.
# The default operating system limits on mmap counts is likely to be too low,
# which may result in out of memory exceptions, increase to at least 262144.
VM_MAX_MAP_COUNT=${VM_MAX_MAP_COUNT:-262144}

MONASCA_LOG_API_BASE_URI=https://${MONASCA_API_BASE_URI}/logs


run_process_sleep() {
    local name=$1
    local cmd=$2
    local sleepTime=${3:-1}
    run_process "$name" "$cmd"
    sleep ${sleepTime}
}

is_logstash_required() {
    is_service_enabled monasca-log-persister \
        || is_service_enabled monasca-log-transformer \
        || is_service_enabled monasca-log-metrics \
        || is_service_enabled monasca-log-agent \
        && return 0
}

# TOP_LEVEL functions called from devstack coordinator
###############################################################################
function pre_install_logs_services {
    install_elk
    install_nodejs
    install_gate_config_holder
}

function install_monasca_log {
    configure_nvm
    configure_yarn
    build_kibana_plugin
    install_log_agent
    if $USE_OLD_LOG_API = true; then
        install_old_log_api
    fi
}

function install_elk {
    install_logstash
    install_elasticsearch
    install_kibana
}

function install_gate_config_holder {
    sudo install -d -o $STACK_USER $GATE_CONFIGURATION_DIR
}

function install_monasca_statsd {
    if use_library_from_git "monasca-statsd"; then
        git_clone_by_name "monasca-statsd"
        setup_dev_lib "monasca-statsd"
    fi
}

function configure_monasca_log {
    configure_kafka
    configure_elasticsearch
    configure_kibana
    install_kibana_plugin
    if $USE_OLD_LOG_API = true; then
        configure_old_monasca_log_api
    fi
    configure_monasca_log_api
    configure_monasca_log_transformer
    configure_monasca_log_metrics
    configure_monasca_log_persister
    configure_monasca_log_agent


}

function init_monasca_log {
    enable_log_management
}

function init_monasca_grafana_dashboards {
    if is_service_enabled horizon; then
        echo_summary "Init Grafana dashboards"

        sudo python "${PLUGIN_FILES}"/grafana/grafana.py "${PLUGIN_FILES}"/grafana/dashboards.d
    fi
}

function install_old_log_api {

    if python3_enabled; then
            enable_python3_package monasca-log-api
    fi

    echo_summary "Installing monasca-log-api"

    git_clone $MONASCA_LOG_API_REPO $MONASCA_LOG_API_DIR $MONASCA_LOG_API_BRANCH
    setup_develop $MONASCA_LOG_API_DIR

    install_keystonemiddleware
    install_monasca_statsd

    if [ "$MONASCA_LOG_API_DEPLOY" == "mod_wsgi" ]; then
        install_apache_wsgi
    elif [ "$MONASCA_LOG_API_DEPLOY" == "uwsgi" ]; then
        pip_install uwsgi
    else
        pip_install gunicorn
    fi

    if [ "$MONASCA_LOG_API_DEPLOY" != "gunicorn" ]; then
        if is_ssl_enabled_service "monasca-log-api"; then
            enable_mod_ssl
        fi
    fi

}


function configure_old_monasca_log_api {
    MONASCA_LOG_API_BIN_DIR=$(get_python_exec_prefix)
    MONASCA_LOG_API_WSGI=$MONASCA_LOG_API_BIN_DIR/monasca-log-api-wsgi

    if is_service_enabled monasca-log-api; then
        echo_summary "Configuring monasca-log-api"
        rm -rf $MONASCA_LOG_API_UWSGI_CONF
        install -m 600 $MONASCA_LOG_API_DIR/etc/monasca/log-api-uwsgi.ini $MONASCA_LOG_API_UWSGI_CONF

        write_uwsgi_config "$MONASCA_LOG_API_UWSGI_CONF" "$MONASCA_LOG_API_WSGI" "/logs"

    fi
}

function configure_old_monasca_log_api_core {
    # Put config files in ``$MONASCA_LOG_API_CONF_DIR`` for everyone to find
    sudo install -d -o $STACK_USER $MONASCA_LOG_API_CONF_DIR
    sudo install -m 700 -d -o $STACK_USER $MONASCA_LOG_API_CACHE_DIR
    sudo install -d -o $STACK_USER $MONASCA_LOG_API_LOG_DIR

    # ensure fresh installation of configuration files
    rm -rf $MONASCA_LOG_API_CONF $MONASCA_LOG_API_PASTE $MONASCA_LOG_API_LOGGING_CONF

    $MONASCA_LOG_API_BIN_DIR/oslo-config-generator \
        --config-file $MONASCA_LOG_API_DIR/config-generator/monasca-log-api.conf \
        --output-file /tmp/monasca-log-api.conf

    install -m 600 /tmp/monasca-log-api.conf $MONASCA_LOG_API_CONF && rm -rf /tmp/monasca-log-api.conf
    install -m 600 $MONASCA_LOG_API_DIR/etc/monasca/log-api-paste.ini $MONASCA_LOG_API_PASTE
    install -m 600 $MONASCA_LOG_API_DIR/etc/monasca/log-api-logging.conf $MONASCA_LOG_API_LOGGING_CONF

    # configure monasca-log-api.conf
    iniset "$MONASCA_LOG_API_CONF" DEFAULT log_config_append $MONASCA_LOG_API_LOGGING_CONF
    iniset "$MONASCA_LOG_API_CONF" service region $REGION_NAME

    iniset "$MONASCA_LOG_API_CONF" log_publisher kafka_url $KAFKA_SERVICE_HOST:$KAFKA_SERVICE_PORT
    iniset "$MONASCA_LOG_API_CONF" log_publisher topics log

    iniset "$MONASCA_LOG_API_CONF" kafka_healthcheck kafka_url $KAFKA_SERVICE_HOST:$KAFKA_SERVICE_PORT
    iniset "$MONASCA_LOG_API_CONF" kafka_healthcheck kafka_topics log

    iniset "$MONASCA_LOG_API_CONF" roles_middleware path "/v2.0/log"
    iniset "$MONASCA_LOG_API_CONF" roles_middleware default_roles monasca-user
    iniset "$MONASCA_LOG_API_CONF" roles_middleware agent_roles monasca-agent
    iniset "$MONASCA_LOG_API_CONF" roles_middleware delegate_roles admin

    # configure keystone middleware
    configure_auth_token_middleware "$MONASCA_LOG_API_CONF" "admin" $MONASCA_LOG_API_CACHE_DIR
    iniset "$MONASCA_LOG_API_CONF" keystone_authtoken region_name $REGION_NAME
    iniset "$MONASCA_LOG_API_CONF" keystone_authtoken project_name "admin"
    iniset "$MONASCA_LOG_API_CONF" keystone_authtoken password $ADMIN_PASSWORD

    # insecure
    if is_service_enabled tls-proxy; then
        iniset "$MONASCA_LOG_API_CONF" keystone_authtoken insecure False
    fi

    # configure log-api-paste.ini
    iniset "$MONASCA_LOG_API_PASTE" server:main bind $MONASCA_LOG_API_SERVICE_HOST:$MONASCA_LOG_API_SERVICE_PORT
    iniset "$MONASCA_LOG_API_PASTE" server:main chdir $MONASCA_LOG_API_DIR
    iniset "$MONASCA_LOG_API_PASTE" server:main workers $API_WORKERS
}

function init_agent {
    echo_summary "Init Monasca agent"

    sudo cp -f "${PLUGIN_FILES}"/monasca-agent/http_check.yaml /etc/monasca/agent/conf.d/http_check.yaml
    sudo cp -f "${PLUGIN_FILES}"/monasca-agent/process.yaml /etc/monasca/agent/conf.d/process.yaml
    sudo cp -f "${PLUGIN_FILES}"/monasca-agent/elastic.yaml /etc/monasca/agent/conf.d/elastic.yaml

    sudo sed -i "s/{{IP}}/$(ip -o -4 addr list eth1 | awk '{print $4}' | cut -d/ -f1 | head -1)/" /etc/monasca/agent/conf.d/*.yaml
    sudo sed -i "s/127\.0\.0\.1/$(hostname)/" /etc/monasca/agent/conf.d/*.yaml
    sudo systemctl restart monasca-collector
}

function stop_monasca_log {
    stop_process "monasca-log-agent" || true
    stop_monasca_log_api
    stop_process "monasca-log-metrics" || true
    stop_process "monasca-log-persister" || true
    stop_process "monasca-log-transformer" || true
    stop_process "kibana" || true
    stop_process "elasticsearch" || true
}

function start_monasca_log {
    start_elasticsearch
    start_kibana
    start_monasca_log_transformer
    start_monasca_log_metrics
    start_monasca_log_persister
    if $USE_OLD_LOG_API = true; then
        start_monasca_log_api
    fi
    start_monasca_log_agent
}

function clean_monasca_log {
    clean_monasca_log_agent
    clean_monasca_log_api
    clean_monasca_log_persister
    clean_monasca_log_transformer
    clean_kibana
    clean_elasticsearch
    clean_logstash
    clean_nodejs
    clean_nvm
    clean_yarn
    clean_gate_config_holder
}
###############################################################################

function configure_monasca_log_api {
    if is_service_enabled monasca-log; then
        echo_summary "Configuring monasca-api"
        iniset "$MONASCA_API_CONF" DEFAULT enable_logs_api "true"
        iniset "$MONASCA_API_CONF" kafka logs_topics "log"

        create_log_management_accounts
    fi
}

function install_logstash {
    if is_logstash_required; then
        echo_summary "Installing Logstash ${LOGSTASH_VERSION}"

        local logstash_tarball=logstash-oss-${LOGSTASH_VERSION}.tar.gz
        local logstash_url=https://artifacts.elastic.co/downloads/logstash/${logstash_tarball}

        local logstash_dest
        logstash_dest=`get_extra_file ${logstash_url}`

        tar xzf ${logstash_dest} -C $DEST

        sudo chown -R $STACK_USER $DEST/logstash-${LOGSTASH_VERSION}
        ln -sf $DEST/logstash-${LOGSTASH_VERSION} $LOGSTASH_DIR

        sudo mkdir -p $LOGSTASH_DATA_DIR
        sudo chown $STACK_USER:monasca $LOGSTASH_DATA_DIR
    fi
}

function clean_logstash {
    if is_logstash_required; then
        echo_summary "Cleaning Logstash ${LOGSTASH_VERSION}"

        sudo rm -rf $LOGSTASH_DIR || true
        sudo rm -rf $FILES/logstash-${LOGSTASH_VERSION}.tar.gz ||  true
        sudo rm -rf $DEST/logstash-${LOGSTASH_VERSION} || true
    fi
}

function install_elasticsearch {
    if is_service_enabled elasticsearch; then
        echo_summary "Installing ElasticSearch ${ELASTICSEARCH_VERSION}"

        local es_tarball=elasticsearch-oss-${ELASTICSEARCH_VERSION}-linux-x86_64.tar.gz
        local es_url=https://artifacts.elastic.co/downloads/elasticsearch/${es_tarball}

        local es_dest
        es_dest=`get_extra_file ${es_url}`

        tar xzf ${es_dest} -C $DEST

        sudo chown -R $STACK_USER $DEST/elasticsearch-${ELASTICSEARCH_VERSION}
        ln -sf $DEST/elasticsearch-${ELASTICSEARCH_VERSION} $ELASTICSEARCH_DIR
    fi
}

function configure_elasticsearch {
    if is_service_enabled elasticsearch; then
        echo_summary "Configuring ElasticSearch ${ELASTICSEARCH_VERSION}"

        local templateDir=$ELASTICSEARCH_CFG_DIR/templates

        for dir in $ELASTICSEARCH_LOG_DIR $templateDir $ELASTICSEARCH_DATA_DIR; do
            sudo install -m 755 -d -o $STACK_USER $dir
        done

        sudo cp -f "${PLUGIN_FILES}"/elasticsearch/elasticsearch.yml $ELASTICSEARCH_CFG_DIR/elasticsearch.yml
        sudo chown -R $STACK_USER $ELASTICSEARCH_CFG_DIR/elasticsearch.yml
        sudo chmod 0644 $ELASTICSEARCH_CFG_DIR/elasticsearch.yml

        sudo sed -e "
            s|%ES_SERVICE_BIND_HOST%|$ES_SERVICE_BIND_HOST|g;
            s|%ES_SERVICE_BIND_PORT%|$ES_SERVICE_BIND_PORT|g;
            s|%ES_DATA_DIR%|$ELASTICSEARCH_DATA_DIR|g;
            s|%ES_LOG_DIR%|$ELASTICSEARCH_LOG_DIR|g;
        " -i $ELASTICSEARCH_CFG_DIR/elasticsearch.yml

        ln -sf $ELASTICSEARCH_CFG_DIR/elasticsearch.yml $GATE_CONFIGURATION_DIR/elasticsearch.yml

        echo "[Service]" | sudo tee --append /etc/systemd/system/devstack\@elasticsearch.service > /dev/null
        echo "LimitNOFILE=$LIMIT_NOFILE" | sudo tee --append /etc/systemd/system/devstack\@elasticsearch.service > /dev/null

        echo "vm.max_map_count=$VM_MAX_MAP_COUNT" | sudo tee --append /etc/sysctl.conf > /dev/null
        sudo sysctl -w vm.max_map_count=$VM_MAX_MAP_COUNT
    fi
}

function clean_elasticsearch {
    if is_service_enabled elasticsearch; then
        echo_summary "Cleaning Elasticsearch ${ELASTICSEARCH_VERSION}"

        sudo rm -rf ELASTICSEARCH_DIR || true
        sudo rm -rf ELASTICSEARCH_CFG_DIR || true
        sudo rm -rf ELASTICSEARCH_LOG_DIR || true
        sudo rm -rf ELASTICSEARCH_DATA_DIR || true
        sudo rm -rf $FILES/elasticsearch-${ELASTICSEARCH_VERSION}.tar.gz || true
        sudo rm -rf $DEST/elasticsearch-${ELASTICSEARCH_VERSION} || true
    fi
}

function start_elasticsearch {
    if is_service_enabled elasticsearch; then
        echo_summary "Starting ElasticSearch ${ELASTICSEARCH_VERSION}"
        # 5 extra seconds to ensure that ES started properly
        local esSleepTime=${ELASTICSEARCH_SLEEP_TIME:-5}
        run_process_sleep "elasticsearch" "$ELASTICSEARCH_DIR/bin/elasticsearch -E logger.org.elasticsearch=DEBUG" $esSleepTime
    fi
}

function _get_kibana_version_name {
    echo "kibana-${KIBANA_VERSION}-linux-x86_64"
}

function _get_kibana_oss_version_name {
    echo "kibana-oss-${KIBANA_VERSION}-linux-x86_64"
}

function install_kibana {
    if is_service_enabled kibana; then
        echo_summary "Installing Kibana ${KIBANA_VERSION}"

        local kibana_oss_version_name
        kibana_oss_version_name=`_get_kibana_oss_version_name`
        local kibana_tarball=${kibana_oss_version_name}.tar.gz
        local kibana_tarball_url=https://artifacts.elastic.co/downloads/kibana/${kibana_tarball}
        local kibana_tarball_dest
        kibana_tarball_dest=`get_extra_file ${kibana_tarball_url}`

        tar xzf ${kibana_tarball_dest} -C $DEST

        local kibana_version_name
        kibana_version_name=`_get_kibana_version_name`
        sudo chown -R $STACK_USER $DEST/${kibana_version_name}
        ln -sf $DEST/${kibana_version_name} $KIBANA_DIR
    fi
}

function configure_kibana {
    if is_service_enabled kibana; then
        echo_summary "Configuring Kibana ${KIBANA_VERSION}"

        sudo install -m 755 -d -o $STACK_USER $KIBANA_CFG_DIR

        sudo cp -f "${PLUGIN_FILES}"/kibana/kibana.yml $KIBANA_CFG_DIR/kibana.yml
        sudo chown -R $STACK_USER $KIBANA_CFG_DIR/kibana.yml
        sudo chmod 0644 $KIBANA_CFG_DIR/kibana.yml

        sudo sed -e "
            s|%KIBANA_SERVICE_HOST%|$KIBANA_SERVICE_HOST|g;
            s|%KIBANA_SERVICE_PORT%|$KIBANA_SERVICE_PORT|g;
            s|%ES_SERVICE_BIND_HOST%|$ES_SERVICE_BIND_HOST|g;
            s|%ES_SERVICE_BIND_PORT%|$ES_SERVICE_BIND_PORT|g;
            s|%KIBANA_SERVER_BASE_PATH%|$KIBANA_SERVER_BASE_PATH|g;
            s|%KEYSTONE_AUTH_URI%|$KEYSTONE_AUTH_URI|g;
        " -i $KIBANA_CFG_DIR/kibana.yml

        ln -sf $KIBANA_CFG_DIR/kibana.yml $GATE_CONFIGURATION_DIR/kibana.yml
    fi
}

function clean_kibana {
    if is_service_enabled kibana; then
        echo_summary "Cleaning Kibana ${KIBANA_VERSION}"

        local kibana_tarball
        kibana_tarball=`_get_kibana_oss_version_name`.tar.gz
        sudo rm -rf $KIBANA_DIR || true
        sudo rm -rf $FILES/${kibana_tarball} || true
        sudo rm -rf $KIBANA_CFG_DIR || true
    fi
}

function start_kibana {
    if is_service_enabled kibana; then
        echo_summary "Starting Kibana ${KIBANA_VERSION}"
        local kibanaSleepTime=${KIBANA_SLEEP_TIME:-120}     # kibana takes some time to load up
        local kibanaCFG="$KIBANA_CFG_DIR/kibana.yml"
        run_process_sleep "kibana" "$KIBANA_DIR/bin/kibana --config $kibanaCFG" $kibanaSleepTime
    fi
}

function configure_nvm {
    if is_service_enabled kibana; then
        echo_summary "Configuring NVM"
        curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.34.0/install.sh | bash
        source ~/.nvm/nvm.sh
        nvm install $KIBANA_DEV_NODE_JS_VERSION
        nvm use $KIBANA_DEV_NODE_JS_VERSION
    fi
}

function configure_yarn {
    if is_service_enabled kibana; then
        echo_summary "Configuring Yarn"
        REPOS_UPDATED=False
        curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | sudo apt-key add -
        echo "deb https://dl.yarnpkg.com/debian/ stable main" | \
            sudo tee /etc/apt/sources.list.d/yarn.list
        apt_get_update
        apt_get install yarn
    fi
}

function clean_nvm {
    if is_service_enabled kibana; then
        echo_summary "Cleaning NVM"
        rm -rf ~/.nvm
        rm -rf ~/.bower
    fi
}

function clean_yarn {
    if is_service_enabled kibana; then
        echo_summary "Cleaning Yarn"
        apt_get purge yarn
    fi
}

function build_kibana_plugin {
    if is_service_enabled kibana; then
        echo "Building Kibana plugin"

        echo_summary "Cloning and initializing Kibana development environment"

        git clone $KIBANA_DEV_REPO $KIBANA_DEV_DIR --branch $KIBANA_DEV_BRANCH --depth 1

        git_clone $MONASCA_KIBANA_PLUGIN_REPO $MONASCA_KIBANA_PLUGIN_DIR $MONASCA_KIBANA_PLUGIN_BRANCH
        cd $MONASCA_KIBANA_PLUGIN_DIR
        git_update_branch $MONASCA_KIBANA_PLUGIN_BRANCH
        cp -r $MONASCA_KIBANA_PLUGIN_DIR "$KIBANA_DEV_DIR/plugins"
        local plugin_dir="$KIBANA_DEV_DIR/plugins/monasca-kibana-plugin"

        yarn --cwd $KIBANA_DEV_DIR kbn bootstrap
        yarn --cwd $plugin_dir build

        local get_version_script="import json; obj = json.load(open('$plugin_dir/package.json')); print obj['version']"
        local monasca_kibana_plugin_version
        monasca_kibana_plugin_version=$(python -c "$get_version_script")
        local pkg="$plugin_dir/build/monasca-kibana-plugin-$monasca_kibana_plugin_version.zip"
        local easyPkg=$DEST/monasca-kibana-plugin.zip
        ln $pkg $easyPkg
        rm -rf $KIBANA_DEV_DIR
    fi
}

function install_kibana_plugin {
    if is_service_enabled kibana; then
        echo_summary "Install Kibana plugin"
        # note(trebskit) that needs to happen after kibana received
        # its configuration otherwise the plugin fails to be installed
        local pkg=file://$DEST/monasca-kibana-plugin.zip
        $KIBANA_DIR/bin/kibana-plugin install $pkg
    fi
}

function configure_monasca_log_persister {
    if is_service_enabled monasca-log-persister; then
        echo_summary "Configuring monasca-log-persister"

        sudo install -m 755 -d -o $STACK_USER $LOG_PERSISTER_DIR

        sudo cp -f "${PLUGIN_FILES}"/monasca-log-persister/persister.conf $LOG_PERSISTER_DIR/persister.conf
        sudo chown $STACK_USER $LOG_PERSISTER_DIR/persister.conf
        sudo chmod 0640 $LOG_PERSISTER_DIR/persister.conf

        sudo sed -e "
            s|%ES_SERVICE_BIND_HOST%|$ES_SERVICE_BIND_HOST|g;
            s|%KAFKA_SERVICE_HOST%|$KAFKA_SERVICE_HOST|g;
            s|%KAFKA_SERVICE_PORT%|$KAFKA_SERVICE_PORT|g;
        " -i $LOG_PERSISTER_DIR/persister.conf

        ln -sf $LOG_PERSISTER_DIR/persister.conf $GATE_CONFIGURATION_DIR/log-persister.conf
    fi
}

function clean_monasca_log_persister {
    if is_service_enabled monasca-log-persister; then
        echo_summary "Cleaning monasca-log-persister"
        sudo rm -rf $LOG_PERSISTER_DIR || true
    fi
}

function start_monasca_log_persister {
    if is_service_enabled monasca-log-persister; then
        echo_summary "Starting monasca-log-persister"
        local logstash="$LOGSTASH_DIR/bin/logstash"
        run_process "monasca-log-persister" "$logstash -f $LOG_PERSISTER_DIR/persister.conf --path.data $LOGSTASH_DATA_DIR/monasca-log-persister"
    fi
}

function configure_monasca_log_transformer {
    if is_service_enabled monasca-log-transformer; then
        echo_summary "Configuring monasca-log-transformer"

        sudo install -m 755 -d -o $STACK_USER $LOG_TRANSFORMER_DIR

        sudo cp -f "${PLUGIN_FILES}"/monasca-log-transformer/transformer.conf $LOG_TRANSFORMER_DIR/transformer.conf
        sudo chown $STACK_USER $LOG_TRANSFORMER_DIR/transformer.conf
        sudo chmod 0640 $LOG_TRANSFORMER_DIR/transformer.conf

        sudo sed -e "
            s|%KAFKA_SERVICE_HOST%|$KAFKA_SERVICE_HOST|g;
            s|%KAFKA_SERVICE_PORT%|$KAFKA_SERVICE_PORT|g;
        " -i $LOG_TRANSFORMER_DIR/transformer.conf

        ln -sf $LOG_TRANSFORMER_DIR/transformer.conf $GATE_CONFIGURATION_DIR/log-transformer.conf
    fi
}

function clean_monasca_log_transformer {
    if is_service_enabled monasca-log-transformer; then
        echo_summary "Cleaning monasca-log-transformer"
        sudo rm -rf $LOG_TRANSFORMER_DIR || true
    fi
}

function start_monasca_log_transformer {
    if is_service_enabled monasca-log-transformer; then
        echo_summary "Starting monasca-log-transformer"
        local logstash="$LOGSTASH_DIR/bin/logstash"
        run_process "monasca-log-transformer" "$logstash -f $LOG_TRANSFORMER_DIR/transformer.conf --path.data $LOGSTASH_DATA_DIR/monasca-log-transformer"
    fi
}

function configure_monasca_log_metrics {
    if is_service_enabled monasca-log-metrics; then
        echo_summary "Configuring monasca-log-metrics"

        sudo install -m 755 -d -o $STACK_USER $LOG_METRICS_DIR

        sudo cp -f "${PLUGIN_FILES}"/monasca-log-metrics/log-metrics.conf $LOG_METRICS_DIR/log-metrics.conf
        sudo chown $STACK_USER $LOG_METRICS_DIR/log-metrics.conf
        sudo chmod 0640 $LOG_METRICS_DIR/log-metrics.conf

        sudo sed -e "
            s|%KAFKA_SERVICE_HOST%|$KAFKA_SERVICE_HOST|g;
            s|%KAFKA_SERVICE_PORT%|$KAFKA_SERVICE_PORT|g;
        " -i $LOG_METRICS_DIR/log-metrics.conf

        ln -sf $LOG_METRICS_DIR/log-metrics.conf $GATE_CONFIGURATION_DIR/log-metrics.conf
    fi
}

function clean_monasca_log_metrics {
    if is_service_enabled monasca-log-metrics; then
        echo_summary "Cleaning monasca-log-metrics"
        sudo rm -rf $LOG_METRICS_DIR || true
    fi
}

function start_monasca_log_metrics {
    if is_service_enabled monasca-log-metrics; then
        echo_summary "Starting monasca-log-metrics"
        local logstash="$LOGSTASH_DIR/bin/logstash"
        run_process "monasca-log-metrics" "$logstash -f $LOG_METRICS_DIR/log-metrics.conf --path.data $LOGSTASH_DATA_DIR/monasca-log-metrics"
    fi
}

function install_log_agent {
    if is_service_enabled monasca-log-agent; then
        echo_summary "Installing monasca-log-agent [logstash-output-monasca-plugin]"

        $LOGSTASH_DIR/bin/logstash-plugin install --version \
            "${LOGSTASH_OUTPUT_MONASCA_VERSION}" logstash-output-monasca_log_api
    fi
}

function configure_monasca_log_agent {
    if is_service_enabled monasca-log-agent; then
        echo_summary "Configuring monasca-log-agent"

        sudo install -m 755 -d -o $STACK_USER $LOG_AGENT_DIR

        sudo cp -f "${PLUGIN_FILES}"/monasca-log-agent/agent.conf $LOG_AGENT_DIR/agent.conf
        sudo chown $STACK_USER $LOG_AGENT_DIR/agent.conf
        sudo chmod 0640 $LOG_AGENT_DIR/agent.conf

        sudo sed -e "
            s|%MONASCA_API_URI_V2%|$MONASCA_API_URI_V2|g;
            s|%KEYSTONE_AUTH_URI%|$KEYSTONE_AUTH_URI_V3|g;
        " -i $LOG_AGENT_DIR/agent.conf

        ln -sf $LOG_AGENT_DIR/agent.conf $GATE_CONFIGURATION_DIR/log-agent.conf

    fi
}

function clean_monasca_log_agent {
    if is_service_enabled monasca-log-agent; then
        echo_summary "Cleaning monasca-log-agent"
        sudo rm -rf $LOG_AGENT_DIR || true
    fi
}


function start_monasca_log_api {
    if is_service_enabled monasca-log-api; then
        echo_summary "Starting monasca-log-api"

        local service_port=$MONASCA_LOG_API_SERVICE_PORT
        local service_protocol=$MONASCA_LOG_API_SERVICE_PROTOCOL
        if is_service_enabled tls-proxy; then
            service_port=$MONASCA_LOG_API_SERVICE_PORT_INT
            service_protocol="http"
        fi
        local service_uri

        if [ "$MONASCA_LOG_API_DEPLOY" == "mod_wsgi" ]; then
            local enabled_site_file
            enabled_site_file=$(apache_site_config_for monasca-log-api)
            service_uri=$service_protocol://$MONASCA_LOG_API_SERVICE_HOST/logs/v3.0
            if [ -f ${enabled_site_file} ]; then
                enable_apache_site monasca-log-api
                restart_apache_server
                tail_log monasca-log-api /var/log/$APACHE_NAME/monasca-log-api.log
            fi
        elif [ "$MONASCA_LOG_API_DEPLOY" == "uwsgi" ]; then
            service_uri=$service_protocol://$MONASCA_LOG_API_SERVICE_HOST/logs/v3.0
            run_process "monasca-log-api" "$MONASCA_LOG_API_BIN_DIR/uwsgi --ini $MONASCA_LOG_API_UWSGI_CONF" ""
        else
            service_uri=$service_protocol://$MONASCA_LOG_API_SERVICE_HOST:$service_port
            run_process "monasca-log-api" "$MONASCA_LOG_API_BIN_DIR/gunicorn --paste $MONASCA_LOG_API_PASTE" ""
        fi

        echo "Waiting for monasca-log-api to start..."
        if ! wait_for_service $SERVICE_TIMEOUT $service_uri; then
            die $LINENO "monasca-log-api did not start"
        fi

        if is_service_enabled tls-proxy; then
            start_tls_proxy monasca-log-api '*' $MONASCA_LOG_API_SERVICE_PORT $MONASCA_LOG_API_SERVICE_HOST $MONASCA_LOG_API_SERVICE_PORT_INT
        fi

        restart_service memcached
    fi
}

function start_monasca_log_agent {
    if is_service_enabled monasca-log-agent; then
        echo_summary "Starting monasca-log-agent"
        local logstash="$LOGSTASH_DIR/bin/logstash"
        run_process "monasca-log-agent" "$logstash -f $LOG_AGENT_DIR/agent.conf --path.data $LOGSTASH_DATA_DIR/monasca-log-agent" "root" "root"
    fi
}

function clean_gate_config_holder {
    sudo rm -rf $GATE_CONFIGURATION_DIR || true
}

function configure_kafka {
    echo_summary "Configuring Kafka topics"
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 \
        --replication-factor 1 --partitions 4 --topic log
    /opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 \
        --replication-factor 1 --partitions 4 --topic transformed-log
}

function delete_kafka_topics {
    echo_summary "Deleting Kafka topics"
        /opt/kafka/bin/kafka-topics.sh --delete --zookeeper localhost:2181 \
                --topic log || true
        /opt/kafka/bin/kafka-topics.sh --delete --zookeeper localhost:2181 \
                --topic transformed-log || true
}

function create_log_management_accounts {
    if is_service_enabled monasca-log; then
        echo_summary "Enable Log Management in Keystone"

        # note(trebskit) following points to Kibana which is bad,
        # but we do not have search-api in monasca-log-api now
        # this code will be removed in future
        local log_search_url="http://$KIBANA_SERVICE_HOST:$KIBANA_SERVICE_PORT/"

        get_or_create_service "logs" "logs" "Monasca Log service"

        if $USE_OLD_LOG_API = true; then
            get_or_create_endpoint \
                "logs" \
                "$REGION_NAME" \
                "$MONASCA_LOG_API_BASE_URI" \
                "$MONASCA_LOG_API_BASE_URI" \
                "$MONASCA_LOG_API_BASE_URI"
        else
            get_or_create_endpoint \
                "logs" \
                "$REGION_NAME" \
                "$MONASCA_API_URI_V2" \
                "$MONASCA_API_URI_V2" \
                "$MONASCA_API_URI_V2"

        fi

        get_or_create_service "logs-search" "logs-search" "Monasca Log search service"
        get_or_create_endpoint \
            "logs-search" \
            "$REGION_NAME" \
            "$log_search_url" \
            "$log_search_url" \
            "$log_search_url"

    fi
}

#Restore errexit
${_ERREXIT_MON_LOG}

# Restore xtrace
${_XTRACE_MON_LOG}
