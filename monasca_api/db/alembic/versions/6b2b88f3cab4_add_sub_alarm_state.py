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

"""Add sub alarm state (Git revision 6b2b88f3cab46cd442369b22da3624611b871169)

Revision ID: 6b2b88f3cab4
Revises: 30181b42434b
Create Date: 2018-04-24 12:16:15.812274

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '6b2b88f3cab4'
down_revision = '30181b42434b'
branch_labels = None
depends_on = None


def upgrade():
    op.add_column(
        'sub_alarm',
        sa.Column('state',
                  sa.String(length=20),
                  sa.ForeignKey('alarm_state.name'),
                  nullable=False,
                  server_default='OK'))


def downgrade():
    op.drop_column('sub_alarm', 'state')
