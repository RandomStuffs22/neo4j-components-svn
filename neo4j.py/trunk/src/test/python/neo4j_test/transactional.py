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

from neo4j_test._support import perform, simple_test

def run(neo):
    import neo4j

    class BaseObject(object):
        @property
        def neo_service(self):
            return neo
    transactional = neo4j.transactional(BaseObject.neo_service)

    class MyService(BaseObject):
        @transactional
        def make_entity(self, name):
            return Entity( self.neo_service.node(name=name) )

    class Entity(BaseObject):
        def __init__(self, node):
            self._node = node

        def __get__name(self):
            return self._node['name']

        def __set__name(self, name):
            self._node['name'] = name

        name = transactional( property(__get__name, __set__name) )

    perform( neo, simple_test(test, MyService(), name=__name__) )

def test(service):

    first = service.make_entity( "John" )
    assert first.name == "John"

    second = service.make_entity( "Kim" )
    assert second.name == "Kim"
    assert first.name == "John"

    first.name = "Lars"
    assert second.name == "Kim"
    assert first.name == "Lars"
