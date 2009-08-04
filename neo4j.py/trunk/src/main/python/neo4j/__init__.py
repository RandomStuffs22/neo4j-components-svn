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
Neo4j.py   --   Python bindings for the Neo4j Graph Database

 A Python wrapper for Neo4j {{http://neo4j.org/}}

 Website: {{http://components.neo4j.org/neo4j.py/}}

 Neo4j.py can be used either with Jython or with JPype or JCC in CPython.
 Neo4j.py is used in exactly the same way regardless of which backend is
 used.

 The typical way to use Neo4j.py is:

----------------------------------------------------------------
    from neo4j import NeoService
    neo = NeoService( "/neo/db/path" )
    with neo.transaction:
        ref_node = neo.reference_node
        new_node = neo.node()
        # put operations that manipulate the node space here ...
    neo.shutdown()
----------------------------------------------------------------

* Getting started

** Requirements

 In order to use Neo4j.py, regardless of wherther Jython or CPython is used
 the system needs to have a JVM installed.

 The required Java classes are automatically downloaded and installed as
 part of the installation process.

** With CPython

 To use Neo4j.py with CPython the system needs to have JPype
 {{http://jpype.sourceforge.net/}} installed.

 To install Neo4j.py simply check out the source code:

-------------------------------------------------------------------
svn export https://svn.neo4j.org/components/neo4j.py/trunk neo4j.py
-------------------------------------------------------------------

 Then install using distutils:

----------------------------
sudo python setup.py install
----------------------------

 This requires connection to the internet since it will download the
 required java libraries.

** With Jython

 Check out and install as with CPython:

-------------------------------------------------------------------
svn export https://svn.neo4j.org/components/neo4j.py/trunk neo4j.py
cd neo4j.py
sudo jython setup.py install
-------------------------------------------------------------------

** Windows installation issues

 Jython (in 2.5b3 or earlier) has a problem with installing packages under
 Windows. You might get this error when installing:

---------------------------------------------------------------------------
running install_egg_info
Creating X:\\<PATH_TO>\\jython-2.5b3\\Lib\\site-packages\\
error: X:\\<PATH_TO>\\jython-2.5b3\\Lib\\site-packages\\: couldn't make directories
---------------------------------------------------------------------------

 If the install output ends like that when installing under Windows,
 don't panic.

 All of Neo4j.py has already been installed at this point. This can be
 verified by checking that
 <<X:\\<PATH_TO>\\jython-2.5b3\\Lib\\site-packages\\neo4j>> contains some
 directories, Python source files and bytecode compiled files. You can also
 verify that
 <<X:\\<PATH_TO>\\jython-2.5b3\\Lib\\site-packages\\neo4j\\classes>>
 contains the required jar-files. What the install script has failed to do
 is to write the package information. This may cause trouble when
 installing a new version of neo4j.py, the fix for this is to manually
 remove neo4j.py before installing a new version.

 This issue has been reported at {{https://trac.neo4j.org/ticket/156}} and
 {{http://bugs.jython.org/issue1110}}. We have fixed this for the next
 release of Jython.

** Starting Neo

 Apart from specifying the path to where the Neo data is stored to
 NeoService a few extra keyword options may be specified. These include:

    [classpath] A list of paths that are to be added to the classpath
                in order to be able to find the Java classes for Neo4j.
                This defaults to the jar files that were installed with
                this package.

    [ext_dirs ] A list of paths to directories that contain jar files
                that in turn contains the Java classes for Neo4j.
                This defaults to the jar file directory that was installed
                with this package.
                The <<classpath>> option is used before <<ext_dirs>>.

    [jvm      ] The path to the JVM to use. This is ignored when using
                Jython since Jython is already running inside a JVM.
                Neo4j.py is usualy able to compute this path.

 <<Note>> that if the Neo4j Java classes are available on your system
 classpath the classpath and ext_dirs options will be ignored.

 <<Example:>>

------------------------------------------------
neo = NeoService("/neo/db/path",
                 classpath=["/a/newer/neo.jar"],
                 jvm="/usr/lib/jvm.so")
------------------------------------------------

* Package content

 Most of the content of this package is loaded lazily. When the package is
 first imported the only thing that it is guaranteed to contain is
 NeoService. When the first NeoService has been initialized the rest of the
 package is loaded.

 The content of this module is:

    [NeoService           ] factory for creating a Neo database service.

    [Traversal            ] Base for defining traversals over the node
                            space.

    [NotFoundError        ] Exception that is raised when a node,
                            relationship or property could not be found.

    [NotInTransactionError] Exception that is raised when the node space
                            is manipulated outside of a transaction.

    << The rest of the content is used for defining Traversals >>

    [Incoming             ] Defines a relationship type traversable in the
                            incoming direction.

    [Outgoing             ] Defines a relationship type traversable in the
                            outgoing direction.

    [Undirected           ] Defines a relationship type traversable in any
                            direction.

    [BREADTH_FIRST        ] Defines a traversal in breadth first order.

    [DEPTH_FIRST          ] Defines a traversal in depth first order.

    [RETURN_ALL_NODES     ] Defines a traversal to return all nodes.

    [RETURN_ALL_BUT_START_NODE] Defines traversal to return all but first
                            node.

    [StopAtDepth(x)       ] Defines a traversal to only traverse to depth=x.

    [STOP_AT_END_OF_GRAPH ] Defines a traversal to traverse the entire
                            subgraph.

* Nodes, Relationships and Properties

 Creating a node:

---
n = neo.node()
---

 Specify properties for new node:

---
n = neo.node(color="Red", widht=16, height=32)
---

 Accessing node by id:

---
n17 = neo.node[14]
---

 Accessing properties:

---
value = e['key'] # get property value
e['key'] = value # set property value
del e['key']     # remove property value
---

 Create relationship:

---
n1.Knows(n2)
---

 Any name that does not mean anything for the node class can be used as
 relationship type:

---
n1.some_reltionship_type(n2)
n1.CASE_MATTERS(n2)
---

 Specify properties for new relationships:

---
n1.Knows(n2, since=123456789,
             introduced_at="Christmas party")
---

* Indexes

 Get index:

---
index = neo.index("index name")
---

 Create index:

---
index = neo.index("some index", create=True)
---

 If an index is created that already exists, the existing index will not be
 replaced, and the existing index will be returned. The create flag is a
 measure to help finding spelling errors in index names.

 Using indexes:

---
index['value'] = node
node = index['value']
del index['value']
---

 Using indexes as multi value indexes:

---
multiIndex.add('value', node)
for node in multiIndex.nodes('value'):
    doStuffWith(node)
---

* Traversals

 Traversals are defined by creating a class that extends
 <<<neo4j.Traversal>>>, and possibly previously defined traversals as well.
 (Note that <<<neo4j.Traversal>>> always needs to be a direct parent of a
 traversal class.) A traversal class needs to define the following members:

    [types     ] A list of relationship types to be traversed in the
                 traversal. These are created using Incoming, Outgoing and
                 Undirected.

    [order     ] The order in which the nodes of the graph are to be
                 traversed. Valid values are BREADTH_FIRST and DEPTH_FIRST

    [stop      ] Definition of when the traversal should stop.
                 Valid values are STOP_AT_DEPTH_ONE and STOP_AT_END_OF_GRAPH
                 Alternatively the traversal class may define a more
                 advanced stop predicate in the form of a method called
                 'isStopNode'.

    [returnable] Definition of which nodes the traversal should yield.
                 Valid values are RETURN_ALL_NODES and
                 RETURN_ALL_BUT_START_NODE. Alternatively the traversal
                 class may define a more advanced returnable predicate in
                 the form of a method called 'isReturnable'.

 To define more advanced stop and returnable predicates the traversal class
 can define the methods 'isStopNode' and 'isReturnable' respectively.
 These methods should accept one argument (in addition to self), a traversal
 position. The position is essentially a node, but with the following extra
 properties:

    [last_relationship] The relationship that was traversed to reach this
                        node. This is None for the start node.

    [is_start         ] True if this is the start node, False otherwise.

    [previous_node    ] The node from which this node was reached.
                        This is None for the start node.

    [depth            ] The depth at which this node was found in the
                        traversal. This is 0 for the start node.

    [returned_count   ] The number of returned nodes so far.

 Nodes yielded by a traversal has an additional 'depth' attribute with the
 same semantics as above.

** Example Traversal declaration

------------------------------------------------------------------
class Hackers(neo4j.Traversal):
    types = [
        neo4j.Outgoing.knows,
        neo4j.Outgoing.coded_by,
        ]
    order = neo4j.DEPTH_FIRST
    stop = neo4j.STOP_AT_END_OF_GRAPH

    def isReturnable(self, position):
        return (not position.is_start
                and position.last_relationship.type == 'coded_by')

# Usage:
for hacker_node in Hackers(traversal_start_node):
    # do stuff with hacker_node
------------------------------------------------------------------

* Further information

 For more information about Neo4j, please visit {{http://neo4j.org/}}

 Please direct questions and discussions about Neo4j.py to the Neo4j
 mailing list: {{https://lists.neo4j.org/mailman/listinfo/user}}


 Copyright (c) 2008-2009 "Neo Technology,"
    Network Engine for Objects in Lund AB {{http://neotechnology.com}}
"""

class NeoService(object):
    # This class defines the API and implementation but is never instantiated
    # This class is instead redefined in the _core module.
    # Having the class defined here serves a documentation purpouse
    """This is the heart of Neo4j.

<<Usage:>>

---------------------------------------------------
neo = NeoService("/path/to/node_store/", **options)
---------------------------------------------------

* Accepted options

    [classpath] A list of paths to jar files or directories that contain
                Java class files.

    [ext_dirs ] A list of paths that contain jar files.

    [jvm      ] The path to the Java virtual machine to use.

    [username ] The username to use when connecting to a remote server.

    [password ] The password to use when connecting to a remote server.

    [start_server] True if attempts should be made to start remote server.

    [server_path ] The path to where the server db is stored.

    The classpath or ext_dirs options are used for finding the Java
    implementation of Neo4j. If they are not specified it defaults to
    the jar files that are disributed with this package.
    """
    @property
    def transaction(self):
        """Access the transaction context for this NeoService.

 <<Usage:>>

---------------------
with neo.transaction:
    # do stuff...
---------------------
        """
        return self.__transaction()
    def index(self, name, create=False, **options):
        """Access an index for this NeoService.

 The name parameter is string containing the name of the
 index. If the create parameter is True the index is created
 if it does not exist already. Otherwise an exception is
 thrown for indexes that does not exist.

 <<Usage:>>

------------------------------
name_index = neo.index('name')
------------------------------
        """
        return self.__index.get(name, options, create)
    @property
    def node(self):
        """Access the nodes in this NeoService.

 <<Usage:>>

-------------------------------------------------------------------
node = neo.node[x] # lookup the node with id=x
node = neo.node()  # create a new node
node = neo.node(name="Thomas Anderson", # create a new node and set
                age=27)           # the 'name' and 'age' properties
-------------------------------------------------------------------
        """
        return self.__nodes
    @property
    def relationship(self):
        """Access the relationships in this NeoService.

 <<Usage:>>

-------------------------------------------------------------------
relationship = neo.relatoionship[x] # lookup relationship with id=x
-------------------------------------------------------------------
        """
        return self.__relationships
    @property
    def reference_node(self):
        """Get the reference node for this NeoService.
        
 <<Usage:>>

-----------------------------
ref_node = neo.reference_node
-----------------------------
        """
        return self.__nodes.reference
    def shutdown(self):
        """Shut down this NeoService."""
        if self.__index is not None:
            self.__index.shutdown()
        if self.__neo is not None:
            self.__neo.shutdown()
    close = shutdown
    def __new__(cls, resource_uri, **params):
        global NotFoundError, NotInTransactionError, Traversal,\
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
        # Define replacement __new__
        @staticmethod
        def __new__(cls, resource_uri, **params):
            return core.load_neo(resource_uri, params)
        cls.__new__ = __new__
        return neo

def transactional(accessor):
    global transactional
    try:
        from neo4j._util import transactional
    except:
        raise NotImplementedError("@transactional requires Python >= 2.5.")
    return transactional(accessor)
