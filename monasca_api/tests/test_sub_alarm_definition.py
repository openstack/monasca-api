# Copyright 2019 FUJITSU LIMITED
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
from monasca_api.common.repositories.model import sub_alarm_definition
from monasca_api.expression_parser import alarm_expr_parser
from monasca_api.tests import base


class TestSubAlarmDefinition(base.BaseTestCase):
    def test_init_from_row(self):
        sub_alarm_d_dict = {'id': '111',
                            'alarm_definition_id': '123',
                            'function': 'AVG',
                            'metric_name': 'darth.vader',
                            'operator': 'GT',
                            'threshold': 10,
                            'period': 60,
                            'periods': 1,
                            'is_deterministic': 1,
                            'dimensions': 'device=1,image_id=888'}
        dimension_dict = {'device': '1',
                          'image_id': '888'}
        sub_alarm_d = sub_alarm_definition.SubAlarmDefinition(row=sub_alarm_d_dict)
        self.assertEqual(sub_alarm_d_dict['id'], sub_alarm_d.id)
        self.assertEqual(sub_alarm_d_dict['alarm_definition_id'], sub_alarm_d.alarm_definition_id)
        self.assertEqual(sub_alarm_d_dict['metric_name'], sub_alarm_d.metric_name)
        self.assertEqual(sub_alarm_d_dict['dimensions'], sub_alarm_d.dimensions_str)
        self.assertEqual(dimension_dict, sub_alarm_d.dimensions)
        self.assertEqual(sub_alarm_d_dict['function'], sub_alarm_d.function)
        self.assertEqual(sub_alarm_d_dict['operator'], sub_alarm_d.operator)
        self.assertEqual(sub_alarm_d_dict['period'], sub_alarm_d.period)
        self.assertEqual(sub_alarm_d_dict['periods'], sub_alarm_d.periods)
        self.assertEqual(sub_alarm_d_dict['threshold'], sub_alarm_d.threshold)
        self.assertEqual(True, sub_alarm_d.deterministic)

    def test_init_from_sub_expr(self):
        sub_alarm_d_dict = {'function': 'AVG',
                            'metric_name': 'darth.vader',
                            'operator': 'GT',
                            'threshold': 10.0,
                            'period': 60,
                            'periods': 1,
                            'is_deterministic': 0,
                            'dimensions': 'device=1,image_id=888'}
        dimension_dict = {'device': '1',
                          'image_id': '888'}
        expression = 'avg(darth.vader{device=1,image_id=888}, 60) GT 10.0 times 1'
        sub_expr_list = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        sub_alarm_d = sub_alarm_definition.SubAlarmDefinition(sub_expr=sub_expr_list[0])
        self.assertEqual(sub_alarm_d_dict['metric_name'], sub_alarm_d.metric_name)
        self.assertEqual(sub_alarm_d_dict['dimensions'], sub_alarm_d.dimensions_str)
        self.assertEqual(dimension_dict, sub_alarm_d.dimensions)
        self.assertEqual(sub_alarm_d_dict['function'], sub_alarm_d.function)
        self.assertEqual(sub_alarm_d_dict['operator'], sub_alarm_d.operator)
        self.assertEqual(sub_alarm_d_dict['period'], sub_alarm_d.period)
        self.assertEqual(sub_alarm_d_dict['periods'], sub_alarm_d.periods)
        self.assertEqual(sub_alarm_d_dict['threshold'], sub_alarm_d.threshold)
        self.assertEqual(False, sub_alarm_d.deterministic)

    def test_init_from_both_row_and_sub_expr(self):
        sub_alarm_d_dict = {'id': '111',
                            'alarm_definition_id': '123',
                            'function': 'AVG',
                            'metric_name': 'darth.vader',
                            'operator': 'GT',
                            'threshold': 10,
                            'period': 60,
                            'periods': 1,
                            'is_deterministic': 0,
                            'dimensions': 'device=1,image_id=888'}
        expression = 'avg(darth.vader.compute{device=1,image_id=888}, 60) GT 10.0 times 1'
        sub_expr_list = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertRaises(Exception, sub_alarm_definition.SubAlarmDefinition,
                          sub_alarm_d_dict, sub_expr_list)  # noqa: E202

    def test_build_expression_all_parameters(self):
        expression = 'avg(darth.vader{over=9000}, deterministic, 60) GT 10.0 times 1'
        sub_expr_list = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        sub_alarm_d = sub_alarm_definition.SubAlarmDefinition(sub_expr=sub_expr_list[0])
        self.assertEqual(expression, sub_alarm_d.expression)

        sub_alarm_d.dimensions_str = None
        sub_alarm_d.dimensions = None
        sub_alarm_d.deterministic = False
        sub_alarm_d.period = None
        sub_alarm_d.periods = None
        self.assertEqual('avg(darth.vader) GT 10.0', sub_alarm_d.expression)

    def test_equality_method(self):
        sub_alarm_d_dict = {'id': '111',
                            'alarm_definition_id': '123',
                            'function': 'AVG',
                            'metric_name': 'darth.vader',
                            'operator': 'GT',
                            'threshold': 10,
                            'period': 60,
                            'periods': 1,
                            'is_deterministic': 0,
                            'dimensions': 'device=1,image_id=888'}
        sub_alarm_d = sub_alarm_definition.SubAlarmDefinition(row=sub_alarm_d_dict)
        sub_alarm_d2 = sub_alarm_definition.SubAlarmDefinition(row=sub_alarm_d_dict)

        # same object
        self.assertEqual(True, sub_alarm_d == sub_alarm_d)
        # different type
        self.assertEqual(False, sub_alarm_d == list())
        # Equal
        self.assertEqual(True, sub_alarm_d == sub_alarm_d2)
        # equal but different id
        sub_alarm_d2.id = '222'
        self.assertEqual(True, sub_alarm_d == sub_alarm_d2)
        # Not Equal
        sub_alarm_d2.metric_name = 'luck.iamyourfather'
        self.assertEqual(False, sub_alarm_d == sub_alarm_d2)
