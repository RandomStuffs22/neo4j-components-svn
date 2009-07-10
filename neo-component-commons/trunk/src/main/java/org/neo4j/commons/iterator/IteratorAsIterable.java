package org.neo4j.commons.iterator;

import java.util.Iterator;

public class IteratorAsIterable<T> implements Iterable<T>
{
    private final Iterator<T> iterator;
    
    public IteratorAsIterable( Iterator<T> iterator )
    {
        this.iterator = iterator;
    }
    
    public Iterator<T> iterator()
    {
        return this.iterator;
    }
}
