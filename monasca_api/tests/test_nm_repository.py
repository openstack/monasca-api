# Copyright 2015 Cray
# Copyright 2016-2017 FUJITSU LIMITED
# (C) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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

import datetime

import fixtures
from oslo_config import cfg
from oslo_config import fixture as fixture_config
from oslo_db.sqlalchemy.engines import create_engine
from sqlalchemy import delete, MetaData, insert, inspect, bindparam

from monasca_api.common.repositories.sqla import models
from monasca_api.tests import base

CONF = cfg.CONF


class TestNotificationMethodRepoDB(base.BaseTestCase):
    @classmethod
    def setUpClass(cls):
        engine = create_engine('sqlite://')

        qry = open('monasca_api/tests/sqlite_alarm.sql', 'r').read()
        sconn = engine.raw_connection()
        c = sconn.cursor()
        c.executescript(qry)
        sconn.commit()
        c.close()
        cls.engine = engine

        def _fake_engine_from_config(*args, **kw):
            return cls.engine
        cls.fixture = fixtures.MonkeyPatch(
            'sqlalchemy.create_engine', _fake_engine_from_config)
        cls.fixture.setUp()

        metadata = MetaData()
        cls.nm = models.create_nm_model(metadata)
        cls._delete_nm_query = delete(cls.nm)
        cls._insert_nm_query = (insert(cls.nm)
                                .values(
                                    id=bindparam('id'),
                                    tenant_id=bindparam('tenant_id'),
                                    name=bindparam('name'),
                                    type=bindparam('type'),
                                    address=bindparam('address'),
                                    period=bindparam('period'),
                                    created_at=bindparam('created_at'),
                                    updated_at=bindparam('updated_at')))

    @classmethod
    def tearDownClass(cls):
        cls.fixture.cleanUp()
        if hasattr(CONF, 'sql_engine'):
            delattr(CONF, 'sql_engine')

    def setUp(self):
        super(TestNotificationMethodRepoDB, self).setUp()

        self._fixture_config = self.useFixture(
            fixture_config.Config(cfg.CONF))
        self._fixture_config.config(connection='sqlite://',
                                    group='database')

        from monasca_api.common.repositories.sqla import notifications_repository as nr

        self.repo = nr.NotificationsRepository()
        self.created_at = datetime.datetime.now()
        self.updated_at = datetime.datetime.now()
        self.default_nms = [{'id': '123',
                             'tenant_id': '444',
                             'name': 'MyEmail',
                             'type': 'EMAIL',
                             'address': 'a@b',
                             'period': 0,
                             'created_at': self.created_at,
                             'updated_at': self.updated_at},
                            {'id': '124',
                             'tenant_id': '444',
                             'name': 'OtherEmail',
                             'type': 'EMAIL',
                             'address': 'a@b',
                             'period': 0,
                             'created_at': self.created_at,
                             'updated_at': self.updated_at},
                            {'id': '125',
                             'tenant_id': '444',
                             'name': 'AEmail',
                             'type': 'EMAIL',
                             'address': 'a@b',
                             'period': 0,
                             'created_at': self.created_at,
                             'updated_at': self.updated_at}
                            ]

        with self.engine.connect() as conn:
            conn.execute(self._delete_nm_query)
            conn.execute(self._insert_nm_query, self.default_nms)
            # TODO(thuvh) find better solution
            conn.commit()

    def test_fixture_and_setup(self):
        class A(object):
            def __init__(self):
                from sqlalchemy import create_engine
                self.engine = create_engine(None)

        a = A()
        expected_list_tables = ['alarm',
                                'alarm_action',
                                'alarm_definition',
                                'alarm_definition_severity',
                                'alarm_metric',
                                'alarm_state',
                                'metric_definition',
                                'metric_definition_dimensions',
                                'metric_dimension',
                                'notification_method',
                                'notification_method_type',
                                'sub_alarm',
                                'sub_alarm_definition',
                                'sub_alarm_definition_dimension']

        self.assertEqual(self.engine, a.engine)
        inspection = inspect(self.engine)
        self.assertEqual(inspection.get_table_names(), expected_list_tables)

    def test_should_create(self):
        from monasca_api.common.repositories import exceptions
        nmA = self.repo.create_notification('555',
                                            'MyEmail',
                                            'EMAIL',
                                            'a@b',
                                            0)
        nmB = self.repo.list_notification('555', nmA)

        self.assertEqual(nmA, nmB['id'])

        self.assertRaises(exceptions.AlreadyExistsException,
                          self.repo.create_notification,
                          '555',
                          'MyEmail',
                          'EMAIL',
                          'a@b',
                          0)

    def test_should_exists(self):
        from monasca_api.common.repositories import exceptions
        self.assertTrue(self.repo.list_notification("444", "123"))
        self.assertRaises(exceptions.DoesNotExistException,
                          self.repo.list_notification, "444", "1234")
        self.assertRaises(exceptions.DoesNotExistException,
                          self.repo.list_notification, "333", "123")

    def test_should_find_by_id(self):
        nm = self.repo.list_notification('444', '123')
        self.assertEqual(nm['id'], '123')
        self.assertEqual(nm['type'], 'EMAIL')
        self.assertEqual(nm['address'], 'a@b')

    def test_should_find_by_name(self):
        nms = self.repo.find_notification_by_name('444', 'MyEmail')
        self.assertEqual(nms['id'], '123')
        self.assertEqual(nms['tenant_id'], '444')
        self.assertEqual(nms['type'], 'EMAIL')
        self.assertEqual(nms['address'], 'a@b')
        self.assertEqual(nms['period'], 0)
        self.assertEqual(nms['created_at'], self.created_at)
        self.assertEqual(nms['updated_at'], self.updated_at)

        from monasca_api.common.repositories import exceptions
        self.assertRaises(exceptions.DoesNotExistException,
                          self.repo.find_notification_by_name, '444', '222222MyEmail22222')

    def test_should_find(self):
        nms = self.repo.list_notifications('444', None, None, 2)
        self.assertEqual(nms, self.default_nms)
        nms = self.repo.list_notifications('444', None, 3, 1)
        self.assertEqual(nms, [])

    def test_should_find_and_sort(self):
        nms = self.repo.list_notifications('444', ['id'], None, 3)
        self.assertEqual(self.default_nms, nms)

        nms = self.repo.list_notifications('444', ['name'], None, 3)
        expected = sorted(self.default_nms, key=lambda k: k['name'])
        self.assertEqual(expected, nms)

    def test_update(self):
        import copy
        self.repo.update_notification('123', '444', 'Foo', 'EMAIL', 'abc', 0)
        nm = self.repo.list_notification('444', '123')
        new_nm = copy.deepcopy(self.default_nms[0])
        new_nm['name'] = b'Foo'
        new_nm['type'] = b'EMAIL'
        new_nm['address'] = b'abc'
        new_nm['created_at'] = nm['created_at']
        new_nm['updated_at'] = nm['updated_at']
        self.assertEqual(nm, new_nm)
        from monasca_api.common.repositories import exceptions
        self.assertRaises(exceptions.DoesNotExistException,
                          self.repo.update_notification,
                          'no really id',
                          'no really tenant',
                          '',
                          '',
                          '',
                          0)

    def test_should_delete(self):
        from monasca_api.common.repositories import exceptions
        self.repo.delete_notification('444', '123')
        self.assertRaises(exceptions.DoesNotExistException,
                          self.repo.list_notification, '444', '123')
        self.assertRaises(exceptions.DoesNotExistException,
                          self.repo.delete_notification, 'no really tenant', '123')

    def test_should_update_duplicate_with_same_values(self):
        import copy
        self.repo.update_notification('123', '444', 'Foo', 'EMAIL', 'abc', 0)
        self.repo.update_notification('123', '444', 'Foo', 'EMAIL', 'abc', 0)
        nm = self.repo.list_notification('444', '123')
        new_nm = copy.deepcopy(self.default_nms[0])
        new_nm['name'] = b'Foo'
        new_nm['type'] = b'EMAIL'
        new_nm['address'] = b'abc'
        new_nm['created_at'] = nm['created_at']
        new_nm['updated_at'] = nm['updated_at']
        self.assertEqual(nm, new_nm)
