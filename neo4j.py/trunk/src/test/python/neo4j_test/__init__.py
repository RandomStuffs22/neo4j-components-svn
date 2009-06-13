# -*- coding: utf-8 -*-
"""Main test package.
Setup the classpath, create neo instance and dispatch to each test module.
"""
import neo4j, os.path, traceback

class Log(object):
    def error(self, message, *args):
        print(message % args)

def test(exe, store, *classpath):
    import os.path
    if classpath:
        dirs = set()
        for file in classpath:
            dirs.add(os.path.dirname(file))
        neo = neo4j.NeoService(store,
                               classpath=classpath,
                               ext_dirs=list(dirs),
                               log = Log())
    else:
        neo = neo4j.NeoService(store, log = Log())
    try:
        for name in os.listdir(os.path.dirname(__file__)):
            if name.endswith('.py') and not name.startswith('_'):
                try:
                    exec('import neo4j_test.%s as test' % (name[:-3],))
                    test.run(neo)
                except:
                    print("FAIL: '%s' is not a proper test module." % (name,))
                    traceback.print_exc()
    finally:
        neo.shutdown()
