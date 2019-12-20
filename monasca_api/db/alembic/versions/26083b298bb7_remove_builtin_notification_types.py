# Copyright 2018 StackHPC Ltd
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
"""Remove builtin notification types

Revision ID: 26083b298bb7
Revises: f69cb3152a76
Create Date: 2018-09-18 13:52:02.170226

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.sql import table

_NM_BUILT_IN_TYPES = set(['EMAIL', 'WEBHOOK', 'PAGERDUTY'])

# revision identifiers, used by Alembic.
revision = '26083b298bb7'
down_revision = 'f69cb3152a76'
branch_labels = None
depends_on = None

_nm_types = table(
    'notification_method_type',
    sa.Column('name',
              sa.String(length=20),
              nullable=False))

_nm = table(
    'notification_method',
    sa.Column('type',
              sa.String(length=20),
              nullable=False))


def upgrade():
    # Built-in notification types have been removed. Here, we
    # remove them (where not in use) and rely on Monasca Notification
    # to re-populate the table according to what is set in its config file.

    # Start by creating a set of all notification method types currently
    # configured in the Monasca DB
    connection = op.get_bind()
    nm_types_configured = connection.execute(_nm.select()).fetchall()
    nm_types_configured = set([nm_type[0] for nm_type in nm_types_configured])

    # Remove all built in notification types which are currently *not*
    # configured.
    nm_types_to_remove = _NM_BUILT_IN_TYPES.difference(nm_types_configured)
    op.execute(_nm_types.delete().where(
        _nm_types.c.name.in_(nm_types_to_remove)))


def downgrade():
    # Some or all of these might be present if they have been explicitly
    # enabled in monasca-notification.
    op.execute(_nm_types.insert().prefix_with("IGNORE").values(
        [{'name': 'EMAIL'},
         {'name': 'WEBHOOK'},
         {'name': 'PAGERDUTY'}]))
