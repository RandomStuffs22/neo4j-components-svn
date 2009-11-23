#!/usr/bin/env python

from collections import defaultdict
import types, inspect

def doc_for(name, element):
    doc = element.__doc__
    header = name
    if isinstance(element, types.MethodType):
        key = (1, 'Methods')
        header = "%s(%s)" % (name, parameters(element.im_func))
        if element.__name__ != name:
            doc = "Alias for <<<%s>>>." % element.__name__
    elif isinstance(element, property):
        key = (4, 'Data descriptors')
        if element.fset is None:
            header = header + " <(immutable)>"
    else:
        key = (100, 'Data')
    return key, (header, doc)

def parameters(method, start=1):
    result = []
    code = method.func_code
    args, var, kw = inspect.getargs(method.func_code)
    default = method.func_defaults or ()
    for i, arg in enumerate(args):
        if i >= len(args)-len(default):
            result.append("%s=%s" % (arg,default[i-len(default)-1]))
        elif i >= start:
            result.append(arg)
    if var is not None: result.append('*' + var)
    if kw is not None: result.append('**' + kw)
    return ', '.join(result)

def classdoc(parts, item):
    chapters = defaultdict(list)
    for name in dir(item):
        if not name.startswith('_'):
            chapter, doc = doc_for(name, getattr(item, name))
            chapters[chapter].append(doc)
    for key,chapter in sorted(chapters):
        parts.append(chapter)
        for header,doc in chapters[key,chapter]:
            parts.append("* %s\n\n %s" % (header, doc))

import sys, os.path
if __name__ == '__main__':
    sys.path.append(sys.argv[1])
    path = sys.argv[2]
    for item in sys.argv[3:]:
        site, package = item.split()
        if '.' in package:
            exec("from %s import %s as item" % tuple(package.split('.',1)))
        else:
            exec("import %s as item" % package)
        if isinstance(item, type):
            parts = ["class %s\n\n %s" % (item.__name__, item.__doc__)]
            classdoc(parts, item)
        elif callable(item):
            parts = ["%s(%s)\n\n %s" % (
                    item.__name__, parameters(item, start=0), item.__doc__)]
        elif '__init__' in item.__file__:
            parts = [item.__doc__]
        else:
            parts = [item.__doc__]
            for name in dir(item):
                element = getattr(item, name)
                if not name.startswith('_') and isinstance(element, type):
                    parts.append('class ' + name)
                    parts.append(" " + element.__doc__)
                    classdoc(parts, element)
        open(os.path.join(path, site+'.apt'),'w').write("\n\n".join(parts))
