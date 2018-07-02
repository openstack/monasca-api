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

"""Remove event related tables and schema_migrations table (git revision
30181b42434bdde0c40abd086e903600b24e9684)

Revision ID: 30181b42434b
Revises: c2f85438d6f3
Create Date: 2018-04-24 09:54:50.024470

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '30181b42434b'
down_revision = 'c2f85438d6f3'
branch_labels = None
depends_on = None


def upgrade():
    op.drop_table('event_transform')
    op.drop_table('schema_migrations')
    op.drop_table('stream_actions')
    op.drop_table('stream_definition')


def downgrade():
    op.create_table(
        'event_transform',
        sa.Column('id',
                  sa.dialects.mysql.VARCHAR(length=36, charset='utf8mb4',
                                            collation='utf8mb4_unicode_ci'),
                  nullable=False),
        sa.Column('tenant_id',
                  sa.dialects.mysql.VARCHAR(length=36, charset='utf8mb4',
                                            collation='utf8mb4_unicode_ci'),
                  nullable=False),
        sa.Column('name',
                  sa.dialects.mysql.VARCHAR(length=64, charset='utf8mb4',
                                            collation='utf8mb4_unicode_ci'),
                  nullable=False),
        sa.Column('description',
                  sa.dialects.mysql.VARCHAR(length=250, charset='utf8mb4',
                                            collation='utf8mb4_unicode_ci'),
                  nullable=False),
        sa.Column('specification',
                  sa.dialects.mysql.LONGTEXT(charset='utf8mb4',
                                             collation='utf8mb4_unicode_ci'),
                  nullable=False),
        sa.Column('enabled',
                  sa.Boolean(),
                  nullable=True,
                  server_default=None),
        sa.Column('created_at',
                  sa.DateTime(),
                  nullable=False),
        sa.Column('updated_at',
                  sa.DateTime(),
                  nullable=False),
        sa.Column('deleted_at',
                  sa.DateTime(),
                  nullable=True,
                  server_default=None),
        sa.PrimaryKeyConstraint('id'),
        sa.Index('name', 'name'),
        sa.Index('tenant_id', 'tenant_id'),
        sa.Index('deleted_at', 'deleted_at'),
        sa.Index('created_at', 'created_at'),
        sa.Index('updated_at', 'updated_at'),
        sa.UniqueConstraint('tenant_id', 'name', name='tenant_name'),
        mysql_charset='utf8mb4')

    op.create_table(
        'schema_migrations',
        sa.Column('version',
                  sa.String(length=255),
                  nullable=False),
        sa.UniqueConstraint('version', name='unique_schema_migrations'),
        mysql_charset='latin1')

    op.create_table(
        'stream_actions',
        sa.Column('stream_definition_id',
                  sa.String(length=36),
                  sa.ForeignKey
                  ('stream_definition.id',
                   name='fk_stream_action_stream_definition_id',
                   ondelete='CASCADE'),
                  nullable=False),
        sa.Column('action_id',
                  sa.String(length=36),
                  sa.ForeignKey('notification_method.id',
                                name='fk_stream_action_notification_method_id',
                                ondelete='CASCADE'),
                  nullable=False),
        sa.Column('action_type',
                  sa.String(length=20),
                  sa.ForeignKey('stream_actions_action_type.name'),
                  nullable=False),
        sa.PrimaryKeyConstraint('stream_definition_id', 'action_id',
                                'action_type'),
        sa.Index('stream_definition_id', 'stream_definition_id'),
        sa.Index('action_type', 'action_type'),
        mysql_charset='utf8mb4',
        mysql_collate='utf8mb4_unicode_ci')

    op.create_table(
        'stream_definition',
        sa.Column('id',
                  sa.String(length=36),
                  nullable=False),
        sa.Column('tenant_id',
                  sa.String(length=36),
                  nullable=False),
        sa.Column('name',
                  sa.String(length=190),
                  nullable=False,
                  server_default=''),
        sa.Column('description',
                  sa.String(length=255),
                  nullable=True,
                  server_default=None),
        sa.Column('select_by',
                  sa.dialects.mysql.LONGTEXT(),
                  nullable=True,
                  server_default=None),
        sa.Column('group_by',
                  sa.dialects.mysql.LONGTEXT(length=20),
                  nullable=True,
                  server_default=None),
        sa.Column('fire_criteria',
                  sa.dialects.mysql.LONGTEXT(length=20),
                  nullable=True,
                  server_default=None),
        sa.Column('expiration',
                  sa.dialects.mysql.INTEGER(display_width=10,
                                            unsigned=True),
                  nullable=True,
                  server_default='0'),
        sa.Column('actions_enabled',
                  sa.Boolean(),
                  nullable=False,
                  server_default='1'),
        sa.Column('created_at',
                  sa.DateTime(),
                  nullable=False),
        sa.Column('updated_at',
                  sa.DateTime(),
                  nullable=False),
        sa.Column('deleted_at',
                  sa.DateTime(),
                  nullable=True,
                  server_default=None),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('tenant_id', 'name', name='tenant_name'),
        sa.Index('name', 'name'),
        sa.Index('tenant_id', 'tenant_id'),
        sa.Index('deleted_at', 'deleted_at'),
        sa.Index('created_at', 'created_at'),
        sa.Index('updated_at', 'updated_at'),
        mysql_charset='utf8mb4',
        mysql_collate='utf8mb4_unicode_ci')
