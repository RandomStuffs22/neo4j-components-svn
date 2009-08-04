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
Backend implementation for the CPython platform using JPype reflection.


Copyright (c) 2008-2009 "Neo Technology,"
    Network Engine for Objects in Lund AB [http://neotechnology.com]
"""

import jpype

def initialize(classpath, parameters):
    global INCOMING, OUTGOING, BOTH,\
        BREADTH_FIRST, DEPTH_FIRST,\
        NotFoundException, NotInTransactionException,\
        RelationshipType, Evaluator, IndexService,\
        ALL, ALL_BUT_START_NODE, END_OF_GRAPH, StopAtDepth,\
        array, to_java, to_python, tx_join
    jvm = parameters.get('jvm', None)
    if jvm is None:
        jvm = jpype.getDefaultJVMPath()
    args = []
    if 'ext_dirs' in parameters:
        args.append('-Djava.ext.dirs=' + ':'.join(parameters['ext_dirs']))
    args.append('-Djava.class.path=' + ':'.join(classpath))
    jpype.startJVM(jvm, *args)
    neo4j = jpype.JPackage('org').neo4j
    core = neo4j.api.core
    INCOMING = core.Direction.INCOMING
    OUTGOING = core.Direction.OUTGOING
    BOTH = core.Direction.BOTH
    Order = getattr(core, 'Traverser$Order')
    Stop = core.StopEvaluator
    Returnable = core.ReturnableEvaluator
    BREADTH_FIRST = Order.BREADTH_FIRST
    DEPTH_FIRST = Order.DEPTH_FIRST
    ALL = Returnable.ALL
    ALL_BUT_START_NODE = Returnable.ALL_BUT_START_NODE
    END_OF_GRAPH = Stop.END_OF_GRAPH
    NotFoundException = jpype.JException(core.NotFoundException)
    NotInTransactionException = jpype.JException(core.NotInTransactionException)
    try:
        EmbeddedNeo = jpype.JClass("org.neo4j.api.core.EmbeddedNeo")
    except:
        EmbeddedNeo = None
    try:
        RemoteNeo = jpype.JClass("org.neo4j.api.remote.RemoteNeo")
    except:
        RemoteNeo = None
    try:
        IndexService = jpype.JClass("org.neo4j.util.index.LuceneIndexService")
    except:
        try:
            IndexService = jpype.JClass("org.neo4j.util.index.NeoIndexService")
        except:
            IndexService = None
    def tx_join():
        if not jpype.isThreadAttachedToJVM():
            jpype.attachThreadToJVM()
    def array(lst):
        return lst
    def to_java(obj):
        return obj
    def to_python(obj):
        return obj
    rel_types = {}
    def RelationshipType(name):
        if name in rel_types:
            return rel_types[name]
        else:
            rel_types[name] = type = jpype.JProxy(core.RelationshipType,dict={
                    'name': lambda:name
                    })
            return type
    def StopAtDepth(limit):
        limit = int(limit)
        assert limit > 0, "Illegal stop depth."
        if limit == 1:
            return core.StopEvaluator.DEPTH_ONE
        else:
            return jpype.JProxy(Stop, dict={
                    'isStopNode': lambda pos: limit <= pos.depth()
                    })
    class Evaluator(object):
        def __init__(self):
            self.stop = jpype.JProxy(Stop, inst=self)
            self.returnable = jpype.JProxy(Returnable, inst=self)
    return EmbeddedNeo, RemoteNeo
