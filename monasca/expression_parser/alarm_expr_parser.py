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

import pyparsing


class SubExpr(object):

    def __init__(self, tokens):

        self._sub_expr = tokens
        self._func = tokens.func
        self._metric_name = tokens.metric_name
        self._dimensions = tokens.dimensions.dimensions_list
        self._operator = tokens.relational_op
        self._threshold = tokens.threshold
        self._period = tokens.period
        self._periods = tokens.periods
        self._id = None

    @property
    def sub_expr_str(self):
        """Get the entire sub expression as a string with no spaces."""
        return "".join(list(itertools.chain(*self._sub_expr)))

    @property
    def fmtd_sub_expr_str(self):
        """Get the entire sub expressions as a string with spaces."""
        result = "{}({}".format(self._func.encode('utf8'),
                                self._metric_name.encode('utf8'))

        if self._dimensions:
            result += "{{{}}}".format(self._dimensions.encode('utf8'))

        if self._period:
            result += ", {}".format(self._period.encode('utf8'))

        result += ")"

        result += " {} {}".format(self._operator.encode('utf8'),
                                  self._threshold.encode('utf8'))

        if self._periods:
            result += " times {}".format(self._periods.encode('utf8'))

        return result.decode('utf8')

    @property
    def dimensions_str(self):
        """Get all the dimensions as a single comma delimited string."""
        return self._dimensions

    @property
    def operands_list(self):
        """Get this sub expression as a list."""
        return [self]

    @property
    def func(self):
        """Get the function as it appears in the orig expression."""
        return self._func

    @property
    def normalized_func(self):
        """Get the function upper-cased."""
        return self._func.upper()

    @property
    def metric_name(self):
        """Get the metric name as it appears in the orig expression."""
        return self._metric_name

    @property
    def normalized_metric_name(self):
        """Get the metric name lower-cased."""
        return self._metric_name.lower()

    @property
    def dimensions(self):
        """Get the dimensions."""
        return self._dimensions

    @property
    def dimensions_as_list(self):
        """Get the dimensions as a list."""
        if self._dimensions:
            return self._dimensions.split(",")
        else:
            return []

    @property
    def operator(self):
        """Get the operator."""
        return self._operator

    @property
    def threshold(self):
        """Get the threshold value."""
        return self._threshold

    @property
    def period(self):
        """Get the period. Default is 60 seconds."""
        if self._period:
            return self._period
        else:
            return u'60'

    @property
    def periods(self):
        """Get the periods. Default is 1."""
        if self._periods:
            return self._periods
        else:
            return u'1'

    @property
    def normalized_operator(self):
        """Get the operator as one of LT, GT, LTE, or GTE."""
        if self._operator.lower() == "lt" or self._operator == "<":
            return u"LT"
        elif self._operator.lower() == "gt" or self._operator == ">":
            return u"GT"
        elif self._operator.lower() == "lte" or self._operator == "<=":
            return u"LTE"
        elif self._operator.lower() == "gte" or self._operator == ">=":
            return u"GTE"

    @property
    def id(self):
        """Get the id used to identify this sub expression in the repo."""
        return self._id

    @id.setter
    def id(self, id):
        """Set the d used to identify this sub expression in the repo."""
        self._id = id


class BinaryOp(object):
    def __init__(self, tokens):
        self.op = tokens[0][1]
        self.operands = tokens[0][0::2]

    @property
    def operands_list(self):
        return ([sub_operand for operand in self.operands for sub_operand in
                 operand.operands_list])


class AndSubExpr(BinaryOp):
    """Expand later as needed."""
    pass


class OrSubExpr(BinaryOp):
    """Expand later as needed."""
    pass


COMMA = pyparsing.Literal(",")
LPAREN = pyparsing.Literal("(")
RPAREN = pyparsing.Literal(")")
EQUAL = pyparsing.Literal("=")
LBRACE = pyparsing.Literal("{")
RBRACE = pyparsing.Literal("}")

# Initialize non-ascii unicode code points in the Basic Multilingual Plane.
unicode_printables = u''.join(
    unichr(c) for c in xrange(128, 65536) if not unichr(c).isspace())

# Does not like comma. No Literals from above allowed.
valid_identifier_chars = (
    (unicode_printables + pyparsing.alphanums + ".-_#!$%&'*+/:;?@[\\]^`|~"))

metric_name = (
    pyparsing.Word(valid_identifier_chars, min=1, max=255)("metric_name"))
dimension_name = pyparsing.Word(valid_identifier_chars, min=1, max=255)
dimension_value = pyparsing.Word(valid_identifier_chars, min=1, max=255)

integer_number = pyparsing.Word(pyparsing.nums)
decimal_number = pyparsing.Word(pyparsing.nums + ".")

max = pyparsing.CaselessLiteral("max")
min = pyparsing.CaselessLiteral("min")
avg = pyparsing.CaselessLiteral("avg")
count = pyparsing.CaselessLiteral("count")
sum = pyparsing.CaselessLiteral("sum")
func = (max | min | avg | count | sum)("func")

less_than_op = (
    (pyparsing.CaselessLiteral("<") | pyparsing.CaselessLiteral("lt")))
less_than_eq_op = (
    (pyparsing.CaselessLiteral("<=") | pyparsing.CaselessLiteral("lte")))
greater_than_op = (
    (pyparsing.CaselessLiteral(">") | pyparsing.CaselessLiteral("gt")))
greater_than_eq_op = (
    (pyparsing.CaselessLiteral(">=") | pyparsing.CaselessLiteral("gte")))

# Order is important. Put longer prefix first.
relational_op = (
    less_than_eq_op | less_than_op | greater_than_eq_op | greater_than_op)(
    "relational_op")

AND = pyparsing.CaselessLiteral("and") | pyparsing.CaselessLiteral("&&")
OR = pyparsing.CaselessLiteral("or") | pyparsing.CaselessLiteral("||")
logical_op = (AND | OR)("logical_op")

times = pyparsing.CaselessLiteral("times")

dimension = pyparsing.Group(dimension_name + EQUAL + dimension_value)

# Cannot have any whitespace after the comma delimiter.
dimension_list = pyparsing.Group(pyparsing.Optional(
    LBRACE + pyparsing.delimitedList(dimension, delim=',', combine=True)(
        "dimensions_list") + RBRACE))

metric = metric_name + dimension_list("dimensions")
period = integer_number("period")
threshold = decimal_number("threshold")
periods = integer_number("periods")

expression = pyparsing.Forward()

sub_expression = (func + LPAREN + metric + pyparsing.Optional(
    COMMA + period) + RPAREN + relational_op + threshold + pyparsing.Optional(
    times + periods) | LPAREN + expression + RPAREN)

sub_expression.setParseAction(SubExpr)

expression = (
    pyparsing.operatorPrecedence(sub_expression,
                                 [(AND, 2, pyparsing.opAssoc.LEFT, AndSubExpr),
                                  (OR, 2, pyparsing.opAssoc.LEFT, OrSubExpr)]))


class AlarmExprParser(object):
    def __init__(self, expr):
        self._expr = expr

    @property
    def sub_expr_list(self):
        # Remove all spaces before parsing. Simple, quick fix for whitespace
        # issue with dimension list not allowing whitespace after comma.
        parseResult = (expression + pyparsing.stringEnd).parseString(
            self._expr.replace(' ', ''))
        sub_expr_list = parseResult[0].operands_list
        return sub_expr_list


def main():
    """Used for development and testing."""

    expr0 = (
        "max(-_.千幸福的笑脸{घोड़ा=馬,  "
        "dn2=dv2,千幸福的笑脸घ=千幸福的笑脸घ}) gte 100 "
        "times 3 && "
        "(min(ເຮືອນ{dn3=dv3,家=дом}) < 10 or sum(biz{dn5=dv5}) >9 9and "
        "count(fizzle) lt 0 or count(baz) > 1)".decode('utf8'))

    expr1 = ("max(foo{hostname=mini-mon,千=千}, 120) > 100 and (max(bar)>100 "
             " or max(biz)>100)".decode('utf8'))

    expr2 = "max(foo)>=100"

    for expr in (expr0, expr1, expr2):
        print ('orig expr: {}'.format(expr.encode('utf8')))
        alarmExprParser = AlarmExprParser(expr)
        sub_expr = alarmExprParser.sub_expr_list
        for sub_expression in sub_expr:
            print ('sub expr: {}'.format(
                sub_expression.sub_expr_str.encode('utf8')))
            print ('fmtd sub expr: {}'.format(
                sub_expression.fmtd_sub_expr_str.encode('utf8')))
            print ('sub_expr dimensions: {}'.format(
                sub_expression.dimensions_str.encode('utf8')))
            print ()
        print ()


if __name__ == "__main__":
    sys.exit(main())
