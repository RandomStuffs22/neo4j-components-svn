# -*- coding: utf-8 -*-
# Copyright (c) 2008-2009 "Neo Technology,"
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
"""
Backend implementation for the Java platform (i.e. Jython).


Copyright (c) 2008-2009 "Neo Technology,"
    Network Engine for Objects in Lund AB [http://neotechnology.com]
"""

def import_api():
    global INCOMING, OUTGOING, BOTH,\
        BREADTH_FIRST, DEPTH_FIRST,\
        NotFoundException, NotInTransactionException,\
        ALL, ALL_BUT_START_NODE, END_OF_GRAPH
    from org.neo4j.api.core.Direction import INCOMING, OUTGOING, BOTH
    from org.neo4j.api.core.Traverser.Order import BREADTH_FIRST, DEPTH_FIRST
    from org.neo4j.api.core import StopEvaluator, ReturnableEvaluator,\
        RelationshipType
    from org.neo4j.api.core.StopEvaluator import END_OF_GRAPH
    from org.neo4j.api.core.ReturnableEvaluator import ALL, ALL_BUT_START_NODE
    from org.neo4j.api.core import NotFoundException, NotInTransactionException
    return StopEvaluator, ReturnableEvaluator, RelationshipType

def initialize(classpath, parameters):
    global RelationshipType, Evaluator, StopAtDepth, IndexService,\
        array, to_java, to_python
    # Import implementation
    try:
        Stop, Returnable, Type = import_api()
    except:
        import sys
        sys.path.extend(classpath)
        Stop, Returnable, Type = import_api()
    try:
        from org.neo4j.api.core import EmbeddedNeo
    except:
        EmbeddedNeo = None
    try:
        from org.neo4j.api.remote import RemoteNeo
    except:
        RemoteNeo = None
    try:
        from org.neo4j.util.index import LuceneIndexService as IndexService
    except:
        try:
            from org.neo4j.util.index import NeoIndexService as IndexService
        except:
            IndexService = None
    # Define conversions
    def array(lst):
        return lst
    def to_java(obj):
        return obj
    def to_python(obj):
        return obj
    class Evaluator(Stop,Returnable):
        def __init__(self):
            self.stop = self
            self.returnable = self
    class StopAtDepth(Stop):
        def __init__(self, limit):
            limit = int(limit)
            assert limit > 0, "Illegal stop depth."
            self.__limit
        def isStopNode(self, position):
            return self.__limit <= position.depth()
    types = {}
    def RelationshipType(name):
        if name in types:
            return types[name]
        else:
            types[name] = type = RelType(name)
            return type
    class RelType(Type):
        def __init__(self, name):
            self.__name = name
        def name(self):
            return self.__name
        def __eq__(self, other):
            return self.name() == other.name()
    return EmbeddedNeo, RemoteNeo
