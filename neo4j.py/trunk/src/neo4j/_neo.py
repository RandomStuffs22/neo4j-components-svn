# -*- coding: utf-8 -*-
# Copyright 2008 Network Engine for Objects in Lund AB [neotechnology.com]
# 
# This program is free software: you can redistribute it and/or modify
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
The engine part of Neo4j.py, EmbeddedNeo mainly.


Copyright (C) 2008  Network Engine for Objects in Lund AB [neotechnology.com]
"""

from __future__ import generators

from _backend import EmbeddedNeo as _EmbeddedNeo
from _base import Base
import weakref, atexit

instances = []
def hook():
    """Shut down all Neo instances at exit."""
    for ref in instances:
        instance = ref()
        if instance is not None:
            instance.shutdown()
atexit.register(hook)

class EmbeddedNeo(Base):
    """The Neo engine of course.
    This class enables the connection to the Neo engine. This is the main
    entry point to Neo. It provedes the first access to nodes.
    Aditionally instances of this class provides accesses to the relationship
    types available in the session that instance represents."""
    __neo = None
    def __init__(self, storeDir):
        """Start the Neo engine, and return the instance.
        Store the database in storeDir.
        """
        self.__neo = _EmbeddedNeo(storeDir)
        instances.append(weakref.ref(self))
    def __getattr__(self, attr):
        return RelationshipType(attr)

    def createNode(self, **props):
        """Create a new node in the Neo installation used in this session.
        Keyword arguments are added as properties to the node."""
        if self.__neo is None:
            raise SystemError("EmbeddedNeo has been shut down.")
        node = Node( self.__neo.createNode() )
        for key,value in props.iteritems():
            node[key] = value
        return node

    def getNodeById(self, id):
        """Return the node with the specified id."""
        if self.__neo is None:
            raise SystemError("EmbeddedNeo has been shut down.")
        return Node( self.__neo.getNodeById() )

    def getReferenceNode(self):
        """Return the reference node."""
        if self.__neo is None:
            raise SystemError("EmbeddedNeo has been shut down.")
        return Node( self.__neo.getReferenceNode() )
    reference = referenceNode = property(getReferenceNode)

    #@property # Would require Python >= 2.4
    def transaction(self):
        """Start a transaction."""
        return Transaction(self.__neo)
    transaction = property(transaction)

    def shutdown(self):
        """Shut down the Neo engine."""
        if self.__neo is None:
            return
        self.__neo.shutdown()
        self.__neo = None


# Imports of requiered parts of the library
from _node import Node
from _relationship import RelationshipType
from _transaction import Transaction
