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
This module dispatches the implementation.


Copyright (c) 2008-2009 "Neo Technology,"
    Network Engine for Objects in Lund AB [http://neotechnology.com]
"""

from neo4j._base import Neo4jObject

def load_neo(resource_uri, parameters):
    global load_neo,\
        NotFoundError, NotInTransactionError, Traversal,\
        Incoming, Outgoing, Undirected, BREADTH_FIRST, DEPTH_FIRST,\
        RETURN_ALL_NODES, RETURN_ALL_BUT_START_NODE,\
        STOP_AT_END_OF_GRAPH, StopAtDepth        
    import neo4j._backend as backend
    import neo4j._primitives as primitives
    import neo4j._traverse as traversals
    import neo4j._index as indexes
    import os.path, atexit
    # Setup the parameters
    class_base = os.path.join(os.path.dirname(__file__), 'classes')
    if 'classpath' not in parameters:
        parameters['classpath'] = classpath = []
        if os.path.isdir(class_base):
            for file in os.listdir(base):
                classpath.append(os.path.join(class_base, file))
    if 'ext_dirs' not in parameters: # JPype cannot have jars on classpath
        if os.path.isdir(class_base):
            parameters['ext_dirs'] = [class_base]
    # Load the backend and the Neo4j classes
    backend.initialize(**parameters)
    # Initialize subsystems
    primitives.initialize(backend.implementation)
    traversals.initialize(backend.implementation)
    indexes.initialize(backend.implementation)
    # Define the namespace
    Node, Relationship        = primitives.Node, primitives.Relationship
    Traversal                 = traversals.Traversal
    NotFoundError             = backend.implementation.NotFoundException
    NotInTransactionError     = backend.implementation.NotInTransactionException
    Incoming                  = traversals.Incoming
    Outgoing                  = traversals.Outgoing
    Undirected                = traversals.Undirected
    BREADTH_FIRST             = backend.implementation.BREADTH_FIRST
    DEPTH_FIRST               = backend.implementation.DEPTH_FIRST
    RETURN_ALL_NODES          = backend.implementation.ALL
    RETURN_ALL_BUT_START_NODE = backend.implementation.ALL_BUT_START_NODE
    STOP_AT_END_OF_GRAPH      = backend.implementation.END_OF_GRAPH
    StopAtDepth               = backend.implementation.StopAtDepth
    # Define replacement load function for use when the initial load is done
    def load_neo(resource_uri, parameters):
        return NeoService(resource_uri)
    # Define the implementation
    class NeoService(Neo4jObject):
        """TODO: Documentation"""
        def __init__(self, resource_uri):
            neo = backend.load_neo(resource_uri)
            Neo4jObject.__init__(self, neo=neo)
            self.__neo = neo
            self.__nodes = NodeFactory(self, neo)
            self.__relationships = RelationshipLookup(self, neo)
            self.__index = indexes.IndexService(self, neo)
            atexit.register(self.shutdown)
        @property
        def transaction(self):
            """TODO: Documentation"""
            return TransactionContext(self.__neo)
        def index(self, name, create=False, **options):
            """TODO: Documentation"""
            return self.__index.get(name, options, create)
        @property
        def node(self):
            """TODO: Documentation"""
            return self.__nodes
        @property
        def relationship(self):
            """TODO: Documentation"""
            return self.__relationships
        @property
        def reference_node(self):
            """TODO: Documentation"""
            return Node(self, self.__neo.getReferenceNode())
        reference = reference_node
        def shutdown(self):
            """TODO: Documentation"""
            if self.__index is not None:
                self.__index.shutdown()
            if self.__neo is not None:
                self.__neo.shutdown()
        close = shutdown
    tx_join = backend.implementation.tx_join\
        if hasattr(backend.implementation, 'tx_join')\
        else None
    class TransactionContext(object):
        def __init__(self, neo):
            self.__neo = neo
            self.__tx = None
        if tx_join is None:
            def begin(self):
                if self.__tx is None:
                    self.__tx = self.__neo.beginTx()
                return self
        else:
            def begin(self):
                tx_join()
                if self.__tx is None:
                    self.__tx = self.__neo.beginTx()
                return self
        def success(self):
            if self.__tx is not None:
                self.__tx.success()
        def failure(self):            
            if self.__tx is not None:
                self.__tx.failure()
        def finish(self):
            if self.__tx is not None:
                self.__tx.finish()
            self.__tx = None
        __enter__ = __call__ = begin
        def __exit__(self, type=None, value=None, traceback=None):
            if self.__tx is not None:
                if type is None:
                    self.__tx.success()
                else:
                    self.__tx.failure()
                self.__tx.finish()
                self.__tx = None
    class NodeFactory(object):
        def __init__(self, neo, backend):
            self.__neo = neo
            self.__backend = backend
        def __getitem__(self, id):
            return Node(self.__neo, self.__backend.getNodeById(id))
        def __call__(self, **attributes):
            node = Node(self.__neo, self.__backend.createNode())
            for key, value in attributes.items():
                node[key] = value
            return node
    class RelationshipLookup(object):
        def __init__(self, neo, backend):
            self.__neo = neo
            self.__backend = backend
        def __getitem__(self, id):
            return Relationship(self.__neo,
                                self.__backend.getRelationshipById(id))
    import neo4j._hooks as hooks
    hooks.initialize(parameters)
    return NeoService(resource_uri)
