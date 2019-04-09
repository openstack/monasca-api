# Copyright 2017 StackHPC
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

import collections


class Dimension(collections.namedtuple('Dimension', 'name values')):
    """Representation of dimension names and optional values list.

    Named-tuple type to represent the pairing of a dimension name and an
    optional list of values.

    :ivar name: Name of the dimension to reference.
    :ivar values: Optional list of values associated with the dimension.

    :vartype name: str
    :vartype values: None or list[str]
    """


class SortBy(collections.namedtuple('SortBy', 'field direction')):
    """Representation of an individual sorting directive.

    Named-tuple type to represent a directive for indicating how a result set
    should be sorted.

    :ivar field: Name of the field which is provides the values to sort by.
    :ivar direction: Either 'asc' or 'desc' specifying the order of values.

    :vartype name: str
    :vartype values: str
    """
