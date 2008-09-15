# -*- coding: utf-8 -*-
# Copyright (c) 2008 "Neo Technology,"
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
"""
An implementation for using Neo4j.py as an RdfStore for RDFLib.

For information about RDFLib, see http://rdflib.net/


Copyright (c) 2008 "Neo Technology,"
    Network Engine for Objects in Lund AB [http://neotechnology.com]
"""

from rdflib.store import Store, VALID_STORE#, CORRUPTED_STORE, NO_STORE
from _rdf import * # TODO: clean up this import

# TODO: complete this
# I need help here by someone with a better understanding of RDF than me.
class NeoRdfStore(Store):
    ""
    context_aware = False
    formula_aware = False
    transaction_aware = True
    batch_unification = False
    def __init__(self, configuration=None, identifier=None):
        self.identifier = identifier
        self.__neo = None
        self.__tx = None
        super(NeoRdfStore, self).__init__(configuration)
    _super = property(lambda self: super(NeoRdfStore, self))

    # Database management methods

    def create(self, configuration):
        self._super.create(configuration)
    
    def open(self, configuration, create=False):
        self.__neo = Neo(self.identifier, create)
        return VALID_STORE
    
    def close(self, commit_pending_transaction=False):
        if commit_pending_transaction:
            self.commit()
        self.__neo.shutdown()
        self.__neo = None
    
    def destroy(self, configuration):
        pass
    
    def gc(self):
        pass
        
    # RDF API

    def add(self, (subject, predicate, object), context, quoted=False):
        self._super.add((subject, predicate, object), context, quoted)

    def remove(self, (subject, predicate, object), context=None):
        self._super.remove((subject, predicate, object), context)

    def triples(self, (subject, predicate, object), context=None):
        pass

    def __len__(self, context=None):
        pass

    def contexts(self, triple=None):
        pass

    def bind(self, prefix, namespace):
        pass

    def prefix(self, namespace):
        pass

    def namespace(self, prefix):
        pass

    def namespaces(self):
        pass

    # Transactional methods

    def commit(self):
        if self.__tx is not None:
            self.__tx.commit()
            self.__tx = None

    def rollback(self):
        if self.__tx is not None:
            self.__tx.rollback()
            self.__tx = None
