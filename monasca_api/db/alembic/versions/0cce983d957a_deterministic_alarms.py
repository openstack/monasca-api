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

""" Add flag for deterministic alarms (Git revision 0cce983d957a3d780b6d206ad25df1271a812b4a).

Revision ID: 0cce983d957a
Revises: 00597b5c8325
Create Date: 2018-04-23 13:57:32.951669

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '0cce983d957a'
down_revision = '00597b5c8325'
branch_labels = None
depends_on = None


def upgrade():
    op.add_column('sub_alarm_definition',
                  sa.Column('is_deterministic',
                            sa.Boolean(),
                            nullable=False,
                            server_default='0'))


def downgrade():
    op.drop_column('sub_alarm_definition', 'is_deterministic')
