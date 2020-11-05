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

from unittest import mock

from oslotest import base

from monasca_api.common.rest import exceptions
from monasca_api.common.rest import utils


class TestRestUtils(base.BaseTestCase):

    def setUp(self):
        """
        Sets the mock.

        Args:
            self: (todo): write your description
        """
        super(TestRestUtils, self).setUp()
        self.mock_json_patcher = mock.patch('monasca_api.common.rest.utils.json')
        self.mock_json = self.mock_json_patcher.start()

    def tearDown(self):
        """
        Stops the consumer.

        Args:
            self: (todo): write your description
        """
        super(TestRestUtils, self).tearDown()
        self.mock_json_patcher.stop()

    def test_read_body_with_success(self):
        """
        Returns the json payload as a string.

        Args:
            self: (todo): write your description
        """
        self.mock_json.loads.return_value = ""
        payload = mock.Mock()

        utils.read_body(payload)

        self.mock_json.loads.assert_called_once_with(payload.read.return_value)

    def test_read_body_empty_content_in_payload(self):
        """
        Return the payload payload payload of the payload.

        Args:
            self: (todo): write your description
        """
        self.mock_json.loads.return_value = ""
        payload = mock.Mock()
        payload.read.return_value = None

        self.assertIsNone(utils.read_body(payload))

    def test_read_body_json_loads_exception(self):
        """
        Test if the json body exists in the validator.

        Args:
            self: (todo): write your description
        """
        self.mock_json.loads.side_effect = Exception
        payload = mock.Mock()

        self.assertRaises(exceptions.DataConversionException,
                          utils.read_body, payload)

    def test_read_body_unsupported_content_type(self):
        """
        Sets the content body.

        Args:
            self: (todo): write your description
        """
        unsupported_content_type = mock.Mock()

        self.assertRaises(
            exceptions.UnsupportedContentTypeException, utils.read_body, None,
            unsupported_content_type)

    def test_read_body_unreadable_content_error(self):
        """
        Test if the content - disreadable.

        Args:
            self: (todo): write your description
        """
        unreadable_content = mock.Mock()
        unreadable_content.read.side_effect = Exception

        self.assertRaises(
            exceptions.UnreadableContentError,
            utils.read_body, unreadable_content)

    def test_as_json_success(self):
        """
        Test if the json as a json string.

        Args:
            self: (todo): write your description
        """
        data = mock.Mock()

        dumped_json = utils.as_json(data)

        self.assertEqual(dumped_json, self.mock_json.dumps.return_value)

    def test_as_json_with_exception(self):
        """
        : return : meth :.

        Args:
            self: (todo): write your description
        """
        data = mock.Mock()
        self.mock_json.dumps.side_effect = Exception

        self.assertRaises(exceptions.DataConversionException,
                          utils.as_json, data)
