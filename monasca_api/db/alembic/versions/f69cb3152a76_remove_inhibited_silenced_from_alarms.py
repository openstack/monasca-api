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

"""Remove inhibited and silenced from alarms (Git revision f69cb3152a76e7c586dcc9a03600d1d4ed32c4e6)

Revision ID: f69cb3152a76
Revises: 8781a256f0c1
Create Date: 2018-04-24 13:16:04.157977

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = 'f69cb3152a76'
down_revision = '8781a256f0c1'
branch_labels = None
depends_on = None


def upgrade():
    op.drop_column('alarm', 'inhibited')
    op.drop_column('alarm', 'silenced')


def downgrade():
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
