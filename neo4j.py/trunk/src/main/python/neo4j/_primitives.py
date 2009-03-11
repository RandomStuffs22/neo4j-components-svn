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
This module defines the primitive objects in Neo4j.


Copyright (c) 2008-2009 "Neo Technology,"
    Network Engine for Objects in Lund AB [http://neotechnology.com]
"""

from neo4j._base import Neo4jObject,\
    node as get_node, relationship as get_relationship

class PropertyDict(object): # NOTE: Should this inherit from dict?
    """An implementation of the dict interface for storeing properties.
    This is the base class for Node and Relation."""
    def __init__(self, owner):
        self.__get  = owner.getProperty
        self.__set  = owner.setProperty
        self.__has  = owner.hasProperty
        self.__del  = owner.removeProperty
        self.__keys = owner.getPropertyKeys
        self.__vals = owner.getPropertyValues
        self.__id   = owner.getId
    @property
    def id(self):
        return self.__id()

    def __getitem__(self,item):
        """s.__getitem__(p) <=> s[p]"""
        return self.__get(item)
    def __setitem__(self,item,value):
        """s.__setitem__(p,v) <=> s[p] = v"""
        self.__set(item, value)
    def __delitem__(self,item):
        """s.__delitem__(p) <=> del s[p]"""
        self.__del(self,item)
    def __contains__(self,item):
        """s.__contains__(p) <=> p in s <=> s.has_key(p)"""
        return self.__has(item)

    def __iter__(self):
        """s.__iter__() <=> iter(s) <=> s.iterkeys()"""
        return self.iterkeys()
    def __len__(self):
        """s.__len__() <=> len(s)
        Returns the number of properties in s."""
        return len( self.keys() )
    
    def get(self,key,*args,**kwargs):
        """s.get(p[,d]) -> s[p] if p in s else d."""
        hasDefault, default = getDefaultValueFrom(args,kwargs)
        if key in self:
            return self[key]
        elif hasDefault:
            return default
        else:
            raise KeyError        
    def clear(self):
        """Remove all properties from this Node/Relationship."""
        for key in self.keys():
            del self[key]
    def copy(self): # TODO: should this perhaps return a copy as a dict?
        """Not Implemented."""
        raise SystemError("Copying of Neo4j object is not supported.")
    def has_key(self,key):
        """s.has_key(p) -> True if s has a property p, else False."""
        return self.__has(key)

    def items(self):
        """s.items() -> list of s's property (key, value) pairs, as 2-tuples."""
        return [item for item in self.iteritems()]
    def iteritems(self):
        """s.iteritems() -> an iterator over the property (key, value) items
        of s."""
        for key in self:
            yield key, self[key]
    def keys(self):
        """s.keys() -> list of s's property keys."""
        return [key for key in self.iterkeys()]
    def iterkeys(self):
        """s.iterkeys() -> an iterator over the property keys in s."""
        return iter(self.__keys())
    def values(self):
        """s.values() -> list of s's property values."""
        return [value for value in self.itervalues()]
    def itervalues(self):
        """s.itervalues() -> an iterator over the property values in s."""
        return iter(self.__vals())

    def pop(self,key,*args,**kwargs):
        """s.pop(k[,d]) -> v, remove the property specified by key and return
        the corresponding value. If key is not found, d is returned if given,
        otherwise KeyError is raised."""
        hasDefault, default = getDefaultValueFrom(args,kwargs)
        if key in self:
            return self.__del(key)
        elif hasDefault:
            return default
        else:
            raise KeyError

    def popitem(self):
        """s.popitem() -> (k, v), remove and return some property (key, value)
        pair as a 2-tuple; raise KeyError if D is empty."""
        try:
            key = iter(self).next()
            return key, self.pop(key)
        except:
            raise KeyError

    def setdefault(self,key,*args,**kwargs):
        """s.setdefault(k[,d]) -> s.get(k,d), also set s[k]=d if k not in s"""
        hasDefault, default = getDefaultValueFrom(args,kwargs)
        if key in self:
            return self[key]
        elif hasDefault:
            self[key] = default
            return default
        else:
            raise KeyError

    def update(self,*args,**more):
        """s.update(E, **F) -> None.  Update s from E and F:
        if hasattr(E,'keys'):
            for k in E: s[k] = E[k] 
        else:
            for (k, v) in E: s[k] = v
        Then:
        for k in F:
            s[k] = F[k]
        """
        if not len(args) in (0,1):
            raise TypeError("To many arguments.")
        if args:
            for key,value in args[0].iteritems():
                self[key] = value
        for key,value in more.iteritems():
            self[key] = value

def getDefaultValueFrom(args, kwargs):
    """Function for finding a default value in var_[kw_]args.
    Finds a default value. Asserts that there is only one default value.
    Returns True and the default value if there is a defult value.
    Returns False and None if there is no default value.
    (Note that the default value could be None...)"""
    if not (len(args)+len(kwargs)) in (0,1):
        raise TypeError("To many arguments.")
    elif kwargs and 'defaultValue' not in kwargs:
        raise TypeError("Unknown argument '%s'" % kwargs.keys()[0])
    elif args:
        return True, args[0]
    elif 'defaultValue' in kwargs:
        return True, kwargs['defaultValue']
    elif 'default' in kwargs:
        return True, kwargs['default']
    elif 'd' in kwargs:
        return True, kwargs['d']
    else:
        return False, None

def initialize(backend):
    global Node, Relationship
    INCOMING = backend.INCOMING
    OUTGOING = backend.OUTGOING
    BOTH = backend.BOTH
    RelationshipType = backend.RelationshipType
    class Node(Neo4jObject, PropertyDict):
        def __init__(self, neo, node):
            Neo4jObject.__init__(self, neo=neo, node=node)
            PropertyDict.__init__(self, node)
            self.__neo = neo
            self.__node = node
        def __getattr__(self, attr):
            return self.relationships(attr)
        def relationships(self, *types):
            rel_types = []
            for type in types:
                rel_types.append(RelationshipType(type))
            return RelationshipFactory(self.__neo, self.__node, BOTH,
                                       rel_types)
    
    class RelationshipFactory(object):
        def __init__(self, neo, node, dir, types):
            self.__neo = neo
            self.__node = node
            self.__dir = dir
            self.__types = types
            if len(types) == 1:
                self.__single_type = types[0]
            else:
                self.__single_type = None
        def __getRelationships(self):
            if len(self.__types) > 1:
                for type in self.__types:
                    for rel in self.__node.getRelationships(type, self.__dir):
                        yield rel
            else:
                for rel in self.__node.getRelationships(self.__single_type,
                                                        self.__dir):
                    yield rel
        def __hasRelationship(self):
            if len(self.__types) > 1:
                for type in self.__types:
                    if self.__node.hasRelationship(type, self.__dir):
                        return True
                return False
            else:
                return self.__node.hasRelationship(self.__single_type,
                                                   self.__dir)
        def __single(self):
            if not self.__single_type:
                raise TypeError("No single relationship type!")
            return self.__node.getSingleRelationship(self.__single_type,
                                                     self.__dir)
        def __call__(self, node, **attributes):
            node = get_node(node)
            if dir is INCOMING:
                relationship = node.createRelationshipTo(
                    self.__node, self.__single_type)
            else:
                relationship = self.__node.createRelationshipTo(
                    node, self.__single_type)
            relationship = Relationship(self.__neo, relationship)
            relationship.update(attributes)
            return relationship
        def __iter__(self):
            for rel in self.__getRelationships():
                yield Relationship(self.__neo, rel)
        def __nonzero__(self):
            return self.__hasRelationship()
        # - single relationship property -
        def get_single(self):
            single = self.__single()
            if single:
                return Relationship(self.__neo, single)
            else:
                return None
        def set_single(self, node):
            del self.single
            self(node)
        def del_single(self):
            single = self.__single
            if single: single.delete()
        single = property(get_single, set_single, del_single)
        del get_single, set_single, del_single
        @property
        def incoming(self):
            return RelationshipFactory(self.__neo, self.__node,
                                       self.__type, INCOMING)
        @property
        def outgoing(self):
            return RelationshipFactory(self.__neo, self.__node,
                                       self.__type, OUTGOING)
    
    class Relationship(Neo4jObject, PropertyDict):
        def __init__(self, neo, relationship):
            Neo4jObject.__init__(self, neo=neo, relationship=relationship)
            PropertyDict.__init__(self, relationship)
            self.__relationship = relationship
            self.__neo = neo
        @property
        def start(self):
            return Node(self.__neo, self.__relationship.getStartNode())
        @property
        def end(self):
            return Node(self.__neo, self.__relationship.getEndNode())
        @property
        def type(self):
            return self.__relationship.getType().name()
        def getOtherNode(self, node):
            node = get_node(node)
            return Node(self.__neo, self.__relationship.getOtherNode(node))
