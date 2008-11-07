package org.neo4j.util;

import java.util.Iterator;

public abstract class NestingIterable<T, U> implements Iterable<T>
{
	private Iterable<U> source;
	
	public NestingIterable( Iterable<U> source )
	{
		this.source = source;
	}
	
	public Iterator<T> iterator()
	{
		return new NestingIterator<T, U>( source.iterator() )
		{
			@Override
			protected Iterator<T> createNestedIterator( U item )
			{
				return NestingIterable.this.createNestedIterator( item );
			}
		};
	}
	
	protected abstract Iterator<T> createNestedIterator( U item );
}
