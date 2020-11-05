# Copyright 2014 Hewlett-Packard
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

from monasca_api.common.messaging import publisher


class FakePublisher(publisher.Publisher):

    def __init__(self, topic):
        """
        Initialize the given topic.

        Args:
            self: (todo): write your description
            topic: (int): write your description
        """
        pass

    def send_message(self, message):
        """
        Send a message.

        Args:
            self: (todo): write your description
            message: (str): write your description
        """
        pass
