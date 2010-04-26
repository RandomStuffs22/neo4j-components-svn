package org.neo4j.kernel.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Use when you want synchronized modification but snapshots to read,
 * i.e. no synchronization on read which makes it faster for reads.
 * @author mattias
 *
 * @param <T> the type of items in the set
 */
public class SynchronizedWriteSet<T> implements Iterable<T>
{
    private Collection<T> collection = newCollection();
    
    private static <R> Collection<R> newCollection()
    {
        return new ArrayList<R>();
    }
    
    public synchronized boolean add( T item )
    {
        Collection<T> newCollection = newCollection();
        newCollection.addAll( collection );
        boolean added = false;
        if ( !newCollection.contains( item ) )
        {
            added = newCollection.add( item );
        }
        collection = newCollection;
        return added;
    }
    
    public synchronized boolean remove( Object item )
    {
        Collection<T> newCollection = newCollection();
        newCollection.addAll( collection );
        boolean removed = newCollection.remove( item );
        collection = newCollection;
        return removed;
    }
    
    public Iterator<T> iterator()
    {
        return collection.iterator();
    }
    
    public boolean isEmpty()
    {
        return collection.isEmpty();
    }
}
