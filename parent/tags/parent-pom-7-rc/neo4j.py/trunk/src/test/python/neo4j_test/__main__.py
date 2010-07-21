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
"""Main entry point for running the tests.
Setup the path to find all modules and dispatch to the test package.
"""
if __name__ == '__main__':
    import sys
    from os.path import dirname, abspath, join
    testdir = dirname(dirname(abspath(__file__)))
    libdir = join(dirname(dirname(testdir)), 'main', 'python')
    sys.path.append(libdir)
    sys.path.append(testdir)
    from neo4j_test import test
    test(*sys.argv)
