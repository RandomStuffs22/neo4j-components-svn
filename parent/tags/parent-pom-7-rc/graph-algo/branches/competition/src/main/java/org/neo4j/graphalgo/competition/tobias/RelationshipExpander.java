/*
 * Copyright 2010 Network Engine for Objects in Lund AB [neotechnology.com]
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
package org.neo4j.graphalgo.competition.tobias;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.commons.iterator.FilteringIterable;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

public class RelationshipExpander
{
    private final RelationshipType[] types;
    private final Map<RelationshipType, Direction> directions;

    private RelationshipExpander( RelationshipType[] types, Direction[] dirs )
    {
        if ( types.length != dirs.length )
        {
            throw new IllegalArgumentException();
        }
        this.types = new RelationshipType[types.length];
        this.directions = new HashMap<RelationshipType, Direction>();
        for ( int i = 0; i < types.length; i++ )
        {
            this.types[i] = types[i];
            this.directions.put( types[i], dirs[i] );
        }
    }

    public Iterable<Relationship> expand( final Node start )
    {
        if ( types.length == 0 )
        {
            return start.getRelationships();
        }
        if ( types.length == 1 )
        {
            RelationshipType type = types[0];
            return start.getRelationships( type, directions.get( type ) );
        }
        return new FilteringIterable<Relationship>(
                start.getRelationships( types ) )
        {
            @Override
            protected boolean passes( Relationship item )
            {
                switch ( directions.get( item.getType() ) )
                {
                case INCOMING:
                    return item.getEndNode().equals( start );
                case OUTGOING:
                    return item.getStartNode().equals( start );
                default:
                    return true;
                }
            }
        };
    }

    private static final RelationshipExpander ALL = new RelationshipExpander(
            new RelationshipType[0], new Direction[0] );

    public static RelationshipExpander all()
    {
        return ALL;
    }

    public static RelationshipExpander forTypes( RelationshipType type,
            Direction dir )
    {
        return new RelationshipExpander( new RelationshipType[] { type },
                new Direction[] { dir } );
    }

    public static RelationshipExpander forTypes( RelationshipType type1,
            Direction dir1, RelationshipType type2, Direction dir2 )
    {
        return new RelationshipExpander(
                new RelationshipType[] { type1, type2 }, new Direction[] {
                        dir1, dir2 } );
    }

    public static RelationshipExpander forTypes( RelationshipType type1,
            Direction dir1, RelationshipType type2, Direction dir2,
            Object... more )
    {
        return new RelationshipExpander( extract( RelationshipType[].class,
                type1, type2, more, false ), extract( Direction[].class, dir1,
                dir2, more, true ) );
    }

    private static Object[] EMPTY_ARRAY = new Object[0];

    private static <T> T[] extract( Class<T[]> type, T obj1, T obj2,
            Object[] more, boolean odd )
    {
        if ( more.length % 2 != 0 )
        {
            throw new IllegalArgumentException();
        }
        Object[] target = Arrays.copyOf( EMPTY_ARRAY, ( more.length / 2 ) + 2,
                type );
        try
        {
            target[0] = obj1;
            target[1] = obj2;
            for ( int i = 2; i < target.length; i++ )
            {
                target[i] = more[( i - 2 ) * 2 + ( odd ? 1 : 0 )];
            }
        }
        catch ( ArrayStoreException cast )
        {
            throw new IllegalArgumentException( cast );
        }
        return type.cast( target );
    }

}
