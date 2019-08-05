# Copyright 2015 FUJITSU LIMITED
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


class UnsupportedContentTypeException(Exception):
    """Exception thrown if content type is not supported."""
    pass


class UnreadableContentError(IOError):
    """Exception thrown if reading data fails

    :py:class`.UnreadableContentError` may be thrown
    if data was impossible to read from input

    """
    pass


class DataConversionException(Exception):
    """Exception thrown if data transformation fails

    :py:class`.DataConversionException` may be thrown
    if data was impossible to transform into target
    representation according to content_type classifier.

    """
    pass
