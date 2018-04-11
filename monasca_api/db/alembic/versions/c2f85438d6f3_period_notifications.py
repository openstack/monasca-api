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

"""Add period to notifications (Git revision c2f85438d6f3b0fd2e1f86d84eee6e9967025eb6)

Revision ID: c2f85438d6f3
Revises: 0cce983d957a
Create Date: 2018-04-23 14:47:49.413502

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = 'c2f85438d6f3'
down_revision = '0cce983d957a'
branch_labels = None
depends_on = None


def upgrade():
    op.add_column('notification_method',
                  sa.Column('period',
                            sa.Integer(),
                            nullable=False,
                            server_default='0'))


def downgrade():
    op.drop_column('notification_method', 'period')
