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
The traversal part of the Neo4j.py api.

Traversers are objects that iterates over the nodes in the nodespace, starting
at a given node, according to two evaluator functions(/objects). The two
evaluators are one that prunes the network (decides wheter the traverser should
stop at a given position or continue) and one that decides which nodes should
be yielded by the traverser.


Copyright (C) 2008  Network Engine for Objects in Lund AB [neotechnology.com]
"""

from __future__ import generators

from _base import Base
from _constants import END_OF_NETWORK, ALL, DEPTH_FIRST
from _backend import StopEvaluator, ReturnableEvaluator, NativeEvaluator
from _backend import javaValue, JavaObjectArray
from _node import Node

class TraversalNode(Node):
    """A node that keeps track of where in the traversal it was yielded."""
    __pos = None
    def __init__(self, pos):
        super(TraversalNode, self).__init__( pos.currentNode() )
        self.__pos = pos

    def currentNode(self):
        """Returns the node at the current traversal position."""
        return Node( self.__pos.currentNode() )

    def depth(self):
        """Returns how many traversal steps from the start node the current
        traversal position is."""
        return self.__pos.depth()

    def lastRelationshipTraversed(self):
        """Returns the last relationship the traverser crossed.
        Please note that for the first traversal position (the start node)
        this returns None."""
        rel = self.__pos.lastRelationshipTraversed()
        if rel is None:
            return None
        return Relationship( lambda:rel )
                             
    def previousNode(self):
        """Returns the node at the previous traversal position.
        Please note that for the first traversal position (the start node)
        this returns None."""
        node = self.__pos.previousNode()
        if node is None:
            return None
        return Node( node )

    def returnedNodesCount(self):
        """Returns the number of nodes the traverser has returned so far."""
        return self.__pos.returnedNodesCount()

class Traversal(Base):
    __node = None
    __order = None
    __stop = None
    __return = None
    __types = None
    def __init__(self, node, order,
                 stop=END_OF_NETWORK, returnable=ALL, types=None):
        self.__node = node
        self.__order = order
        self.__stop = stop
        self.__return = returnable
        if types is None:
            types = []
        self.__types = types
    def __iter__(self):
        if not self.__types:
            return
        reltypes = []
        for rel in self.__types:
            reltypes.append(javaValue(rel))
            reltypes.append(rel.direction)
        traverser = self.__node.traverse(self.__order, javaValue(self.__stop),
                                         javaValue(self.__return),
                                         JavaObjectArray(reltypes))
        while traverser.hasNext():
            traverser.next()
            yield TraversalNode( traverser.currentPosition() )

class TraversalFactory(Base):
    __node = None
    __order = None
    __stop = None
    __return = None
    __relTypes = None
    def __init__(self, node, order, stop=END_OF_NETWORK,
                 returnable=ALL, relTypes=None):
        self.__node = node
        self.__order = order
        self.__stop = stop
        self.__return = returnable
        if relTypes is None:
            relTypes = []
        self.__relTypes = relTypes
    def __iter__(self):
        return iter(self.__node.traverse(order=self.__order,
                                         stop=self.__stop,
                                         returnable=self.__return,
                                         types=self.__relTypes))
    def __update(self, stop, returnable, *relTypes):
        types = list()
        types.extend(self.__relTypes)
        types.extend(relTypes)
        return TraversalFactory(self.__node, self.__order,
                                stop, returnable, types)
    def __getattribute__(self, attr):
        if attr is '__class__':
            return self.__traversalBase
        else:
            return object.__getattribute__(self,attr)
    def __traversalBase(self, name, bases, dict):
        if 'isStopNode' in dict:
            isStopNode = dict['isStopNode']
        else:
            isStopNode = None
        if 'isReturnableNode' in dict:
            isReturnableNode = dict['isReturnableNode']
        else:
            isReturnableNode = None
        evaluator = Evaluator(isStopNode, isReturnableNode)
        stop = self.__stop
        returnable = self.__returnable
        if 'isStopNode' in dict:
            stop = evaluator
        if 'isReturnableNode' in dict:
            returnable = evaluator
        return self.__update(stop, returnable)
    def __call__(self, *args):
        result = self
        for arg in args:
            if isinstance(arg, (StopEvaluator, ReturnableEvaluator)):
                stop = self.__stop
                returnable = self.__return
                if isinstance(arg, StopEvaluator):
                    stop = arg
                if isinstance(arg, ReturnableEvaluator):
                    returnable = arg
                result = self.__update(stop, returnable)
                continue
            elif hasattr(arg, '__name__'):
                if arg.__name__ == 'isStopNode':
                    evaluator = Evaluator(stop=arg)
                    result = self.__update(evaluator, self.__return)
                    continue
                elif arg.__name__ == 'isReturnableNode':
                    evaluator = Evaluator(returnable=arg)
                    result = self.__update(self.__stop, evaluator)
                    continue
                else:
                    try:
                        if arg.canEvaluateStop:
                            stop = arg
                        else:
                            stop = self.__stop
                        if arg.canEvaluateReturn:
                            returnable = arg
                        else:
                            returnable = self.__return
                        result = self.__update(stop, returnable)
                        continue
                    except:
                        raise ValueError("Illegal evaluator")
            else:
                return self.__update(self.__stop, self.__return, *args)
        return result



class Evaluator(NativeEvaluator):
    __stop = None
    __return = None
    canEvaluateStop = property(lambda self: self.__stop is not None)
    canEvaluateReturn = property(lambda self: self.__return is not None)
    def __init__(self, stop=None, returnable=None):
        self.__stop = stop
        self.__return = returnable

    def isStopNode(self,position):
        """Is the node at the current traversal position a stop node?"""
        return bool( self.__stop( TraversalNode(position) ) )

    def isReturnableNode(self,position):
        """Should the node at the current traversal position be returned?"""
        return bool( self.__return( TraversalNode(position) ) )


from _relationship import Relationship

