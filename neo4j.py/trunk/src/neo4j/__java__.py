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
Import mechanism for Java classes.

Neo4j.py works with the Jython platform or JPype on the CPython platform.

This part of Neo4j.py defines a compatibility layer to provide the same import
mechanism for Java classes with CPython/JPype as with Jython.


Copyright (C) 2008  Network Engine for Objects in Lund AB [neotechnology.com]
"""
import os
__all__ = ('classtype',)
if os.name == 'java':
    # We are running Jython, so java-import is native :)
    by = 'jython'
    from org.python.core import PyJavaClass as classtype
else:
    # We are running CPython, so use JPype to include java,
    # then use an import hook to support transparent java importing.
    import sys
    import jpype
    by = 'jpype'
    classtype = jpype._jclass._JavaClass
    from types import ModuleType as module
    if not jpype.isJVMStarted():
        try:
            jvmPath = jpype.getDefaultJVMPath()
        except:
            raise RuntimeError, "Environment variable 'JAVA_HOME' not defined"
        CLASSPATH = os.environ.get('CLASSPATH')
        if CLASSPATH:
            jpype.startJVM( jvmPath, '-Djava.class.path=%s' % CLASSPATH )
        else:
            jpype.startJVM( jvmPath )

    class JavaModule(module):
        def __init__(self, path, package):
            file, dot, name = path.rpartition('.')
            module.__init__(self, name)
            self.__package = package
            self.__path__ = path
            self.__file__ = "Java:" + file
            sys.modules[path] = self
        def __getattr__(self, attr):
            res = getattr(self.__package, attr)
            if isinstance(res, jpype._jclass._JavaClass):
                # It's a class, add a getter that handles internal classes
                res.__class__.__getattribute__ = makeGetAttr(
                    res.__class__.__getattribute__, attr, self)
            elif isinstance(res, jpype.JPackage):
                # It's a package, convert it to a module
                return JavaModule(self.__path__+'.'+attr, res)
            return res

    def makeGetAttr(get, name, module):
        def __getattribute__(self, attr):
            try:
                return get(self, attr)
            except AttributeError, ex:
                try:
                    res = getattr(module, name+'$'+attr)
                    setattr(self, attr, res)
                    return res
                except:
                    raise ex
        return __getattribute__

    class JavaLoader(object):
        _base_paths_ = ('java','javax', 'org','com','net')
        def __init__(self, base):
            self.module = JavaModule(base, jpype.JPackage(base))
        def load_module(self, fullname):
            if fullname in sys.modules:
                return sys.modules[fullname]
            else:
                res = self.module
                parts = fullname.split('.')[1:]
                for i, part in enumerate(parts):
                    res = getattr(res, part)
                return res

    class ImportHook(object):
        def __init__(self):
            self.__loaders = {}
        def find_module(self, fullname, path):
            base = fullname.split('.')[0]
            if base in self.__loaders:
                return self.__loaders[base]
            if path is None:
                if fullname in JavaLoader._base_paths_:
                    loader = self.__loaders[fullname] = JavaLoader(fullname)
                    return loader

    sys.meta_path.insert(0, ImportHook())
