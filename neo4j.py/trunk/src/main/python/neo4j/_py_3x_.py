# -*- coding: utf-8 -*-
import sys
if sys.version_info[0] != 3:
    raise ImportError("Unsupported Python version")

def is_string(string):
    return isinstance(string, str)

def create_class(cls, *bases, meta=None):
    pass

def add_metaclass(cls, meta):
    pass
