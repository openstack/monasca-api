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

"""Initial migration for full schema (Git revision 00597b5c8325664c2c534625525f59232d243d66).

Revision ID: 00597b5c8325
Revises: N/A
Create Date: 2018-04-12 09:09:48.212206

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '00597b5c8325'
down_revision = None
branch_labels = None
depends_on = None


def upgrade():
    # Enum tables (will be prepopulated with values through bulk_insert)

    alarm_states = op.create_table('alarm_state',
                                   sa.Column('name',
                                             sa.String(length=20),
                                             nullable=False),
                                   sa.PrimaryKeyConstraint('name'),
                                   mysql_charset='utf8mb4',
                                   mysql_collate='utf8mb4_unicode_ci'
                                   )

    op.bulk_insert(alarm_states,
                   [{'name': 'UNDETERMINED'},
                    {'name': 'OK'},
                    {'name': 'ALARM'}])

    ad_severities = op.create_table(
        'alarm_definition_severity',
        sa.Column('name',
                  sa.String(length=20),
                  nullable=False),
        sa.PrimaryKeyConstraint('name'),
        mysql_charset='utf8mb4',
        mysql_collate='utf8mb4_unicode_ci')

    op.bulk_insert(ad_severities,
                   [{'name': 'LOW'},
                    {'name': 'MEDIUM'},
                    {'name': 'HIGH'},
                    {'name': 'CRITICAL'}])

    nm_types = op.create_table(
        'notification_method_type',
        sa.Column('name',
                  sa.String(length=20),
                  nullable=False),
        sa.PrimaryKeyConstraint('name'),
        mysql_charset='utf8mb4',
        mysql_collate='utf8mb4_unicode_ci')

    op.bulk_insert(nm_types,
                   [{'name': 'EMAIL'},
                    {'name': 'WEBHOOK'},
                    {'name': 'PAGERDUTY'}])

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

    op.create_table(
        'alarm_definition',
        sa.Column('id',
                  sa.String(length=36),
                  nullable=False),
        sa.Column('tenant_id',
                  sa.String(length=36),
                  nullable=False),
        sa.Column('name',
                  sa.String(length=255),
                  nullable=False,
                  server_default=''),
        sa.Column('description',
                  sa.String(length=255),
                  nullable=True,
                  server_default=None),
        sa.Column('expression',
                  sa.dialects.mysql.LONGTEXT(),
                  nullable=False),
        sa.Column('severity',
                  sa.String(length=20),
                  nullable=False),
        sa.Column('match_by',
                  sa.String(length=255),
                  nullable=True,
                  server_default=''),
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
        sa.Index('tenant_id', 'tenant_id'),
        sa.Index('deleted_at', 'deleted_at'),
        sa.Index('fk_alarm_definition_severity', 'severity'),
        sa.ForeignKeyConstraint(['severity'],
                                ['alarm_definition_severity.name']),
        mysql_charset='utf8mb4',
        mysql_collate='utf8mb4_unicode_ci')

    op.create_table(
        'alarm',
        sa.Column('id',
                  sa.String(length=36),
                  nullable=False),
        sa.Column('alarm_definition_id',
                  sa.String(length=36),
                  nullable=False,
                  server_default=''),
        sa.Column('state',
                  sa.String(length=20),
                  nullable=False),
        sa.Column('lifecycle_state',
                  sa.String(length=50, collation=False),
                  nullable=True,
                  server_default=None),
        sa.Column('link',
                  sa.String(length=512, collation=False),
                  nullable=True,
                  server_default=None),
        sa.Column('created_at',
                  sa.DateTime(),
                  nullable=False),
        sa.Column('state_updated_at',
                  sa.DateTime(),
                  nullable=True),
        sa.Column('updated_at',
                  sa.DateTime(),
                  nullable=False),
        sa.PrimaryKeyConstraint('id'),
        sa.Index('alarm_definition_id', 'alarm_definition_id'),
        sa.Index('fk_alarm_alarm_state', 'state'),
        sa.ForeignKeyConstraint(['alarm_definition_id'],
                                ['alarm_definition.id'],
                                name='fk_alarm_definition_id',
                                ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['state'],
                                ['alarm_state.name'],
                                name='fk_alarm_alarm_state'),
        mysql_charset='utf8mb4',
        mysql_collate='utf8mb4_unicode_ci')

    op.create_table(
        'notification_method',
        sa.Column('id',
                  sa.String(length=36),
                  nullable=False),
        sa.Column('tenant_id',
                  sa.String(length=36),
                  nullable=False),
        sa.Column('name',
                  sa.String(length=250),
                  nullable=True,
                  server_default=None),
        sa.Column('type',
                  sa.String(length=20),
                  # Note: the typo below is deliberate since we need to match
                  # the constraint name from the SQL script where it is
                  # misspelled as well.
                  sa.ForeignKey('notification_method_type.name',
                                name='fk_alarm_noticication_method_type'),
                  nullable=False),
        sa.Column('address',
                  sa.String(length=512),
                  nullable=True,
                  server_default=None),
        sa.Column('created_at',
                  sa.DateTime(),
                  nullable=False),
        sa.Column('updated_at',
                  sa.DateTime(),
                  nullable=False),
        sa.PrimaryKeyConstraint('id'),
        mysql_charset='utf8mb4',
        mysql_collate='utf8mb4_unicode_ci')

    op.create_table(
        'alarm_action',
        sa.Column('alarm_definition_id',
                  sa.String(length=36),
                  nullable=False,),
        sa.Column('alarm_state',
                  sa.String(length=20),
                  nullable=False),
        sa.Column('action_id',
                  sa.String(length=36),
                  nullable=False),
        sa.PrimaryKeyConstraint('alarm_definition_id', 'alarm_state',
                                'action_id'),
        sa.ForeignKeyConstraint(['action_id'],
                                ['notification_method.id'],
                                name='fk_alarm_action_notification_method_id',
                                ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['alarm_state'],
                                ['alarm_state.name']),
        sa.ForeignKeyConstraint(['alarm_definition_id'],
                                ['alarm_definition.id'],
                                ondelete='CASCADE'),
        mysql_charset='utf8mb4',
        mysql_collate='utf8mb4_unicode_ci')

    op.create_table(
        'alarm_metric',
        sa.Column('alarm_id',
                  sa.String(length=36),
                  nullable=False),
        sa.Column('metric_definition_dimensions_id',
                  sa.BINARY(20),
                  nullable=False,
                  server_default='\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0'),
        sa.PrimaryKeyConstraint('alarm_id', 'metric_definition_dimensions_id'),
        sa.Index('alarm_id', 'alarm_id'),
        sa.Index('metric_definition_dimensions_id', 'metric_definition_dimensions_id'),
        mysql_charset='utf8mb4',
        mysql_collate='utf8mb4_unicode_ci')

    # For some mysterious alembic/sqlalchemy reason this foreign key constraint
    # ends up missing when specified upon table creation. Hence we need to add
    # it through an ALTER TABLE operation:
    op.create_foreign_key('fk_alarm_id',
                          'alarm_metric',
                          'alarm',
                          ['alarm_id'],
                          ['id'], ondelete='CASCADE')

    op.create_table(
        'metric_definition',
        sa.Column('id',
                  sa.BINARY(20),
                  nullable=False,
                  server_default='\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0'),
        sa.Column('name',
                  sa.String(length=255),
                  nullable=False),
        sa.Column('tenant_id',
                  sa.String(length=36),
                  nullable=False),
        sa.Column('region',
                  sa.String(length=255),
                  nullable=False,
                  server_default=''),
        sa.PrimaryKeyConstraint('id'),
        mysql_charset='utf8mb4',
        mysql_collate='utf8mb4_unicode_ci')

    op.create_table(
        'metric_definition_dimensions',
        sa.Column('id',
                  sa.BINARY(20),
                  nullable=False,
                  server_default='\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0'),
        sa.Column('metric_definition_id',
                  sa.BINARY(20),
                  nullable=False,
                  server_default='\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0'),
        sa.Column('metric_dimension_set_id',
                  sa.BINARY(20),
                  nullable=False,
                  server_default='\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0'),
        sa.PrimaryKeyConstraint('id'),
        sa.Index('metric_definition_id', 'metric_definition_id'),
        sa.Index('metric_dimension_set_id', 'metric_dimension_set_id'),
        mysql_charset='utf8mb4',
        mysql_collate='utf8mb4_unicode_ci')

    # mysql limits the size of a unique key to 767 bytes. The utf8mb4 charset
    # requires 4 bytes to be allocated for each character while the utf8
    # charset requires 3 bytes.  The utf8 charset should be sufficient for any
    # reasonable characters, see the definition of supplementary characters for
    # what it doesn't support.  Even with utf8, the unique key length would be
    # 785 bytes so only a subset of the name is used. Potentially the size of
    # the name should be limited to 250 characters which would resolve this
    # issue.
    #
    # The unique key is required to allow high performance inserts without
    # doing a select by using the "insert into metric_dimension ... on
    # duplicate key update dimension_set_id=dimension_set_id syntax

    op.create_table(
        'metric_dimension',
        sa.Column('dimension_set_id',
                  sa.BINARY(20),
                  nullable=False,
                  server_default='\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0'),
        sa.Column('name',
                  sa.String(length=255),
                  nullable=False,
                  server_default=''),
        sa.Column('value',
                  sa.String(length=255),
                  nullable=False,
                  server_default=''),
        sa.Index('metric_dimension_key',
                 'dimension_set_id', 'name',
                 unique=True,
                 mysql_length={'name': 252}),
        sa.Index('dimension_set_id', 'dimension_set_id'),
        mysql_charset='utf8',
        mysql_collate='utf8_unicode_ci',
        mysql_comment='PRIMARY KEY (`id`)')

    op.create_table(
        'sub_alarm_definition',
        sa.Column('id',
                  sa.String(length=36),
                  nullable=False),
        sa.Column('alarm_definition_id',
                  sa.String(length=36),
                  sa.ForeignKey('alarm_definition.id', ondelete='CASCADE',
                                name='fk_sub_alarm_definition'),
                  nullable=False,
                  server_default=''),
        sa.Column('function',
                  sa.String(length=10),
                  nullable=False),
        sa.Column('metric_name',
                  sa.String(length=100),
                  nullable=True,
                  server_default=None),
        sa.Column('operator',
                  sa.String(length=5),
                  nullable=False),
        sa.Column('threshold',
                  sa.dialects.mysql.DOUBLE(),
                  nullable=False),
        sa.Column('period',
                  sa.Integer(),
                  nullable=False),
        sa.Column('periods',
                  sa.Integer(),
                  nullable=False),
        sa.Column('created_at',
                  sa.DateTime(),
                  nullable=False),
        sa.Column('updated_at',
                  sa.DateTime(),
                  nullable=False),
        sa.PrimaryKeyConstraint('id'),
        mysql_charset='utf8mb4',
        mysql_collate='utf8mb4_unicode_ci')

    op.create_table(
        'sub_alarm_definition_dimension',
        sa.Column('sub_alarm_definition_id',
                  sa.String(length=36),
                  sa.ForeignKey('sub_alarm_definition.id', ondelete='CASCADE',
                                name='fk_sub_alarm_definition_dimension'),
                  nullable=False,
                  server_default=''),
        sa.Column('dimension_name',
                  sa.String(length=255),
                  nullable=False,
                  server_default=''),
        sa.Column('value',
                  sa.String(length=255),
                  nullable=True,
                  server_default=None),
        mysql_charset='utf8mb4',
        mysql_collate='utf8mb4_unicode_ci')

    op.create_table(
        'sub_alarm',
        sa.Column('id',
                  sa.String(length=36),
                  nullable=False),
        sa.Column('alarm_id',
                  sa.String(length=36),
                  sa.ForeignKey('alarm.id', ondelete='CASCADE',
                                name='fk_sub_alarm'),
                  nullable=False,
                  server_default=''),
        sa.Column('sub_expression_id',
                  sa.String(length=36),
                  sa.ForeignKey('sub_alarm_definition.id',
                                name='fk_sub_alarm_expr'),
                  nullable=False,
                  server_default=''),
        sa.Column('expression',
                  sa.dialects.mysql.LONGTEXT(),
                  nullable=False),
        sa.Column('created_at',
                  sa.DateTime(),
                  nullable=False),
        sa.Column('updated_at',
                  sa.DateTime(),
                  nullable=False),
        sa.PrimaryKeyConstraint('id'),
        mysql_charset='utf8mb4',
        mysql_collate='utf8mb4_unicode_ci')

    op.create_table(
        'schema_migrations',
        sa.Column('version',
                  sa.String(length=255),
                  nullable=False),
        sa.UniqueConstraint('version', name='unique_schema_migrations'),
        mysql_charset='latin1')

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


def downgrade():
    op.drop_table('alarm_state')
    op.drop_table('alarm_definition_severity')
    op.drop_table('notification_method_type')
    op.drop_table('stream_actions_action_type')
    op.drop_table('alarm_definition')
    op.drop_table('alarm')
    op.drop_table('notification_method')
    op.drop_table('alarm_action')
    op.drop_table('alarm_metric')
    op.drop_table('metric_definition')
    op.drop_table('metric_definition_dimensions')
    op.drop_table('metric_dimension')
    op.drop_table('sub_alarm_definition')
    op.drop_table('sub_alarm_definition_dimension')
    op.drop_table('sub_alarm')
    op.drop_table('schema_migrations')
    op.drop_table('stream_definition')
    op.drop_table('stream_actions')
    op.drop_table('event_transform')
