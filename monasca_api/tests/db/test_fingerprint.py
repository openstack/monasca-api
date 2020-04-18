# Copyright 2018 StackHPC Ltd.
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
from unittest import mock

import monasca_api.db.fingerprint as fingerprint
from monasca_api.tests import base


class TestFingerprint(base.BaseTestCase):

    @mock.patch('monasca_api.db.fingerprint.Fingerprint._get_metadata')
    @mock.patch('monasca_api.db.fingerprint.Fingerprint._get_schema_raw')
    def test_get_schema_raw_pre_alembic(self, mock_schema_raw, mock_metadata):
        mock_schema_raw.return_value = 'dummy_schema_raw'

        tables = mock.PropertyMock = {
            'dummy_table': 'dummy_columns'
        }
        mock_metadata.return_value.tables = tables

        # No Alembic revision ID exists in the DB so we look it up from the
        # table of fingerprints. Since we use a dummy schema, we insert a dummy
        # entry into the lookup table.
        fingerprint._REVS[
            hashlib.sha1(b'dummy_schema_raw').hexdigest()] = 'dummy_revision'

        f = fingerprint.Fingerprint('mock_engine')
        self.assertEqual(f.schema_raw, 'dummy_schema_raw')
        self.assertEqual(f.sha1, hashlib.sha1(b'dummy_schema_raw').hexdigest())
        self.assertEqual(f.revision, 'dummy_revision')

    @mock.patch('monasca_api.db.fingerprint.Fingerprint._get_db_session')
    @mock.patch('monasca_api.db.fingerprint.Fingerprint._get_metadata')
    @mock.patch('monasca_api.db.fingerprint.Fingerprint._get_schema_raw')
    def test_get_schema_raw_post_alembic(
            self, mock_schema_raw, mock_metadata, mock_db_session):
        mock_schema_raw.return_value = 'dummy_schema_raw'

        tables = mock.PropertyMock = {
            'alembic_version': 'dummy_version',
            'dummy_table': 'dummy_columns'
        }
        mock_metadata.return_value.tables = tables

        # Alembic sets the version in the DB, so we look it up from there
        mock_db_session.return_value.query.return_value.one.return_value = (
            'dummy_revision',)

        f = fingerprint.Fingerprint('mock_engine')
        self.assertEqual(f.schema_raw, 'dummy_schema_raw')
        self.assertEqual(f.sha1, hashlib.sha1(b'dummy_schema_raw').hexdigest())
        self.assertEqual(f.revision, 'dummy_revision')
