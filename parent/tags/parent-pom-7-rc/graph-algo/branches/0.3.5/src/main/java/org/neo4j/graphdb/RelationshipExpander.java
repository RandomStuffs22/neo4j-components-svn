package org.neo4j.graphdb;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.commons.iterator.FilteringIterable;

/**
 * A convenience for specifying multiple {@link RelationshipType} /
 * {@link Direction} pairs.
 */
public class RelationshipExpander
{
    public static RelationshipExpander ALL = new RelationshipExpander(
            new RelationshipType[0], new HashMap<String, Direction>() );
    
    private final RelationshipType[] types;
    private final Map<String, Direction> directions;
    
    /**
     * Creates a new {@link RelationshipExpander} which is set to expand
     * relationships with {@code type} and {@code direction}.
     *
     * @param type the {@link RelationshipType} to expand.
     * @param dir the {@link Direction} to expand.
     * @return a new {@link RelationshipExpander}.
     */
    public static RelationshipExpander forTypes( RelationshipType type,
            Direction dir )
    {
        return new RelationshipExpander().add( type, dir );
    }

    /**
     * Creates a new {@link RelationshipExpander} which is set to expand
     * relationships with two different types and directions.
     *
     * @param type1 a {@link RelationshipType} to expand.
     * @param dir1 a {@link Direction} to expand.
     * @param type2 another {@link RelationshipType} to expand.
     * @param dir2 another {@link Direction} to expand.
     * @return a new {@link RelationshipExpander}.
     */
    public static RelationshipExpander forTypes( RelationshipType type1,
            Direction dir1, RelationshipType type2, Direction dir2 )
    {
        return new RelationshipExpander().add( type1, dir1 ).add( type2, dir2 );
    }

    /**
     * Creates a new {@link RelationshipExpander} which is set to expand
     * relationships with multiple types and directions.
     *
     * @param type1 a {@link RelationshipType} to expand.
     * @param dir1 a {@link Direction} to expand.
     * @param type2 another {@link RelationshipType} to expand.
     * @param dir2 another {@link Direction} to expand.
     * @param more additional pairs or type/direction to expand.
     * @return a new {@link RelationshipExpander}.
     */
    public static RelationshipExpander forTypes( RelationshipType type1,
            Direction dir1, RelationshipType type2, Direction dir2,
            Object... more )
    {
        RelationshipType[] types = extract(
                RelationshipType[].class, type1, type2, more, false );
        Direction[] directions = extract( Direction[].class, dir1, dir2, more, true );
        RelationshipExpander expander = new RelationshipExpander();
        for ( int i = 0; i < types.length; i++ )
        {
            expander = expander.add( types[i], directions[i] );
        }
        return expander;
    }

    private static <T> T[] extract( Class<T[]> type, T obj1, T obj2,
            Object[] more, boolean odd )
    {
        if ( more.length % 2 != 0 )
        {
            throw new IllegalArgumentException();
        }
        Object[] target = (Object[]) Array.newInstance( type,
                ( more.length / 2 ) + 2 );
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
    
    protected RelationshipExpander( RelationshipType[] types,
            Map<String, Direction> directions)
    {
        this.types = types;
        this.directions = directions;
    }
    
    public RelationshipExpander()
    {
        this.types = new RelationshipType[0];
        this.directions = new HashMap<String, Direction>();
    }
    
    protected RelationshipType[] getTypes()
    {
        return this.types;
    }
    
    protected Direction getDirection( RelationshipType type )
    {
        return this.directions.get( type );
    }

    /**
     * Gets relationships from the {@code start} {@link Node}. The returned
     * {@link Relationship}s will match those criterias specified when
     * creating this expander.
     * 
     * @return an {@link Iterable} of {@link Relationship}s matching the
     * criterias of this expander.
     */
    public Iterable<Relationship> expand( Node start )
    {
        if ( types.length == 0 )
        {
            return start.getRelationships();
        }
        if ( types.length == 1 )
        {
            RelationshipType type = types[0];
            Direction direction = directions.get( type.name() );
            return start.getRelationships( type, direction );
        }
        return getRelationshipsForMultipleTypes( start, types, directions );
    }
    
    protected Iterable<Relationship> getRelationshipsForMultipleTypes(
            final Node start, RelationshipType[] types,
            final Map<String, Direction> directions )
    {
        return new FilteringIterable<Relationship>(
                start.getRelationships( types ) )
        {
            @Override
            protected boolean passes( Relationship item )
            {
                switch ( directions.get( item.getType().name() ) )
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
    
    @Override
    public int hashCode()
    {
        return Arrays.hashCode( types );
    }

    @Override
    public boolean equals( Object obj )
    {
        if (this == obj)
        {
            return true;
        }
        if ( obj instanceof RelationshipExpander )
        {
            RelationshipExpander that = (RelationshipExpander) obj;
            return Arrays.equals( this.types, that.types ) &&
                    this.directions.equals( that.directions );
        }
        return false;
    }

    /**
     * Adds a {@link RelationshipType} / {@link Direction} pair to the
     * criterias of {@link Relationship}s to be returned from
     * {@link #expand(Node)}. This instance will be left intact and a new
     * instance including the new addition will be returned.
     * 
     * @param type the {@link RelationshipType}.
     * @param direction the {@link Direction} for that type.
     * @return the resulting expander.
     */
    public RelationshipExpander add( RelationshipType type, Direction direction )
    {
        Direction existingDirection = directions.get( type.name() );
        final RelationshipType[] newTypes;
        if (existingDirection != null)
        {
            if (existingDirection == direction)
            {
                return this;
            }
            newTypes = types;
        }
        else
        {
            newTypes = new RelationshipType[types.length + 1];
            System.arraycopy( types, 0, newTypes, 0, types.length );
            newTypes[types.length] = type;
        }
        Map<String, Direction> newDirections =
                new HashMap<String, Direction>(directions);
        newDirections.put( type.name(), direction );
        return (RelationshipExpander) newExpander(newTypes, newDirections);
    }
    
    protected RelationshipExpander newExpander( RelationshipType[] types,
            Map<String, Direction> directions )
    {
        return new RelationshipExpander( types, directions );
    }

    /**
     * Returns an expander which has all its {@link Direction}s reversed,
     * also see {@link Direction#reverse()}
     */
    public RelationshipExpander reversed()
    {
        return newExpander( types, reverseDirections( directions ) );
    }

    private static Map<String, Direction> reverseDirections(
            Map<String, Direction> directions )
    {
        Map<String, Direction> result = new HashMap<String, Direction>();
        for ( Map.Entry<String, Direction> entry : directions.entrySet() )
        {
            result.put( entry.getKey(), entry.getValue().reverse() );
        }
        return result;
    }
}
