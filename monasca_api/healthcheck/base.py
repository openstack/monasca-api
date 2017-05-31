# Copyright 2017 FUJITSU LIMITED
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

import abc
import collections

import six


class CheckResult(collections.namedtuple('CheckResult', ['healthy', 'message'])):
    """Result for the health check

    healthy - boolean
    message - string
    """


@six.add_metaclass(abc.ABCMeta)
class BaseHealthCheck(object):
    """Abstract class implemented by the monasca-api healthcheck classes"""

    @abc.abstractmethod
    def health_check(self):
        """Evaluate health of given service"""
        raise NotImplementedError  # pragma: no cover
