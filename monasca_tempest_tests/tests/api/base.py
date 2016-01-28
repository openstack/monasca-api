# (C) Copyright 2015 Hewlett Packard Enterprise Development Company LP
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
from tempest import config
from tempest.common import credentials_factory
import tempest.test
from tempest_lib import exceptions

from monasca_tempest_tests import clients

CONF = config.CONF


class BaseMonascaTest(tempest.test.BaseTestCase):
    """Base test case class for all Monasca API tests."""

    @classmethod
    def skip_checks(cls):
        super(BaseMonascaTest, cls).skip_checks()

    @classmethod
    def resource_setup(cls):
        super(BaseMonascaTest, cls).resource_setup()
        auth_version = CONF.identity.auth_version
        cls.cred_provider = credentials_factory.get_credentials_provider(
            cls.__name__,
            force_tenant_isolation=True,
            identity_version=auth_version)
        credentials = cls.cred_provider.get_creds_by_roles(
            ['monasca-user', 'anotherrole'])
        cls.os = clients.Manager(credentials=credentials)
        cls.monasca_client = cls.os.monasca_client

    @staticmethod
    def cleanup_resources(method, list_of_ids):
        for resource_id in list_of_ids:
            try:
                method(resource_id)
            except exceptions.NotFound:
                pass

    @classmethod
    def resource_cleanup(cls):
        super(BaseMonascaTest, cls).resource_cleanup()
        resp, response_body = cls.monasca_client.list_alarm_definitions()
        if resp.status == 200:
            if 'elements' in response_body:
                elements = response_body['elements']
                for element in elements:
                    id = element['id']
                    cls.monasca_client.delete_alarm_definition(id)

        resp, response_body = cls.monasca_client.list_notification_methods()
        if resp.status == 200:
            if 'elements' in response_body:
                elements = response_body['elements']
                for element in elements:
                    id = element['id']
                    cls.monasca_client.delete_notification_method(id)

        resp, response_body = cls.monasca_client.list_alarms()
        if resp.status == 200:
            if 'elements' in response_body:
                elements = response_body['elements']
                for element in elements:
                    id = element['id']
                    cls.monasca_client.delete_alarm(id)
        cls.cred_provider.clear_creds()
