# -*- coding: utf-8 -*-
"""Main entry point for running the tests.
Setup the path to find all modules and dispatch to the test package.
"""
def setup_env():
    import sys
    from os.path import dirname, abspath, join
    testdir = dirname(dirname(abspath(__file__)))
    libdir = join(dirname(dirname(testdir)), 'main', 'python')
    sys.path.append(libdir)
    sys.path.append(testdir)    

if __name__ == '__main__':
    setup_env()
    from neo4j_test import test
    test(*sys.argv)
