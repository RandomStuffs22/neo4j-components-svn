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
import java.util.List;
import java.util.logging.Logger;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.impl.cache.AdaptiveCacheManager;
import org.neo4j.impl.cache.LruCache;
import org.neo4j.impl.event.Event;
import org.neo4j.impl.event.EventData;
import org.neo4j.impl.event.EventManager;
import org.neo4j.impl.persistence.IdGenerator;
import org.neo4j.impl.persistence.PersistenceException;
import org.neo4j.impl.persistence.PersistenceManager;
import org.neo4j.impl.transaction.IllegalResourceException;
import org.neo4j.impl.transaction.LockManager;
import org.neo4j.impl.transaction.LockNotFoundException;
import org.neo4j.impl.transaction.LockType;
import org.neo4j.impl.transaction.NotInTransactionException;
import org.neo4j.impl.transaction.TransactionFactory;


public class NodeManager
{
	private static Logger log = Logger.getLogger( NodeManager.class.getName() );
	
	private static final NodeManager instance = new NodeManager();
	
	private static final LockManager lockManager = LockManager.getManager();
	private static final LockReleaser lockReleaser = LockReleaser.getManager();
	
	private int referenceNodeId = 0;
	
	private final LruCache<Integer,Node> nodeCache = 
		new LruCache<Integer,Node>( "NodeCache", 1500 );
	private final LruCache<Integer,Relationship> relCache = 
		new LruCache<Integer,Relationship>( "RelationshipCache", 3500 );
	
	private NodeManager()
	{
		AdaptiveCacheManager.getManager().registerCache(
			nodeCache, 0.77f, 0 );
		AdaptiveCacheManager.getManager().registerCache(
			relCache, 0.77f, 0 );
	}
	
	public static NodeManager getManager()
	{
		return instance;
	}
	
	public Node createNode() // throws CreateException
	{
		int id = IdGenerator.getGenerator().nextId( Node.class );
		NodeImpl node = new NodeImpl( id, true );
		acquireLock( node, LockType.WRITE );
		try
		{
			EventManager em = EventManager.getManager();
			EventData eventData = new EventData( new NodeOpData( node, id ) );
			if ( !em.generateProActiveEvent( Event.NODE_CREATE, eventData ) )
			{
				setRollbackOnly();
				throw new CreateException( "Unable to create node, " +
					"pro-active event failed." );
			}
			addNodeToCache( node );
			em.generateReActiveEvent( Event.NODE_CREATE, eventData );
			return new NodeProxy( id );
		}
		finally
		{
			releaseLock( node, LockType.WRITE );
		}
	}
	
	public Relationship createRelationship( Node startNode, Node endNode, 
		RelationshipType type )
	{
		if ( startNode == null || endNode == null || type == null )
		{
			throw new IllegalArgumentException( "Null parameter, startNode=" + 
				startNode + ", endNode=" + endNode + ", type=" + type );
		}
		if ( !RelationshipTypeHolder.getHolder().isValidRelationshipType( 
			type ) )
		{
			RelationshipTypeHolder.getHolder().addValidRelationshipType( 
				type.name(), true );
		}
		
		NodeImpl firstNode = getLightNode( (int) startNode.getId() );
		if ( firstNode == null )
		{
			setRollbackOnly();
			throw new RuntimeException( "First node[" + startNode.getId() + 
				"] deleted" );
		}
		NodeImpl secondNode = getLightNode( (int) endNode.getId() );
		if ( secondNode == null )
		{
			setRollbackOnly();
			throw new RuntimeException( "Second node[" + endNode.getId() + 
				"] deleted" );
		}
		int id = IdGenerator.getGenerator().nextId( Relationship.class );
		int startNodeId = (int) startNode.getId();
		int endNodeId = (int) endNode.getId();
		RelationshipImpl rel = new RelationshipImpl( id, startNodeId, 
			endNodeId, type, true );
		// RelationshipCommands relationshipCommand = null;
		boolean firstNodeTaken = false;
		boolean secondNodeTaken = false;
		acquireLock( rel, LockType.WRITE );
		try
		{
			acquireLock( firstNode, LockType.WRITE );
			firstNodeTaken = true;
			if ( firstNode.isDeleted() )
			{
				setRollbackOnly();
				throw new CreateException( "" + startNode + 
					" has been deleted in other transaction" );
			}
			acquireLock( secondNode, LockType.WRITE );
			secondNodeTaken = true;
			if ( secondNode.isDeleted() )
			{
				setRollbackOnly();
				throw new CreateException( "" + endNode + 
					" has been deleted in other transaction" );
			}
			int typeId = RelationshipTypeHolder.getHolder().getIdFor( type ); 
			EventManager em = EventManager.getManager();
			EventData eventData =
				new EventData( new RelationshipOpData( rel, id, typeId, 
					startNodeId, endNodeId ) );
			if ( !em.generateProActiveEvent( Event.RELATIONSHIP_CREATE, 
				eventData ) )
			{
				setRollbackOnly();
				throw new CreateException( "Unable to create relationship, " +
					"pro-active event failed." );
			}

			firstNode.addRelationship( type, id );
			secondNode.addRelationship( type, id );
			addRelationshipToCache( rel );
			
			em.generateReActiveEvent( Event.RELATIONSHIP_CREATE, eventData );
			return new RelationshipProxy( id );
		}
		finally
		{
			boolean releaseFailed = false;
			if ( firstNodeTaken )
			{
				try
				{
					releaseLock( firstNode, LockType.WRITE ); //, level );
				}
				catch ( Exception e )
				{
					releaseFailed = true;
					e.printStackTrace();
					log.severe( "Failed to release lock" );
				}
			}
			if ( secondNodeTaken )
			{
				try
				{
					releaseLock( secondNode, LockType.WRITE ); // , level );
				}
				catch ( Exception e )
				{
					releaseFailed = true;
					e.printStackTrace();
					log.severe( "Failed to release lock" );
				}
			}
			releaseLock( rel, LockType.WRITE ); // , level );
			if ( releaseFailed )
			{
				throw new RuntimeException( "Unable to release locks [" + 
					startNode + "," + endNode + "] in relationship create->" + 
					rel );
			}
		}
	}
	
	/**
	 * Gets a node by id.
	 * <p>
	 * First checks the cache, if the node is in cache it is returned.
	 * If the node isn't in cache it is loaded from persistent storage.
	 * 
	 * @param id the node id
	 * @return the node with node id <CODE>id</CODE>
	 * @throws NotFoundException
	 */
	public Node getNodeById( int nodeId ) throws NotFoundException
	{
		if ( nodeId < 0 )
		{
			throw new IllegalArgumentException( "Negative node id " + nodeId );
		}
		Node node = nodeCache.get( nodeId );
		if ( node != null )
		{
			return new NodeProxy( nodeId );
		}
		node = new NodeImpl( nodeId );
		acquireLock( node, LockType.READ );
		try
		{
			if ( nodeCache.get( nodeId ) != null )
			{
				node = nodeCache.get( nodeId );
				return new NodeProxy( nodeId );
			}
			if ( PersistenceManager.getManager().loadLightNode( nodeId ) == 
				null )
			{
				throw new NotFoundException( "Node[" + nodeId + "]" );
			}
			
			// If we get here, loadLightNode didn't throw exception and
			// a node with the given id does exist... good.
			nodeCache.add( nodeId, node );
			return new NodeProxy( nodeId );
		}
		catch ( PersistenceException pe )
		{
			log.severe( "Persistence error while trying to get node #" +
					   nodeId + " by id. " + pe );
			throw new NotFoundException( pe );
		}
		finally
		{
			forceReleaseReadLock( node );
		}
	}
	
	NodeImpl getLightNode( int nodeId )
	{
		Node node = nodeCache.get( nodeId );
		if ( node != null )
		{
			return (NodeImpl) node;
		}
		node = new NodeImpl( nodeId );
		acquireLock( node, LockType.READ );
		try
		{
			if ( nodeCache.get( nodeId ) != null )
			{
				node = nodeCache.get( nodeId );
				return (NodeImpl) node;
			}
			if ( PersistenceManager.getManager().loadLightNode( nodeId ) == 
				null )
			{
				return null;
			}
			nodeCache.add( nodeId, node );
			return (NodeImpl) node;
		}
		catch ( PersistenceException pe )
		{
			log.severe( "Persistence error while trying to get node #" +
					   nodeId + " by id. " + pe );
			throw new RuntimeException( "Node deleted?", pe );
		}
		finally
		{
			forceReleaseReadLock( node );
		}
	}
	
	NodeImpl getNodeForProxy( int nodeId )
	{
		Node node = nodeCache.get( nodeId );
		if ( node != null )
		{
			return (NodeImpl) node;
		}
		node = new NodeImpl( nodeId );
		acquireLock( node, LockType.READ );
		try
		{
			if ( nodeCache.get( nodeId ) != null )
			{
				node = nodeCache.get( nodeId );
				return (NodeImpl) node;
			}
			if ( PersistenceManager.getManager().loadLightNode( nodeId ) == 
				null )
			{
				throw new NotFoundException( "Node[" + nodeId + "] not found" );
			}
			nodeCache.add( nodeId, node );
			return (NodeImpl) node;
		}
		catch ( PersistenceException pe )
		{
			log.severe( "Persistence error while trying to get node #" +
					   nodeId + " by id. " + pe );
			throw new RuntimeException( "Node deleted?", pe );
		}
		finally
		{
			forceReleaseReadLock( node );
		}
	}
	
	/**
	 * Returns the reference node in this node space. If the node isn't found 
	 * (reference node hasn't been configured properly) a 
	 * {@link NotFoundException} is thrown. See {@link NeoModule} for more 
	 * information
	 *
	 * @return the reference node
	 * @throws NotFoundException
	 */
	public Node getReferenceNode() throws NotFoundException
	{
		if ( referenceNodeId == -1 )
		{
			throw new NotFoundException( "No reference node set" );
		}
		return getNodeById( referenceNodeId );
	}
	
	void setReferenceNodeId( int nodeId )
	{
		this.referenceNodeId = nodeId;
	}
	
	// checks the cache first, if not in cache load it using PM
	Relationship getRelationshipById( int relId ) 
		throws NotFoundException
	{
		Relationship relationship = relCache.get( relId );
		if ( relationship != null )
		{
			return new RelationshipProxy( relId );
		}
		relationship = new RelationshipImpl( relId );
		acquireLock( relationship, LockType.READ );
		try
		{
			if ( relCache.get( relId ) != null )
			{
				relationship = relCache.get( relId );
				return new RelationshipProxy( relId );
			}

			RawRelationshipData data = 
				PersistenceManager.getManager().loadLightRelationship( relId );
			if ( data == null )
			{
				throw new NotFoundException( "Relationship[" + relId + 
					"] not found" );
			}
			RelationshipType type = getRelationshipTypeById( data.getType() );
			if ( type == null )
			{
				throw new NotFoundException( "Relationship[" + data.getId() +
					"] exist but relationship type[" + data.getType() +
					"] not registered." );
			}
			relationship = new RelationshipImpl( relId, data.getFirstNode(), 
				data.getSecondNode(), type, false );
			relCache.add( relId, relationship );
			return new RelationshipProxy( relId );
		}
		catch ( PersistenceException e )
		{
			throw new NotFoundException( "Could not get relationship[" + 
				relId + "].", e );
		}
		finally
		{
			// forceReleaseWriteLock( dummyRel );
			forceReleaseReadLock( relationship );
		}
	}
	
	RelationshipType getRelationshipTypeById( int id )
	{
		return RelationshipTypeHolder.getHolder().getRelationshipType( id );
	}
	
	Relationship getRelForProxy( int relId )
	{
		Relationship relationship = relCache.get( relId );
		if ( relationship != null )
		{
			return relationship;
		}
		relationship = new RelationshipImpl( relId );
		acquireLock( relationship, LockType.READ );
		try
		{
			if ( relCache.get( relId ) != null )
			{
				relationship = relCache.get( relId );
				return relationship;
			}
			RawRelationshipData data = 
				PersistenceManager.getManager().loadLightRelationship( relId );
			if ( data == null )
			{
				throw new RuntimeException( "Relationship[" + relId + 
					"] deleted?" );
			}
			RelationshipType type = getRelationshipTypeById( data.getType() );
			if ( type == null )
			{
				throw new NotFoundException( "Relationship[" + data.getId() +
					"] exist but relationship type[" + data.getType() +
					"] not registered." );
			}
			relationship = new RelationshipImpl( relId, data.getFirstNode(), 
				data.getSecondNode(), type, false );
			relCache.add( relId, relationship );
			return relationship;
		}
		catch ( PersistenceException e )
		{
			throw new RuntimeException( "Relationship deleted?", e );
		}
		finally
		{
			forceReleaseReadLock( relationship );
		}
	}
	
	public void removeNodeFromCache( int nodeId ) // throws DeleteException
	{
		nodeCache.remove( nodeId );
	}
	
	void addNodeToCache( NodeImpl node )//  throws CreateException
	{
		nodeCache.add( (int) node.getId(), node );
	}

	public void removeRelationshipFromCache( int id )
	{
		relCache.remove( id );
	}

	void addRelationshipToCache( Relationship rel )
	{
		relCache.add( (int) rel.getId(), rel );
	}
	
	Object loadPropertyValue( int id )
	{
		try
		{
			return PersistenceManager.getManager().loadPropertyValue( id );
		}
		catch ( PersistenceException e )
		{
			e.printStackTrace();
			log.severe( "Failed loading property value[" +
				id + "]" );
		}
		return null;
	}
	
	List<Relationship> loadRelationships( NodeImpl node )
	{
		try
		{
			RawRelationshipData rawRels[] = 
				PersistenceManager.getManager().loadRelationships( node );
			List<Relationship> relList = new ArrayList<Relationship>();
			for ( RawRelationshipData rawRel : rawRels )
			{
				int relId = rawRel.getId();
				Relationship rel = relCache.get( relId );
				if ( rel == null )
				{
					RelationshipType type = getRelationshipTypeById( rawRel.getType() ); 
					if ( type == null )
					{
						// relationship with type that hasn't been registered
						continue;
					}
					rel = new RelationshipImpl( relId, 
						rawRel.getFirstNode(), rawRel.getSecondNode(), 
						type, false );
					relCache.add( relId, rel );
				}
				relList.add( rel );
			}
			return relList;
		}
		catch ( Exception e )
		{
			log.severe( "Failed loading relationships for node[" +
				node.getId() + "]" );
			throw new RuntimeException( e );
		}
	}
	
	RawPropertyData[] loadProperties( NodeImpl node )
	{
		try
		{
			RawPropertyData properties[]  = 
				PersistenceManager.getManager().loadProperties( node );
			return properties;
		}
		catch ( Exception e )
		{
			log.severe( "Failed loading properties for node[" +
				node.getId() + "]" );
			throw new RuntimeException( e );
		}
	}
	
	RawPropertyData[] loadProperties( RelationshipImpl relationship )
	{
		try
		{
			RawPropertyData properties[]  = 
				PersistenceManager.getManager().loadProperties( relationship );
			return properties;
		}
		catch ( Exception e )
		{
			log.severe( "Failed loading properties for relationship[" +
				relationship.getId() + "]" );
			throw new RuntimeException( e );
		}
	}
	
	int getNodeMaxCacheSize()
	{
		return nodeCache.maxSize();
	}
	
	int getRelationshipMaxCacheSize()
	{
		return relCache.maxSize();
	}
	
	public void clearCache()
	{
		nodeCache.clear();
		relCache.clear();
	}
	
	private void setRollbackOnly()
	{
		try
		{
			TransactionFactory.getTransactionManager().setRollbackOnly();
		}
		catch ( javax.transaction.SystemException se )
		{
			se.printStackTrace();
			log.severe( "Failed to set transaction rollback only" );
		}
	}

	private void acquireLock( Object resource, LockType lockType )
	{
		try
		{
			if ( lockType == LockType.READ )
			{
				lockManager.getReadLock( resource );
			}
			else if ( lockType == LockType.WRITE )
			{
				lockManager.getWriteLock( resource );
			}
			else
			{
				throw new RuntimeException( "Unkown lock type: " + lockType );
			}
		}
		catch ( IllegalResourceException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	private void releaseLock( Object resource, LockType lockType )
	{
		try
		{
			if ( lockType == LockType.READ )
			{
				lockManager.releaseReadLock( resource );
			}
			else if ( lockType == LockType.WRITE )
			{
				lockReleaser.addLockToTransaction( resource, lockType );
			}
			else
			{
				throw new RuntimeException( "Unkown lock type: " + 
					lockType );
			}
		}
		catch ( NotInTransactionException e )
		{
			throw new RuntimeException( 
				"Unable to get transaction isolation level.", e );
		}
		catch ( LockNotFoundException e )
		{
			throw new RuntimeException( 
				"Unable to release locks.", e );
		}
		catch ( IllegalResourceException e )
		{
			throw new RuntimeException( 
				"Unable to release locks.", e );
		}
	}
	
	// only used during creation/loading of nodes/rels
	private void forceReleaseReadLock( Object resource )
	{
		try
		{
			LockManager.getManager().releaseReadLock( resource );
		}
		catch ( LockNotFoundException e )
		{
			throw new RuntimeException( 
				"Unable to release lock.", e );
		}
		catch ( IllegalResourceException e )
		{
			throw new RuntimeException( 
				"Unable to release lock.", e );
		}
	}
	
	public boolean isValidRelationship( Relationship rel )
	{
		try
		{
			return !( ( RelationshipImpl ) getRelForProxy( (int) rel.getId() ) 
				).isDeleted();
		}
		catch ( RuntimeException e )
		{
			return false;
		}
	}
	
	public boolean isValidRelationshipType( RelationshipType type )
	{
		RelationshipTypeHolder rth = RelationshipTypeHolder.getHolder();
		return rth.isValidRelationshipType( type );
	}

	public void addEnumRelationshipTypes( 
		Class<? extends RelationshipType> relationshipTypes )
    {
		RelationshipTypeHolder rth = RelationshipTypeHolder.getHolder();
		rth.addValidRelationshipTypes( relationshipTypes );
    }

	public RelationshipType registerRelationshipType( String name, 
		boolean create )
    {
		RelationshipTypeHolder rth = RelationshipTypeHolder.getHolder();
		return rth.addValidRelationshipType( name, create );
    }
	
	public int getHighestPossibleIdInUse( Class<?> clazz )
	{
		return IdGenerator.getGenerator().getHighestPossibleIdInUse( clazz );
	}
	
	public int getNumberOfIdsInUse( Class<?> clazz )
	{
		return IdGenerator.getGenerator().getNumberOfIdsInUse( clazz );
	}

	public void removeRelationshipTypeFromCache( int id )
    {
		RelationshipTypeHolder rth = RelationshipTypeHolder.getHolder();
		rth.removeRelType( id );
    }
}