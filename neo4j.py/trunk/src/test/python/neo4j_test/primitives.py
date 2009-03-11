# -*- coding: utf-8 -*-

from neo4j_test._support import perform, define_verify_test

def define(neo):
    return neo.node()
def verify(node):
    pass

def run(neo):
    perform(neo, define_verify_test(__name__, define, verify))
