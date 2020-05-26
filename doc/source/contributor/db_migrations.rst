Database Migrations
-------------------

Monasca uses `Alembic <http://alembic.zzzcomputing.com/en/latest/>`_
migrations to set up its configuration database. If you need to change the
configuration database's schema, you need to create a migration to adjust the
database accordingly, as follows::

    cd monasca_api/db/
    alembic revision

This will create a new skeleton revision for you to edit. You will find
existing revisions to use for inspiration in the
``/monasca_api/db/alembic/versions/`` directory.

Measurement data stored in a Time Series database (such as InfluxDB) would
be migrated to a new version using standard practice for a given TSDB.
