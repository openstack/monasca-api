# Copyright 2018 SUSE Linux GmbH
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

import hashlib

from oslo_log import log
from oslo_utils import encodeutils
from sqlalchemy import MetaData
from sqlalchemy.orm import sessionmaker

LOG = log.getLogger(__name__)

# Map of SHA1 fingerprints to alembic revisions. Note that this is
# used in the pre-alembic case and does not need to be updated if a
# new revision is introduced.
_REVS = {"43e5913b0272077321ab6f25ffbcda7149b6284b": "00597b5c8325",
         "c4e5c870c705421faa4041405b5a895970faa434": "0cce983d957a",
         "f7a79c4eea9c9d130277a64eb6d2d16587088dbb": "30181b42434b",
         "529f266f7ed42929d5405616810546e4615153e8": "6b2b88f3cab4",
         "857904f960af77c0554c4c38d73ed47df7c949b4": "8781a256f0c1",
         "773489fb7bfa84bf2db0e1ff1ab96bce7fb4ecd7": "c2f85438d6f3",
         "f29f18a30519a1bae9dcee85a604eb72886e34d3": "d8b801498850",
         "dd47cb01f11cb5cd7fec6bda6a190bc10b4659a6": "f69cb3152a76",

         # Database created with UTF8 default charset
         "5dda7af1fd708095e6c9298976abb1242bbd1848": "8781a256f0c1",
         "7fb1ce4a60f0065505096843bfd21f4ef4c5d1e0": "f69cb3152a76"}


class Fingerprint(object):

    def __init__(self, engine):
        metadata = self._get_metadata(engine)
        self.schema_raw = self._get_schema_raw(metadata)
        self.sha1 = self._get_schema_sha1(self.schema_raw)
        self.revision = self._get_revision(metadata, engine, self.sha1)

    @staticmethod
    def _get_metadata(engine):
        return MetaData(bind=engine)

    @staticmethod
    def _get_schema_raw(metadata):
        schema_strings = []

        for table in metadata.sorted_tables:
            # Omit this table to maintain a consistent fingerprint when
            # fingerprint a migrated schema is fingerprinted.
            if table.name == "alembic_version":
                continue
            table.metadata = None
            columns = []
            for column in table.columns:
                column.server_default = None
                columns.append(repr(column))
            table.columns = []
            schema_strings.append(repr(table))

            for column in columns:
                schema_strings.append("  " + repr(column))

            schema_strings.append("")

        return "\n".join(schema_strings)

    @staticmethod
    def _get_schema_sha1(schema_raw):
        return hashlib.sha1(encodeutils.to_utf8(schema_raw)).hexdigest()

    @staticmethod
    def _get_revision(metadata, engine, sha1):
        # Alembic stores the current version in the DB so check that first
        # and fall back to the lookup table for the pre-alembic case.
        versions_table = metadata.tables.get('alembic_version')
        if versions_table is not None:
            return Fingerprint._lookup_version_from_db(versions_table, engine)
        elif sha1:
            return Fingerprint._lookup_version_from_table(sha1)

    @staticmethod
    def _get_db_session(engine):
        Session = sessionmaker(bind=engine)
        return Session()

    @staticmethod
    def _lookup_version_from_db(versions_table, engine):
        session = Fingerprint._get_db_session(engine)
        # This will throw an exception for the unexpected case when there is
        # more than one row. The query returns a tuple which is stripped off
        # before returning.
        return session.query(versions_table).one()[0]

    @staticmethod
    def _lookup_version_from_table(sha1):
        revision = _REVS.get(sha1)
        if not revision:
            LOG.warning("Fingerprint: {} does not match any revisions."
                        .format(sha1))
        return revision
