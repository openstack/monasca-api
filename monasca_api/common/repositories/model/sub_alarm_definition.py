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
            self.deterministic = str(row['is_deterministic']) == '1'

        if sub_expr:
            # id is not used for compare or hash.
            self.id = ''
            # Must be injected.
            self.alarm_definition_id = ''
            self.metric_name = sub_expr.metric_name
            self.dimensions_str = sub_expr.dimensions_str
            self.dimensions = self._init_dimensions(sub_expr.dimensions_str)
            self.function = sub_expr.normalized_func.decode('utf8')
            self.operator = sub_expr.normalized_operator
            self.period = sub_expr.period
            self.periods = sub_expr.periods
            self.threshold = sub_expr.threshold
            self.deterministic = sub_expr.deterministic

    def _init_dimensions(self, dimensions_str):

        dimensions = {}

        if dimensions_str:
            for dimension in dimensions_str.split(','):
                name, value = dimension.split('=')
                dimensions[name] = value

        return dimensions

    @property
    def expression(self):

        """Build the entire expressions as a string with spaces."""

        result = "{}({}".format(self.function.lower().encode('utf8'),
                                self.metric_name.encode('utf8'))

        if self.dimensions_str:
            result += "{{{}}}".format(self.dimensions_str.encode('utf8'))

        if self.deterministic:
            result += ', deterministic'

        if self.period:
            result += ", {}".format(str(self.period).encode('utf8'))

        result += ")"

        result += " {} {}".format(self.operator.encode('utf8'),
                                  str(self.threshold).encode('utf8'))

        if self.periods:
            result += " times {}".format(str(self.periods).encode('utf8'))

        return result.decode('utf8')

    def __hash__(self):

        dimensions_str = "".join(sorted([name + value for name, value in
                                         self.dimensions.iteritems()]))

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

    def __eq__(self, other):

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

        # compare everything but operator, threshold and deterministic
        return (self.metric_name == other.metric_name and
                self.dimensions == other.dimensions and
                self.function == other.function and
                self.period == other.period and
                self.periods == other.periods)
