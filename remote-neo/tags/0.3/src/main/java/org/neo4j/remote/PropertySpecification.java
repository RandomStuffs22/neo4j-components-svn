/*
 * Copyright 2008-2009 Network Engine for Objects in Lund AB [neotechnology.com]
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.remote;

import java.io.Serializable;

class PropertySpecification implements Serializable
{
    private static final long serialVersionUID = 1L;
    final long id;
    final String key;
    final Object value;

    PropertySpecification( long id, String key, Object value )
    {
        this.id = id;
        this.key = key;
        this.value = value;
    }
}