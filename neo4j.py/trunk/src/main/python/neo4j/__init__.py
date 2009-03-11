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
Neo4j.py

A Python wrapper for Neo4j [ http://neo4j.org/ ]
Website: http://components.neo4j.org/neo4j.py/

Neo4j.py can be used either with Jython or with JPype or JCC in CPython.
Neo4j.py is used in exactly the same way regardless of which backend is used.

The typical way to use Neo4j.py is:
   from neo4j import NeoService
   neo = NeoService( "/neo/db/path" )
   with neo.transaction:
       ref_node = neo.reference_node
       new_node = neo.node()
       # put operations that manipulate the node space here ...
   neo.shutdown()

Most of the names in this module are loaded lazily, with NeoService being the
only guaranteed exception. After the first NeoService has been initialized the
rest of the module is loaded.

The content of this module is:
    NeoService            - factory for creating a Neo database service.
    Traversal             - Base for defining traversals over the node space.
    NotFoundError         - Exception that is raised when a node,
                            relationship or property could not be found.
    NotInTransactionError - Exception that is raised when the node space
                            is manipulated outside of a transaction.
    - The rest of the content is used for defining Traversals -
    Incoming              - Defines a relationship type traversable in the
                            incoming direction.
    Outgoing              - Defines a relationship type traversable in the
                            outgoing direction.
    Undirected            - Defines a relationship type traversable in any
                            direction.
    BREADTH_FIRST         - Defines a traversal in breadth first order.
    DEPTH_FIRST           - Defines a traversal in depth first order.
    RETURN_ALL_NODES      - Defines a traversal to return all nodes.
    RETURN_ALL_BUT_START_NODE - Defines traversal to return all but first node.
    StopAtDepth(x)        - Defines a traversal to only traverse to depth=x.
    STOP_AT_END_OF_GRAPH  - Defines a traversal to traverse the entire subgraph.

Traversals are defined by creating a class that extends neo4j.Traversal, and
possibly previously defined traversals as well. (Note that neo4j.Traversal
always needs to be a direct parent of a traversal class.)
A traversal class needs to define the following members:
    types      - A list of relationship types to be traversed in the traversal.
                 These are created using Incoming, Outgoing and Undirected.
    order      - The order in which the nodes of the graph are to be traversed.
                 Valid values are BREADTH_FIRST and DEPTH_FIRST
    stop       - Definition of when the traversal should stop.
                 Valid values are STOP_AT_DEPTH_ONE and STOP_AT_END_OF_GRAPH
                 Alternatively the traversal class may define a more advanced
                 stop predicate in the form of a method called 'isStopNode'.
    returnable - Definition of which nodes the traversal should yield.
                 Valid values are RETURN_ALL_NODES and RETURN_ALL_BUT_START_NODE
                 Alternatively the traversal class may define a more advanced
                 returnable predicate in the form of a method called
                 'isReturnable'.
To define more advanced stop and returnable predicates the traversal class can
define the methods 'isStopNode' and 'isReturnable' respectively.
These methods should accept one argument (in addition to self), a traversal
position. The position is essentially a node, but with the following extra
properties:
    last_relationship - The relationship that was traversed to reach this node.
                        This is None for the start node.
    is_start          - True if this is the start node, False otherwise.
    previous_node     - The node from which this node was reached.
                        This is None for the start node.
    depth             - The depth at which this node was found in the traversal.
                        This is 0 for the start node.
    returned_count    - The number of returned nodes so far.
Nodes yielded by a traversal has an additional 'depth' attribute with the same
semantics as above.

Further information about Neo4j can be found at http://neo4j.org/


Copyright (c) 2008-2009 "Neo Technology,"
    Network Engine for Objects in Lund AB [http://neotechnology.com]
"""

def NeoService(resource_uri, **params):
    """TODO: Documentation"""
    global NeoService, NotFoundError, NotInTransactionError, Traversal,\
        Incoming, Outgoing, Undirected, BREADTH_FIRST, DEPTH_FIRST,\
        RETURN_ALL_NODES, RETURN_ALL_BUT_START_NODE,\
        STOP_AT_END_OF_GRAPH, StopAtDepth
    from neo4j import _core as core
    neo = core.load_neo(resource_uri, params)
    # Define values for globals
    NotFoundError             = core.NotFoundError
    NotInTransactionError     = core.NotInTransactionError
    Traversal                 = core.Traversal
    Incoming                  = core.Incoming
    Outgoing                  = core.Outgoing
    Undirected                = core.Undirected
    BREADTH_FIRST             = core.BREADTH_FIRST
    DEPTH_FIRST               = core.DEPTH_FIRST
    RETURN_ALL_NODES          = core.RETURN_ALL_NODES
    RETURN_ALL_BUT_START_NODE = core.RETURN_ALL_BUT_START_NODE
    STOP_AT_END_OF_GRAPH      = core.STOP_AT_END_OF_GRAPH
    StopAtDepth               = core.StopAtDepth
    # Define replacement NeoService
    doc = NeoService.__doc__
    def NeoService(resource_uri, **params):
        return core.load_neo(resource_uri, params)
    NeoService.__doc__ = doc
    return neo
