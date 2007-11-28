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

import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.impl.cache.AdaptiveCacheManager;
import org.neo4j.impl.persistence.PersistenceManager;
import org.neo4j.impl.transaction.TransactionFactory;

import java.util.logging.Logger;

/**
 * The Neo module handles valid relationship types and manages the 
 * reference node. Also the Node and Relationship caches sizes can be
 * configured here. 
 * <p>
 * The reference node is a reference point in the node space. See it as a 
 * starting point that can be used to traverse to other nodes in the node 
 * space. Using different reference nodes one can manage many applications 
 * in the same node space.
 */
public class NeoModule
{
	private static Logger log = Logger.getLogger( NeoModule.class.getName() );

	private boolean startIsOk = true;
	private Class<? extends RelationshipType> relTypeClass;
	
	private static final int INDEX_COUNT = 2500;
	
	public void init()
	{
	}
	
	public void start()
	{
		if ( !startIsOk )
		{
			return;
		}
		// load and verify from PS
		RawRelationshipTypeData relTypes[] = null;
		RawPropertyIndex propertyIndexes[] = null;
		try
		{
			NeoConstraintsListener.getListener().registerEventListeners();
			TransactionFactory.getUserTransaction().begin();
			relTypes = 
				PersistenceManager.getManager().loadAllRelationshipTypes();
			propertyIndexes = 
				PersistenceManager.getManager().loadPropertyIndexes( 
					INDEX_COUNT );
			TransactionFactory.getUserTransaction().commit();
		}
		catch ( Exception e )
		{
			try
			{
				TransactionFactory.getUserTransaction().rollback();
			}
			catch ( Exception ee )
			{
				ee.printStackTrace();
				log.severe( "Unable to rollback tx" );
			}
			throw new RuntimeException( "Unable to load all relationships", 
				e );
		}
		RelationshipTypeHolder rth = RelationshipTypeHolder.getHolder();
		rth.addRawRelationshipTypes( relTypes );
		if ( relTypeClass != null )
		{
			rth.addValidRelationshipTypes( relTypeClass );
		}
		PropertyIndex.addPropertyIndexes( propertyIndexes );
		if ( propertyIndexes.length < INDEX_COUNT )
		{
			PropertyIndex.setHasAll( true );
		}
		AdaptiveCacheManager.getManager().start();
		startIsOk = false;
	}
	
	public void setRelationshipTypes( Class<? extends RelationshipType> clazz )
	{
		this.relTypeClass = clazz;
	}

	public int getNodeCacheSize()
	{
		return NodeManager.getManager().getNodeMaxCacheSize();
	}
	
	public int getRelationshipCacheSize()
	{
		return NodeManager.getManager().getRelationshipMaxCacheSize();
	}
	
	public void setReferenceNodeId( Integer nodeId )
	{
		NodeManager.getManager().setReferenceNodeId( nodeId.intValue() );
		try
		{
			NodeManager.getManager().getReferenceNode();
		}
		catch ( NotFoundException e )
		{
			log.warning( "Reference node[" + nodeId + "] not valid." );
		}
	}
	
	public Integer getCurrentReferenceNodeId()
	{
		try
		{
			return (int) NodeManager.getManager().getReferenceNode().getId();
		}
		catch ( NotFoundException e )
		{
			return -1;
		}
	}
	
	public void createNewReferenceNode()
	{
		try
		{
			Node node = NodeManager.getManager().createNode();
			NodeManager.getManager().setReferenceNodeId( (int) node.getId() );
			log.info( "Created a new reference node. " + 
				"Current reference node is now " + node );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			log.severe( "Unable to create new reference node." );
		}
	}

	public void reload()
	{
		stop();
		start();
	}
	
	public void stop()
	{
		RelationshipTypeHolder rth = RelationshipTypeHolder.getHolder();
		rth.clear();
		PropertyIndex.clear();
		NodeManager.getManager().clearCache();
		NeoConstraintsListener.getListener().unregisterEventListeners();
		AdaptiveCacheManager.getManager().stop();
	}
	
	public void destroy()
	{
	}

	public RelationshipType getRelationshipTypeByName( String name )
    {
		RelationshipTypeHolder rth = RelationshipTypeHolder.getHolder();
		return rth.getRelationshipTypeByName( name );
    }

	public void addEnumRelationshipTypes( 
		Class<? extends RelationshipType> relationshipTypes )
    {
		RelationshipTypeHolder rth = RelationshipTypeHolder.getHolder();
		rth.addValidRelationshipTypes( relationshipTypes );
    }

	public Iterable<RelationshipType> getRelationshipTypes()
    {
		RelationshipTypeHolder rth = RelationshipTypeHolder.getHolder();
		return rth.getRelationshipTypes();
    }

	public boolean hasRelationshipType( String name )
    {
		RelationshipTypeHolder rth = RelationshipTypeHolder.getHolder();
		return rth.hasRelationshipType( name );
    }

	public RelationshipType registerRelationshipType( String name, 
		boolean create )
    {
		RelationshipTypeHolder rth = RelationshipTypeHolder.getHolder();
		return rth.addValidRelationshipType( name, create );
    }
}
