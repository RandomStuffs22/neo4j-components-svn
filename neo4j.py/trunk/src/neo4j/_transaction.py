# -*- coding: utf-8 -*-
# Copyright 2008 Network Engine for Objects in Lund AB [neotechnology.com]
# 
# This program is free software: you can redistribute it and/or modify
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
Exposes the Neo4j Transaction.

All actions performed against Neo4j are required to be enclosed in a
transaction. With the use of pythons with-statement this is achived like this:
with Transaction():
    # perform actions against neo
Otherwise the same functionallity can be achived like this:
transaction = Transaction()
try:
    # perform actions against neo
    transaction.success()
except:
    transaction.failuere()
    raise
finally:
    transaction.finish()


Copyright (C) 2008  Network Engine for Objects in Lund AB [neotechnology.com]
"""

from _base import Base

class Transaction(Base):
    """Represents a transaction for Neo4j.py

    Typical usage:

    with neo.transaction:
        # do stuff with neo.

    Or when the with statement isn't available:

    tr = neo.transaction.begin()
    try:
        # do stuff with neo.
        tr.success()
    except:
        tr.failure()
        raise
    finally:
        tr.finish()

    The behaviur of both above versions are equivalent."""
    __transaction = None
    __neo = None
    def __init__(self, neo):
        self.__neo = neo
    
    def __enter__(self):
        """Called upon entering the with statement. Starts the transaction."""
        self.begin()
        return self

    def __exit__(self,exctype,value,traceback):
        """Called upon exiting the with statement. Finishes the transaction.
        If an exception is raised the transaction fails, and the exception
        is propagated. Otherwise the transaction succeds."""
        if exctype is None:
            self.success()
        else:
            self.failure()
        self.finish()

    def begin(self):
        """Starts the transaction."""
        if self.__transaction is None:
            self.__transaction = self.__neo.beginTx()
        return self
    
    def failure(self):
        """Marks the transaction as failed.
        Failure always has precedence over success."""
        if self.__transaction is not None:
            self.__transaction.failure()
        else:
            raise SystemError("Illegal transaction state for failure()")
    
    def success(self):
        """Marks the transaction as successfull. success() is ignored if
        failure() is called anytime before finish is called()."""
        if self.__transaction is not None:
            self.__transaction.success()
        else:
            raise SystemError("Illegal transaction state for success()")

    def finish(self):
        """Finishes the transaction. Either commits the changes (on success)
        or rolls back the transaction (on failure)."""
        if self.__transaction is not None:
            self.__transaction.finish()
            self.__transaction = None
        else:
            raise SystemError("Illegal transaction state for finish()")
