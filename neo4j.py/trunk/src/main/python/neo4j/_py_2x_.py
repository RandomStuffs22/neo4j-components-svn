# -*- coding: utf-8 -*-
import sys
if sys.version_info[0] != 2:
    raise ImportError("Unsupported Python version")

try:
    stringtypes = basestring
except:
    try:
        stringtypes = str, unicode
    except:
        raise ImportError("Unsupported Python version")
try:
    set = set # bind to local namespace
except NameError:
    from sets import Set as set # Python 2.3 fallback.

def is_string(string):
    return isinstance(string, stringtypes)

def create_class(cls, *bases, **args):
    if 'meta' in args:
        metaclass = args['meta']
        if metaclass is None:
            meta = type
        else:
            metabases = set([type(base) for base in bases])
            meta = type(metaclass.__name__,
                        tuple(metabases), dict(metaclass.__dict__))
    else:
        meta = type

    return meta(cls.__name__, bases, dict(cls.__dict__))

def add_metaclass(cls, meta):
    return create_class(cls, *cls.__bases__, **{'meta':meta})
