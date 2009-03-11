# -*- coding: utf-8 -*-

from neo4j.model.base import (
    NodeModel as Node, RelationshipModel as Relationship,
    Property,
    Relationship, Related, StartNode, EndNode,
    String, Integer, Float, Boolean,
    StringArray, IntegerArray, FloatArray, BooleanArray,
    )
from neo4j.model.fields import (String, StringArray,
                                Integer, IntegerArray,
                                Float, FloatArray,)
from django.db import models as django
from django.db.models.fields.related import TODO as Field

__all__ = (
    # Model wrappers
    'NodeModel', 'RelationshipModel',
    # Relationships
    'Relationship', 'Related',
    'StartNode',    'EndNode',
    # RDBMS integration
    'NodeField', 'RelationshipField', 'Entry',
    # Properties
    'String',  'StringArray',
    'Integer', 'IntegerArray',
    'Float',   'FloatArray',
    'Boolean', 'BooleanArray',
    )

def metaclass(*bases):
    meta_bases = []
    for base in bases:
        if hasattr(base, '__metaclass__'):
            meta_bases.append(base.__metaclass__)
    def __init__(self, name, bases, dict):
        super(MetaClass, self).__init__(name, bases, dict)
    def make_accessor(self, is_node, key, desc):
        pass # TODO
    MetaClass = type('NeoDjangoModelMetaClass', meta_bases, {
            '__init__':      __init__,
            'make_accessor': make_accessor,
            })
    return MetaClass

# Model base specifications for Neo entities in Django

class NodeModel(Node, django.Model):
    """Defines a model backed by a Node."""
    __metaclass__ = metaclass(Node, django.Model)
    def __init__(self, *args, **kwargs):
        Node.__init__(self, *args, **kwargs)

class RelationshipModel(Relationship, django.Model):
    """Defines a model backed by a Relationship."""
    __metaclass__ = metaclass(Relationship, django.Model)
    def __init__(self, *args, **kwargs):
        Relationship.__init__(self, *args, **kwargs)

# Property specifications for integrating with RDBMS models in Django

class NodeField(Field):
    """Defines a field in a RDBMS table that references a Neo Node."""
    pass

class RelationshipField(Field):
    """Defines a field in a RDBMS table that references a Neo Relationship."""
    pass

class Entry(Property):
    """Defines a link to an entry in a RDBMS table from a Neo entity."""
    pass
