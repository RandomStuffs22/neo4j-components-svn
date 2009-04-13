Neo4j.py is the Python bindings for the Neo4j graph database.

Neo4j.py can be used either with Jython or with JPype or JCC in CPython.
Neo4j.py is used in exactly the same way regardless of which backend is used.

The typical way to use Neo4j.py is:
   from neo4j import NeoService
   neo = NeoService( "/neo/db/path" )
   with neo.transaction:
       node = neo.reference_node
       node = neo.node()
       # start manipulating the node space, starting from node.
   neo.shutdown()

Note that this example requires the with statement that was added in Python 2.5.
In Python 2.6 and later the with statement is always enabled. In Python 2.5 it
needs to be imported from the __future__ module:
    from __future__ import with_statement

Further information about Neo4j can be found at http://neo4j.org/
