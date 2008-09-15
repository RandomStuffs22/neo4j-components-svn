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
Constants defined in Neo4j.


Copyright (c) 2008 "Neo Technology,"
    Network Engine for Objects in Lund AB [http://neotechnology.com]
"""

from __future__ import generators

from _backend import Direction, StopEvaluator, ReturnableEvaluator, Order

BOTH = Direction.BOTH
INCOMING = Direction.INCOMING
OUTGOING = Direction.OUTGOING

DEPTH_ONE = StopEvaluator.DEPTH_ONE
END_OF_NETWORK = StopEvaluator.END_OF_NETWORK

ALL = ReturnableEvaluator.ALL
ALL_BUT_START_NODE = ReturnableEvaluator.ALL_BUT_START_NODE

BREADTH_FIRST = Order.BREADTH_FIRST
DEPTH_FIRST = Order.DEPTH_FIRST

__all__ = ('DEPTH_ONE', 'END_OF_NETWORK',
           'ALL', 'ALL_BUT_START_NODE',
           'BREADTH_FIRST', 'DEPTH_FIRST',
           )
