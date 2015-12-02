# Copyright 2015 Cray
# Copyright 2016 FUJITSU LIMITED
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
import time

import fixtures
from oslo_config import cfg
from oslo_config import fixture as fixture_config
import testtools

from sqlalchemy import delete, MetaData, insert, bindparam
from monasca_api.common.repositories.sqla import models

CONF = cfg.CONF


class TestAlarmRepoDB(testtools.TestCase, fixtures.TestWithFixtures):
    @classmethod
    def setUpClass(cls):
        from sqlalchemy import engine_from_config

        engine = engine_from_config({'url': 'sqlite://'}, prefix='')

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

        cls.aa = models.create_aa_model(metadata)
        cls._delete_aa_query = delete(cls.aa)
        cls._insert_aa_query = (insert(cls.aa)
                                .values(
                                    alarm_definition_id=bindparam('alarm_definition_id'),
                                    alarm_state=bindparam('alarm_state'),
                                    action_id=bindparam('action_id')))

        cls.ad = models.create_ad_model(metadata)
        cls._delete_ad_query = delete(cls.ad)
        cls._insert_ad_query = (insert(cls.ad)
                                .values(
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
        cls._insert_sad_query = (insert(cls.sad)
                                 .values(
                                     id=bindparam('id'),
                                     alarm_definition_id=bindparam('alarm_definition_id'),
                                     function=bindparam('function'),
                                     metric_name=bindparam('metric_name'),
                                     operator=bindparam('operator'),
                                     threshold=bindparam('threshold'),
                                     period=bindparam('period'),
                                     periods=bindparam('periods'),
                                     created_at=bindparam('created_at'),
                                     updated_at=bindparam('updated_at')))

        cls.sadd = models.create_sadd_model(metadata)
        cls._delete_sadd_query = delete(cls.sadd)
        cls._insert_sadd_query = (insert(cls.sadd)
                                  .values(
                                      sub_alarm_definition_id=bindparam('sub_alarm_definition_id'),
                                      dimension_name=bindparam('dimension_name'),
                                      value=bindparam('value')))

        cls.nm = models.create_nm_model(metadata)
        cls._delete_nm_query = delete(cls.nm)
        cls._insert_nm_query = (insert(cls.nm)
                                .values(
                                    id=bindparam('id'),
                                    tenant_id=bindparam('tenant_id'),
                                    name=bindparam('name'),
                                    type=bindparam('type'),
                                    address=bindparam('address'),
                                    created_at=bindparam('created_at'),
                                    updated_at=bindparam('updated_at')))

        cls.a = models.create_a_model(metadata)
        cls._delete_a_query = delete(cls.a)
        cls._insert_a_query = (insert(cls.a)
                               .values(
                                   id=bindparam('id'),
                                   alarm_definition_id=bindparam('alarm_definition_id'),
                                   state=bindparam('state'),
                                   lifecycle_state=bindparam('lifecycle_state'),
                                   link=bindparam('link'),
                                   created_at=bindparam('created_at'),
                                   updated_at=bindparam('updated_at'),
                                   state_updated_at=bindparam('state_updated_at')))

        cls.sa = models.create_sa_model(metadata)
        cls._delete_sa_query = delete(cls.sa)
        cls._insert_sa_query = (insert(cls.sa)
                                .values(
                                id=bindparam('id'),
                                    sub_expression_id=bindparam('sub_expression_id'),
                                    alarm_id=bindparam('alarm_id'),
                                    expression=bindparam('expression'),
                                    created_at=bindparam('created_at'),
                                    updated_at=bindparam('updated_at')))

        cls.am = models.create_am_model(metadata)
        cls._delete_am_query = delete(cls.am)
        cls._insert_am_query = (insert(cls.am)
                                .values(
                                    alarm_id=bindparam('alarm_id'),
                                    metric_definition_dimensions_id=bindparam(
                                        'metric_definition_dimensions_id')))

        cls.md = models.create_md_model(metadata)
        cls._delete_md_query = delete(cls.md)
        cls._insert_md_query = (insert(cls.md)
                                .values(
                                    dimension_set_id=bindparam('dimension_set_id'),
                                    name=bindparam('name'),
                                    value=bindparam('value')))

        cls.mdd = models.create_mdd_model(metadata)
        cls._delete_mdd_query = delete(cls.mdd)
        cls._insert_mdd_query = (insert(cls.mdd)
                                 .values(
                                     id=bindparam('id'),
                                     metric_definition_id=bindparam('metric_definition_id'),
                                     metric_dimension_set_id=bindparam('metric_dimension_set_id')))

        cls.mde = models.create_mde_model(metadata)
        cls._delete_mde_query = delete(cls.mde)
        cls._insert_mde_query = (insert(cls.mde)
                                 .values(
                                     id=bindparam('id'),
                                     name=bindparam('name'),
                                     tenant_id=bindparam('tenant_id'),
                                     region=bindparam('region')))

    @classmethod
    def tearDownClass(cls):
        cls.fixture.cleanUp()

    def setUp(self):
        super(TestAlarmRepoDB, self).setUp()

        self._fixture_config = self.useFixture(
            fixture_config.Config(cfg.CONF))
        self._fixture_config.config(url='sqlite://',
                                    group='database')

        from monasca_api.common.repositories.sqla import alarms_repository as ar
        self.repo = ar.AlarmsRepository()

        timestamp1 = datetime.datetime(2015, 3, 14, 9, 26, 53)
        timestamp2 = datetime.datetime(2015, 3, 14, 9, 26, 54)
        timestamp3 = datetime.datetime(2015, 3, 14, 9, 26, 55)
        timestamp4 = datetime.datetime(2015, 3, 15, 9, 26, 53)

        self.default_as = [{'id': '1',
                            'alarm_definition_id': '1',
                            'state': 'OK',
                            'lifecycle_state': 'OPEN',
                            'link': 'http://somesite.com/this-alarm-info',
                            'created_at': timestamp1,
                            'updated_at': timestamp1,
                            'state_updated_at': timestamp1},
                           {'id': '2',
                            'alarm_definition_id': '1',
                            'state': 'UNDETERMINED',
                            'lifecycle_state': 'OPEN',
                            'link': 'http://somesite.com/this-alarm-info',
                            'created_at': timestamp2,
                            'updated_at': timestamp2,
                            'state_updated_at': timestamp2},
                           {'id': '3',
                            'alarm_definition_id': '1',
                            'state': 'ALARM',
                            'lifecycle_state': None,
                            'link': 'http://somesite.com/this-alarm-info',
                            'created_at': timestamp3,
                            'updated_at': timestamp3,
                            'state_updated_at': timestamp3},
                           {'id': '234111',
                            'alarm_definition_id': '234',
                            'state': 'UNDETERMINED',
                            'lifecycle_state': None,
                            'link': None,
                            'created_at': timestamp4,
                            'updated_at': timestamp4,
                            'state_updated_at': timestamp4}]

        self.default_ads = [{'id': '1',
                             'tenant_id': 'bob',
                             'name': '90% CPU',
                             'severity': 'LOW',
                             'expression': 'AVG(cpu.idle_perc{flavor_id=777,'
                             ' image_id=888, device=1}) > 10',
                             'match_by': 'flavor_id,image_id',
                             'actions_enabled': False,
                             'created_at': datetime.datetime.now(),
                             'updated_at': datetime.datetime.now(),
                             'deleted_at': None},
                            {'id': '234',
                             'tenant_id': 'bob',
                             'name': '50% CPU',
                             'severity': 'LOW',
                             'expression': 'AVG(cpu.sys_mem'
                             '{service=monitoring})'
                             ' > 20 and AVG(cpu.idle_perc'
                             '{service=monitoring}) < 10',
                             'match_by': 'hostname,region',
                             'actions_enabled': False,
                             'created_at': datetime.datetime.now(),
                             'updated_at': datetime.datetime.now(),
                             'deleted_at': None}]

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

        self.default_sads = [{'id': '43',
                              'alarm_definition_id': '234',
                              'function': 'f_43',
                              'metric_name': 'm_43',
                              'operator': 'GT',
                              'threshold': 0,
                              'period': 1,
                              'periods': 2,
                              'created_at': datetime.datetime.now(),
                              'updated_at': datetime.datetime.now()},
                             {'id': '45',
                              'alarm_definition_id': '234',
                              'function': 'f_45',
                              'metric_name': 'm_45',
                              'operator': 'GT',
                              'threshold': 0,
                              'period': 1,
                              'periods': 2,
                              'created_at': datetime.datetime.now(),
                              'updated_at': datetime.datetime.now()},
                             {'id': '47',
                              'alarm_definition_id': '234',
                              'function': 'f_47',
                              'metric_name': 'm_47',
                              'operator': 'GT',
                              'threshold': 0,
                              'period': 1,
                              'periods': 2,
                              'created_at': datetime.datetime.now(),
                              'updated_at': datetime.datetime.now()},
                             {'id': '8484',
                              'alarm_definition_id': '234',
                              'function': 'f_49',
                              'metric_name': 'm_49',
                              'operator': 'GT',
                              'threshold': 0,
                              'period': 1,
                              'periods': 2,
                              'created_at': datetime.datetime.now(),
                              'updated_at': datetime.datetime.now()},
                             {'id': '8686',
                              'alarm_definition_id': '234',
                              'function': 'f_51',
                              'metric_name': 'm_51',
                              'operator': 'GT',
                              'threshold': 0,
                              'period': 1,
                              'periods': 2,
                              'created_at': datetime.datetime.now(),
                              'updated_at': datetime.datetime.now()}]

        self.default_sas = [{'sub_expression_id': '43',
                             'id': '42',
                             'alarm_id': '1',
                             'expression': 'avg(cpu.idle_perc{flavor_id=777,'
                             ' image_id=888, device=1}) > 10',
                             'created_at': datetime.datetime.now(),
                             'updated_at': datetime.datetime.now()},
                            {'sub_expression_id': '45',
                             'id': '43',
                             'alarm_id': '2',
                             'expression': 'avg(cpu.idle_perc{flavor_id=777,'
                             ' image_id=888, device=1}) > 10',
                             'created_at': datetime.datetime.now(),
                             'updated_at': datetime.datetime.now()},
                            {'sub_expression_id': '47',
                             'id': '44',
                             'alarm_id': '3',
                             'expression': 'avg(cpu.idle_perc{flavor_id=777,'
                             ' image_id=888, device=1}) > 10',
                             'created_at': datetime.datetime.now(),
                             'updated_at': datetime.datetime.now()}]

        self.default_ams = [{'alarm_id': '1',
                             'metric_definition_dimensions_id': '11'},
                            {'alarm_id': '1',
                             'metric_definition_dimensions_id': '22'},
                            {'alarm_id': '2',
                             'metric_definition_dimensions_id': '11'},
                            {'alarm_id': '3',
                             'metric_definition_dimensions_id': '22'},
                            {'alarm_id': '234111',
                             'metric_definition_dimensions_id': '31'},
                            {'alarm_id': '234111',
                             'metric_definition_dimensions_id': '32'}]

        self.default_mdes = [{'id': '1',
                             'name': 'cpu.idle_perc',
                              'tenant_id': 'bob',
                              'region': 'west'},
                             {'id': '111',
                              'name': 'cpu.sys_mem',
                              'tenant_id': 'bob',
                              'region': 'west'},
                             {'id': '112',
                              'name': 'cpu.idle_perc',
                              'tenant_id': 'bob',
                              'region': 'west'}]

        self.default_mdds = [{'id': '11',
                              'metric_definition_id': '1',
                              'metric_dimension_set_id': '1'},
                             {'id': '22',
                              'metric_definition_id': '1',
                              'metric_dimension_set_id': '2'},
                             {'id': '31',
                              'metric_definition_id': '111',
                              'metric_dimension_set_id': '21'},
                             {'id': '32',
                              'metric_definition_id': '112',
                              'metric_dimension_set_id': '22'}]

        self.default_mds = [{'dimension_set_id': '1',
                             'name': 'instance_id',
                             'value': '123'},
                            {'dimension_set_id': '1',
                             'name': 'service',
                             'value': 'monitoring'},
                            {'dimension_set_id': '2',
                             'name': 'flavor_id',
                             'value': '222'},
                            {'dimension_set_id': '21',
                             'name': 'service',
                             'value': 'monitoring'},
                            {'dimension_set_id': '22',
                             'name': 'service',
                             'value': 'monitoring'},
                            {'dimension_set_id': '21',
                             'name': 'hostname',
                             'value': 'roland'},
                            {'dimension_set_id': '22',
                             'name': 'hostname',
                             'value': 'roland'},
                            {'dimension_set_id': '21',
                             'name': 'region',
                             'value': 'colorado'},
                            {'dimension_set_id': '22',
                             'name': 'region',
                             'value': 'colorado'},
                            {'dimension_set_id': '22',
                             'name': 'extra',
                             'value': 'vivi'}]

        self.alarm1 = {'alarm_definition': {'id': '1',
                                            'name': '90% CPU',
                                            'severity': 'LOW'},
                       'created_timestamp': '2015-03-14T09:26:53Z',
                       'id': '1',
                       'lifecycle_state': 'OPEN',
                       'link': 'http://somesite.com/this-alarm-info',
                       'metrics': [{'dimensions': {'instance_id': '123',
                                                   'service': 'monitoring'},
                                    'name': 'cpu.idle_perc'},
                                   {'dimensions': {'flavor_id': '222'},
                                    'name': 'cpu.idle_perc'}],
                       'state': 'OK',
                       'state_updated_timestamp': '2015-03-14T09:26:53Z',
                       'updated_timestamp': '2015-03-14T09:26:53Z'}
        self.alarm2 = {'alarm_definition': {'id': '1',
                                            'name': '90% CPU',
                                            'severity': 'LOW'},
                       'created_timestamp': '2015-03-14T09:26:54Z',
                       'id': '2',
                       'lifecycle_state': 'OPEN',
                       'link': 'http://somesite.com/this-alarm-info',
                       'metrics': [{'dimensions': {'instance_id': '123',
                                                   'service': 'monitoring'},
                                    'name': 'cpu.idle_perc'}],
                       'state': 'UNDETERMINED',
                       'state_updated_timestamp': '2015-03-14T09:26:54Z',
                       'updated_timestamp': '2015-03-14T09:26:54Z'}
        self.alarm_compound = {'alarm_definition': {'id': '234',
                                                    'name': '50% CPU',
                                                    'severity': 'LOW'},
                               'created_timestamp': '2015-03-15T09:26:53Z',
                               'id': '234111',
                               'lifecycle_state': None,
                               'link': None,
                               'metrics': [
                                   {'dimensions': {'hostname': 'roland',
                                                   'region': 'colorado',
                                                   'service': 'monitoring'},
                                    'name': 'cpu.sys_mem'},
                                   {'dimensions': {'extra': 'vivi',
                                                   'hostname': 'roland',
                                                   'region': 'colorado',
                                                   'service': 'monitoring'},
                                    'name': 'cpu.idle_perc'}],
                               'state': 'UNDETERMINED',
                               'state_updated_timestamp':
                               '2015-03-15T09:26:53Z',
                               'updated_timestamp': '2015-03-15T09:26:53Z'}
        self.alarm3 = {'alarm_definition': {'id': '1',
                                            'name': '90% CPU',
                                            'severity': 'LOW'},
                       'created_timestamp': '2015-03-14T09:26:55Z',
                       'id': '3',
                       'lifecycle_state': None,
                       'link': 'http://somesite.com/this-alarm-info',
                       'metrics': [{'dimensions': {'flavor_id': '222'},
                                    'name': 'cpu.idle_perc'}],
                       'state': 'ALARM',
                       'state_updated_timestamp': '2015-03-14T09:26:55Z',
                       'updated_timestamp': '2015-03-14T09:26:55Z'}

        with self.engine.begin() as conn:
            conn.execute(self._delete_am_query)
            conn.execute(self._insert_am_query, self.default_ams)
            conn.execute(self._delete_md_query)
            conn.execute(self._insert_md_query, self.default_mds)
            conn.execute(self._delete_mdd_query)
            conn.execute(self._insert_mdd_query, self.default_mdds)
            conn.execute(self._delete_a_query)
            conn.execute(self._insert_a_query, self.default_as)
            conn.execute(self._delete_sa_query)
            conn.execute(self._insert_sa_query, self.default_sas)
            conn.execute(self._delete_mde_query)
            conn.execute(self._insert_mde_query, self.default_mdes)
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

    def helper_builder_result(self, alarm_rows):
        result = []

        if not alarm_rows:
            return result

        # Forward declaration
        alarm = {}
        prev_alarm_id = None
        for alarm_row in alarm_rows:
            if prev_alarm_id != alarm_row['alarm_id']:
                if prev_alarm_id is not None:
                    result.append(alarm)

                ad = {u'id': alarm_row['alarm_definition_id'],
                      u'name': alarm_row['alarm_definition_name'],
                      u'severity': alarm_row['severity'], }

                metrics = []
                alarm = {u'id': alarm_row['alarm_id'], u'metrics': metrics,
                         u'state': alarm_row['state'],
                         u'lifecycle_state': alarm_row['lifecycle_state'],
                         u'link': alarm_row['link'],
                         u'state_updated_timestamp':
                             alarm_row['state_updated_timestamp'].isoformat() +
                             'Z',
                         u'updated_timestamp':
                             alarm_row['updated_timestamp'].isoformat() + 'Z',
                         u'created_timestamp':
                             alarm_row['created_timestamp'].isoformat() + 'Z',
                         u'alarm_definition': ad}

                prev_alarm_id = alarm_row['alarm_id']

            dimensions = {}
            metric = {u'name': alarm_row['metric_name'],
                      u'dimensions': dimensions}

            if alarm_row['metric_dimensions']:
                for dimension in alarm_row['metric_dimensions'].split(','):
                    parsed_dimension = dimension.split('=')
                    dimensions[parsed_dimension[0]] = parsed_dimension[1]

            metrics.append(metric)

        result.append(alarm)
        return result

    def test_should_delete(self):
        tenant_id = 'bob'
        alarm_id = '1'

        alarm1 = self.repo.get_alarm(tenant_id, alarm_id)
        alarm1 = self.helper_builder_result(alarm1)
        self.assertEqual(alarm1[0], self.alarm1)
        self.repo.delete_alarm(tenant_id, alarm_id)
        from monasca_api.common.repositories import exceptions
        self.assertRaises(exceptions.DoesNotExistException,
                          self.repo.get_alarm, tenant_id, alarm_id)

    def test_should_throw_exception_on_delete(self):
        tenant_id = 'bob'
        from monasca_api.common.repositories import exceptions
        self.assertRaises(exceptions.DoesNotExistException,
                          self.repo.delete_alarm, tenant_id, 'Not an alarm ID')

    def test_should_find_alarm_def(self):
        tenant_id = 'bob'
        alarm_id = '1'

        expected = {'actions_enabled': False,
                    'deleted_at': None,
                    'description': None,
                    'expression': 'AVG(cpu.idle_perc{flavor_id=777,'
                    ' image_id=888, device=1}) > 10',
                    'id': '1',
                    'match_by': 'flavor_id,image_id',
                    'name': '90% CPU',
                    'severity': 'LOW',
                    'tenant_id': 'bob'}
        alarm_def = self.repo.get_alarm_definition(tenant_id, alarm_id)
        expected['created_at'] = alarm_def['created_at']
        expected['updated_at'] = alarm_def['updated_at']
        self.assertEqual(alarm_def, expected)
        from monasca_api.common.repositories import exceptions
        self.assertRaises(exceptions.DoesNotExistException,
                          self.repo.get_alarm_definition,
                          tenant_id, 'Not an alarm ID')

    def test_should_find(self):
        res = self.repo.get_alarms(tenant_id='Not a tenant id', limit=1)
        self.assertEqual(res, [])

        tenant_id = 'bob'
        res = self.repo.get_alarms(tenant_id=tenant_id, limit=1000)
        res = self.helper_builder_result(res)

        expected = [self.alarm1,
                    self.alarm2,
                    self.alarm_compound,
                    self.alarm3]

        self.assertEqual(res, expected)

        alarm_def_id = self.alarm_compound['alarm_definition']['id']
        query_parms = {'alarm_definition_id': alarm_def_id}
        res = self.repo.get_alarms(tenant_id=tenant_id,
                                   query_parms=query_parms,
                                   limit=1000)
        res = self.helper_builder_result(res)
        expected = [self.alarm_compound]

        self.assertEqual(res, expected)

        query_parms = {'metric_name': 'cpu.sys_mem'}
        res = self.repo.get_alarms(tenant_id=tenant_id,
                                   query_parms=query_parms,
                                   limit=1000)
        res = self.helper_builder_result(res)
        expected = [self.alarm_compound]

        self.assertEqual(res, expected)

        query_parms = {'metric_name': 'cpu.idle_perc'}
        res = self.repo.get_alarms(tenant_id=tenant_id,
                                   query_parms=query_parms,
                                   limit=1000)
        res = self.helper_builder_result(res)
        expected = [self.alarm1,
                    self.alarm2,
                    self.alarm_compound,
                    self.alarm3]

        self.assertEqual(res, expected)

        query_parms = {'metric_name': 'cpu.idle_perc',
                       'metric_dimensions': ['flavor_id:222']}
        res = self.repo.get_alarms(tenant_id=tenant_id,
                                   query_parms=query_parms,
                                   limit=1000)
        res = self.helper_builder_result(res)
        expected = [self.alarm1,
                    self.alarm3]

        self.assertEqual(res, expected)

        query_parms = {'metric_name': 'cpu.idle_perc',
                       'metric_dimensions': ['service:monitoring', 'hostname:roland']}
        res = self.repo.get_alarms(tenant_id=tenant_id,
                                   query_parms=query_parms,
                                   limit=1000)
        res = self.helper_builder_result(res)
        expected = [self.alarm_compound]

        self.assertEqual(res, expected)

        query_parms = {'state': 'UNDETERMINED'}
        res = self.repo.get_alarms(tenant_id=tenant_id,
                                   query_parms=query_parms,
                                   limit=1000)
        res = self.helper_builder_result(res)
        expected = [self.alarm2,
                    self.alarm_compound]

        self.assertEqual(res, expected)

        alarm_def_id = self.alarm1['alarm_definition']['id']
        query_parms = {'metric_name': 'cpu.idle_perc',
                       'metric_dimensions': ['service:monitoring'],
                       'alarm_definition_id': alarm_def_id}
        res = self.repo.get_alarms(tenant_id=tenant_id,
                                   query_parms=query_parms,
                                   limit=1000)
        res = self.helper_builder_result(res)
        expected = [self.alarm1,
                    self.alarm2]

        self.assertEqual(res, expected)

        alarm_def_id = self.alarm1['alarm_definition']['id']
        query_parms = {'metric_name': 'cpu.idle_perc',
                       'alarm_definition_id': alarm_def_id}
        res = self.repo.get_alarms(tenant_id=tenant_id,
                                   query_parms=query_parms,
                                   limit=1000)
        res = self.helper_builder_result(res)
        expected = [self.alarm1,
                    self.alarm2,
                    self.alarm3]

        self.assertEqual(res, expected)

        alarm_def_id = self.alarm_compound['alarm_definition']['id']
        query_parms = {'alarm_definition_id': alarm_def_id,
                       'state': 'UNDETERMINED'}
        res = self.repo.get_alarms(tenant_id=tenant_id,
                                   query_parms=query_parms,
                                   limit=1000)
        res = self.helper_builder_result(res)
        expected = [self.alarm_compound]

        self.assertEqual(res, expected)

        query_parms = {'metric_name': 'cpu.sys_mem',
                       'state': 'UNDETERMINED'}
        res = self.repo.get_alarms(tenant_id=tenant_id,
                                   query_parms=query_parms,
                                   limit=1000)
        res = self.helper_builder_result(res)
        expected = [self.alarm_compound]

        self.assertEqual(res, expected)

        query_parms = {'metric_name': 'cpu.idle_perc',
                       'metric_dimensions': ['service:monitoring'],
                       'state': 'UNDETERMINED'}
        res = self.repo.get_alarms(tenant_id=tenant_id,
                                   query_parms=query_parms,
                                   limit=1000)
        res = self.helper_builder_result(res)
        expected = [self.alarm2,
                    self.alarm_compound]

        self.assertEqual(res, expected)

        time_now = datetime.datetime.now().isoformat() + 'Z'
        query_parms = {'metric_name': 'cpu.idle_perc',
                       'metric_dimensions': ['service:monitoring'],
                       'state': 'UNDETERMINED',
                       'state_updated_start_time': time_now}
        res = self.repo.get_alarms(tenant_id=tenant_id,
                                   query_parms=query_parms,
                                   limit=1000)
        res = self.helper_builder_result(res)
        expected = []

        self.assertEqual(res, expected)

        time_now = '2015-03-15T00:00:00.0Z'
        query_parms = {'state_updated_start_time': time_now}
        res = self.repo.get_alarms(tenant_id=tenant_id,
                                   query_parms=query_parms,
                                   limit=1000)
        res = self.helper_builder_result(res)
        expected = [self.alarm_compound]

        self.assertEqual(res, expected)

        time_now = '2015-03-14T00:00:00.0Z'
        query_parms = {'state_updated_start_time': time_now}
        res = self.repo.get_alarms(tenant_id=tenant_id,
                                   query_parms=query_parms,
                                   limit=1000)
        res = self.helper_builder_result(res)
        expected = [self.alarm1,
                    self.alarm2,
                    self.alarm_compound,
                    self.alarm3]

        self.assertEqual(res, expected)

        query_parms = {'state_updated_start_time': time_now,
                       'link': 'http://google.com',
                       'lifecycle_state': 'OPEN'}
        res = self.repo.get_alarms(tenant_id=tenant_id,
                                   query_parms=query_parms,
                                   limit=None,
                                   offset='10')
        res = self.helper_builder_result(res)
        expected = []

        self.assertEqual(res, expected)

    def test_should_update(self):
        tenant_id = 'bob'
        alarm_id = '2'

        alarm = self.repo.get_alarm(tenant_id, alarm_id)
        alarm = self.helper_builder_result(alarm)[0]
        original_state_updated_date = alarm['state_updated_timestamp']
        original_updated_timestamp = alarm['updated_timestamp']
        self.assertEqual(alarm['state'], 'UNDETERMINED')

        prev_state, _ = self.repo.update_alarm(tenant_id, alarm_id, 'OK', None, None)
        alarm_new = self.repo.get_alarm(tenant_id, alarm_id)
        alarm_new = self.helper_builder_result(alarm_new)[0]
        new_state_updated_date = alarm_new['state_updated_timestamp']
        new_updated_timestamp = alarm_new['updated_timestamp']
        self.assertNotEqual(original_updated_timestamp,
                            new_updated_timestamp,
                            'updated_at did not change')
        self.assertNotEqual(original_state_updated_date,
                            new_state_updated_date,
                            'state_updated_at did not change')
        alarm_tmp = tuple(alarm[k] for k in ('state', 'link', 'lifecycle_state'))
        self.assertEqual(alarm_tmp, prev_state)
        alarm['state_updated_timestamp'] = alarm_new['state_updated_timestamp']
        alarm['updated_timestamp'] = alarm_new['updated_timestamp']
        alarm['state'] = alarm_new['state']
        alarm['link'] = alarm_new['link']
        alarm['lifecycle_state'] = alarm_new['lifecycle_state']

        self.assertEqual(alarm, alarm_new)

        time.sleep(1)
        prev_state, _ = self.repo.update_alarm(tenant_id, alarm_id, 'OK', None, None)
        alarm_unchanged = self.repo.get_alarm(tenant_id, alarm_id)
        alarm_unchanged = self.helper_builder_result(alarm_unchanged)[0]
        unchanged_state_updated_date = alarm_unchanged['state_updated_timestamp']
        unchanged_updated_timestamp = alarm_unchanged['updated_timestamp']
        self.assertNotEqual(unchanged_updated_timestamp,
                            new_updated_timestamp,
                            'updated_at did not change')
        self.assertEqual(unchanged_state_updated_date,
                         new_state_updated_date,
                         'state_updated_at did change')
        alarm_new_tmp = tuple(alarm_new[k] for k in ('state', 'link', 'lifecycle_state'))
        self.assertEqual(alarm_new_tmp, prev_state)

    def test_should_throw_exception_on_update(self):
        tenant_id = 'bob'
        alarm_id = 'Not real alarm id'
        from monasca_api.common.repositories import exceptions

        self.assertRaises(exceptions.DoesNotExistException,
                          self.repo.update_alarm,
                          tenant_id,
                          alarm_id,
                          'UNDETERMINED',
                          None,
                          None)

    def test_get_alarm_metrics(self):
        alarm_id = '2'
        alarm_metrics = self.repo.get_alarm_metrics(alarm_id)

        expected = [{'alarm_id': '2',
                     'dimensions': 'instance_id=123,service=monitoring',
                     'name': 'cpu.idle_perc'}]

        self.assertEqual(alarm_metrics, expected)

    def test_get_subalarms(self):
        tenant_id = 'bob'
        alarm_id = '2'

        sub_alarms = self.repo.get_sub_alarms(tenant_id, alarm_id)
        expected = [{'alarm_definition_id': '1',
                     'alarm_id': '2',
                     'expression': 'avg(cpu.idle_perc{flavor_id=777, image_id=888, device=1}) > 10',
                     'sub_alarm_id': '43'}]
        self.assertEqual(sub_alarms, expected)
