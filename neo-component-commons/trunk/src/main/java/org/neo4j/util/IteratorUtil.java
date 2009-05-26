package org.neo4j.util;

import java.util.Iterator;

public class IteratorUtil
{
    public static <T> T singleValueOrNull( Iterator<T> iterator )
    {
        T value = iterator.hasNext() ? iterator.next() : null;
        if ( iterator.hasNext() )
        {
            throw new IllegalArgumentException( "More than one item in " +
                iterator + ". First is " + value );
        }
        return value;
    }
}
