# -*- coding: utf-8 -*-

# Copyright (c) 2008-2010 "Neo Technology,"
#     Network Engine for Objects in Lund AB [http://neotechnology.com]
# 
# This file is part of Neo4j.py.
# 
# Neo4j.py is free software: you can redistribute it and/or modify
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
        return neo4j.GraphDatabase(store,
                                   classpath=classpath,
                                   ext_dirs=list(dirs),
                                   log = Log())
    else:
        return neo4j.GraphDatabase(store, log = Log())

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
