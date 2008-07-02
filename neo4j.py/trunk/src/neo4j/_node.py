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
The node part of Neo4j.py


Copyright (C) 2008  Network Engine for Objects in Lund AB [neotechnology.com]
"""

from __future__ import generators

from _base import Base
from _backend import pythonValue, javaValue, JavaObjectArray
from _property import PropertyDict, getDefaultValueFrom
from _constants import BREADTH_FIRST, DEPTH_FIRST


class Node(PropertyDict):
    """Represents a node in the Neo node space.
    Nodes implements the dict interface for storeing properties."""
    __node = None
    __id = None
    def __init__(self, node):
        self.__node = node
        self.__id = node.getId()

    def __str__(self):
        return "Node[%s]" % self.__id

    def __setattr__(self, attr, value):
        if hasattr(self, attr) and attr.startswith('_'):
            object.__setattr__(self, attr, value)
        else:
            super(Node, self).__setattr__(attr,value)
    def __getattr__(self, attr):
        return RelationshipFactory(self, attr)
    def __delattr__(self, attr):
        if hasattr(self, attr):
            super(Node, self).__delattr__(attr)
        else:
            # Delete a single relationship
            getattr(self, attr).delete()

    def getProperty(self, key, *args, **kwargs):
        """Get the value of a property.
        node.getProperty(key[,default]) -> the value of property indexed by
        key, or default if that property does not exist."""
        hasDefault, default = getDefaultValueFrom(args,kwargs)
        if hasDefault:
            return pythonValue( self.__node.getProperty(key,
                                                        javaValue(default)) )
        else:
            return pythonValue( self.__node.getProperty(key) )
    def hasProperty(self, key):
        """Returns True iff the node has the specified property."""
        return bool(self.__node.hasProperty(key))
    def removeProperty(self, key):
        """Remove the specified property from the node.
        Return the property value."""
        return pythonValue( self.__node.removeProperty(key) )
    def setProperty(self, key, value):
        """Set the value of the specified property."""
        self.__node.setProperty(key, javaValue(value))
    def getPropertyKeys(self):
        """Returns an iterator over all property keys."""
        for key in self.__node.getPropertyKeys():
            yield key
    def getPropertyValues(self):
        """Returns an iterator over all property values."""
        for value in self.__node.getPropertyKeys():
            yield pythonValue(value)

    def __eq__(self, other):
        """a.__eq__(b) <=> a == b"""
        if isinstance(other, Node):
            return bool( self.__node.equals(other.__node) )
        return False

    def createRelationshipTo(self, otherNode, typ):
        """Create a relationship of specified type to the specified node.
        Returns the newly created relationship."""
        rel = self.__node.createRelationshipTo(otherNode.__node, javaValue(typ))
        return Relationship(lambda:rel)

    def delete(self):
        """Delete this node"""
        self.__node.delete()

    def getId(self):
        """Returns the id of this node."""
        return self.__node.getId()
    id = property(getId)

    def getRelationships(self, *types):
        """Returns an iterator over the relationships of the specified types."""
        if not types:
            it = self.__node.getRelationships().iterator()
        elif len(types) == 1:
            typ = types[0]
            if isinstance(typ, Direction):
                it = self.__node.getRelationships(typ).iterator()
            else:
                it = self.__node.getRelationships(javaValue(typ),
                                                  typ.direction).iterator()
        else:
            typs = []
            for typ in types:
                typs.append(javaValue(typ))
            it = self.__node.getRelationships(*typs).iterator()
        for rel in it:
            yield Relationship(lambda:rel)

    def getSingleRelationship(self, typ):
        """Return a single relationship of a specified type (with direction).
        Raises an exception if more than one relationships exists."""
        rel = self.__node.getSingleRelationship(javaValue(typ), typ.direction)
        return Relationship(lambda:rel)

    #@property # Would require Python >= 2.4
    def traverse(self, *args, **kwargs):
        """Returns a traversal definer that starts traversing at this node."""
        return Traversal(self.__node, *args, **kwargs)

    #@property # Would require Python >= 2.4
    def BreadthFirst(self):
        """A traverser factory for a breadth first traversal"""
        return TraversalFactory(self, BREADTH_FIRST)
    BreadthFirst = property(BreadthFirst) # Python < 2.4 support

    #@property # Would require Python >= 2.4
    def DepthFirst(self):
        """A traverser factory for a depth first traversal"""
        return TraversalFactory(self, DEPTH_FIRST)
    DepthFirst = property(DepthFirst) # Python < 2.4 support


# Imports

from _relationship import Relationship, RelationshipFactory
from _traverse import Traversal, TraversalFactory

