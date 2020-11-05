# Copyright 2017 FUJITSU LIMITED
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

from oslo_config import cfg
from oslo_config import types
from oslo_utils import netutils


class HostAddressPortOpt(cfg.Opt):
    """Option for HostAddressPortType.

    Accept hostname or ip address with TCP/IP port number.
    """
    def __init__(self, name, **kwargs):
        """
        Initialize a new port.

        Args:
            self: (todo): write your description
            name: (str): write your description
        """
        ip_port_type = HostAddressPortType()
        super(HostAddressPortOpt, self).__init__(name,
                                                 type=ip_port_type,
                                                 **kwargs)


class HostAddressPortType(types.HostAddress):
    """HostAddress with additional port."""

    def __init__(self, version=None):
        """
        Initialize a connection.

        Args:
            self: (todo): write your description
            version: (todo): write your description
        """
        type_name = 'ip and port value'
        super(HostAddressPortType, self).__init__(version, type_name=type_name)

    def __call__(self, value):
        """
        Returns a string representing the port.

        Args:
            self: (todo): write your description
            value: (str): write your description
        """
        addr, port = netutils.parse_host_port(value)
        # NOTE(gmann): parse_host_port() return port as None if no port is
        # supplied in value so setting port as string for correct
        # parsing and error otherwise it will not be parsed for NoneType.
        port = 'None' if port is None else port
        addr = self.validate_addr(addr)
        port = self._validate_port(port)
        if not addr and not port:
            raise ValueError('%s is not valid ip with optional port')
        return '%s:%d' % (addr, port)

    @staticmethod
    def _validate_port(port):
        """
        Validate a port.

        Args:
            port: (int): write your description
        """
        return types.Port()(port)

    def validate_addr(self, addr):
        """
        Validate the given address.

        Args:
            self: (todo): write your description
            addr: (str): write your description
        """
        try:
            addr = self.ip_address(addr)
        except ValueError:
            try:
                addr = self.hostname(addr)
            except ValueError:
                raise ValueError("%s is not a valid host address", addr)
        return addr
