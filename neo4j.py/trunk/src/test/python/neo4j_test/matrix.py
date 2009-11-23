# -*- coding: utf-8 -*-

import neo4j

class Friends(neo4j.Traversal):
    types = [
        neo4j.Outgoing.knows,
        ]
    order = neo4j.BREADTH_FIRST
    stop = neo4j.STOP_AT_END_OF_GRAPH
    returnable = neo4j.RETURN_ALL_BUT_START_NODE

class Hackers(neo4j.Traversal):
    types = [
        neo4j.Outgoing.knows,
        neo4j.Outgoing.coded_by,
        ]
    order = neo4j.DEPTH_FIRST
    stop = neo4j.STOP_AT_END_OF_GRAPH
    def isReturnable(self, position):
        return not position.is_start and\
            position.last_relationship.type == 'coded_by'

def define(neo,verbose=False):
    thomas = neo.node(name="Thomas Andersson",
                      age =29)
    trinity = neo.node(name="Trinity")
    thomas.knows(trinity, age="3 days")
    trinity.loves(thomas)
    morpheus = neo.node(name="Morpheus",
                        rank="Captain",
                        occupation="Total badass")
    thomas.knows(morpheus)
    morpheus.knows(trinity, age="12 years")
    cypher = neo.node(name="Cypher")
    cypher['last name'] = "Reagan"
    morpheus.knows(cypher, disclosure="public")
    smith = neo.node(name="Agent Smith",
                     version="1.0b",
                     language="C++")
    cypher.knows(smith, disclosure="secret", age="6 months")
    architect = neo.node(name="The Architect")
    smith.coded_by(architect)
    return thomas
def verify(thomas,verbose=False):
    friends = {}
    for friend in Friends(thomas):
        friends[friend['name']] = friend.depth
        if verbose:
            print("Friend: At depth %s => %s" % (friend.depth, friend['name']))
    assert friends == {'Trinity':1,
                       'Morpheus':1,
                       'Cypher':2,
                       'Agent Smith':3,
                       }, "Found wrong friends: %s" % (friends,)
    hackers = {}
    for hacker in Hackers(thomas):
        hackers[hacker['name']] = hacker.depth
        if verbose:
            print("Hacker: At depth %s => %s" % (hacker.depth, hacker['name']))
    assert hackers == {'The Architect':4}, "Found wrong hackers: %s"%(hackers,)


if __name__ == '__main__':
    import sys
    from __init__ import setup_neo
    try:
        _neo = setup_neo(*sys.argv)
    except:
        print "USAGE: %s <path_to_neo4j_store_dir>" % (sys.argv[0],)
        sys.exit(-1)

    tx = _neo.transaction.begin()
    try:
        verify(define(_neo), verbose=True)
        tx.success()
    finally:
        tx.finish()

else:
    from neo4j_test._support import perform, define_verify_test

    def run(neo, **options):
        perform(neo, define_verify_test(__name__, define, verify), **options)

