# -*- coding: utf-8 -*-
# Copyright (c) 2008 "Neo Technology,"
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

A Python wrapper for Neo4j, http://neo4j.org/
Websites: http://neo4j.org/py/, https://projects.thobe.org/neo4j.py/

Neo4j.py can be used either with Jython or with JPype in CPython.
The usage of Neo4j.py should be enirely transparent to the usage of the
Jython or JPype backend.

The typical way to use Neo4j.py is:
   from neo4j import * # this imports EmbeddedNeo, Transaction and neo4j
   neo = EmbeddedNeo( "/neo/db/path" )
   with Transaction():
       node = neo.getReferenceNode()
       node = neo.createNode()
       # start manipulating the node space, starting from node.

   neo.shutdown()

Further information about Neo4j can be found at http://neo4j.org/
The Java api documentation can be found at http://api.neo4j.org/

Copyright (c) 2008 "Neo Technology,"
    Network Engine for Objects in Lund AB [http://neotechnology.com]
"""

# NOTE: We would like to use absolute imports,
# but that is not supported in the current version of Jython
#from __future__ import absolute_import

# Initialize the backend engine. Assert that required libraries are available.
import _backend; del _backend
# Initialize all hooks
import _hooks; del _hooks

# Define the names that should get imported on "from neo4j import *"
__all__ = ('EmbeddedNeo',
           'Transaction',
           'transactional',
           'neo4j', # import the module itself on import *
           )

# Import the objects that should exist in the Neo4j.py namespace
from _neo import EmbeddedNeo
from _transaction import Transaction
# StopEvaluator constants
from _constants import DEPTH_ONE, END_OF_NETWORK
# ReturnableEvaluator constants
from _constants import ALL, ALL_BUT_START_NODE
# Traverser.Order constants
from _constants import BREADTH_FIRST, DEPTH_FIRST

# Make the module expose itself in its namespace
import neo4j
