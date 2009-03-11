# -*- coding: utf-8 -*-

from __future__ import with_statement

from datetime import datetime
import traceback

class SkipTest(Exception):
    def __init__(self, msg=None):
        super(SkipTest,self).__init__(msg)

def perform(neo, test, **opt):
    try:
        test(neo, opt)
    except SkipTest, message:
        print "SKIPPED:", test.__name__, "--", message
    except AssertionError, err:
        print "FAILURE:", test.__name__
        print "   ", err
    except:
        print "ERROR:  ", test.__name__
        traceback.print_exc()
    else:
        print "PASSED: ", test.__name__

def define_verify_test(name, define, verify):
    def test(neo, opt):
        time = datetime.now().isoformat()
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
