======================
Command Line Interface
======================

monasca (python-monascaclient)
==============================
This is the main command line interface for working with the
Monasca services, including retrieving metrics from storage.

See the https://docs.openstack.org/python-monascaclient/latest/ for details.


monasca_db
==========
CLI for Monasca database management.
::

  usage: api [-h] [--config-dir DIR] [--config-file PATH] [--version]
             {fingerprint,detect-revision,stamp,upgrade,version} ...


monasca-status
==============
CLI for checking the status of Monasca.

Use the command `monasca-status upgrade check` to check
the readiness of the system for an upgrade.

**Return Codes**

  .. list-table::
     :widths: 20 80
     :header-rows: 1

     * - Return code
       - Description
     * - 0
       - All upgrade readiness checks passed successfully and there is nothing
         to do.
     * - 1
       - At least one check encountered an issue and requires further
         investigation. This is considered a warning but the upgrade may be OK.
     * - 2
       - There was an upgrade status check failure that needs to be
         investigated. This should be considered something that stops an
         upgrade.
     * - 255
       - An unexpected error occurred.

**History**

Introduced in the Stein cycle as part of the OpenStack Community wide goal.
https://governance.openstack.org/tc/goals/stein/upgrade-checkers.html
