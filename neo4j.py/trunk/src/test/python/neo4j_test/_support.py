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

from __future__ import with_statement

from datetime import datetime
import traceback

class SkipTest(Exception):
    def __init__(self, msg=None):
        super(SkipTest,self).__init__(msg)

def timestamp(dt=None):
    if dt is None: dt = datetime.now()
    return "%s-%s-%s_%s-%s-%s" % (
        dt.year, dt.month, dt.day, dt.hour, dt.minute, dt.second)

def perform(neo, test, **opt):
    try:
        test(neo, opt)
    except SkipTest, message:
        print("SKIPPED: %s -- %s" % (test.__name__, message))
    except AssertionError, err:
        print("FAILURE: %s" % (test.__name__,))
        print("    %s" % (err,))
    except:
        print("ERROR:   %s" % (test.__name__,))
        traceback.print_exc()
    else:
        print("PASSED:  %s" % (test.__name__,))

def simple_test(function, *args, **options):
    if options.get('transactional', False):
        def test(neo, opt):
            with neo.transaction:
                function(*args)
    else:
        def test(neo, opt):
            function(*args)
    test.__name__ = options.get('name', function.__name__)
    return test

def define_verify_test(name, define, verify):
    def test(neo, opt):
        time = timestamp()
        with neo.transaction:
            getattr(neo.reference_node, name)(define(neo,**opt),
                                              test=name, time=time)
        with neo.transaction:
            ref = neo.reference_node
            for test in getattr(ref, name):
                if test['time'] == time:
                    verify(test.getOtherNode(ref),**opt)
                    break
            else:
                assert False, "Test node could not be found"
    test.__name__ = name
    return test
