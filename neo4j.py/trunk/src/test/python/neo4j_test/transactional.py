# -*- coding: utf-8 -*-

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
