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
The relationship part of Neo4j.py

This defines the various routines for handling relationships and relationhip
types.


Copyright (C) 2008  Network Engine for Objects in Lund AB [neotechnology.com]
"""

from __future__ import generators

from _base import Base
from _backend import NativeRelationshipType
from _backend import pythonValue, javaValue
from _property import PropertyDict, getDefaultValueFrom
from _constants import BOTH, INCOMING, OUTGOING

class RelationshipType(NativeRelationshipType):
    __RelationshipTypes = {}
    __name = None
    __dir = None
    def __init__(self, name, dir=BOTH):
        self.__name = name
        self.__dir = BOTH
    def __new__(cls, name, dir=BOTH):
        if not dir in (BOTH, INCOMING, OUTGOING):
            raise TypeError("Illegal direction")
        if not isinstance(name, (str,unicode)):
            name = name.name()
        if name in cls.__RelationshipTypes:
            set = cls.__RelationshipTypes[name]
        else:
            set = []
            for direction in (BOTH, INCOMING, OUTGOING):
                self = object.__new__(cls)
                #self.__name = name
                #self.__dir = direction
                set.append(self)
            cls.__RelationshipTypes[name] = set = tuple(set)
        if dir is BOTH:
            return set[0]
        elif dir is INCOMING:
            return set[1]
        elif dir is OUTGOING:
            return set[2]
        else:
            raise TypeError("Illegal direction.")

    def __eq__(self, other):
        try:
            return self.name() == other.name()
        except:
            return False
    def name(self):
        return self.__name
    #@property # Would require Python >= 2.4
    def direction(self):
        return self.__dir
    direction = property(direction)
    # Direction getters
    outgoing = property(lambda self: RelationshipType(self.__name, OUTGOING))
    OUTGOING = Outgoing = OUT = Out = out = outgoing
    incoming = property(lambda self: RelationshipType(self.__name, INCOMING))
    INCOMING = Incoming = IN = In = incoming
    

class Relationship(PropertyDict):
    """Represents a relationship in the Neo node space.
    Relationships implements the dict interface for storeing properties."""
    __getRel = None
    __actualRel = None
    __typeName = None
    __string = None
    #@property # Would require Python >= 2.4
    def __rel(self):
        if self.__actualRel is None:
            self.__actualRel = self.__getRel()
        return self.__actualRel
    __rel = property(__rel)
    def __init__(self, getRel):
        self.__getRel = getRel
    
    def __str__(self):
        if self.__string is None:
            self.__string = "%s[%s -> %s]" % (self.type.name(),
                                              self.start, self.end)
        return self.__string

    def getProperty(self, key, *args, **kwargs):
        """Get the value of a property.
        relationship.getProperty(key[,default]) -> the value of property
        indexed by key, or default if that property does not exist."""
        hasDefault, default = getDefaultValueFrom(args,kwargs)
        if hasDefault:
            return pythonValue( self.__rel.getProperty(key,
                                                       javaValue(default)) )
        else:
            return pythonValue( self.__rel.getProperty(key) )
    def hasProperty(self, key):
        """Returns True iff the relationship has the specified property."""
        return self.__rel.hasProperty(key)
    def removeProperty(self, key):
        """Remove the specified property from the relationship.
        Return the property value."""
        return pythonValue( self.__rel.removeProperty(key) )
    def setProperty(self, key, value):
        """Set the value of the specified property."""
        self.__rel.setProperty(key, javaValue(value))
    def getPropertyKeys(self):
        """Returns an iterator over all property keys."""
        # Would require Python >= 2.5
        #return ( key for key in self.__rel.getPropertyKeys().iterator() )
        for key in self.__rel.getPropertyKeys():
            yield key
    def getPropertyValues(self):
        """Returns an iterator over all property values."""
        # Would require Python >= 2.5
        #it = self.__rel.getPropertyKeys().iterator()
        #return ( pythonValue(value) for value in it )
        for value in self.__rel.getPropertyKeys():
            yield pythonValue(value)

    def __eq__(self,other):
        """a.__eq__(b) <=> a == b"""
        if isinstance(other, Relationship):
            return bool( self.__rel.equals(other.__rel) )
        return False

    def delete(self):
        """Delete this relationship."""
        self.__rel.delete()

    def getId(self):
        """Return the id of this relationship."""
        return self.__rel.getId()
    id = property(getId)
    
    def getStartNode(self):
        """Return the node where this relationship starts."""
        return self.__Node( self.__rel.getStartNode() )
    start = property(getStartNode)
    def getEndNode(self):
        """Return the node where this relationship ends."""
        return self.__Node( self.__rel.getEndNode() )
    end = property(getEndNode)

    def getNodes(self):
        """Return a 2-tuple (start, end) containing the two nodes related by
        this relationship."""
        nodes = self.__rel.getNodes()
        return self.__Node(nodes[0]), self.__Node(nodes[1])
    nodes = property(getNodes)

    def getOtherNode(self,node):
        """Given one node of the relationship this method returns the other."""
        # NOTE: __Node_node is a bit of an ugly hack...
        return self.__Node( self.__rel.getOtherNode( node._Node__node ) )

    def getType(self):
        """Returns the relationship type of this relationship."""
        return RelationshipType( self.__rel.getType().name() )
    type = property(getType)

class RelationshipFactory(Relationship):
    __start = None
    __typeName = None
    __actualType = None
    #@property # Would require Python >= 2.4
    def __type(self):
        if self.__actualType is None:
            self.__actualType = RelationshipType(self.__typeName)
        return self.__actualType
    __type = property(__type)
    def __init__(self, start, typ):
        if isinstance(typ, (str,unicode)):
            self.__typeName = typ
        else:
            self.__actualType = typ
        def getRel():
            return start.getSingleRelationship(self.__type)
        super(RelationshipFactory, self).__init__(getRel)
        self.__start = start
    outgoing = property(lambda self: RelationshipFactory(self.__start,
                                                         self.__type.outgoing))
    OUTGOING = Outgoing = OUT = Out = out = outgoing
    incoming = property(lambda self: RelationshipFactory(self.__start,
                                                         self.__type.incoming))
    INCOMING = Incoming = IN = In = incoming
    def __call__(self, node, **properties):
        relation = self.__start.createRelationshipTo(node, self.__type)
        for key,value in properties.iteritems():
            relation[key] = value
        return relation
    def __iter__(self):
        return self.__start.getRelationships(self.__type)
