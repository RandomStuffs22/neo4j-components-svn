package org.neo4j.kernel.impl.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Use when you want synchronized modification but snapshots to read,
 * i.e. no synchronization on read which makes it faster for reads.
 * @author mattias
 *
 * @param <T> the type of items in the set
 */
public class SynchronizedWriteSet<T> implements Iterable<T>
{
    private Set<T> set = new HashSet<T>();
    
    public synchronized boolean add( T item )
    {
        Set<T> newSet = new HashSet<T>( set );
        boolean added = newSet.add( item );
        set = newSet;
        return added;
    }
    
    public synchronized boolean remove( Object item )
    {
        Set<T> newSet = new HashSet<T>( set );
        boolean removed = newSet.remove( item );
        set = newSet;
        return removed;
    }
    
    public Iterator<T> iterator()
    {
        return set.iterator();
    }
    
    public boolean isEmpty()
    {
        return set.isEmpty();
    }
}
