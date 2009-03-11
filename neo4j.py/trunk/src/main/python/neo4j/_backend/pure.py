# -*- coding: utf-8 -*-

raise ImportError("Pure Python backend not implemented.")

def initialize(classpath, parameters):
    return None, RemoteNeo

class RemoteNeo(object):
    pass

class IndexService(object):
    pass

INCOMING           = object()
OUTGOING           = object()
BOTH               = object()

BREADTH_FIRST      = object()
DEPTH_FIRST        = object()

ALL                = object()
ALL_BUT_START_NODE = object()
END_OF_GRAPH       = object()

def array(obj):
    return obj
def to_java(obj):
    return obj
def to_python(obj):
    return obj
def RelationshipType(type):
    return type

class NotFoundException(Exception):
    pass
class NotInTransactionException(Exception):
    pass

class Evaluator(object):
    pass
class StopAtDepth(object):
    pass
