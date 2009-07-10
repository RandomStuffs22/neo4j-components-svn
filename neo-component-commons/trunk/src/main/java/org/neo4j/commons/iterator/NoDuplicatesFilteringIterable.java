package org.neo4j.commons.iterator;

import java.util.HashSet;
import java.util.Set;

public class NoDuplicatesFilteringIterable<T> extends FilteringIterable<T>
{
    private final Set<T> items = new HashSet<T>();
    
    public NoDuplicatesFilteringIterable( Iterable<T> source )
    {
        super( source );
    }

    @Override
    protected boolean passes( T item )
    {
        return items.add( item );
    }
}
