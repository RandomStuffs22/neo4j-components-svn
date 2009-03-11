# -*- coding: utf-8 -*-
from __future__ import with_statement

from distutils.core import setup, Extension, Command
from distutils.command.build_ext import build_ext
import re, os, os.path as path, sys, urllib2

from xml.etree.ElementTree import ElementTree

class Pom(object):
    def __init__(self, path):
        self.tree = ElementTree()
        self.tree.parse(path)
        self.pattern = re.compile(r"\${(?P<var>[^}]*)}")
        self.developers = None
    def find(self, what):
        if what == 'project':
            return self.tree
    def match(self, match):
        root = self
        for part in match.groupdict()['var'].split('.'):
            root = root.find(part)
        return root.text
    def __getitem__(self, element):
        return self.pattern.sub(self.match, self.tree.find(element).text)
    def developer(self, developer_role, element):
        for developer in self.tree.findall("developers/developer"):
            for role in developer.findall("roles/role"):
                if role.text == developer_role:
                    return developer.find(element).text.strip()
    def dependencies(self):
        for dependency in self.tree.findall("dependencies/dependency"):
            scope = dependency.find('scope')
            if scope is not None and scope.text == 'test': continue
            yield (dependency.find('groupId').text,
                   dependency.find('artifactId').text,
                   dependency.find('version').text,
                   scope is None or scope.text == 'runtime',)

REPOSITORIES = ["http://m2.neo4j.org/",
                "http://mirrors.ibiblio.org/pub/mirrors/maven2/",
                ]

def download(source, target):
    if path.exists(target): return # Don't download existing files
    source = urllib2.urlopen(source)
    try:
        with open(target,'w') as target:
            target.write(source.read())
    finally:
        source.close()

def download_dependency(target_dir, group, artifact, version):
    if not path.exists(target_dir):
        os.mkdir(target_dir)
    url = '/'.join(group.split('.')
                   + [artifact, version, "%s-%s.%%s" % (artifact,version)])
    jar = path.join(target_dir, "%s.jar" % artifact)
    pom = path.join(target_dir, "%s.pom" % artifact)
    for repo in REPOSITORIES:
        try:
            download(repo + url%'jar', jar)
            download(repo + url%'pom', pom)
        except:
            continue
        else:
            break
    else:
        raise ValueError("Could not find artifact %s/%s-%s" % (
                group,artifact,version))
    return jar, pom

def download_dependencies(target_dir, pom):
    for group,artifact,version,include in pom.dependencies():
        jar, pom_file = download_dependency(target_dir,group,artifact,version)
        yield jar, include
        for jar, include in download_dependencies(target_dir, Pom(pom_file)):
            yield jar, include

class build_extensions(build_ext):
    def run(self):
        for extension in self.extensions:
            if isinstance(extension, JCCExtension):
                extension.jcc(self.build_temp, self.dry_run, self.verbose)
        build_ext.run(self)

class test(Command): # FIXME: this is ugly
    description = "Execute unit tests."
    user_options = []
    def run(self):
        import imp
        file, filename, stuff = imp.find_module('__main__', [
                path.join(path.dirname(__file__),'src','test','python','neo4j_test')])
        sys.argv = [file, 'var'] + self.dependencies
        imp.load_module('__main__', file, filename, stuff)

    def initialize_options(self):
        pass
    def finalize_options(self):
        pass

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
        if True or not dry_run:
            jcc(args)
        self.sources = [path.join('build', '_'+self.name, '__wrap__.cpp'),
                        path.join('build', '_'+self.name, '__init__.cpp'),
                        path.join('build', self.name + '.cpp'),
                        ]

def main():
    DEPEND = 'dependencies' # directory to download dependancies to
    pom = Pom(path.join(path.dirname(path.abspath(__file__)), 'pom.xml'))
    # Load dependancies
    dependencies = []
    jars = []
    for dependency,include in download_dependencies(DEPEND, pom):
        dependencies.append(dependency)
        if include: # Add the jar (file only) to the inclusions list
            jars.append(path.split(dependency)[1])
    test.dependencies = [path.join(DEPEND,jar) for jar in jars] # FIXME: ugly
    # Configure setup
    args = dict(cmdclass=dict(build_ext=build_extensions, test=test),
                # The source structure is not defined in the pom, define it here
                packages=['neo4j','neo4j.classes',
                          'neo4j._backend','neo4j.model',],
                package_dir={'neo4j':'src/main/python/neo4j',
                             'neo4j.classes':DEPEND},
                # Include the runtime dependencies
                package_data={'neo4j.classes': jars},
                # Get author from the developers in the pom
                author=pom.developer('Author', 'name'),
                author_email=pom.developer('Author', 'email'),
                # Get maintainer from the developers in the pom
                maintainer=pom.developer('Maintainer', 'name'),
                maintainer_email=pom.developer('Maintainer', 'email'),
                # Get the extension modules for this Python implementation
                ext_modules = extension_modules(dependencies),
                )
    # Get extra attributes from the pom
    for attr in 'name,version,description,url'.split(','):
        args[attr] = pom[attr]
    # Read descriptions from files
    with open('README.txt') as readme:
        args['long_description'] = readme.read()
    with open('COPYRIGHT.txt') as copyright:
        args['license'] = copyright.read()
    # Run the setup system
    setup(**args)

def extension_modules(dependencies):
    extensions = []
    if sys.platform == 'java': # Jython
        pass
    elif sys.platform == 'cli': # IronPython
        pass
    else: # CPython (or possibly PyPy - but ignore that)
        return [] # FIXME: the JCC build process needs to be updated
        extensions.append(
            JCCExtension('neo4j._backend._jcc',
                         classpath=dependencies,
                         classes=["org.neo4j.api.core.EmbeddedNeo",],
                         packages=["org.neo4j.api.core",
                                   "java.lang", "java.util"]
                         )
            )
    return extensions

if __name__ == '__main__':
    main()
