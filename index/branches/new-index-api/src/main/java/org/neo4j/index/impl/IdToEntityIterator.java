package org.neo4j.index.impl;

import java.util.Iterator;

import org.neo4j.commons.iterator.PrefetchingIterator;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;

public abstract class IdToEntityIterator<T extends PropertyContainer>
        extends PrefetchingIterator<T>
{
    private final Iterator<Long> ids;

    public IdToEntityIterator( Iterator<Long> ids )
    {
        this.ids = ids;
    }
    
    @Override
    protected T fetchNextOrNull()
    {
        T result = null;
        while ( result == null )
        {
            if ( !ids.hasNext() )
            {
                return null;
            }
            
            long id = ids.next();
            try
            {
                return getEntity( id );
            }
            catch ( NotFoundException e )
            {
                // Rare exception which can occur under normal
                // circumstances
            }
        }
        return result;
    }

    protected abstract T getEntity( long id );
}
