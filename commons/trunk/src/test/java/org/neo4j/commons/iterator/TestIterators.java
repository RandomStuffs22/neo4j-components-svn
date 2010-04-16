package org.neo4j.commons.iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Iterator;

import org.junit.Test;

public class TestIterators
{
    @Test
    public void testArrayIterator()
    {
        Integer[] array = new Integer[] { 1, 3, 10, 4 };
        Iterator<Integer> iterator = new ArrayIterator<Integer>( array );
        assertEquals( (Integer) 1, iterator.next() );
        assertEquals( (Integer) 3, iterator.next() );
        assertEquals( (Integer) 10, iterator.next() );
        assertEquals( (Integer) 4, iterator.next() );
        assertFalse( iterator.hasNext() );
    }

    @Test
    public void testRangeIteratorFromZero()
    {
        Iterator<Integer> iterator = new RangeIterator( 3 );
        assertEquals( (Integer) 0, iterator.next() );
        assertEquals( (Integer) 1, iterator.next() );
        assertEquals( (Integer) 2, iterator.next() );
        assertFalse( iterator.hasNext() );
    }

    @Test
    public void testRangeIteratorWithStartEnd()
    {
        Iterator<Integer> iterator = new RangeIterator( 3, 6 );
        assertEquals( (Integer) 3, iterator.next() );
        assertEquals( (Integer) 4, iterator.next() );
        assertEquals( (Integer) 5, iterator.next() );
        assertFalse( iterator.hasNext() );
    }

    @Test
    public void testRangeIteratorWithStartEndAndStride()
    {
        Iterator<Integer> iterator = new RangeIterator( 3, 9, 2 );
        assertEquals( (Integer) 3, iterator.next() );
        assertEquals( (Integer) 5, iterator.next() );
        assertEquals( (Integer) 7, iterator.next() );
        assertFalse( iterator.hasNext() );

        iterator = new RangeIterator( 3, 10, 2 );
        assertEquals( (Integer) 3, iterator.next() );
        assertEquals( (Integer) 5, iterator.next() );
        assertEquals( (Integer) 7, iterator.next() );
        assertEquals( (Integer) 9, iterator.next() );
        assertFalse( iterator.hasNext() );
    }
}
