package org.neo4j.util;

import java.util.Collection;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

/**
 * Abstract super class for implementations of Neo4j collections and sets.
 * @author mattias
 *
 * @param <T> The type of objects in this collection.
 */
public abstract class AbstractSet<T> implements Collection<T>
{
	private GraphDatabaseService graphDB;
	
	public AbstractSet( GraphDatabaseService graphDb )
	{
		this.graphDB = graphDb;
	}
	
	protected GraphDatabaseService graphDb()
	{
		return this.graphDB;
	}
	
	public boolean addAll( Collection<? extends T> items )
	{
		Transaction tx = graphDb().beginTx();
		try
		{
			boolean changed = false;
			for ( T item : items )
			{
				if ( add( item ) )
				{
					changed = true;
				}
			}
			tx.success();
			return changed;
		}
		finally
		{
			tx.finish();
		}
	}

	public boolean containsAll( Collection<?> items )
	{
		Transaction tx = graphDb().beginTx();
		try
		{
			boolean ok = true;
			for ( Object item : items )
			{
				if ( !contains( item ) )
				{
					ok = false;
					break;
				}
			}
			tx.success();
			return ok;
		}
		finally
		{
			tx.finish();
		}
	}

	public boolean removeAll( Collection<?> items )
	{
		Transaction tx = graphDb().beginTx();
		try
		{
			boolean changed = false;
			for ( Object item : items )
			{
				if ( remove( item ) )
				{
					changed = true;
				}
			}
			tx.success();
			return changed;
		}
		finally
		{
			tx.finish();
		}
	}
}
