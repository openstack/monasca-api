====================
Administration guide
====================

.. toctree::
   :maxdepth: 2

Schema Setup
~~~~~~~~~~~~

For setting up the Monasca configuration database, we provide ``monasca_db``,
an Alembic based database migration tool. Historically, the schema for the
configuration database was created by a SQL script. This SQL was changed a
couple of times, so ``monasca_db`` comes with a mechanism to detect the SQL
script revision being used to create it and stamp the database with the
matching Alembic revision.

Setting up a new database
-------------------------

If you are deploying Monasca from scratch, database setup is quite
straightforward:

1. Create a database and configure access credentials with ``ALL PRIVILEGES``
   permission level on it in the Monasca API configuration file's
   ``[database]`` section.

2. Run schema migrations: ``monasca_db upgrade``. It will run all migrations up
   to and including the most recent one (``head``) unless a revision to migrate
   to is explicitly specified.


Upgrading Existing Database from Legacy Schema
----------------------------------------------

If you have been running an older version of Monasca, you can attempt to
identify and stamp its database schema:

::

    monasca_db stamp --from-fingerprint

This command will generate a unique fingerprint for the database schema in
question and match that fingerprint with an in-code map of fingerprints to
database schema revisions. This should work for all official (shipped as part
of the ``monasca-api`` repository) schema scripts. If you used a custom
third-party schema script to set up the database, it may not be listed and
you'll get an error message similar to this one (the fingerprint hash will
vary):

::

    Schema fingerprint 3d45493070e3b8e6fc492d2369e51423ca4cc1ac does not match any known legacy revision.

If this happens to you, please create a Storyboard story against the
`openstack/monasca-api project <https://storyboard.openstack.org/#!/project/863>`_.
Provide the following alongside the story:

1. A copy of or pointer to the schema SQL script being used to set up the
   database.

2. The fingerprint shown in the error message.

3. The output of ``monasca_db fingerprint --raw``.

Time Series Databases Setup
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Enabling InfluxDB Time Series Index in existing deployments
-----------------------------------------------------------

If enabling TSI on an existing InfluxDB install please follow the instructions
for migrating existing data here:
https://docs.influxdata.com/influxdb/v1.7/administration/upgrading/#upgrading-influxdb-1-3-1-4-no-tsi-preview-to-1-7-x-tsi-enabled

Database Per Tenant
-------------------

It is envisaged that separate database per tenant will be the default
behaviour in a future release of Monasca. Not only would it make queries
faster for tenants, it would also allow administrators to define
retention policy per tenancy. To enable this, set
`influxdb.db_per_tenant` to `True` in `monasca-{api,persister}` config
(it defaults to `False` at the moment if not set).

To migrate existing data to database per tenant, refer to README.rst
under the following URL which also contains the Python script to
facilitate migration:
https://opendev.org/openstack/monasca-persister/src/branch/master/monasca_persister/tools/db-per-tenant/
