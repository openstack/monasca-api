# (C) Copyright 2016-2017 Hewlett Packard Enterprise Development LP
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
import unittest

from monasca_api.expression_parser import alarm_expr_parser


class TestAlarmExpression(unittest.TestCase):

    good_simple_expression = "max(cpu.idle_perc{hostname=fred}, 60) > 10 times 4"

    def test_good_expression(self):
        expression = self.good_simple_expression
        sub_exprs = alarm_expr_parser.AlarmExprParser(expression).sub_expr_list
        self.assertEqual(1, len(sub_exprs))

    def _ensure_parse_fails(self, expression):
        parser = alarm_expr_parser.AlarmExprParser(expression)
        self.assertRaises(
            (pyparsing.ParseException,
             pyparsing.ParseFatalException),
            getattr, parser, "sub_expr_list")

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
