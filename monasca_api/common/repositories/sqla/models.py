# Copyright 2015 Robin Hood
# Copyright 2016 FUJITSU LIMITED
# (C) Copyright 2016 Hewlett Packard Enterprise Development LP
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function
from __future__ import unicode_literals

from sqlalchemy import Column
from sqlalchemy.ext import compiler
from sqlalchemy.sql import expression
from sqlalchemy import String, DateTime, Boolean, Integer, Binary, Float
from sqlalchemy import Table


def create_a_model(metadata=None):
    return Table('alarm', metadata,
                 Column('id', String(36)),
                 Column('alarm_definition_id', String(36)),
                 Column('state', String(20)),
                 Column('lifecycle_state', String(50)),
                 Column('link', String(512)),
                 Column('created_at', DateTime),
                 Column('state_updated_at', DateTime),
                 Column('updated_at', DateTime))


def create_sadd_model(metadata=None):
    return Table('sub_alarm_definition_dimension', metadata,
                 Column('sub_alarm_definition_id', String(36)),
                 Column('dimension_name', String(255)),
                 Column('value', String(255)))


def create_aa_model(metadata=None):
    return Table('alarm_action', metadata,
                 Column('alarm_definition_id', String(36)),
                 Column('alarm_state', String(20)),
                 Column('action_id', String(36)))


def create_md_model(metadata=None):
    return Table('metric_dimension', metadata,
                 Column('dimension_set_id', Binary),
                 Column('name', String(255)),
                 Column('value', String(255)))


def create_mde_model(metadata=None):
    return Table('metric_definition', metadata,
                 Column('id', Binary),
                 Column('name', String(255)),
                 Column('tenant_id', String(255)),
                 Column('region', String(255)))


def create_nm_model(metadata=None):
    return Table('notification_method', metadata,
                 Column('id', String(36)),
                 Column('tenant_id', String(36)),
                 Column('name', String(250)),
                 Column('type', String(20)),
                 Column('address', String(512)),
                 Column('period', Integer),
                 Column('created_at', DateTime),
                 Column('updated_at', DateTime))


def create_nmt_model(metadata=None):
    return Table('notification_method_type', metadata,
                 Column('name', String(20), primary_key=True))


def create_mdd_model(metadata=None):
    return Table('metric_definition_dimensions', metadata,
                 Column('id', Binary),
                 Column('metric_definition_id', Binary),
                 Column('metric_dimension_set_id', Binary))


def create_am_model(metadata=None):
    return Table('alarm_metric', metadata,
                 Column('alarm_id', String(36)),
                 Column('metric_definition_dimensions_id', Binary))


def create_ad_model(metadata=None):
    return Table('alarm_definition', metadata,
                 Column('id', String(36)),
                 Column('tenant_id', String(36)),
                 Column('name', String(255)),
                 Column('description', String(255)),
                 Column('expression', String),
                 Column('severity', String(20)),
                 Column('match_by', String(255)),
                 Column('actions_enabled', Boolean),
                 Column('created_at', DateTime),
                 Column('updated_at', DateTime),
                 Column('deleted_at', DateTime))


def create_sa_model(metadata=None):
    return Table('sub_alarm', metadata,
                 Column('id', String(36)),
                 Column('alarm_id', String(36)),
                 Column('sub_expression_id', String(36)),
                 Column('expression', String),
                 Column('created_at', DateTime),
                 Column('updated_at', DateTime))


def create_sad_model(metadata=None):
    return Table('sub_alarm_definition', metadata,
                 Column('id', String(36)),
                 Column('alarm_definition_id', String(36)),
                 Column('function', String(10)),
                 Column('metric_name', String(100)),
                 Column('operator', String(5)),
                 Column('threshold', Float),
                 Column('period', Integer),
                 Column('periods', Integer),
                 Column('is_deterministic', Boolean),
                 Column('created_at', DateTime),
                 Column('updated_at', DateTime))


class group_concat(expression.ColumnElement):
    name = "group_concat"
    order_by = None
    separator = ','
    columns = []

    def __init__(self, columns, separator=',', order_by=None):
        self.order_by = order_by
        self.separator = separator
        self.columns = columns


@compiler.compiles(group_concat, 'oracle')
def _group_concat_oracle(element, compiler_, **kw):
    str_order_by = ''
    if element.order_by is not None and len(element.order_by) > 0:
        str_order_by = "ORDER BY {0}".format(", ".join([compiler_.process(x) for x in element.order_by]))
    else:
        str_order_by = "ORDER BY {0}".format(", ".join([compiler_.process(x) for x in element.columns]))
    return "LISTAGG({0}, '{2}') WITHIN GROUP ({1})".format(
        ", ".join([compiler_.process(x) for x in element.columns]),
        str_order_by,
        element.separator,
    )


@compiler.compiles(group_concat, 'postgresql')
def _group_concat_postgresql(element, compiler_, **kw):
    str_order_by = ''
    if element.order_by is not None and len(element.order_by) > 0:
        str_order_by = "ORDER BY {0}".format(", ".join([compiler_.process(x) for x in element.order_by]))

    return "STRING_AGG({0}, '{2}' {1})".format(
        ", ".join([compiler_.process(x) for x in element.columns]),
        str_order_by,
        element.separator,
    )


@compiler.compiles(group_concat, 'sybase')
def _group_concat_sybase(element, compiler_, **kw):
    return "LIST({0}, '{1}')".format(
        ", ".join([compiler_.process(x) for x in element.columns]),
        element.separator,
    )


@compiler.compiles(group_concat, 'mysql')
def _group_concat_mysql(element, compiler_, **kw):
    str_order_by = ''
    if element.order_by is not None and len(element.order_by) > 0:
        str_order_by = "ORDER BY {0}".format(",".join([compiler_.process(x) for x in element.order_by]))
    return "GROUP_CONCAT({0} {1} SEPARATOR '{2}')".format(
        ", ".join([compiler_.process(x) for x in element.columns]),
        str_order_by,
        element.separator,
    )


@compiler.compiles(group_concat)
def _group_concat_default(element, compiler_, **kw):
    return "GROUP_CONCAT({0}, '{1}')".format(
        ", ".join([compiler_.process(x) for x in element.columns]),
        element.separator,
    )


class field_sort(expression.ColumnElement):
    name = "field_sort"
    column = None
    fields = []

    def __init__(self, column, fields):
        self.column = column
        self.fields = fields


@compiler.compiles(field_sort, "mysql")
def _field_sort_mysql(element, compiler_, **kw):
    if element.fields:
        return "FIELD({0}, {1})".format(compiler_.process(element.column),
                                        ", ".join(map(compiler_.process,
                                                      element.fields)))
    else:
        return str(compiler_.process(element.column))


@compiler.compiles(field_sort)
def _field_sort_general(element, compiler_, **kw):
    fields_list = []
    if element.fields:
        fields_list.append("CASE")
        for idx, field in enumerate(element.fields):
            fields_list.append("WHEN {0}={1} THEN {2}".format(compiler_.process(element.column),
                                                              compiler_.process(field),
                                                              idx))
        fields_list.append("ELSE {0}".format(len(element.fields)))
        fields_list.append("END")
    return " ".join(fields_list)
