#!/usr/bin/env python
# coding=utf-8

# (C) Copyright 2018 FUJITSU LIMITED
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

"""Health check will returns 0 when service is working properly."""

import logging
import os
import sys
from urllib import request


LOG_LEVEL = logging.getLevelName(os.environ.get('LOG_LEVEL', 'INFO'))
logging.basicConfig(level=LOG_LEVEL)
logger = logging.getLogger(__name__)

API_PORT = os.environ.get('MONASCA_CONTAINER_API_PORT', '8070')
url = "http://localhost:" + API_PORT + "/healthcheck"


def main():
    """Send health check request to health check endpoint of Monasca API."""
    logger.debug('Send health check request to %s', url)
    try:
        request.urlopen(url=url)
    except Exception as ex:
        logger.error('Exception during request handling: ' + repr(ex))
        sys.exit(1)


if __name__ == '__main__':
    main()
