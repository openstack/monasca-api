#!/usr/bin/python
# -*- coding: utf-8 -*-
# Copyright 2014 Hewlett-Packard
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
import itertools
import sys

from pyparsing import CaselessLiteral
from pyparsing import alphanums
from pyparsing import delimitedList
from pyparsing import Forward
from pyparsing import Group
from pyparsing import Literal
from pyparsing import nums
from pyparsing import opAssoc
from pyparsing import operatorPrecedence
from pyparsing import Optional
from pyparsing import stringEnd
from pyparsing import Word


class SubExpr(object):
    def __init__(self, tokens):
        self.sub_expr = tokens
        self.func = tokens.func
        self.metric_name = tokens.metric_name
        self.dimensions = tokens.dimensions.dimensions_list
        self.operator = tokens.relational_op
        self.threshold = tokens.threshold
        self.period = tokens.period
        self.periods = tokens.periods

    def get_sub_expr_str(self):
        return "".join(list(itertools.chain(*self.sub_expr)))

    def get_fmtd_sub_expr(self):

        result = "{}({}".format(self.func.encode('utf8'),
                                self.metric_name.encode('utf8'))

        if self.dimensions:
            result += "{{{}}}".format(self.dimensions.encode('utf8'))

        if self.period:
            result += ", {}".format(self.period.encode('utf8'))

        result += ")"

        result += " {} {}".format(self.operator.encode('utf8'),
                                  self.threshold.encode('utf8'))

        if self.periods:
            result += " times {}".format(self.periods.encode('utf8'))

        return result.decode('utf8')

    def get_dimensions_str(self):
        return self.dimensions

    def get_operands_list(self):
        return [self]

    def get_func(self):
        return self.func

    def get_normalized_func(self):
        return self.func.upper()

    def get_metric_name(self):
        return self.metric_name

    def get_normalized_metric_name(self):
        return self.metric_name.lower()

    def get_dimensions(self):
        return self.dimensions

    def get_dimensions_as_list(self):
        if self.dimensions:
            return self.dimensions.split(",")
        else:
            return []

    def get_operator(self):
        return self.operator

    def get_threshold(self):
        return self.threshold

    def get_period(self):
        if self.period:
            return self.period
        else:
            return u'60'

    def get_periods(self):
        if self.periods:
            return self.periods
        else:
            return u'1'

    def get_normalized_operator(self):
        if self.operator.lower() == "lt" or self.operator == "<":
            return u"LT"
        elif self.operator.lower() == "gt" or self.operator == ">":
            return u"GT"
        elif self.operator.lower() == "lte" or self.operator == "<=":
            return u"LTE"
        elif self.operator.lower() == "gte" or self.operator == ">=":
            return u"GTE"


class BinaryOp(object):
    def __init__(self, tokens):
        self.op = tokens[0][1]
        self.operands = tokens[0][0::2]

    def get_operands_list(self):
        return ([sub_operand for operand in self.operands for sub_operand in
                 operand.get_operands_list()])


class AndSubExpr(BinaryOp):
    """ Expand later as needed.
    """
    pass


class OrSubExpr(BinaryOp):
    """Expand later as needed.
    """
    pass


COMMA = Literal(",")
LPAREN = Literal("(")
RPAREN = Literal(")")
EQUAL = Literal("=")
LBRACE = Literal("{")
RBRACE = Literal("}")

# Initialize non-ascii unicode code points in the Basic Multilingual Plane.
unicode_printables = u''.join(
    unichr(c) for c in xrange(128, 65536) if not unichr(c).isspace())

# Does not like comma. No Literals from above allowed.
valid_identifier_chars = (unicode_printables + alphanums + ".-_#!$%&'*+/:;?@["
                                                           "\\]^`|~")

metric_name = Word(valid_identifier_chars, min=1, max=255)("metric_name")
dimension_name = Word(valid_identifier_chars, min=1, max=255)
dimension_value = Word(valid_identifier_chars, min=1, max=255)

integer_number = Word(nums)
decimal_number = Word(nums + ".")

max = CaselessLiteral("max")
min = CaselessLiteral("min")
avg = CaselessLiteral("avg")
count = CaselessLiteral("count")
sum = CaselessLiteral("sum")
func = (max | min | avg | count | sum)("func")

less_than_op = (CaselessLiteral("<") | CaselessLiteral("lt"))
less_than_eq_op = (CaselessLiteral("<=") | CaselessLiteral("lte"))
greater_than_op = (CaselessLiteral(">") | CaselessLiteral("gt"))
greater_than_eq_op = (CaselessLiteral(">=") | CaselessLiteral("gte"))

# Order is important. Put longer prefix first.
relational_op = (
    less_than_eq_op | less_than_op | greater_than_eq_op | greater_than_op)(
    "relational_op")

AND = CaselessLiteral("and") | CaselessLiteral("&&")
OR = CaselessLiteral("or") | CaselessLiteral("||")
logical_op = (AND | OR)("logical_op")

times = CaselessLiteral("times")

dimension = Group(dimension_name + EQUAL + dimension_value)
dimension_list = Group(Optional(
    LBRACE + delimitedList(dimension, delim=",", combine=True)(
        "dimensions_list") + RBRACE))

metric = metric_name + dimension_list("dimensions")
period = integer_number("period")
threshold = decimal_number("threshold")
periods = integer_number("periods")

expression = Forward()

sub_expression = (func + LPAREN + metric + Optional(
    COMMA + period) + RPAREN + relational_op + threshold + Optional(
    times + periods) | LPAREN + expression + RPAREN)

sub_expression.setParseAction(SubExpr)

expression = operatorPrecedence(sub_expression,
                                [(AND, 2, opAssoc.LEFT, AndSubExpr),
                                 (OR, 2, opAssoc.LEFT, OrSubExpr)])


class AlarmExprParser(object):
    def __init__(self, expr):
        self._expr = expr

    def get_sub_expr_list(self):
        parseResult = (expression + stringEnd).parseString(self._expr)
        sub_expr_list = parseResult[0].get_operands_list()
        return sub_expr_list


def main():
    """ Used for development and testing.

    :return:
    """
    expr = "max(-_.千幸福的笑脸{घोड़ा=馬,dn2=dv2}, 60) gte 100 times 3 && " \
           "(min(ເຮືອນ{dn3=dv3,家=дом}) < 10 or sum(biz{dn5=dv5}) > 99 and " \
           "count(fizzle) lt 0 or count(baz) > 1)".decode('utf8')
    # expr = "max(foo{hostname=mini-mon,千=千}, 120) > 100 and (max(bar)>100 \
    # or max(biz)>100)".decode('utf8')
    alarmExprParser = AlarmExprParser(expr)
    r = alarmExprParser.get_sub_expr_list()
    for sub_expression in r:
        print sub_expression.get_sub_expr_str()
        print sub_expression.get_fmtd_sub_expr()
        print sub_expression.get_dimensions_str()
        print

if __name__ == "__main__":
    sys.exit(main())
