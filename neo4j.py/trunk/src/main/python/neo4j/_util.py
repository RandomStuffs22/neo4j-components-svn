# -*- coding: utf-8 -*-
# Copyright (c) 2008-2009 "Neo Technology,"
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
This module defines utility functions.

The Neo4j.py utitities require Python >= 2.5


Copyright (c) 2009 "Neo Technology,"
    Network Engine for Objects in Lund AB [http://neotechnology.com]
"""
### TODO: Documentation! ###
from __future__ import with_statement
from functools import partial

class Transactional(object):
    def __init__(self, accessor, method):
        self.accessor = accessor
        self.method = method
    def __get__(self, obj, cls=None):
        method = self.method.__get__(obj, cls)
        neo = self.accessor.__get__(obj, cls)
        def result(*args, **kwargs):
            with neo.transaction:
                return method(*args, **kwargs)
        return result

def transactional(accessor):
    return partial(Transactional, accessor)
