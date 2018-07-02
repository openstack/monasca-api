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

"""Add inhibited and silenced to alarms (Git revision 8781a256f0c19662b81f04b014e2b769e625bd6b)

Revision ID: 8781a256f0c1
Revises: d8b801498850
Create Date: 2018-04-24 13:16:04.157977

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '8781a256f0c1'
down_revision = 'd8b801498850'
branch_labels = None
depends_on = None


def upgrade():
    op.add_column('alarm',
                  sa.Column('inhibited',
                            sa.Boolean(),
                            nullable=False,
                            server_default='0'))
    op.add_column('alarm',
                  sa.Column('silenced',
                            sa.Boolean(),
                            nullable=False,
                            server_default='0'))


def downgrade():
    op.drop_column('alarm', 'inhibited')
    op.drop_column('alarm', 'silenced')
