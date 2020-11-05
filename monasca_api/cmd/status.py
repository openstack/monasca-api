# Copyright (c) 2018 SUSE LLC
#
#    Licensed under the Apache License, Version 2.0 (the "License"); you may
#    not use this file except in compliance with the License. You may obtain
#    a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#    License for the specific language governing permissions and limitations
#    under the License.

"""
CLI interface for monasca status commands.
https://governance.openstack.org/tc/goals/stein/upgrade-checkers.html
"""

import sys

from oslo_config import cfg
from oslo_upgradecheck import upgradecheck


def _(message):
    """
    Returns a function that takes a message.

    Args:
        message: (str): write your description
    """
    # TODO(joadavis): simplified localization, Monasca not using oslo_i18n
    return message


class Checks(upgradecheck.UpgradeCommands):

    """Various upgrade checks should be added as separate methods in this class
    and added to _upgrade_checks tuple.
    """

    def _check_placeholder(self):
        """
        Check if the placeholder object

        Args:
            self: (todo): write your description
        """
        # This is just a placeholder for upgrade checks, it should be
        # removed when the actual checks are added
        return upgradecheck.Result(upgradecheck.Code.SUCCESS)

    # The format of the check functions is to return an
    # oslo_upgradecheck.upgradecheck.Result
    # object with the appropriate
    # oslo_upgradecheck.upgradecheck.Code and details set.
    # If the check hits warnings or failures then those should be stored
    # in the returned Result's "details" attribute. The
    # summary will be rolled up at the end of the check() method.
    _upgrade_checks = (
        # In the future there should be some real checks added here
        (_('Placeholder'), _check_placeholder),
    )


def main():
    """
    Main entry point.

    Args:
    """
    return upgradecheck.main(
        cfg.CONF, project='monasca', upgrade_command=Checks())


if __name__ == '__main__':
    sys.exit(main())
