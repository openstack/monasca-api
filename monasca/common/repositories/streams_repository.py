# Copyright 2015 Hewlett-Packard
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
import abc

import six


@six.add_metaclass(abc.ABCMeta)
class StreamsRepository(object):

    def __init__(self):
        super(StreamsRepository, self).__init__()

    @abc.abstractmethod
    def create_stream_definition(self,
                                 tenant_id,
                                 name,
                                 description,
                                 select,
                                 group_by,
                                 fire_criteria,
                                 expiration,
                                 fire_actions,
                                 expire_actions):
        pass

    @abc.abstractmethod
    def delete_stream_definition(self, tenant_id, stream_definition_id):
        pass

    @abc.abstractmethod
    def get_stream_definition(self, tenant_id, stream_definition_id):
        pass

    @abc.abstractmethod
    def get_stream_definitions(self, tenant_id, name, offset, limit):
        pass
