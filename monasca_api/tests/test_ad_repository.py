# Copyright 2015 Cray
# Copyright 2016-2017 FUJITSU LIMITED
# (C) Copyright 2016-2017 Hewlett Packard Enterprise Development LP
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
from oslo_db.sqlalchemy.engines import create_engine
from sqlalchemy import delete, MetaData, insert, bindparam, select, func

from monasca_api.common.repositories import exceptions
from monasca_api.common.repositories.model import sub_alarm_definition
from monasca_api.common.repositories.sqla import models
from monasca_api.expression_parser import alarm_expr_parser
from monasca_api.tests import base

CONF = cfg.CONF
ALARM_DEF_123_FIELDS = {'actions_enabled': False,
                        'alarm_actions': u'29387234,77778687',
                        'description': None,
                        'expression': u'AVG(hpcs.compute{flavor_id=777, '
                        'image_id=888, metric_name=cpu, device=1},deterministic) > 10',
                        'id': u'123',
                        'match_by': u'flavor_id,image_id',
                        'name': u'90% CPU',
                        'ok_actions': None,
                        'severity': u'LOW',
                        'undetermined_actions': None}
TENANT_ID = 'bob'


class TestAlarmDefinitionRepoDB(base.BaseTestCase):

    @classmethod
    def setUpClass(cls):
        """
        Create an engine engine.

        Args:
            cls: (todo): write your description
        """
        engine = create_engine('sqlite://')
        qry = open('monasca_api/tests/sqlite_alarm.sql', 'r').read()
        sconn = engine.raw_connection()
        c = sconn.cursor()
        c.executescript(qry)
        sconn.commit()
        c.close()
        cls.engine = engine

        def _fake_engine_from_config(*args, **kw):
            """
            Return an engine engine instance from the engine.

            Args:
                kw: (todo): write your description
            """
            return cls.engine

        cls.fixture = fixtures.MonkeyPatch(
            'sqlalchemy.create_engine', _fake_engine_from_config)
        cls.fixture.setUp()
        metadata = MetaData()

        cls.aa = models.create_aa_model(metadata)
        cls._delete_aa_query = delete(cls.aa)
        cls._insert_aa_query = (insert(cls.aa).values(
            alarm_definition_id=bindparam('alarm_definition_id'),
            alarm_state=bindparam('alarm_state'),
            action_id=bindparam('action_id')))

        cls.ad = models.create_ad_model(metadata)
        cls._delete_ad_query = delete(cls.ad)
        cls._insert_ad_query = (insert(cls.ad).values(
            id=bindparam('id'),
            tenant_id=bindparam('tenant_id'),
            name=bindparam('name'),
            severity=bindparam('severity'),
            expression=bindparam('expression'),
            match_by=bindparam('match_by'),
            actions_enabled=bindparam('actions_enabled'),
            created_at=bindparam('created_at'),
            updated_at=bindparam('updated_at'),
            deleted_at=bindparam('deleted_at')))

        cls.sad = models.create_sad_model(metadata)
        cls._delete_sad_query = delete(cls.sad)
        cls._insert_sad_query = (insert(cls.sad).values(
            id=bindparam('id'),
            alarm_definition_id=bindparam('alarm_definition_id'),
            function=bindparam('function'),
            metric_name=bindparam('metric_name'),
            operator=bindparam('operator'),
            threshold=bindparam('threshold'),
            period=bindparam('period'),
            periods=bindparam('periods'),
            is_deterministic=bindparam('is_deterministic'),
            created_at=bindparam('created_at'),
            updated_at=bindparam('updated_at')))

        cls.sadd = models.create_sadd_model(metadata)
        cls._delete_sadd_query = delete(cls.sadd)
        cls._insert_sadd_query = (insert(cls.sadd).values(
            sub_alarm_definition_id=bindparam('sub_alarm_definition_id'),
            dimension_name=bindparam('dimension_name'),
            value=bindparam('value')))

        cls.nm = models.create_nm_model(metadata)
        cls._delete_nm_query = delete(cls.nm)
        cls._insert_nm_query = (insert(cls.nm).values(
            id=bindparam('id'),
            tenant_id=bindparam('tenant_id'),
            name=bindparam('name'),
            type=bindparam('type'),
            address=bindparam('address'),
            created_at=bindparam('created_at'),
            updated_at=bindparam('updated_at')))

    @classmethod
    def tearDownClass(cls):
        """
        Tear wrapper for the class.

        Args:
            cls: (todo): write your description
        """
        cls.fixture.cleanUp()
        if hasattr(CONF, 'sql_engine'):
            delattr(CONF, 'sql_engine')

    def setUp(self):
        """
        Sets the default config

        Args:
            self: (todo): write your description
        """
        super(TestAlarmDefinitionRepoDB, self).setUp()
        self.conf_default(connection='sqlite://', group='database')

        from monasca_api.common.repositories.sqla import alarm_definitions_repository as adr
        self.repo = adr.AlarmDefinitionsRepository()
        self.default_ads = [{'id': '123',
                             'tenant_id': 'bob',
                             'name': '90% CPU',
                             'severity': 'LOW',
                             'expression': 'AVG(hpcs.compute{flavor_id=777,'
                             ' image_id=888, metric_name=cpu, device=1},deterministic) > 10',
                             'match_by': 'flavor_id,image_id',
                             'actions_enabled': False,
                             'created_at': datetime.datetime.now(),
                             'updated_at': datetime.datetime.now(),
                             'deleted_at': None},
                            {'id': '234',
                             'tenant_id': 'bob',
                             'name': '50% CPU',
                             'severity': 'LOW',
                             'expression': 'AVG(hpcs.compute{flavor_id=777, '
                             'image_id=888, metric_name=mem}) > 20'
                             ' and AVG(hpcs.compute) < 100',
                             'match_by': 'flavor_id,image_id',
                             'actions_enabled': False,
                             'created_at': datetime.datetime.now(),
                             'updated_at': datetime.datetime.now(),
                             'deleted_at': None}]

        self.default_sads = [{'id': '111',
                              'alarm_definition_id': '123',
                              'function': 'AVG',
                              'metric_name': 'hpcs.compute',
                              'operator': 'GT',
                              'threshold': 10,
                              'period': 60,
                              'periods': 1,
                              'is_deterministic': True,
                              'created_at': datetime.datetime.now(),
                              'updated_at': datetime.datetime.now()},
                             {'id': '222',
                              'alarm_definition_id': '234',
                              'function': 'AVG',
                              'metric_name': 'hpcs.compute',
                              'operator': 'GT',
                              'threshold': 20,
                              'period': 60,
                              'periods': 1,
                              'is_deterministic': False,
                              'created_at': datetime.datetime.now(),
                              'updated_at': datetime.datetime.now()},
                             {'id': '223',
                              'alarm_definition_id': '234',
                              'function': 'AVG',
                              'metric_name': 'hpcs.compute',
                              'operator': 'LT',
                              'threshold': 100,
                              'period': 60,
                              'periods': 1,
                              'is_deterministic': False,
                              'created_at': datetime.datetime.now(),
                              'updated_at': datetime.datetime.now()},
                             ]

        self.default_sadds = [{'sub_alarm_definition_id': '111',
                               'dimension_name': 'flavor_id',
                               'value': '777'},
                              {'sub_alarm_definition_id': '111',
                               'dimension_name': 'image_id',
                               'value': '888'},
                              {'sub_alarm_definition_id': '111',
                               'dimension_name': 'metric_name',
                               'value': 'cpu'},
                              {'sub_alarm_definition_id': '111',
                               'dimension_name': 'device',
                               'value': '1'},
                              {'sub_alarm_definition_id': '222',
                               'dimension_name': 'flavor_id',
                               'value': '777'},
                              {'sub_alarm_definition_id': '222',
                               'dimension_name': 'image_id',
                               'value': '888'},
                              {'sub_alarm_definition_id': '222',
                               'dimension_name': 'metric_name',
                               'value': 'mem'}]

        self.default_nms = [{'id': '29387234',
                             'tenant_id': 'alarm-test',
                             'name': 'MyEmail',
                             'type': 'EMAIL',
                             'address': 'a@b',
                             'created_at': datetime.datetime.now(),
                             'updated_at': datetime.datetime.now()},
                            {'id': '77778687',
                             'tenant_id': 'alarm-test',
                             'name': 'OtherEmail',
                             'type': 'EMAIL',
                             'address': 'a@b',
                             'created_at': datetime.datetime.now(),
                             'updated_at': datetime.datetime.now()}]

        self.default_aas = [{'alarm_definition_id': '123',
                             'alarm_state': 'ALARM',
                             'action_id': '29387234'},
                            {'alarm_definition_id': '123',
                             'alarm_state': 'ALARM',
                             'action_id': '77778687'},
                            {'alarm_definition_id': '234',
                             'alarm_state': 'ALARM',
                             'action_id': '29387234'},
                            {'alarm_definition_id': '234',
                             'alarm_state': 'ALARM',
                             'action_id': '77778687'}]

        with self.engine.begin() as conn:
            conn.execute(self._delete_ad_query)
            conn.execute(self._insert_ad_query, self.default_ads)
            conn.execute(self._delete_sad_query)
            conn.execute(self._insert_sad_query, self.default_sads)
            conn.execute(self._delete_sadd_query)
            conn.execute(self._insert_sadd_query, self.default_sadds)
            conn.execute(self._delete_nm_query)
            conn.execute(self._insert_nm_query, self.default_nms)
            conn.execute(self._delete_aa_query)
            conn.execute(self._insert_aa_query, self.default_aas)

    def test_should_create(self):
        """
        Creates a new alarm.

        Args:
            self: (todo): write your description
        """
        expression = ('AVG(hpcs.compute{flavor_id=777, image_id=888,'
                      ' metric_name=cpu}) > 10')
        description = ''
        match_by = ['flavor_id', 'image_id']
        sub_expr_list = (alarm_expr_parser.AlarmExprParser(expression).sub_expr_list)
        alarm_actions = ['29387234', '77778687']
        alarmA_id = self.repo.create_alarm_definition('555',
                                                      '90% CPU',
                                                      expression,
                                                      sub_expr_list,
                                                      description,
                                                      'LOW',
                                                      match_by,
                                                      alarm_actions,
                                                      None,
                                                      None)

        alarmB = self.repo.get_alarm_definition('555', alarmA_id)

        self.assertEqual(alarmA_id, alarmB['id'])

        query_sad = (select([self.sad.c.id])
                     .select_from(self.sad)
                     .where(self.sad.c.alarm_definition_id == alarmA_id))

        query_sadd = (select([func.count()])
                      .select_from(self.sadd)
                      .where(self.sadd.c.sub_alarm_definition_id == bindparam('id')))

        with self.engine.connect() as conn:
            count_sad = conn.execute(query_sad).fetchall()
            self.assertEqual(len(count_sad), 1)
            count_sadd = conn.execute(query_sadd, id=count_sad[0][0]).fetchone()
            self.assertEqual(count_sadd[0], 3)

    def test_should_try_to_create_with_wrong_alarm_action(self):
        """
        Create an action to create an alarm.

        Args:
            self: (todo): write your description
        """
        expression = ('AVG(hpcs.compute{flavor_id=777, image_id=888,'
                      ' metric_name=cpu}) > 10')
        description = ''
        match_by = ['flavor_id', 'image_id']
        sub_expr_list = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        alarm_actions = ['66666666']
        args = ('555',
                '90% CPU',
                expression,
                sub_expr_list,
                description,
                'LOW',
                match_by,
                alarm_actions,
                None,
                None)
        self.assertRaises(exceptions.InvalidUpdateException,
                          self.repo.create_alarm_definition, *args)

    def test_should_update(self):
        """
        Updates the update.

        Args:
            self: (todo): write your description
        """
        expression = ''.join(['AVG(hpcs.compute{flavor_id=777, image_id=888,',
                              ' metric_name=mem}) > 20 and',
                              ' AVG(hpcs.compute) < 100'])
        description = ''
        match_by = ['flavor_id', 'image_id']
        sub_expr_list = (alarm_expr_parser.AlarmExprParser(expression).sub_expr_list)
        alarm_actions = ['29387234', '77778687']
        self.repo.update_or_patch_alarm_definition('bob', '234',
                                                   '90% CPU', expression,
                                                   sub_expr_list, False,
                                                   description, alarm_actions,
                                                   None, None,
                                                   match_by, 'LOW')
        alarm = self.repo.get_alarm_definition('bob', '234')
        expected = {'actions_enabled': False,
                    'alarm_actions': '29387234,77778687',
                    'description': '',
                    'expression': 'AVG(hpcs.compute{flavor_id=777, '
                    'image_id=888, metric_name=mem}) > 20 and'
                    ' AVG(hpcs.compute) < 100',
                    'id': '234',
                    'match_by': 'flavor_id,image_id',
                    'name': '90% CPU',
                    'ok_actions': None,
                    'severity': 'LOW',
                    'undetermined_actions': None}

        self.assertEqual(alarm, expected)

        sub_alarms = self.repo.get_sub_alarm_definitions('234')

        expected = [{'alarm_definition_id': '234',
                     'dimensions': 'flavor_id=777,image_id=888,'
                     'metric_name=mem',
                     'function': 'AVG',
                     'id': '222',
                     'metric_name': 'hpcs.compute',
                     'operator': 'GT',
                     'period': 60,
                     'periods': 1,
                     'is_deterministic': False,
                     'threshold': 20.0},
                    {'alarm_definition_id': '234',
                     'dimensions': None,
                     'function': 'AVG',
                     'id': '223',
                     'metric_name': 'hpcs.compute',
                     'operator': 'LT',
                     'period': 60,
                     'periods': 1,
                     'is_deterministic': False,
                     'threshold': 100.0}]

        self.assertEqual(len(sub_alarms), len(expected))

        for s, e in zip(sub_alarms, expected):
            e['created_at'] = s['created_at']
            e['updated_at'] = s['updated_at']

        self.assertEqual(sub_alarms, expected)
        am = self.repo.get_alarm_metrics('bob', '234')
        self.assertEqual(am, [])

        sub_alarms = self.repo.get_sub_alarms('bob', '234')
        self.assertEqual(sub_alarms, [])

        ads = self.repo.get_alarm_definitions(
            'bob', '90% CPU', {
                'image_id': '888'}, None, None, 0, 100)
        expected = [{'actions_enabled': False,
                     'alarm_actions': '29387234,77778687',
                     'description': None,
                     'expression': 'AVG(hpcs.compute{flavor_id=777, '
                     'image_id=888, metric_name=cpu, device=1},deterministic) > 10',
                     'id': '123',
                     'match_by': 'flavor_id,image_id',
                     'name': '90% CPU',
                     'ok_actions': None,
                     'severity': 'LOW',
                     'undetermined_actions': None},
                    {'actions_enabled': False,
                     'alarm_actions': '29387234,77778687',
                     'description': '',
                     'expression': 'AVG(hpcs.compute{flavor_id=777, '
                     'image_id=888, metric_name=mem}) > 20 and'
                     ' AVG(hpcs.compute) < 100',
                     'id': '234',
                     'match_by': 'flavor_id,image_id',
                     'name': '90% CPU',
                     'ok_actions': None,
                     'severity': 'LOW',
                     'undetermined_actions': None}]
        self.assertEqual(ads, expected)

        ads = self.repo.get_alarm_definitions(
            'bob', '90% CPU', {
                'image_id': '888'}, 'LOW', None, 0, 100)
        expected = [{'actions_enabled': False,
                     'alarm_actions': '29387234,77778687',
                     'description': None,
                     'expression': 'AVG(hpcs.compute{flavor_id=777, '
                     'image_id=888, metric_name=cpu, device=1},deterministic) > 10',
                     'id': '123',
                     'match_by': 'flavor_id,image_id',
                     'name': '90% CPU',
                     'ok_actions': None,
                     'severity': 'LOW',
                     'undetermined_actions': None},
                    {'actions_enabled': False,
                     'alarm_actions': '29387234,77778687',
                     'description': '',
                     'expression': 'AVG(hpcs.compute{flavor_id=777, '
                     'image_id=888, metric_name=mem}) > 20 and'
                     ' AVG(hpcs.compute) < 100',
                     'id': '234',
                     'match_by': 'flavor_id,image_id',
                     'name': '90% CPU',
                     'ok_actions': None,
                     'severity': 'LOW',
                     'undetermined_actions': None}]
        self.assertEqual(ads, expected)

        ads = self.repo.get_alarm_definitions(
            'bob', '90% CPU', {
                'image_id': '888'}, 'CRITICAL', None, 0, 100)
        expected = []
        self.assertEqual(ads, expected)

        self.repo.update_or_patch_alarm_definition('bob', '234',
                                                   '90% CPU', None,
                                                   sub_expr_list, False,
                                                   description, alarm_actions,
                                                   None, None,
                                                   match_by, 'LOW')

        self.repo.update_or_patch_alarm_definition('bob', '234',
                                                   None, None,
                                                   None, True,
                                                   None, None,
                                                   None, None,
                                                   None, None,
                                                   True)

        self.repo.update_or_patch_alarm_definition('bob', '234',
                                                   None, None,
                                                   None, None,
                                                   None, None,
                                                   None, None,
                                                   None, None,
                                                   True)

        self.repo.update_or_patch_alarm_definition('bob', '234',
                                                   None, None,
                                                   None, None,
                                                   None, [],
                                                   [], [],
                                                   None, None,
                                                   True)

        self.repo.update_or_patch_alarm_definition('bob', '234',
                                                   None, None,
                                                   None, False,
                                                   None, None,
                                                   None, None,
                                                   match_by, None,
                                                   False)

        self.assertRaises(exceptions.InvalidUpdateException,
                          self.repo.update_or_patch_alarm_definition,
                          'bob', '234',
                          None, None,
                          None, False,
                          None, None,
                          None, None,
                          None, None,
                          False)

        self.assertRaises(exceptions.InvalidUpdateException,
                          self.repo.update_or_patch_alarm_definition,
                          'bob', '234',
                          '90% CPU', None,
                          sub_expr_list, False,
                          description, alarm_actions,
                          None, None,
                          'update_match_by', 'LOW')

        self.repo.delete_alarm_definition('bob', '234')

        self.assertRaises(exceptions.DoesNotExistException,
                          self.repo.get_alarm_definition, 'bob', '234')

    def test_should_find_by_id(self):
        """
        Test for a alarm by id.

        Args:
            self: (todo): write your description
        """
        alarmDef1 = self.repo.get_alarm_definition('bob', '123')
        expected = {'actions_enabled': False,
                    'alarm_actions': '29387234,77778687',
                    'description': None,
                    'expression': 'AVG(hpcs.compute{flavor_id=777, '
                    'image_id=888, metric_name=cpu, device=1},deterministic) > 10',
                    'id': '123',
                    'match_by': 'flavor_id,image_id',
                    'name': '90% CPU',
                    'ok_actions': None,
                    'severity': 'LOW',
                    'undetermined_actions': None}
        self.assertEqual(alarmDef1, expected)
        with self.engine.begin() as conn:
            conn.execute(self._delete_aa_query)

        alarmDef2 = self.repo.get_alarm_definition('bob', '123')
        expected['alarm_actions'] = None
        self.assertEqual(alarmDef2, expected)

    def test_shoud_find_sub_alarm_metric_definitions(self):
        """
        Test for alarms of the alarms. cfgarm.

        Args:
            self: (todo): write your description
        """
        sub_alarms = self.repo.get_sub_alarm_definitions('123')

        expected = [{'alarm_definition_id': '123',
                     'dimensions': 'flavor_id=777,image_id=888,'
                     'metric_name=cpu,device=1',
                     'function': 'AVG',
                     'id': '111',
                     'metric_name': 'hpcs.compute',
                     'operator': 'GT',
                     'period': 60,
                     'periods': 1,
                     'is_deterministic': True,
                     'threshold': 10.0}]

        self.assertEqual(len(sub_alarms), len(expected))

        for s, e in zip(sub_alarms, expected):
            e['created_at'] = s['created_at']
            e['updated_at'] = s['updated_at']

        self.assertEqual(sub_alarms, expected)

        sub_alarms = self.repo.get_sub_alarm_definitions('234')

        expected = [{'alarm_definition_id': '234',
                     'dimensions': 'flavor_id=777,image_id=888,metric_name=mem',
                     'function': 'AVG',
                     'id': '222',
                     'metric_name': 'hpcs.compute',
                     'operator': 'GT',
                     'period': 60,
                     'periods': 1,
                     'is_deterministic': False,
                     'threshold': 20.0},
                    {'alarm_definition_id': '234',
                     'dimensions': None,
                     'function': 'AVG',
                     'id': '223',
                     'metric_name': 'hpcs.compute',
                     'operator': 'LT',
                     'period': 60,
                     'periods': 1,
                     'is_deterministic': False,
                     'threshold': 100.0}]

        self.assertEqual(len(sub_alarms), len(expected))

        for s, e in zip(sub_alarms, expected):
            e['created_at'] = s['created_at']
            e['updated_at'] = s['updated_at']

        self.assertEqual(sub_alarms, expected)

        sub_alarms = self.repo.get_sub_alarm_definitions('asdfasdf')
        self.assertEqual(sub_alarms, [])

    def test_try_update_alarm_that_does_not_exist(self):
        """
        Updates an alarm update operation.

        Args:
            self: (todo): write your description
        """
        args = ('koala', '999',
                None, None,
                None, True,
                None, None,
                None, None,
                None, None,
                True)
        self.assertRaises(exceptions.DoesNotExistException,
                          self.repo.update_or_patch_alarm_definition, *args)

    def test_exists(self):
        """
        Check if the alarm exists.

        Args:
            self: (todo): write your description
        """
        alarmDef1 = self.repo.get_alarm_definitions(tenant_id='bob',
                                                    name='90% CPU')
        expected = {'actions_enabled': False,
                    'alarm_actions': '29387234,77778687',
                    'description': None,
                    'expression': 'AVG(hpcs.compute{flavor_id=777, '
                    'image_id=888, metric_name=cpu, device=1},deterministic) > 10',
                    'id': '123',
                    'match_by': 'flavor_id,image_id',
                    'name': '90% CPU',
                    'ok_actions': None,
                    'severity': 'LOW',
                    'undetermined_actions': None}

        self.assertEqual(alarmDef1, [expected])
        alarmDef2 = self.repo.get_alarm_definitions(tenant_id='bob',
                                                    name='999% CPU')
        self.assertEqual(alarmDef2, [])

    def test_should_find(self):
        """
        Find the alarm for alarms.

        Args:
            self: (todo): write your description
        """
        alarmDef1 = self.repo.get_alarm_definitions(tenant_id='bob',
                                                    limit=1)
        expected = [{'actions_enabled': False,
                     'alarm_actions': '29387234,77778687',
                     'description': None,
                     'expression': 'AVG(hpcs.compute{flavor_id=777, '
                     'image_id=888, metric_name=cpu, device=1},deterministic) > 10',
                     'id': '123',
                     'match_by': 'flavor_id,image_id',
                     'name': '90% CPU',
                     'ok_actions': None,
                     'severity': 'LOW',
                     'undetermined_actions': None},
                    {'actions_enabled': False,
                     'alarm_actions': '29387234,77778687',
                     'description': None,
                     'expression': 'AVG(hpcs.compute{flavor_id=777, '
                     'image_id=888, metric_name=mem}) > 20 and '
                     'AVG(hpcs.compute) < 100',
                     'id': '234',
                     'match_by': 'flavor_id,image_id',
                     'name': '50% CPU',
                     'ok_actions': None,
                     'severity': 'LOW',
                     'undetermined_actions': None}]

        self.assertEqual(alarmDef1, expected)
        with self.engine.begin() as conn:
            conn.execute(self._delete_aa_query)

        alarmDef2 = self.repo.get_alarm_definitions(tenant_id='bob',
                                                    limit=1)
        expected[0]['alarm_actions'] = None
        expected[1]['alarm_actions'] = None
        self.assertEqual(alarmDef2, expected)

        alarmDef3 = self.repo.get_alarm_definitions(tenant_id='bill',
                                                    limit=1)
        self.assertEqual(alarmDef3, [])

        alarmDef3 = self.repo.get_alarm_definitions(tenant_id='bill',
                                                    offset='10',
                                                    limit=1)
        self.assertEqual(alarmDef3, [])

    def test_should_find_by_dimension(self):
        """
        This method is used alarms.

        Args:
            self: (todo): write your description
        """
        expected = [{'actions_enabled': False,
                     'alarm_actions': '29387234,77778687',
                     'description': None,
                     'expression': 'AVG(hpcs.compute{flavor_id=777,'
                     ' image_id=888, metric_name=mem}) > 20 '
                     'and AVG(hpcs.compute) < 100',
                     'id': '234',
                     'match_by': 'flavor_id,image_id',
                     'name': '50% CPU',
                     'ok_actions': None,
                     'severity': 'LOW',
                     'undetermined_actions': None}]

        dimensions = {'image_id': '888', 'metric_name': 'mem'}
        alarmDef1 = self.repo.get_alarm_definitions(tenant_id='bob',
                                                    dimensions=dimensions,
                                                    limit=1)
        self.assertEqual(alarmDef1, expected)

        expected = [{'actions_enabled': False,
                     'alarm_actions': '29387234,77778687',
                     'description': None,
                     'expression': 'AVG(hpcs.compute{flavor_id=777, '
                     'image_id=888, metric_name=cpu, device=1},deterministic) > 10',
                     'id': '123',
                     'match_by': 'flavor_id,image_id',
                     'name': '90% CPU',
                     'ok_actions': None,
                     'severity': 'LOW',
                     'undetermined_actions': None},
                    {'actions_enabled': False,
                     'alarm_actions': '29387234,77778687',
                     'description': None,
                     'expression': 'AVG(hpcs.compute{flavor_id=777, '
                     'image_id=888, metric_name=mem}) > 20 and '
                     'AVG(hpcs.compute) < 100',
                     'id': '234',
                     'match_by': 'flavor_id,image_id',
                     'name': '50% CPU',
                     'ok_actions': None,
                     'severity': 'LOW',
                     'undetermined_actions': None}]

        dimensions = {'image_id': '888'}
        alarmDef1 = self.repo.get_alarm_definitions(tenant_id='bob',
                                                    dimensions=dimensions,
                                                    limit=1)
        self.assertEqual(alarmDef1, expected)

        expected = [{'actions_enabled': False,
                     'alarm_actions': '29387234,77778687',
                     'description': None,
                     'expression': 'AVG(hpcs.compute{flavor_id=777, '
                     'image_id=888, metric_name=cpu, device=1},deterministic) > 10',
                     'id': '123',
                     'match_by': 'flavor_id,image_id',
                     'name': '90% CPU',
                     'ok_actions': None,
                     'severity': 'LOW',
                     'undetermined_actions': None}]

        dimensions = {'device': '1'}
        alarmDef1 = self.repo.get_alarm_definitions(tenant_id='bob',
                                                    dimensions=dimensions,
                                                    limit=1)
        self.assertEqual(alarmDef1, expected)

        dimensions = {'Not real': 'AA'}
        alarmDef1 = self.repo.get_alarm_definitions(tenant_id='bob',
                                                    dimensions=dimensions,
                                                    limit=1)
        self.assertEqual(alarmDef1, [])

    def test_should_find_and_sort(self):
        """
        Check for tests and alarms.

        Args:
            self: (todo): write your description
        """
        expected = [{'actions_enabled': False,
                     'alarm_actions': '29387234,77778687',
                     'description': None,
                     'expression': 'AVG(hpcs.compute{flavor_id=777, '
                     'image_id=888, metric_name=cpu, device=1},deterministic) > 10',
                     'id': '123',
                     'match_by': 'flavor_id,image_id',
                     'name': '90% CPU',
                     'ok_actions': None,
                     'severity': 'LOW',
                     'undetermined_actions': None},
                    {'actions_enabled': False,
                     'alarm_actions': '29387234,77778687',
                     'description': None,
                     'expression': 'AVG(hpcs.compute{flavor_id=777, '
                     'image_id=888, metric_name=mem}) > 20 and '
                     'AVG(hpcs.compute) < 100',
                     'id': '234',
                     'match_by': 'flavor_id,image_id',
                     'name': '50% CPU',
                     'ok_actions': None,
                     'severity': 'LOW',
                     'undetermined_actions': None}]

        alarmDef1 = self.repo.get_alarm_definitions(tenant_id='bob',
                                                    sort_by=['id'])
        self.assertEqual(expected, alarmDef1)
        alarmDef2 = self.repo.get_alarm_definitions(tenant_id='bob',
                                                    sort_by=['name'])
        self.assertEqual(expected[::-1], alarmDef2)

    def test_should_delete_by_id(self):
        """
        Deletes the specified alarms.

        Args:
            self: (todo): write your description
        """
        self.repo.delete_alarm_definition('bob', '123')
        from monasca_api.common.repositories import exceptions
        self.assertRaises(exceptions.DoesNotExistException,
                          self.repo.get_alarm_definition,
                          'bob',
                          '123')

        expected = [{'actions_enabled': False,
                     'alarm_actions': '29387234,77778687',
                     'description': None,
                     'expression': 'AVG(hpcs.compute{flavor_id=777, '
                     'image_id=888, metric_name=mem}) > 20 '
                     'and AVG(hpcs.compute) < 100',
                     'id': '234',
                     'match_by': 'flavor_id,image_id',
                     'name': '50% CPU',
                     'ok_actions': None,
                     'severity': 'LOW',
                     'undetermined_actions': None}]

        alarmDef1 = self.repo.get_alarm_definitions(tenant_id='bob',
                                                    limit=1)
        self.assertEqual(alarmDef1, expected)

    def test_try_delete_alarm_that_does_not_exist(self):
        """
        Test if the test is deleted.

        Args:
            self: (todo): write your description
        """
        response = self.repo.delete_alarm_definition('goku', '123')
        self.assertEqual(False, response)

    def test_should_patch_name(self):
        """
        Decorator to test if test_name exists.

        Args:
            self: (todo): write your description
        """
        self.run_patch_test(name=u'90% CPU New')

    def test_should_patch_description(self):
        """
        Decorator to run a test description

        Args:
            self: (todo): write your description
        """
        self.run_patch_test(description=u'New Description')

    def test_should_patch_severity(self):
        """
        Decorator that the test test_shouldity.

        Args:
            self: (todo): write your description
        """
        self.run_patch_test(severity=u'CRITICAL')

    def test_should_patch_actions_enabled(self):
        """
        Determine if a test test test is enabled.

        Args:
            self: (todo): write your description
        """
        self.run_patch_test(actions_enabled=False)

    def test_should_patch_ok_actions(self):
        """
        Determine if - test test test actions.

        Args:
            self: (todo): write your description
        """
        self.run_patch_test(ok_actions=[u'29387234'])

    def test_should_patch_alarm_actions(self):
        """
        Return the actions actions test actions.

        Args:
            self: (todo): write your description
        """
        self.run_patch_test(alarm_actions=[u'29387234'])

    def test_should_patch_undetermined_actions(self):
        """
        Determine whether the test test test test test test actions.

        Args:
            self: (todo): write your description
        """
        self.run_patch_test(undetermined_actions=[u'29387234', u'77778687'])

    def test_should_patch_match_by(self):
        """
        Returns true if the test should be executed.

        Args:
            self: (todo): write your description
        """
        # match_by can't change, so make sure old value works
        self.run_patch_test(match_by=[u'flavor_id', u'image_id'])

    def test_should_patch_expression_no_change(self):
        """
        Determine whether the test test should be skipped.

        Args:
            self: (todo): write your description
        """
        # match_by can't change, so make sure old value works
        self.run_patch_test(expression=ALARM_DEF_123_FIELDS['expression'])

    def test_should_patch_expression_threshold_change(self):
        """
        Determine if the test threshold.

        Args:
            self: (todo): write your description
        """
        self.run_patch_test(expression=ALARM_DEF_123_FIELDS['expression'].replace(' 10', ' 20'))

    def test_should_patch_expression_deterministic_change(self):
        """
        Determine whether the test test should be skipped.

        Args:
            self: (todo): write your description
        """
        self.run_patch_test(expression=ALARM_DEF_123_FIELDS['expression'].replace(',deterministic',
                                                                                  ''))

    def test_should_patch_expression_function_change(self):
        """
        Determine whether the test function should be called.

        Args:
            self: (todo): write your description
        """
        self.run_patch_test(expression=ALARM_DEF_123_FIELDS['expression'].replace('AVG', 'MAX'))

    def test_should_patch_expression_operation_change(self):
        """
        Determine if the test operation should be run in a test.

        Args:
            self: (todo): write your description
        """
        self.run_patch_test(expression=ALARM_DEF_123_FIELDS['expression'].replace('>', '<'))

    def test_should_patch_expression_period_change(self):
        """
        Determine whether the test change should be skipped.

        Args:
            self: (todo): write your description
        """
        self.run_patch_test(expression=ALARM_DEF_123_FIELDS['expression'].replace(')', ', 120)'))

    def test_should_patch_expression_periods_change(self):
        """
        Determine whether the test test_patch.

        Args:
            self: (todo): write your description
        """
        self.run_patch_test(
            expression=ALARM_DEF_123_FIELDS['expression'].replace(
                ' 10', ' 10 times 2'))

    def test_patch_fails_change_match_by(self):
        """
        Test whether the patch has changed.

        Args:
            self: (todo): write your description
        """
        self.assertRaises(
            exceptions.InvalidUpdateException,
            self.run_patch_test,
            match_by=u'device')

    def test_patch_fails_change_metric_name(self):
        """
        Toggle metric name of the metric

        Args:
            self: (todo): write your description
        """
        self.assertRaises(exceptions.InvalidUpdateException, self.run_patch_test,
                          expression=ALARM_DEF_123_FIELDS['expression'].replace('hpcs.compute',
                                                                                'new_metric_name'))

    def test_patch_fails_change_metric_dimensions(self):
        """
        Takes the metric_patch and updates the metric_patch.

        Args:
            self: (todo): write your description
        """
        self.assertRaises(exceptions.InvalidUpdateException, self.run_patch_test,
                          expression=ALARM_DEF_123_FIELDS['expression'].replace('image_id=888',
                                                                                'image_id=42'))

    def test_patch_fails_change_num_sub_expressions(self):
        """
        Set the number of patches after the given number of patches.

        Args:
            self: (todo): write your description
        """
        self.assertRaises(exceptions.InvalidUpdateException, self.run_patch_test,
                          expression=ALARM_DEF_123_FIELDS['expression']
                          .replace(' 10', ' 10 and MAX(cpu.idle_perc) < 10'))

    def run_patch_test(self, name=None, expression=None, description=None, actions_enabled=None,
                       alarm_actions=None, ok_actions=None, undetermined_actions=None,
                       match_by=None, severity=None):
        """
        Run an update test.

        Args:
            self: (todo): write your description
            name: (str): write your description
            expression: (todo): write your description
            description: (str): write your description
            actions_enabled: (str): write your description
            alarm_actions: (todo): write your description
            ok_actions: (todo): write your description
            undetermined_actions: (todo): write your description
            match_by: (todo): write your description
            severity: (todo): write your description
        """
        if expression:
            sub_expr_list = (alarm_expr_parser.AlarmExprParser(expression).sub_expr_list)
        else:
            sub_expr_list = None
        updates = self.repo.update_or_patch_alarm_definition(TENANT_ID, '123',
                                                             name, expression,
                                                             sub_expr_list, actions_enabled,
                                                             description, alarm_actions,
                                                             ok_actions, undetermined_actions,
                                                             match_by, severity,
                                                             patch=True)

        alarm_def_row = \
            (ALARM_DEF_123_FIELDS['id'],
             name if name else ALARM_DEF_123_FIELDS['name'],
             description if description else ALARM_DEF_123_FIELDS['description'],
             expression if expression else ALARM_DEF_123_FIELDS['expression'],
             ALARM_DEF_123_FIELDS['match_by'],  # match-by can't change
             severity if severity else ALARM_DEF_123_FIELDS['severity'],
             actions_enabled if actions_enabled else ALARM_DEF_123_FIELDS['actions_enabled'],
             u','.join(alarm_actions) if alarm_actions else ALARM_DEF_123_FIELDS['alarm_actions'],
             u','.join(ok_actions) if ok_actions else ALARM_DEF_123_FIELDS['ok_actions'],
             (u','.join(undetermined_actions) if undetermined_actions else
              ALARM_DEF_123_FIELDS['undetermined_actions']))

        sad = self.default_sads[0]
        if expression and ALARM_DEF_123_FIELDS['expression'] != expression:
            sub_expr = sub_expr_list[0]
            sub_alarm_def = sub_alarm_definition.SubAlarmDefinition(
                row={'id': '',
                     'alarm_definition_id': sad['alarm_definition_id'],
                     'function': sub_expr.normalized_func,
                     'metric_name': sub_expr.metric_name,
                     'dimensions': u'device=1,image_id=888,flavor_id=777,metric_name=cpu',
                     'operator': sub_expr.normalized_operator,
                     'threshold': sub_expr.threshold,
                     'period': sub_expr.period,
                     'is_deterministic': sub_expr.deterministic,
                     'periods': sub_expr.periods})
            expected_sub_alarm_maps = {
                'changed': {
                    u'111': sub_alarm_def},
                'new': {},
                'old': {},
                'unchanged': {}}
        else:
            sub_alarm_def = sub_alarm_definition.SubAlarmDefinition(
                row={'id': sad['id'],
                     'alarm_definition_id': sad['alarm_definition_id'],
                     'function': sad['function'],
                     'metric_name': sad['metric_name'],
                     'dimensions': u'device=1,image_id=888,flavor_id=777,metric_name=cpu',
                     'operator': sad['operator'],
                     'threshold': sad['threshold'],
                     'period': sad['period'],
                     'is_deterministic': sad['is_deterministic'],
                     'periods': sad['periods']})
            expected_sub_alarm_maps = {'changed': {}, 'new': {},
                                       'old': {}, 'unchanged': {u'111': sub_alarm_def}}
        self.assertEqual((alarm_def_row, expected_sub_alarm_maps), updates)
