# -*- coding: utf-8 -*-

try: # This should only be available on CPython - so check for that
    from platform import python_implementation
except:
    import sys
    if sys.platform.lower().startswith('java'):
        raise ImportError
    elif sys.platform.lower().startswith('cli'):
        raise ImportError
else:
    if python_implementation() != 'CPython':
        raise ImportError

raise ImportError("TODO: implement JCC-support")

from distutils.core import Extension
from distutils.command.build_ext import build_ext

class build_extensions(build_ext):
    def run(self):
        for extension in self.extensions:
            if isinstance(extension, JCCExtension):
                extension.jcc(self.build_temp, self.dry_run, self.verbose)
        build_ext.run(self)

class JCCExtension(Extension):
    def __init__(self, name,
                 jars=None,
                 packages=None,
                 classpath=None,
                 classes=None,
                 excludes=None,
                 mappings=None,
                 sequences=None,):
        Extension.__init__(self, name, [])
        self.jcc_jars = jars or []
        self.jcc_packages = packages or []
        self.jcc_classpath = classpath or []
        self.jcc_classes = classes or []
        self.jcc_excludes = excludes or []
        self.jcc_mappings = mappings or {}
        self.jcc_sequences = mappings or {}
    def load_jcc(self):
        try:
            from jcc.cpp import jcc
        except:
            jcc = None
        return jcc
    def jcc(self, dest, dry_run, verbose): # FIXME: the jcc code generation
        jcc = self.load_jcc()
        args = [None,
                #'--output',dest,
                ]
        for jar in self.jcc_jars:
            args.append('--jar'); args.append(jar)
        for classpath in self.jcc_classpath:
            args.append('--classpath'); args.append(classpath)
        for package in self.jcc_packages:
            args.append('--package'); args.append(package)
        for cls in self.jcc_classes:
            args.append(cls)
        for exclude in self.jcc_excludes:
            args.append('--exclude'); args.append(exclude)
        args.append('--python'); args.append(self.name)
        for cls,(method,key,value) in self.jcc_mappings.items():
            args.append('--mapping')
            args.append(cls)
            args.append('%s:(%s)%s' % (method,key,value))
        for cls,(length,get,value) in self.jcc_sequences.items():
            args.append('--sequence')
            args.appned(cls)
            args.append('%s()I' % length)
            args.append('%s(I)%s' % (get, value))
        if verbose:
            print("generating JCC extension '%s'" % self.name),
            for arg in args:
                if arg is None: continue
                if arg.startswith('--'):
                    print; print "   ",
                print arg,
            print
        if not dry_run:
            jcc(args)
        self.sources = [path.join('build', '_'+self.name, '__wrap__.cpp'),
                        path.join('build', '_'+self.name, '__init__.cpp'),
                        path.join('build', self.name + '.cpp'),
                        ]
