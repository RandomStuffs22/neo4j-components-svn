# -*- coding: utf-8 -*-

__all__ = (
    # Model wrappers
    'NodeModel', 'RelationshipModel',
    # Relationships
    'Relationship', 'Related',
    'StartNode',    'EndNode',
    # Properties
    'String',  'StringArray',
    'Integer', 'IntegerArray',
    'Float',   'FloatArray',
    'Boolean', 'BooleanArray',
    )

def metaclass(is_node):
    class NeoModelMetaClass(type):
        def __init__(self, name, bases, dict):
            for key in dict:
                desc = dict[key]
                if isinstance(desc, Descriptor):
                    desc = self.make_accessor(is_node, key, desc)
                    dict[key] = desc
                    setattr(self, key, desc)
        def __setattr__(self, key, value):
            if isinstance(desc, Descriptor):
                value = self.make_accessor(is_node, key, desc)
            type.__setattr__(self, key, value)
        def make_accessor(self, is_node, key, desc):
            return Accessor(self, is_node, key, desc)
    return NeoModelMetaClass

class NodeModel(object):
    __metaclass__ = metaclass(is_node=True)
    def __init__(self, __neo__=None, __node__=None, __relationship__=None,
                 **properties):
        self.__neo = __neo__
        if __node__ is None:
            self.__node = __neo__(lambda __neo__, **ignore:
                                      __neo__.createNode())
        else:
            self.__node = __node__
        set_properties(self, properties)

class RelationshipModel(object):
    __metaclass__ = metaclass(is_node=False)
    def __init__(self, __neo__=None, __relationship__=None, __node__=None,
                 __start__=None, __end__=None, **properties):
        self.__neo = __neo__
        if __relationship__ is None:
            if __start__ is not None and __end__ is not None:
                raise RuntimeError("Do something!")
            else:
                raise TypeError("Non-persisted models not implemented.")
        else:
            assert ((__start__ is None)
                    or (__start__ == __relationship__.getStartNode())), (
                "Illegal start Node.")
            assert ((__end__ is None)
                    or (__end__ == __relationship__.getEndNode())), (
                "Illegal end Node.")
            self.__relationship = __relationship__
        set_properties(self, properties)

def set_properties(model, properties):
    for key in properties:
        setattr(model, key, properties[key])

class Accessor(object):
    def __init__(self, model, is_node, name, desc):
        if is_node:
            assert not isinstance(desc, Node)
        else:
            assert not isinstance(desc, Relationship)
        self.model = model
        self.is_node = is_node
        if desc.name is not None:
            name = desc.name
        self.name = desc.cast_name(name)
        self.desc = desc
    def __get__(self, obj, type=None):
        if obj is None:
            return self.desc.index(type, self.is_node, self.name)
        else:
            return self.desc.get(obj, self.is_node, self.name)
    def __set__(self, obj, value):
        self.desc.set(obj, self.is_node, self.name, value)
    def __delete__(self, obj):
        self.desc.delete(obj, self.is_node, self.name)

class Descriptor(object):
    def __init__(self, model=None, name=None):
        self.name = name
        self.__model = model
        self.__initialized = False
    def cast_name(self, name):
        return name
    @property
    def model(self):
        if isinstance(self.__model, str):
            self.__model = None
        return self.__model

def error(type, message):
    def function(*args, **kwargs):
        raise type(message)
    return function

def relationship_accessors(single, self):
    if single:
        def get(origin, is_node, type):
            neo, node = get_node(origin)
            return self.get_single(neo, node, type)
        def set(origin, is_node, type, value):
            neo, node = get_node(origin)
            other = self.get_target(value)
            if other is None:
                self.set_create(neo, node, type, value)
            else:
                self.set_single(neo, node, type, other)
        def delete(origin, is_node, type):
            neo, node = get_node(origin)
            self.del_single(obj, type)
    else:
        def get(origin, is_node, type):
            neo, node = get_node(origin)
            return self.get_multiple(neo, node, type)
        set = error(TypeError, "Cannot set Multi-relationship.")
        delete = error(TypeError, "Cannot delete Multi-relationship.")
    return get, set, delete

def get_node(entity):
    raise RuntimeError("TODO: implement this")
def get_relationship(entity):
    raise RuntimeError("TODO: implement this")

class Relationship(Descriptor):
    def __init__(self, model, single=False, type=None):
        super(Relationship, self).__init__(model, type)
        self.get, self.set, self.delete = relationship_accessors(single, self)
    def cast_name(self, name):
        if name is None:
            return None
        return RelationshipType(name)
    def index(self, *args):
        return None

    def get_target(self, entity):
        neo, relationship = get_relationship(entity)
        return relationship
    
    def get_multiple(self, neo, node, type):
        pass
    def get_single(self, neo, node, type):
        pass
    def set_single(self, neo, node, type, relation):
        pass
    def del_single(self, neo, node, type):
        pass
    def set_create(self, neo, node, type, entity):
        pass

class Related(Relationship):
    def get_target(self, entity):
        neo, node = get_node(entity)
        return node
    
    def get_multiple(self, neo, node, type):
        pass
    def get_single(self, neo, node, type):
        pass
    def set_single(self, neo, node, type, relation):
        pass
    def del_single(self, neo, node, type):
        pass
    def set_create(self, neo, node, type, entity):
        pass

class Node(Descriptor):
    def __init__(self, model):
        super(Node, self).__init__(model)
    def index(self, *args):
        return None

    def get(self, owner, is_node, name):
        neo, relationship = get_relationship(owner)
        self.model(__neo__=neo, __node__=self.get_node(relationship))
    def set(self, owner, is_node, name, value):
        neo, relationship = get_relationship(owner)
        neo, node = get_node(value)
        if node is None:
            self.new_node(relationship, value)
        else:
            self.set_node(relationship, node)
    def delete(self, owner, is_node, name):
        raise AttributeError("Cannot delete edge node.")

class StartNode(Node):
    def get_node(self, relationship):
        return relationship.getStartNode()
    def set_node(self, relationship, node):
        pass
    def new_node(self, relationship, value):
        pass

class EndNode(Node):
    def get_node(self, relationship):
        return relationship.getEndNode()
    def set_node(self, relationship, node):
        pass
    def new_node(self, relationship, value):
        pass

class Property(Descriptor):
    def __init__(self, key=None, index=None):
        super(Property, self).__init__(None, key)
        self.__index = index
    def get_target(self, owner, is_node):
        if is_node:
            getter = get_node
        else:
            getter = get_relationship
        neo, target = getter(owner)
        return target
    def index(self, type, is_node, key):
        pass # TODO

    def get(self, owner, is_node, key):
        target = self.get_target(owner, is_node)
        return self.read_convert(target.getProperty(key, None))
    def set(self, owner, is_node, key, value):
        target = self.get_target(owner, is_node)
        target.setProperty(key, self.write_convert(value))
    def delete(self, owner, is_node, key):
        target = self.get_target(owner, is_node)
        target.removeProperty(key)

class Array(Property):
    def __init__(self, **kwargs):
        super(Array, self).__init__(**kwargs)

    def read_convert(self, value):
        return self.read_array(value)
    def write_convert(self, value):
        return self.write_array(value)


class String(Property):
    def __init__(self, **kwargs):
        super(String, self).__init__(**kwargs)

    def read_convert(self, value):
        return value
    def write_convert(self, value):
        return value
    def read_array(self, value):
        return value
    def write_array(self, value):
        return value

class StringArray(Array,String):
    def __init__(self, **kwargs):
        super(StringArray, self).__init__(**kwargs)

class Integer(Property):
    __accepted_sizes = set([1, # byte
                            2, # short
                            4, # int
                            8, # long
                            ])
    def __init__(self, bytes=4, **kwargs):
        super(Integer, self).__init__(**kwargs)
        assert bytes in self.__accepted_sizes
        self.bytes = bytes

    def read_convert(self, value):
        return value
    def write_convert(self, value):
        return value
    def read_array(self, value):
        return value
    def write_array(self, value):
        return value

class IntegerArray(Array,Integer):
    def __init__(self, **kwargs):
        super(IntegerArray, self).__init__(**kwargs)

class Float(Property):
    def __init__(self, double=True, **kwargs):
        super(Float, self).__init__(**kwargs)
        self.double = double

    def read_convert(self, value):
        return value
    def write_convert(self, value):
        return value
    def read_array(self, value):
        return value
    def write_array(self, value):
        return value

class FloatArray(Array,Float):
    def __init__(self, **kwargs):
        super(FloatArray, self).__init__(**kwargs)

class Boolean(Property):
    def __init__(self, **kwargs):
        super(Boolean, self).__init__(**kwargs)
    def read_convert(self, value):
        return value
    def write_convert(self, value):
        return value
    def read_array(self, value):
        return value
    def write_array(self, value):
        return value

class BooleanArray(Array,Boolean):
    def __init__(self, **kwargs):
        super(BooleanArray, self).__init__(**kwargs)
