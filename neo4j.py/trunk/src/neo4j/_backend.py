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
Wrapper for different backends for Neo4j.py

Neo4j.py works with the Jython platform or JPype on the CPython platform.

This part of Neo4j.py defines the difference between Jython and CPython/JPype,
starting the needed connections and importing the required classes. Here we
also take care of defining wrappers for maping types correcty with the different
backends.


Copyright (c) 2008 "Neo Technology,"
    Network Engine for Objects in Lund AB [http://neotechnology.com]
"""

# There is some stuff to do here... Such as implement conversions of arrays.

from __future__ import generators # Python >= 2.2 feature. Assert Python version
try:
    import _neo_jcc
    by = 'jcc'
except:
    import __java__ # Import java class importing capabilitied
    by = __java__.by

try:
    from org.neo4j.api.core import EmbeddedNeo
except:
    raise EnvironmentError("Neo4j is not available on the classpath")
else:
    if not isinstance(EmbeddedNeo, __java__.classtype):
        raise EnvironmentError("Neo4j is not available on the classpath")

import sys
if sys.version_info < (2,2):
    raise EnvironmentError("Neo4j.py depends on Python >= 2.2 features.")

from org.neo4j.api.core import Direction
from org.neo4j.api.core import RelationshipType
from org.neo4j.api.core import ReturnableEvaluator, StopEvaluator
from org.neo4j.api.core.Traverser import Order

if by == 'jcc':
    raise EnvironmentError("JCC support not implemented yet.")
elif by == "jython":
    NativeRelationshipType = RelationshipType
    class NativeEvaluator(StopEvaluator, ReturnableEvaluator): pass
    from jarray import array as JArray
    import java.lang.Object

    def JavaObjectArray(sequence):
        return JArray(sequence, java.lang.Object)

    def pythonValue(java_value):
        """Convert a java object into a python object"""
        # TODO: implement this conversion scheme
        return java_value

    def javaValue(python_value):
        """Convert a python object into a java object"""
        # TODO: implement this conversion scheme
        return python_value

    class Evaluator(StopEvaluator,ReturnableEvaluator):
        """A Jython adaptor for StopEvaluator and ReturnableEvaluator."""
        def __init__(self,evaluator):
            self.__evaluator = evaluator
        def isReturnableNode(self,position):
            return self.__evaluator.isReturnableNode(position)
        def isStopNode(self,position):
            return self.__evaluator.isStopNode(position)
    
elif by == 'jpype':
    from jpype import JPackage, java, JProxy, JArray
    class Native(object): pass
    class NativeRelationshipType(Native):
        _Native__java_proxy_type = RelationshipType
    class NativeEvaluator(Native):
        _Native__java_proxy_type = [StopEvaluator, ReturnableEvaluator]

    JavaObjectArray = JArray(java.lang.Object)

    def pythonValue(java_value):
        """Convert a java object into a python object"""
        # TODO: implement this conversion scheme
        return java_value

    def defaultJavaType(value):
        raise TypeError, "Cannot automatically convert type '%s'" % type(value)
    javaTypes = {
        str: java.lang.String,
        unicode: java.lang.String,
        int: java.lang.Integer,
        long: java.lang.Long,
        float: java.lang.Float,
        bool: java.lang.Boolean,
        }
    def javaValue(python_value):
        """Convert a python object into a java object"""
        # TODO: implement this conversion scheme
        if isinstance(python_value, Native):
            proxy_type = python_value._Native__java_proxy_type
            return JProxy(proxy_type, inst=python_value)
        if isinstance(python_value, (StopEvaluator, ReturnableEvaluator)):
            return python_value
        return javaTypes.get(type(python_value),defaultJavaType)(python_value)

else:
    raise EnvironmentError("Unknown Java enabler: '%s'" % by)
