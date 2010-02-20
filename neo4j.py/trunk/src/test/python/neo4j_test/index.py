# -*- coding: utf-8 -*-

# Copyright (c) 2008-2010 "Neo Technology,"
#     Network Engine for Objects in Lund AB [http://neotechnology.com]
# 
# This file is part of Neo4j.py.
# 
# Neo4j.py is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
# 
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

from __future__ import with_statement

from neo4j_test._support import perform, define_verify_test, SkipTest, timestamp

def single_index_association(neo, options):
    name = options['name']
    with neo.transaction:
        index = neo.index(name, create=True)
        index['one'] = neo.node(id='one')
        index['two'] = neo.node(id='two')
        index['three'] = neo.node(id='three')
    with neo.transaction:
        index = neo.index(name)
        assert index['one']['id'] == 'one'
        assert index['two']['id'] == 'two'
        assert index['three']['id'] == 'three'

def multiple_index_associations(neo, options):
    name = options['name']
    with neo.transaction:
        index = neo.index(name, create=True)
        index.add('number', neo.node(id='one'))
        index.add('number', neo.node(id='two'), neo.node(id='three'))
        index.add('letter', neo.node(id='A'))
        index.add('letter', neo.node(id='B'))
        index.add('letter', neo.node(id='C'))
        index.add('letter', neo.node(id='A'))
        index.add('letter', neo.node(id='B'))
        index.add('letter', neo.node(id='C'))
    with neo.transaction:
        index = neo.index(name)
        numbers = set()
        for number in index.nodes('number'):
            number = number['id']
            assert number not in numbers, "Multiple"
            numbers.add(number)
        assert numbers == set(['one', 'two', 'three']), "Wrong numbers"
        letters = set()
        count = 0
        for letter in index.nodes('letter'):
            count += 1
            letters.add(letter['id'])
        assert letters == set('ABC'), "Wrong letters"
        assert count == 6, "Wrong count"

def run(neo, **options):
    postfix = timestamp()
    perform(neo, single_index_association, name='single'+postfix, **options)
    perform(neo, multiple_index_associations, name='multi'+postfix, **options)
