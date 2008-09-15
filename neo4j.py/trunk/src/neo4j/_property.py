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
Common base functionality for items that hold properties. Nodes and Relations.

This makes nodes and relations implement the python dict in how properties are
handeled.


Copyright (c) 2008 "Neo Technology,"
    Network Engine for Objects in Lund AB [http://neotechnology.com]
"""

from __future__ import generators

from _base import Base

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

class PropertyDict(Base): # NOTE: Should this inherit from dict?
    """An implementation of the dict interface for storeing properties.
    This is the base class for Node and Relation."""
    def getProperty(self,key,*args,**kwargs):
        raise SystemError("Method getProperty not implemented")
    def hasProperty(self,key):
        raise SystemError("Method hasProperty not implemented")
    def removeProperty(self,key):
        raise SystemError("Method removeProperty not implemented")
    def setProperty(self,key,value):
        raise SystemError("Method setProperty not implemented")
    def getPropertyKeys(self):
        raise SystemError("Method getPropertyKeys not implemented")
    def getPropertyValues(self):
        raise SystemError("Method getPropertyValues not implemented")

    def __getitem__(self,item):
        """s.__getitem__(p) <=> s[p] <=> s.getProperty(p)"""
        return self.getProperty(item)
    def __setitem__(self,item,value):
        """s.__setitem__(p,v) <=> s[p] = v <=> s.setProperty(p,v)"""
        self.setProperty(item,value)
    def __delitem__(self,item):
        """s.__delitem__(p) <=> del s[p] <=> s.removeProperty(p)"""
        self.removeProperty(self,item)
    def __contains__(self,item):
        """s.__contains__(p) <=> p in s <=> s.has_key(p) <=> s.hasProperty(p)"""
        return self.has_key(item)

    def __iter__(self):
        """s.__iter__() <=> iter(s) <=> s.iterkeys() <=> s.getPropertyKeys()"""
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
        return self.hasProperty(key)

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
        return self.getPropertyKeys()
    def values(self):
        """s.values() -> list of s's property values."""
        return [value for value in self.itervalues()]
    def itervalues(self):
        """s.itervalues() -> an iterator over the property values in s."""
        return self.getPropertyValues()

    def pop(self,key,*args,**kwargs):
        """s.pop(k[,d]) -> v, remove the property specified by key and return
        the corresponding value. If key is not found, d is returned if given,
        otherwise KeyError is raised."""
        hasDefault, default = getDefaultValueFrom(args,kwargs)
        if key in self:
            return self.removeProperty(key)
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
    
