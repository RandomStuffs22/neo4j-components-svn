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
This module selects the appropriate backend depending on platform.

Copyright (c) 2008-2009 "Neo Technology,"
    Network Engine for Objects in Lund AB [http://neotechnology.com]
"""

import traceback

def initialize(classpath, **params):
    import sys
    global implementation, load_neo
    log = params.get('log', None)
    try: # Native implementation
        if 'java' in sys.platform.lower():
            from neo4j._backend import java as implementation
            embedded, remote = implementation.initialize(classpath, params)
        elif 'cli' in sys.platform.lower():
            from neo4j._backend import cli as implementation
            embedded, remote = implementation.initialize(classpath, params)
        else:
            try: # JCC
                from neo4j._backend import jcc as implementation
                embedded, remote = implementation.initialize(classpath, params)
            except: # Fall back to JPype
                from neo4j._backend import reflection as implementation
                embedded, remote = implementation.initialize(classpath, params)
    except:
        if log: log.error(traceback.format_exc())
        try: # Falling back to pure python implementation
            from neo4j._backend import pure as implementation
            embedded, remote = implementation.initialize(classpath, params)
        except: # FAIL.
            raise ImportError("No applicable backend found.")
    # Define load function
    def load_neo(resource_uri):
        if '://' not in resource_uri and embedded is not None:
            impl = embedded
        elif remote is not None:
            impl = remote
        elif resource_uri.startswith('file://') and embedded is not None:
            resource_uri = resource_uri[7:]
            impl = embedded
        else:
            raise RuntimeError("Cannot connect to Neo instance at '%s'." %
                               (resource_uri,))
        return impl(resource_uri)
