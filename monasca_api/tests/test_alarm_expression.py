# (C) Copyright 2016-2017 Hewlett Packard Enterprise Development LP
# Copyright 2017 Fujitsu LIMITED
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

import pyparsing

from monasca_api.expression_parser import alarm_expr_parser
from monasca_api.tests import base


class TestAlarmExpression(base.BaseTestCase):

    good_simple_expression = "max(cpu.idle_perc{hostname=fred}, 60) <= 3 times 4 OR \
                             avg(CPU.PERCENT)<5 OR min(cpu.percent, deterministic) gte 3"

    def test_good_expression(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertEqual(3, len(sub_exprs))

    def test_fmtd_sub_expr(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertEqual([x.fmtd_sub_expr_str for x in sub_exprs],
                         ['MAX(cpu.idle_perc{hostname=fred}) <= 3.0 times 4',
                          'AVG(CPU.PERCENT{}) < 5.0', 'MIN(cpu.percent{}) gte 3.0'])

    def test_dimensions_str(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertEqual([x.dimensions_str for x in sub_exprs], ['hostname=fred', '', ''])

    def test_function(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertEqual([x.func for x in sub_exprs], ['max', 'avg', 'min'])

    def test_normalized_function(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertEqual([x.normalized_func for x in sub_exprs], ['MAX', 'AVG', 'MIN'])

    def test_metric_name(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertEqual([x.metric_name for x in sub_exprs],
                         ['cpu.idle_perc', 'CPU.PERCENT', 'cpu.percent'])

    def test_normalized_metric_name(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertEqual([x.normalized_metric_name for x in sub_exprs],
                         ['cpu.idle_perc', 'cpu.percent', 'cpu.percent'])

    def test_dimensions(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertEqual([x.dimensions for x in sub_exprs], ['hostname=fred', '', ''])

    def test_dimensions_as_list(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        print([x.dimensions_as_list for x in sub_exprs].__str__())
        self.assertEqual([x.dimensions_as_list for x in sub_exprs].__str__(),
                         "[ParseResults(['hostname=fred'], {}), [], []]")

    def test_operator(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertEqual([x.operator for x in sub_exprs], ['<=', '<', 'gte'])

    def test_threshold(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertEqual([x.threshold for x in sub_exprs], [3.0, 5.0, 3.0])

    def test_period(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertEqual([x.period for x in sub_exprs], [60, 60, 60])

    def test_periods(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertEqual([x.periods for x in sub_exprs], [4, 1, 1])

    def test_deterministic(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertEqual([x.deterministic for x in sub_exprs], [False, False, True])

    def test_normalized_operator(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertEqual([x.normalized_operator for x in sub_exprs], ['LTE', 'LT', 'GTE'])

    def test_id(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertEqual([x.id for x in sub_exprs], [None, None, None])

    def test_set_id(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        for x in sub_exprs:
            x.id = 88
        self.assertEqual([x.id for x in sub_exprs], [88, 88, 88])

    def _ensure_parse_fails(self, expression):
        parser = alarm_expr_parser.AlarmExprParser(expression)
        self.assertRaises(
            (pyparsing.ParseException,
             pyparsing.ParseFatalException),
            getattr, parser, "sub_expr_list")

    def test_incomplete_operator(self):
        expression = self.good_simple_expression.replace('<= 3', '')
        self._ensure_parse_fails(expression)

    def test_no_dimension_name(self):
        expression = self.good_simple_expression.replace('hostname', '')
        self._ensure_parse_fails(expression)

    def test_no_metric_name(self):
        expression = self.good_simple_expression.replace('cpu.idle_perc', '')
        self._ensure_parse_fails(expression)

    def test_invalid_period(self):
        expression = self.good_simple_expression.replace('60', '42')
        self._ensure_parse_fails(expression)

    def test_zero_period(self):
        expression = self.good_simple_expression.replace('60', '0')
        self._ensure_parse_fails(expression)

    def test_negative_period(self):
        expression = self.good_simple_expression.replace('60', '-60')
        self._ensure_parse_fails(expression)

    def test_zero_periods(self):
        expression = self.good_simple_expression.replace('times 4', 'times 0')
        self._ensure_parse_fails(expression)
