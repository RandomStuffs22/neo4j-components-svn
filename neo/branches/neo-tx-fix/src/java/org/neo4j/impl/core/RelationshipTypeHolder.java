/*
 * Copyright 2002-2007 Network Engine for Objects in Lund AB [neotechnology.com]
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.impl.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.transaction.TransactionManager;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.impl.persistence.IdGenerator;
import org.neo4j.impl.persistence.PersistenceManager;
import org.neo4j.impl.util.ArrayMap;

class RelationshipTypeHolder
{
	private static Logger log = 
		Logger.getLogger( RelationshipTypeHolder.class.getName() );
	
	private ArrayMap<String,Integer> relTypes = new ArrayMap<String,Integer>();
	private Map<Integer,String> relTranslation =
		new ConcurrentHashMap<Integer,String>();
	
	private final TransactionManager transactionManager;
	private final PersistenceManager persistenceManager;
	private final IdGenerator idGenerator;
	
	RelationshipTypeHolder( TransactionManager transactionManager, 
        PersistenceManager persistenceManager, IdGenerator idGenerator )
	{
		this.transactionManager = transactionManager;
        this.persistenceManager = persistenceManager;
		this.idGenerator = idGenerator;
	}
	
	void addRawRelationshipTypes( RawRelationshipTypeData[] types )
	{
		for ( int i = 0; i < types.length; i++ )
		{
			relTypes.put( types[i].getName(), types[i].getId() );
			relTranslation.put( types[i].getId(), types[i].getName() );
		}
	}

	public void addValidRelationshipTypes( 
		Class<? extends RelationshipType> relTypeClass )
	{
		for ( RelationshipType enumConstant : relTypeClass.getEnumConstants() )
		{
			String name = Enum.class.cast( enumConstant ).name();
			if ( relTypes.get( name ) == null )
			{
				int id = createRelationshipType( name );
				relTranslation.put( id, name );
			}
			else
			{
				relTranslation.put( relTypes.get( name ), name );
			}
		}
	}

	public RelationshipType addValidRelationshipType( String name, 
		boolean create ) 
	{
		if ( relTypes.get( name ) == null )
		{
			if ( !create )
			{
				return null;
			}
			int id = createRelationshipType( name );
			relTranslation.put( id, name );
		}
		else
		{
			relTranslation.put( relTypes.get( name ), name );
		}
		return new RelationshipTypeImpl( name );
	}
	
	boolean isValidRelationshipType( RelationshipType type )
	{
		return relTypes.get( type.name() ) != null;
	}
	
	RelationshipType getRelationshipTypeByName( String name )
	{
		if ( relTypes.get( name  ) != null )
		{
			return new RelationshipTypeImpl( name );
		}
		return null;
	}
	
	private static class RelationshipTypeImpl implements RelationshipType
	{
		private String name;
		
		RelationshipTypeImpl( String name )
		{
			assert name != null;
			this.name = name;
		}
		
		public String name()
		{
			return name;
		}
		
		public String toString()
		{
			return name;
		}
		
		public boolean equals( Object o )
		{
			if ( !(o instanceof RelationshipType) )
			{
				return false;
			}
			return name.equals( ((RelationshipType) o ).name() );
		}
		
		public int hashCode()
		{
			return name.hashCode();
		}
	}
	
	// temporary hack for b6 that will be changed to property index like 
	// implementation
	private class RelTypeCreater extends Thread
	{
		private boolean success = false;
		private String name;
		private int id = -1;
		
		RelTypeCreater( String name )
		{
			super();
			this.name = name;
		}
		
		synchronized boolean succeded()
		{
			return success;
		}
		
		synchronized int getRelTypeId()
		{
			return id;
		}
		
		public synchronized void run()
		{
			try
			{
				transactionManager.begin();
				id = idGenerator.nextId( RelationshipType.class );
                persistenceManager.createRelationshipType( id, name );
				transactionManager.commit();
				success = true;
			}
			catch ( Throwable t )
			{
				try
				{
					transactionManager.rollback();
				}
				catch ( Throwable tt )
				{
				}
			}
			finally
			{
				this.notify();
			}
		}
	}

	private synchronized int createRelationshipType( String name )
	{
		Integer id = relTypes.get( name );
		if ( id != null )
		{
			return id;
		}
		RelTypeCreater createrThread = new RelTypeCreater( name );
		synchronized ( createrThread )
		{
			createrThread.start();
			while( createrThread.isAlive() )
			{
				try
                {
	                createrThread.wait( 50 );
                }
                catch ( InterruptedException e )
                { // ok
                }
			}
		}
		if ( createrThread.succeded() )
		{
			addRelType( name, createrThread.getRelTypeId() );
			return createrThread.getRelTypeId();
		}
		throw new RuntimeException( "Unable to create relationship type " + 
			name );
	}

	void addRelType( String name, Integer id )
	{
		relTypes.put( name, id );
	}
	
	void removeRelType( String name )
	{
		relTypes.remove( name  );
	}

	void removeRelType( int id )
	{
		String name = relTranslation.remove( id );
		if ( name != null )
		{
			relTypes.remove( name  );
		}
	}
	
	int getIdFor( RelationshipType type )
	{
		return relTypes.get( type.name() );
	}
	
	RelationshipType getRelationshipType( int id )
	{
		String name = relTranslation.get( id );
		if ( name != null )
		{
			return new RelationshipTypeImpl( name );
		}
		return null;
	}

	public Iterable<RelationshipType> getRelationshipTypes()
    {
		List<RelationshipType> relTypeList = new ArrayList<RelationshipType>(); 
	    for ( String name : relTypes.keySet() )
	    {
	    	relTypeList.add( new RelationshipTypeImpl( name ) );
	    }
		return relTypeList;
    }

	public boolean hasRelationshipType( String name )
    {
		return relTypes.get( name ) != null;
    }
	
	void clear()
	{
		relTypes = new ArrayMap<String,Integer>();
		relTranslation = new HashMap<Integer,String>();
	}
}