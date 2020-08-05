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

"""
   CLI interface for monasca database management.
"""

from oslo_config import cfg
from oslo_db.sqlalchemy.migration_cli.ext_alembic import AlembicExtension

from monasca_api.common.repositories.sqla import sql_repository
from monasca_api import conf
from monasca_api.db.alembic import env
from monasca_api.db.fingerprint import Fingerprint
from monasca_api import version

import monasca_api.config

import sys

CONF = cfg.CONF

_FP_NOREVISION = ("Schema fingerprint %s does not match any known legacy "
                  "revision.")

migration_config = {'alembic_ini_path': env.ini_file_path}


def do_detect_revision():
    fingerprint = Fingerprint(sql_repository.get_engine())

    if fingerprint.revision is None:
        print(_FP_NOREVISION % fingerprint.sha1)
        sys.exit(1)
    else:
        print(fingerprint.revision)


def do_fingerprint():
    fingerprint = Fingerprint(sql_repository.get_engine())
    if CONF.command.raw:
        print(fingerprint.schema_raw, end="")
    else:
        print(fingerprint.sha1)


def do_stamp():
    rev = CONF.command.revision
    from_fingerprint = CONF.command.from_fingerprint

    engine = sql_repository.get_engine()
    alembic_ext = AlembicExtension(engine, migration_config)

    if rev is None:
        if from_fingerprint is False:
            print("No revision specified. Specify --from-fingerprint to "
                  "attempt a guess based on the current database schema's "
                  "fingerprint.")
            sys.exit(1)
        else:
            fp = Fingerprint(engine)
            if fp.revision is None:
                print(_FP_NOREVISION % fp.sha1)
                sys.exit(1)
            rev = fp.revision

    alembic_ext.stamp(rev)


def do_upgrade():
    engine = sql_repository.get_engine()
    alembic_ext = AlembicExtension(engine, migration_config)

    rev = CONF.command.revision
    db_rev = alembic_ext.version()

    fp = Fingerprint(engine)

    if fp.schema_raw != "" and db_rev is None:
        print("Non-empty database schema without Alembic version metadata "
              "detected. Please use the `stamp` subcommand to add version "
              "metadata.")
        sys.exit(1)

    alembic_ext.upgrade(rev)


def do_version():
    engine = sql_repository.get_engine()
    alembic_ext = AlembicExtension(engine, migration_config)

    version = alembic_ext.version()
    if version is None:
        print("Cannot determine version. Check if this database has Alembic "
              "version information. ")
        sys.exit(1)
    print(version)


def add_command_parsers(subparsers):
    parser = subparsers.add_parser('fingerprint',
                                   help="Compute SHA1 fingerprint of "
                                        "current database schema ")
    parser.add_argument('-r', '--raw', action='store_true',
                        help='Print raw schema dump used for '
                             'fingerprinting')
    parser.set_defaults(func=do_fingerprint)

    parser = subparsers.add_parser('detect-revision',
                                   help="Attempt to detect revision "
                                        "matching current database "
                                        " schema ")
    parser.set_defaults(func=do_detect_revision)

    parser = subparsers.add_parser('stamp', help='Stamp database with an '
                                                 'Alembic revision')
    parser.add_argument('revision', nargs='?', metavar='VERSION',
                        help='Revision to stamp database with',
                        default=None)
    parser.add_argument('-f', '--from-fingerprint', action='store_true',
                        help='Try to determine VERSION from fingerprint')
    parser.set_defaults(func=do_stamp)

    parser = subparsers.add_parser('upgrade',
                                   help='Upgrade database to given or '
                                        'latest revision')
    parser.add_argument('revision', metavar='VERSION', nargs='?',
                        help='Alembic revision to upgrade database to',
                        default='head')
    parser.add_argument('-f', '--from-fingerprint', action='store_true',
                        help='Try to determine VERSION from fingerprint')
    parser.set_defaults(func=do_upgrade)

    parser = subparsers.add_parser('version', help="Show database's current Alembic version")
    parser.set_defaults(func=do_version)


command_opt = cfg.SubCommandOpt('command',
                                title='Monasca DB manager',
                                help='Available commands',
                                handler=add_command_parsers)


def main():
    CONF.register_cli_opt(command_opt)
    CONF(args=sys.argv[1:],
         default_config_files=monasca_api.config.get_config_files(),
         prog='api',
         project='monasca',
         version=version.version_str)

    conf.register_opts()

    CONF.command.func()
