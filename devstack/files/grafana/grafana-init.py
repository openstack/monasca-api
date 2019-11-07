# coding=utf-8

# (C) Copyright 2017 Hewlett Packard Enterprise Development LP
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

import glob
import json
import logging
import os
import sys
import time

from requests import RequestException
from requests import Session

LOG_LEVEL = logging.getLevelName(os.environ.get('LOG_LEVEL', 'INFO'))
logging.basicConfig(level=LOG_LEVEL)

logger = logging.getLogger(__name__)

GRAFANA_URL = os.environ.get('GRAFANA_URL', 'http://localhost:3000')
GRAFANA_USERNAME = os.environ.get('GRAFANA_USERNAME', 'mini-mon')
GRAFANA_PASSWORD = os.environ.get('GRAFANA_PASSWORD', 'password')
GRAFANA_USERS = [{'user': GRAFANA_USERNAME, 'password': GRAFANA_PASSWORD, 'email': ''}]

DATASOURCE_NAME = os.environ.get('DATASOURCE_NAME', 'monasca')
DATASOURCE_URL = os.environ.get('DATASOURCE_URL', 'http://localhost/metrics')
DATASOURCE_ACCESS_MODE = os.environ.get('DATASOURCE_ACCESS_MODE', 'proxy')
DATASOURCE_AUTH = os.environ.get('DATASOURCE_AUTH', 'Keystone').capitalize()
DATASOURCE_AUTH_TOKEN = os.environ.get('DATASOURCE_AUTH_TOKEN', '')

DASHBOARDS_DIR = os.environ.get('DASHBOARDS_DIR', '/dashboards.d')


def retry(retries=5, delay=2.0, exc_types=(RequestException,)):
    def decorator(func):
        def f_retry(*args, **kwargs):
            for i in range(retries):
                try:
                    return func(*args, **kwargs)
                except exc_types as exc:
                    if i < retries - 1:
                        logger.debug('Caught exception, retrying...',
                                     exc_info=True)
                        time.sleep(delay)
                    else:
                        logger.exception('Failed after %d attempts', retries)
                        if isinstance(exc, RequestException):
                            logger.debug('Response was: %r', exc.response.text)

                        raise
        return f_retry
    return decorator


def create_login_payload():
    if os.environ.get('GRAFANA_USERS'):
        try:
            json.loads(os.environ.get('GRAFANA_USERS'))
        except ValueError:
            print("Invalid type GRAFANA_USERS")
            raise
        grafana_users = json.loads(os.environ.get('GRAFANA_USERS'))
    else:
        grafana_users = GRAFANA_USERS
    return grafana_users


@retry(retries=24, delay=5.0)
def login(session, user):
    r = session.post('{url}/login'.format(url=GRAFANA_URL),
                     json=user,
                     timeout=5)
    r.raise_for_status()


@retry(retries=12, delay=5.0)
def check_initialized(session):
    r = session.get('{url}/api/datasources'.format(url=GRAFANA_URL), timeout=5)
    r.raise_for_status()

    logging.debug('existing datasources = %r', r.json())

    for datasource in r.json():
        if datasource['name'] == DATASOURCE_NAME:
            return True

    return False


def create_datasource_payload():
    payload = {
        'name': DATASOURCE_NAME,
        'url': DATASOURCE_URL,
        'access': DATASOURCE_ACCESS_MODE,
        'isDefault': True,
    }

    if DATASOURCE_AUTH not in ['Keystone', 'Horizon', 'Token']:
        logger.error('Unknown Keystone authentication option: %s',
                     DATASOURCE_AUTH)
        sys.exit(1)

    keystone_auth = False
    if DATASOURCE_AUTH in ['Keystone']:
        keystone_auth = True

    payload.update({
        'monasca': {
            'type': 'monasca-datasource',
            'jsonData': {
                'authMode': DATASOURCE_AUTH,
                'keystoneAuth': keystone_auth,
                'token': DATASOURCE_AUTH_TOKEN,
            }
        }
    }.get(DATASOURCE_NAME, {}))

    logging.debug('payload = %r', payload)

    return payload


def create_dashboard_payload(json_path):
    with open(json_path, 'r') as f:
        dashboard = json.load(f)
        dashboard['id'] = None

        return {
            'dashboard': dashboard,
            'overwrite': False
        }


def main():
    for user in create_login_payload():
        logging.info('Opening a Grafana session...')
        session = Session()
        login(session, user)

        if check_initialized(session):
            logging.info('Grafana has already been initialized, skipping!')
            return

        logging.info('Attempting to add configured datasource...')
        r = session.post('{url}/api/datasources'.format(url=GRAFANA_URL),
                         json=create_datasource_payload())
        logging.debug('Response: %r', r.json())
        r.raise_for_status()

        for path in sorted(glob.glob('{dir}/*.json'.format(dir=DASHBOARDS_DIR))):
            logging.info('Creating dashboard from file: {path}'.format(path=path))
            r = session.post('{url}/api/dashboards/db'.format(url=GRAFANA_URL),
                             json=create_dashboard_payload(path))
            logging.debug('Response: %r', r.json())
            r.raise_for_status()

        logging.info('Ending %r session...', user.get('user'))
        session.get('{url}/logout'.format(url=GRAFANA_URL))

    logging.info('Finished successfully.')


if __name__ == '__main__':
    main()
