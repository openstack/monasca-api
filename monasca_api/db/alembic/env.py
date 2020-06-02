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

import os
import sys

from alembic import config as alembic_config
from alembic import context
from logging.config import fileConfig

from monasca_api.common.repositories.sqla import models
from monasca_api.common.repositories.sqla import sql_repository
import monasca_api.config

ini_file_path = os.path.join(os.path.dirname(__file__), '..', 'alembic.ini')

# This indicates whether we are running with a viable Alembic
# context (necessary to skip run_migrations_online() below
# if sphinx imports this file without a viable Alembic
# context)
have_context = True

try:
    config = context.config
    # Only load Monasca configuration if imported by alembic CLI tool (the
    # monasca_db command will handle this on its own).
    if os.path.basename(sys.argv[0]) == 'alembic':
        monasca_api.config.parse_args(argv=[])
except AttributeError:
    config = alembic_config.Config(ini_file_path)
    have_context = False


# Interpret the config file for Python logging.
# This line sets up loggers basically.
fileConfig(config.config_file_name)

# Model metadata. This is needed for 'autogenerate' support. If you add new
# tables, you will need to add them to the get_all_metadata() method as well.
target_metadata = models.get_all_metadata()

nc = {"ix": "ix_%(column_0_label)s",
      "uq": "uq_%(table_name)s_%(column_0_name)s",
      "fk": "fk_%(table_name)s_%(column_0_name)s",
      "pk": "pk_%(table_name)s"}

target_metadata.naming_convention = nc


def run_migrations_online():
    """Run migrations in 'online' mode.

    In this scenario we need to create an Engine
    and associate a connection with the context.

    """
    engine = sql_repository.get_engine()

    with engine.connect() as connection:
        context.configure(
            connection=connection,
            target_metadata=target_metadata
        )

        with context.begin_transaction():
            context.run_migrations()


if have_context:
    run_migrations_online()
