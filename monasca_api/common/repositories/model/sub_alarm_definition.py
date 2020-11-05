# (C) Copyright 2014-2017 Hewlett Packard Enterprise Development LP
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

from oslo_utils import encodeutils


class SubAlarmDefinition(object):
    """Holds sub alarm definition

    Used for comparing sub alarm definitions for equality.
    """

    def __init__(self, row=None, sub_expr=None):

        """Initialize

        :param row: Database row
        :param sub_expr: Result from expression parser
        :return:
        """

        super(SubAlarmDefinition, self).__init__()

        if row and sub_expr:
            raise Exception('Only one of row or sub_expr can be specified, '
                            'not both')

        if row:
            # id is not used for compare or hash.
            self.id = row['id']
            self.alarm_definition_id = row['alarm_definition_id']
            self.metric_name = row['metric_name']
            self.dimensions_str = row['dimensions']
            self.dimensions = self._init_dimensions(row['dimensions'])
            self.function = row['function']
            self.operator = row['operator']
            self.period = row['period']
            self.periods = row['periods']
            self.threshold = row['threshold']
            self.deterministic = row['is_deterministic']

        if sub_expr:
            # id is not used for compare or hash.
            self.id = ''
            # Must be injected.
            self.alarm_definition_id = ''
            self.metric_name = sub_expr.metric_name
            self.dimensions_str = sub_expr.dimensions_str
            self.dimensions = self._init_dimensions(sub_expr.dimensions_str)
            self.function = encodeutils.safe_decode(sub_expr.normalized_func, 'utf-8')
            self.operator = sub_expr.normalized_operator
            self.period = sub_expr.period
            self.periods = sub_expr.periods
            self.threshold = sub_expr.threshold
            self.deterministic = sub_expr.deterministic

    def _init_dimensions(self, dimensions_str):
        """
        Extract dimensions from a string.

        Args:
            self: (todo): write your description
            dimensions_str: (str): write your description
        """

        dimensions = {}

        if dimensions_str:
            for dimension in dimensions_str.split(','):
                name, value = dimension.split('=')
                dimensions[name] = value

        return dimensions

    @property
    def expression(self):

        """Build the entire expressions as a string with spaces."""

        result = u"{}({}".format(self.function.lower(),
                                 self.metric_name)

        if self.dimensions_str:
            result += u"{{{}}}".format(self.dimensions_str)

        if self.deterministic:
            result += u", deterministic"

        if self.period:
            result += u", {}".format(str(self.period))

        result += u")"

        result += u" {} {}".format(self.operator,
                                   str(self.threshold))

        if self.periods:
            result += u" times {}".format(str(self.periods))

        return result

    def __hash__(self):
        """
        Returns a hash of the metric.

        Args:
            self: (todo): write your description
        """

        dimensions_str = "".join(sorted([name + value for name, value in
                                         self.dimensions.items()]))

        # don't use id to hash.
        return (hash(self.alarm_definition_id) ^
                hash(dimensions_str) ^
                hash(self.function) ^
                hash(self.metric_name) ^
                hash(self.operator) ^
                hash(self.period) ^
                hash(self.periods) ^
                hash(self.deterministic) ^
                # Convert to float to handle cases like 0.0 == 0
                hash(float(self.threshold)))

    def __repr__(self):
        """
        Return a representation of this definition.

        Args:
            self: (todo): write your description
        """

        result = 'id={},alarm_definition_id={},function={},metric_name={},dimensions={}' .format(
            self.id, self.alarm_definition_id, self.function, self.metric_name, self.dimensions)
        result += ',operator={},period={},periods={},determinstic={}'\
            .format(self.operator, self.period, self.periods, self.deterministic)
        return result

    def __eq__(self, other):
        """
        Determine if other is the same as other.

        Args:
            self: (todo): write your description
            other: (todo): write your description
        """

        if id(self) == id(other):
            return True

        if not isinstance(other, SubAlarmDefinition):
            return False

        # don't use id to compare.
        return (self.alarm_definition_id == other.alarm_definition_id and
                self.dimensions == other.dimensions and
                self.function == other.function and
                self.metric_name == other.metric_name and
                self.operator == other.operator and
                self.period == other.period and
                self.periods == other.periods and
                self.deterministic == other.deterministic and
                # Convert to float to handle cases like 0.0 == 0
                float(self.threshold) == float(other.threshold))

    def same_key_fields(self, other):
        """
        Return true if other fields of the same.

        Args:
            self: (todo): write your description
            other: (todo): write your description
        """

        # The metrics matched can't change
        return (self.metric_name == other.metric_name and
                self.dimensions == other.dimensions)
