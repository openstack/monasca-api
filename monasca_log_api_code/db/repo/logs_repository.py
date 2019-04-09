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

import abc
import six


@six.add_metaclass(abc.ABCMeta)
class LogsRepository(object):

    def __init__(self):
        super(LogsRepository, self).__init__()

    @abc.abstractmethod
    def list_logs(self, tenant_id, dimensions, start_time, end_time, offset,
                  limit, sort_by):
        """Obtain log listing based on simple criteria of dimension values.

        Performs queries on the underlying log storage against a time range and
        set of dimension values. Additionally, it is possible to optionally
        sort results by timestamp.

        :param tenant_id:
            Tenant/project id for which to obtain logs (required).
        :param dimensions:
            List of Dimension tuples containing pairs of dimension names and
            optional lists of dimension values. These will be used to filter
            the logs returned. If no dimensions are specified, then no
            filtering is performed. When multiple values are given, the
            dimension must match any of the given values. If None is given,
            logs with any value for the dimension will be returned.
        :param start_time:
            Optional starting time in UNIX time (seconds, inclusive).
        :param end_time:
            Optional ending time in UNIX time (seconds, inclusive).
        :param offset:
            Number of matching results to skip past, if specified.
        :param limit:
            Number of matching results to return (required).
        :param sort_by:
            List of SortBy tuples specifying fields to sort by and the
            direction to sort the result set by. e.g. ('timestamp','asc'). The
            direction is specified by either the string 'asc' for ascending
            direction, or 'desc' for descending. If not specified, no order
            must be enforced and the implementation is free to choose the most
            efficient method to return the results.

        :type tenant_id: str
        :type dimensions: None or list[Dimension[str, list[str] or None]]
        :type start_time: None or int
        :type end_time: None or int
        :type offset: None or int
        :type limit: int
        :type sort_by: None or list[SortBy[str, str]]

        :return:
            Log messages matching the given criteria. The dict representing
            each message entry will contain attributes extracted from the
            underlying structure; 'message', 'timestamp' and 'dimensions'.

        :rtype: list[dict]
        """
        pass
