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
This module defines the basic behaviour for all Neo4j.py objects.


Copyright (c) 2008 "Neo Technology,"
    Network Engine for Objects in Lund AB [http://neotechnology.com]
"""

from __future__ import generators

class Base(object):
    """This class is the base class of all Neo4j.py objects."""
    def __setattr__(self,attr,value):
        """Prevent creation of new attributes"""
        if hasattr(self,attr) and attr.startswith('_'):
            super(Base,self).__setattr__(attr,value)
        else:
            raise AttributeError("Cannot add attributes to %s" % self)

    def __delattr__(self,attr):
        """Prevent deletion of attributes"""
        raise AttributeError("Cannot delete attributes from %s" % self)

