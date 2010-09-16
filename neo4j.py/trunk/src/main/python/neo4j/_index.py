# -*- coding: utf-8 -*-

# Copyright (c) 2008-2010 "Neo Technology,"
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
This module defines the index behaviour.


 Copyright (c) 2008-2010 "Neo Technology,"
     Network Engine for Objects in Lund AB [http://neotechnology.com]
"""

from neo4j._base import node as get_node

def initialize(backend):
    global IndexService
    from neo4j._primitives import Node


    class IndexService(object):
        def __init__(self, core, neo):
            self.__neo = core
            self.__index = backend.IndexService(neo)
            self.__ftindex = backend.FulltextIndexService(neo)
            self.__ftqindex = backend.FulltextQueryIndexService(neo)
            self.__indexes = {}
        def get(self, name, options, create=False):
            if options and not create:
                raise TypeError(
                    "Options may only be defined when index can be created.")
            if name in self.__indexes:
                return self.__indexes[name]
            elif create:
                return self.create(name, options)
            else:
                raise KeyError("No index named '%s'." % name)
        def create(self, name, options):
            index = None
            if options and 'full_text_with_query' in options and options['full_text_with_query']:
                index = Index(self.__ftqindex, self.__neo, name, **options)
            elif options and 'full_text' in options and options['full_text']:
                index = Index(self.__ftindex, self.__neo, name, **options)
            else:
                index = Index(self.__index, self.__neo, name, **options)
            self.__indexes[name] = index
            try:
                self.__neo.admin.keep_logical_logs = None
            except:
                pass
            return index
        def shutdown(self):
            if self.__index is not None:
                self.__index.shutdown()
            if self.__ftindex is not None:
                self.__ftindex.shutdown()
            if self.__ftqindex is not None:
                self.__ftqindex.shutdown()
            self.__indexes = {}
            self.__index = None
            self.__ftindex = None
            self.__ftqindex = None

    class Index(object):
        #NOTE: there may be a better place for these - mhuffman
        SORT_RELEVANCE = backend.SORT_RELEVANCE
        SORT_INDEXORDER = backend.SORT_INDEXORDER

        #NOTE: full text is not used here, give the change in the create() method - mhuffman
        def __init__(self, index, neo, key, full_text=False, **ignored):
            self.__index = index
            self.__neo = neo
            self.__key = key

        # Multiple values
        #NOTE: match and nodes methods overloaded with options to handle sorting - mhuffman
        def match(self, query, **options):
            if options and 'sort_order' in options:
                return IndexHits(self.__neo, self.__index.getNodes(self.__key, query, options['sort_order']))
            else:
                return IndexHits(self.__neo, self.__index.getNodes(self.__key, query))
            #raise NotImplementedError('"Fulltext" query is not implemented.')
        def nodes(self, key, **options):
            if options and 'sort_order' in options:
                return IndexHits(self.__neo, self.__index.getNodes(self.__key, key, options['sort_order']))
            else:
                return IndexHits(self.__neo, self.__index.getNodes(self.__key, key))
        def add(self, key, *nodes):
            for node in nodes:
                node = get_node(node)
                self.__index.index(node, self.__key, key)
        def remove(self, key, *nodes):
            for node in nodes:
                node = get_node(node)
                self.__index.removeIndex(node, self.__key, key)
        # Single values
        def __getitem__(self, key):
            node = self.__index.getSingleNode(self.__key, key)
            if node is None: return None # XXX: or should this raise KeyError?
            return Node(self.__neo, node)
        def __setitem__(self, key, node):
            node = get_node(node)
            del self[key]
            self.__index.index(node, self.__key, key)
        def __delitem__(self, key):
            old = self.__index.getSingleNode(self.__key, key)
            if old is not None:
                self.__index.removeIndex(old, self.__key, key)

    class IndexHits(object):
        def __init__(self, neo, hits):
            self.__neo = neo
            self.__hits = hits
        def __len__(self):
            return self.__hits.size()
        def __iter__(self):
            hits = self.__hits
            while hits.hasNext():
                yield Node(self.__neo, hits.next())

