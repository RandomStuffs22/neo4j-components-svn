package org.neo4j.commons.iterator;

public class RangeIterator extends PrefetchingIterator<Integer>
{
    private int current;
    private final int end;
    
    public RangeIterator( int start, int length )
    {
        this.current = start;
        this.end = start + length;
    }
    
    @Override
    protected Integer fetchNextOrNull()
    {
        int result = current++;
        return result < end ? result : null;
    }
}
