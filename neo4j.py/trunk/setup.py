# -*- coding: utf-8 -*-

try: # to use setuptools
    from setuptools import setup, Command
    from pom import Pom
except: # fallback to distutils
    try:
        from distutils.core import setup, Command
        from pom import Pom
        def setuptools_arguments(args, **more):
            dependencies(None)
    except: # if both are unavailable: fail with requirements message
        raise EnvironmentError(
            """Neo4j.py installation requirements unfulfilled.

 * For Jython, the source installation requires Jython version 2.5.
 * For Jython 2.2 you can still install pre-packaged binary distributions.
   These can be obtained at http://components.neo4j.org/neo4j.py/
 * For Python, the source installation requires Python 2.5.
""")
else: # define strategy for extra arguments that setuptools support
    def setuptools_arguments(args, **more):
        args.update(more)
        dependencies(args)
import sys, os, warnings, os.path as path

try: # JCC for integrating Neo4j with CPython
    from jccext import build_extensions, JCCExtension
except:
    from distutils.command.build_ext import build_ext as build_extensions
    def extension_modules(packages, dependencies):
        return []
else:
    def extension_modules(packages, dependencies):
        #packages.append('_neo4jcc')
        return [
            JCCExtension('_neo4jcc',
                         classpath=dependencies,
                         classes=["org.neo4j.api.core.EmbeddedNeo",
                                  "org.neo4j.remote.RemoteNeo",
                                  "org.neo4j.util.index.LuceneIndexService",
                                  "org.neo4j.util.index.NeoIndexService",
                                  "org.neo4j.remote.RemoteIndexService",
                                  "org.neo4j.util.timeline.Timeline",],
                         packages=["org.neo4j.api.core",
                                   "java.lang", "java.util",
                                   "org.neo4j.remote","org.neo4j.remote.sites",
                                   ],
                         )
            ]

def dependencies(parameters):
    if parameters is None:
        warnings.warn("""Setuptools is not available.
You will need to make sure that you either have JCC installed prior to
installing Neo4j.py, or that you have JPype installed alongside with
Neo4j.py. With setuptools these could have been automatically installed.
""")
        return
    if sys.version_info >= (3,0):
        warnings.warn("""Limited support for Python 3.
At the time of writing thre are no Java integration labraries available for the
CPython implementation of the Python 3 series.
""")
    try:
        import platform
        impl = platform.python_implementation()
    except:
        return
    if impl == 'CPython':
        parameters['setup_requires'] = ['jcc'] # TODO: implement JCC-support
        #parameters['install_requires'] = ['jpype']
#        warnings.warn("""Neo4j.py requires JPype.
#Neo4j.py depends on JPype (found here: http://jpype.sourceforge.net) for
#Java integration. JPype is not installable through easy-install, which means
#that you will have to install it manually.
#""")

class test(Command):
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

def main(dependencies_path='dependencies'):
    pom = Pom(path.join(path.dirname(path.abspath(__file__)), 'pom.xml'),
              repositories=("http://m2.neo4j.org/",
                            "http://repo1.maven.org/maven2/",
                            "http://repo2.maven.org/maven2/",
                            "http://mirrors.ibiblio.org/pub/mirrors/maven2/",
                            ))
    # Load dependancies
    dependencies = []
    jars = []
    for dependency,include in pom.download_dependencies(dependencies_path):
        dependencies.append(dependency)
        if True or include: #XXX: Add the jar (file only) to the inclusions list
            jars.append(path.split(dependency)[1])
    test.dependencies = test_dependencies = []
    for jar in jars:
        test_dependencies.append(path.join(dependencies_path,jar))
    # Configure setup
    packages = ['neo4j', 'neo4j.model',
                'neo4j._backend', 'neo4j._hooks',
                'neo4j.classes', ]
    args = dict(cmdclass=dict(build_ext=build_extensions, test=test),
                # The source structure is not defined in the pom, define it here
                packages=packages,
                package_dir={'neo4j': 'src/main/python/neo4j',
                             'neo4j.classes': dependencies_path},
                # Include the runtime dependencies
                package_data={'neo4j.classes': jars},
                # Get author from the developers in the pom
                author=pom.developer('Author', 'name'),
                author_email=pom.developer('Author', 'email'),
                # Get maintainer from the developers in the pom
                maintainer=pom.developer('Maintainer', 'name'),
                maintainer_email=pom.developer('Maintainer', 'email'),
                # Get the extension modules for this Python implementation
                ext_modules = extension_modules(packages, dependencies),
                )
    setuptools_arguments(args, zip_safe=False)
    # Get extra attributes from the pom
    for attr in 'name,version,description,url'.split(','):
        args[attr] = pom[attr]
    # Read descriptions from files
    readme = open('README.txt')
    args['long_description'] = readme.read()
    readme.close()
    copyright = open('COPYRIGHT.txt')
    args['license'] = copyright.read()
    copyright.close()
    # Run the setup system
    setup(**args)

if __name__ == '__main__':
    main()
