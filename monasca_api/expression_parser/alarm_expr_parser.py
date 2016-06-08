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
import sys

import pyparsing

_DETERMINISTIC_ASSIGNMENT_LEN = 3
_DETERMINISTIC_ASSIGNMENT_SHORT_LEN = 1
_DETERMINISTIC_ASSIGNMENT_VALUE_INDEX = 2


class SubExpr(object):

    def __init__(self, tokens):

        if not tokens.func:
            if tokens.relational_op.lower() in ['gte', 'gt', '>=', '>']:
                self._func = "max"
            else:
                self._func = "min"
        else:
            self._func = tokens.func
        self._metric_name = tokens.metric_name
        self._dimensions = tokens.dimensions_list
        self._operator = tokens.relational_op
        self._threshold = tokens.threshold
        self._period = tokens.period
        self._periods = tokens.periods
        self._deterministic = tokens.deterministic
        self._id = None

    @property
    def fmtd_sub_expr_str(self):
        """Get the entire sub expressions as a string with spaces."""
        result = u"{}({}".format(self.normalized_func,
                                 self._metric_name)

        if self._dimensions is not None:
            result += "{" + self.dimensions_str + "}"

        if self._period:
            result += ", {}".format(self._period)

        result += ")"

        result += " {} {}".format(self._operator,
                                  self._threshold)

        if self._periods:
            result += " times {}".format(self._periods)

        return result

    @property
    def dimensions_str(self):
        """Get all the dimensions as a single comma delimited string."""
        return u",".join(self._dimensions)

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
        return u",".join(self._dimensions)

    @property
    def dimensions_as_list(self):
        """Get the dimensions as a list."""
        if self._dimensions:
            return self._dimensions
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
    def deterministic(self):
        return True if self._deterministic else False

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


COMMA = pyparsing.Suppress(pyparsing.Literal(","))
LPAREN = pyparsing.Suppress(pyparsing.Literal("("))
RPAREN = pyparsing.Suppress(pyparsing.Literal(")"))
EQUAL = pyparsing.Literal("=")
LBRACE = pyparsing.Suppress(pyparsing.Literal("{"))
RBRACE = pyparsing.Suppress(pyparsing.Literal("}"))

# Initialize non-ascii unicode code points in the Basic Multilingual Plane.
unicode_printables = u''.join(
    unichr(c) for c in xrange(128, 65536) if not unichr(c).isspace())

# Does not like comma. No Literals from above allowed.
valid_identifier_chars = (
    (unicode_printables + pyparsing.alphanums + ".-_#!$%&'*+/:;?@[\\]^`|~"))

metric_name = (
    pyparsing.Word(valid_identifier_chars, min=1, max=255)("metric_name"))
dimension_name = pyparsing.Word(valid_identifier_chars + ' ', min=1, max=255)
dimension_value = pyparsing.Word(valid_identifier_chars + ' ', min=1, max=255)

MINUS = pyparsing.Literal('-')
integer_number = pyparsing.Word(pyparsing.nums)
decimal_number = (pyparsing.Optional(MINUS) + integer_number +
                  pyparsing.Optional("." + integer_number))
decimal_number.setParseAction(lambda tokens: "".join(tokens))

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

dimension = dimension_name + EQUAL + dimension_value
dimension.setParseAction(lambda tokens: "".join(tokens))

dimension_list = pyparsing.Group((LBRACE + pyparsing.Optional(
    pyparsing.delimitedList(dimension)) +
    RBRACE))("dimensions_list")

metric = metric_name + pyparsing.Optional(dimension_list)
period = integer_number("period")
threshold = decimal_number("threshold")
periods = integer_number("periods")

deterministic = (
    pyparsing.CaselessLiteral('deterministic')
)('deterministic')

function_and_metric = (
    func + LPAREN + metric +
    pyparsing.Optional(COMMA + deterministic) +
    pyparsing.Optional(COMMA + period) +
    RPAREN
)

expression = pyparsing.Forward()

sub_expression = ((function_and_metric | metric) + relational_op + threshold +
                  pyparsing.Optional(times + periods) |
                  LPAREN + expression + RPAREN)
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
        parse_result = (expression + pyparsing.stringEnd).parseString(
            self._expr)
        sub_expr_list = parse_result[0].operands_list
        return sub_expr_list


def main():
    """Used for development and testing."""

    expr_list = [
        "max(-_.千幸福的笑脸{घोड़ा=馬,  "
        "dn2=dv2,千幸福的笑脸घ=千幸福的笑脸घ}) gte 100 "
        "times 3 && "
        "(min(ເຮືອນ{dn3=dv3,家=дом}) < 10 or sum(biz{dn5=dv5}) >99 and "
        "count(fizzle) lt 0or count(baz) > 1)".decode('utf8'),

        "max(foo{hostname=mini-mon,千=千}, 120) > 100 and (max(bar)>100 "
        " or max(biz)>100)".decode('utf8'),

        "max(foo)>=100",

        "test_metric{this=that, that =  this} < 1",

        "max  (  3test_metric5  {  this  =  that  })  lt  5 times    3",

        "3test_metric5 lt 3",

        "ntp.offset > 1 or ntp.offset < -5",

        "max(3test_metric5{it's this=that's it}) lt 5 times 3",

        "count(log.error{test=1}, deterministic) > 1.0",

        "count(log.error{test=1}, deterministic, 120) > 1.0"
    ]

    for expr in expr_list:
        print('orig expr: {}'.format(expr.encode('utf8')))
        sub_exprs = []
        try:
            alarm_expr_parser = AlarmExprParser(expr)
            sub_exprs = alarm_expr_parser.sub_expr_list
        except Exception as ex:
            print("Parse failed: {}".format(ex))
        for sub_expr in sub_exprs:
            print('sub expr: {}'.format(
                sub_expr.fmtd_sub_expr_str.encode('utf8')))
            print('sub_expr dimensions: {}'.format(
                sub_expr.dimensions_str.encode('utf8')))
            print('sub_expr deterministic: {}'.format(
                sub_expr.deterministic))
            print('sub_expr period: {}'.format(
                sub_expr.period))
            print("")
        print("")


if __name__ == "__main__":
    sys.exit(main())
