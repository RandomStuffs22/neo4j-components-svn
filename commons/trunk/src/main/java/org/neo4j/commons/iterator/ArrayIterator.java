package org.neo4j.commons.iterator;

public class ArrayIterator<T> extends PrefetchingIterator<T>
{
    private final T[] array;
    private int index;

    public ArrayIterator( T[] array )
    {
        this.array = array;
    }
    
    @Override
    protected T fetchNextOrNull()
    {
        int i = index++;
        return i < array.length ? array[i] : null;
    }
}
