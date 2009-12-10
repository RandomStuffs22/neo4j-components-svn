# -*- coding: utf-8 -*-

# TODO: this is just a temporary implementation - make all modules use this
def __bootstrap__(bootstrap):
    global __bootstrap__
    def __bootstrap__(bootstrap):
        if pyneo.python.is_string(bootstrap):
            return lambda definition: definition(pyneo.newmodule(bootstrap))
        else:
            return bootstrap(pyneo)

    class pyneo(object):
        class NotInitializedError(RuntimeError): pass

        make = staticmethod(lambda factory: factory())

        pyneo = property(lambda self: pyneo)
        def newmodule(self, name):
            raise NotImplementedError("should pyneo really have modules?")

        def __call__(self, function):
            setattr(self, function.__name__, function)
            return function

        def __getattr__(self, attr):
            raise self.NotInitializedError("""Neo4j has not been initialized.
        "%s" cannot be accessed until the first NeoService is started.""" % (
                    attr,))

    pyneo = pyneo() # pyneo is a singleton

    import sys
    if sys.version_info >= (3,):
        from neo4j import _py_3x_ as python_implementation
    else:
        from neo4j import _py_2x_ as python_implementation
    pyneo.python = python_implementation

    #@pyneo
    #def bootstrap_neo(resource_uri, params):
    #    import neo4j._backend
    #    import neo4j._core

    return __bootstrap__(bootstrap)

@__bootstrap__
def temporary_implementation(pyneo):
    from neo4j._base import primitives, node, relationship
    pyneo.get_primitives = primitives
    pyneo.get_node = node
    pyneo.get_relationship = relationship

