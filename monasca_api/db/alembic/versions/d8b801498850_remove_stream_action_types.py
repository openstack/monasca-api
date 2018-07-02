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

"""Remove stream action types (Git revision d8b80149885016ede0ee403cf9bb07f9b7253297)

Revision ID: d8b801498850
Revises: 6b2b88f3cab4
Create Date: 2018-04-24 12:53:02.342849

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = 'd8b801498850'
down_revision = '6b2b88f3cab4'
branch_labels = None
depends_on = None


def upgrade():
    op.drop_table('stream_actions_action_type')


def downgrade():
    stream_action_types = op.create_table(
        'stream_actions_action_type',
        sa.Column('name',
                  sa.String(length=20),
                  nullable=False),
        sa.PrimaryKeyConstraint('name'),
        mysql_charset='utf8mb4',
        mysql_collate='utf8mb4_unicode_ci')

    op.bulk_insert(stream_action_types,
                   [{'name': 'FIRE'},
                    {'name': 'EXPIRE'}])
