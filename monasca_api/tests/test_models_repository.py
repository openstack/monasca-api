# Copyright 2015 Cray
# Copyright 2016 FUJITSU LIMITED
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

import fixtures
import testtools

from sqlalchemy import select, MetaData, text, asc
from monasca_api.common.repositories.sqla import models


class TestModelsDB(testtools.TestCase, fixtures.TestWithFixtures):
    @classmethod
    def setUpClass(cls):
        metadata = MetaData()

        md = models.create_md_model(metadata)
        gc_columns = [md.c.name + text("'='") + md.c.value]
        cls.group_concat_md = (select([md.c.dimension_set_id,
                                       models.group_concat(gc_columns).label('dimensions')])
                               .select_from(md)
                               .group_by(md.c.dimension_set_id))

        cls.group_concat_md_order = (select([md.c.dimension_set_id,
                                             models.group_concat(gc_columns,
                                                                 order_by=[md.c.name.asc()]).label('dimensions')])
                                     .select_from(md)
                                     .group_by(md.c.dimension_set_id))

        cls.order_by_field = (select([md.c.dimension_set_id])
                              .select_from(md)
                              .order_by(asc(models.field_sort(md.c.dimension_set_id, map(text,
                                                                                         ["'A'",
                                                                                          "'B'",
                                                                                          "'C'"])))))

    def test_oracle(self):
        from sqlalchemy.dialects import oracle
        dialect = oracle.dialect()
        query = str(self.group_concat_md.compile(dialect=dialect))

        expected = ('''SELECT metric_dimension.dimension_set_id, LISTAGG(metric_dimension.name '''
                    '''|| '=' || metric_dimension.value, ',') WITHIN GROUP (ORDER BY '''
                    '''metric_dimension.name || '=' || metric_dimension.value) AS dimensions '''
                    '''
FROM metric_dimension GROUP BY metric_dimension.dimension_set_id''')
        self.assertEqual(expected, query)

        query = str(self.group_concat_md_order.compile(dialect=dialect))

        expected = ('''SELECT metric_dimension.dimension_set_id, LISTAGG(metric_dimension.name '''
                    '''|| '=' || metric_dimension.value, ',') WITHIN GROUP (ORDER BY '''
                    '''metric_dimension.name ASC) AS dimensions '''
                    '''
FROM metric_dimension GROUP BY metric_dimension.dimension_set_id''')
        self.assertEqual(expected, query)

        expected = ("""SELECT metric_dimension.dimension_set_id \n"""
                    """FROM metric_dimension ORDER BY CASE WHEN metric_dimension.dimension_set_id='A'"""
                    """ THEN 0 WHEN metric_dimension.dimension_set_id='B' THEN 1 WHEN"""
                    """ metric_dimension.dimension_set_id='C' THEN 2 ELSE 3 END ASC""")
        query = str(self.order_by_field.compile(dialect=dialect))
        self.assertEqual(expected, query)

    def test_postgres(self):
        from sqlalchemy.dialects import postgres as diale_
        dialect = diale_.dialect()
        query = str(self.group_concat_md.compile(dialect=dialect))

        expected = ('''SELECT metric_dimension.dimension_set_id, STRING_AGG(metric_dimension.name '''
                    '''|| '=' || metric_dimension.value, ',' ) AS dimensions '''
                    '''
FROM metric_dimension GROUP BY metric_dimension.dimension_set_id''')
        self.assertEqual(expected, query)

        query = str(self.group_concat_md_order.compile(dialect=dialect))

        expected = ('''SELECT metric_dimension.dimension_set_id, STRING_AGG(metric_dimension.name '''
                    '''|| '=' || metric_dimension.value, ',' ORDER BY metric_dimension.name ASC) '''
                    '''AS dimensions '''
                    '''
FROM metric_dimension GROUP BY metric_dimension.dimension_set_id''')
        self.assertEqual(expected, query)

        expected = ("""SELECT metric_dimension.dimension_set_id \n"""
                    """FROM metric_dimension ORDER BY CASE WHEN metric_dimension.dimension_set_id='A'"""
                    """ THEN 0 WHEN metric_dimension.dimension_set_id='B' THEN 1 WHEN"""
                    """ metric_dimension.dimension_set_id='C' THEN 2 ELSE 3 END ASC""")
        query = str(self.order_by_field.compile(dialect=dialect))
        self.assertEqual(expected, query)

    def test_sybase(self):
        from sqlalchemy.dialects import sybase as diale_
        dialect = diale_.dialect()
        query = str(self.group_concat_md.compile(dialect=dialect))

        expected = ('''SELECT metric_dimension.dimension_set_id, LIST(metric_dimension.name || '=' '''
                    '''|| metric_dimension.value, ',') AS dimensions '''
                    '''
FROM metric_dimension GROUP BY metric_dimension.dimension_set_id''')
        self.assertEqual(expected, query)

        query = str(self.group_concat_md_order.compile(dialect=dialect))

        expected = ('''SELECT metric_dimension.dimension_set_id, LIST(metric_dimension.name || '=' '''
                    '''|| metric_dimension.value, ',') AS dimensions '''
                    '''
FROM metric_dimension GROUP BY metric_dimension.dimension_set_id''')
        self.assertEqual(expected, query)

        expected = ("""SELECT metric_dimension.dimension_set_id \n"""
                    """FROM metric_dimension ORDER BY CASE WHEN metric_dimension.dimension_set_id='A'"""
                    """ THEN 0 WHEN metric_dimension.dimension_set_id='B' THEN 1 WHEN"""
                    """ metric_dimension.dimension_set_id='C' THEN 2 ELSE 3 END ASC""")
        query = str(self.order_by_field.compile(dialect=dialect))
        self.assertEqual(expected, query)

    def test_mysql(self):
        from sqlalchemy.dialects import mysql as diale_
        dialect = diale_.dialect()
        query = str(self.group_concat_md.compile(dialect=dialect))

        expected = ('''SELECT metric_dimension.dimension_set_id, GROUP_CONCAT(concat(concat(metric_dimension.name, '''
                    ''''='), metric_dimension.value)  SEPARATOR ',') AS dimensions '''
                    '''
FROM metric_dimension GROUP BY metric_dimension.dimension_set_id''')
        self.assertEqual(expected, query)

        query = str(self.group_concat_md_order.compile(dialect=dialect))

        expected = ('''SELECT metric_dimension.dimension_set_id, GROUP_CONCAT(concat(concat(metric_dimension.name, '''
                    ''''='), metric_dimension.value) ORDER BY metric_dimension.name ASC '''
                    '''SEPARATOR ',') AS dimensions '''
                    '''
FROM metric_dimension GROUP BY metric_dimension.dimension_set_id''')
        self.assertEqual(expected, query)

        expected = ('''SELECT metric_dimension.dimension_set_id \n'''
                    '''FROM metric_dimension ORDER BY FIELD(metric_dimension.dimension_set_id, 'A', 'B', 'C') ASC''')

        query = str(self.order_by_field.compile(dialect=dialect))
        self.assertEqual(expected, query)
