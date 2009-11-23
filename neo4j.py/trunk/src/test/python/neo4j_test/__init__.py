# -*- coding: utf-8 -*-
"""Main test package.
Setup the classpath, create neo instance and dispatch to each test module.
"""
import neo4j, os.path, traceback

class Log(object):
    def error(self, message, *args, **kwargs):
        print(message % args)
        if kwargs.get('exc_info', False):
            traceback.print_exc()
    warn = info = debug = error

def setup_neo(exe, store, *classpath):
    import os.path
    if classpath:
        dirs = set()
        for file in classpath:
            dirs.add(os.path.dirname(file))
        return neo4j.NeoService(store,
                                classpath=classpath,
                                ext_dirs=list(dirs),
                                log = Log())
    else:
        return neo4j.NeoService(store, log = Log())

def test(exe, store, *classpath):
    tests = []
    for name in os.listdir(os.path.dirname(__file__)):
        if name.endswith('.py') and not name.startswith('_'):
            try:
                exec('import neo4j_test.%s as test' % (name[:-3],))
                tests.append(test)
            except:
                print("FAIL: error importing '%s'!" % (name,))
                traceback.print_exc()
    neo = setup_neo(exe, store, *classpath)
    try:
        for test in tests:
            try:
                test.run(neo)
            except:
                print("FAIL: '%s' is not a proper test module." % (
                        test.__name__,))
                traceback.print_exc()
    finally:
        neo.shutdown()
