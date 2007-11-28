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
package unit.neo.api;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.impl.core.IllegalValueException;
import org.neo4j.impl.core.NodeManager;
import org.neo4j.impl.core.NotFoundException;
import org.neo4j.impl.transaction.TransactionFactory;
import unit.neo.MyRelTypes;

public class TestNeoConstrains extends TestCase
{
	private Level level1 = null;
	private Level level2 = null;
	
	private String key = "testproperty";
	
	public TestNeoConstrains(String testName)
	{
		super( testName );
	}
	
	public static void main(java.lang.String[] args)
	{
		junit.textui.TestRunner.run( suite() );
	}
	
	public static Test suite()
	{
		TestSuite suite = new TestSuite( TestNeoConstrains.class );
		return suite;
	}
	
	public void setUp()
	{
		// turn off logging since the code may print nasty stacktrace
		Logger log = Logger.getLogger( 
			"org.neo4j.impl.persistence.BusinessLayerMonitor" );
		level1 = log.getLevel();
		log.setLevel( Level.OFF );
		log = Logger.getLogger(  
			"org.neo4j.impl.core.NeoConstraintsListener" );
		level2 = log.getLevel();
		log.setLevel( Level.OFF );
		UserTransaction ut = TransactionFactory.getUserTransaction();
		try
		{
			if ( ut.getStatus() != Status.STATUS_NO_TRANSACTION )
			{
				fail ( "Status is not STATUS_NO_TRANSACTION but: " + 
					ut.getStatus() );
			}
			ut.begin();
		}
		catch ( Exception e )
		{
			fail( "Failed to start transaction, " + e );
		}
	}

	public void tearDown()
	{
		Logger log = Logger.getLogger( 
			"org.neo4j.impl.persistence.BusinessLayerMonitor" );
		log.setLevel( level1 );
		log = Logger.getLogger(  
			"org.neo4j.impl.core.NeoConstraints" );
		log.setLevel( level2 );
		UserTransaction ut = TransactionFactory.getUserTransaction();
		try
		{
			if ( ut.getStatus() == Status.STATUS_ACTIVE )
			{
				ut.commit();
			}
			else if ( ut.getStatus() == Status.STATUS_MARKED_ROLLBACK )
			{
				ut.rollback();
			}
			else if ( ut.getStatus() == Status.STATUS_NO_TRANSACTION )
			{
				// do nothing
			}
			else
			{
				System.out.println( "ARGH." );
				fail( "Unkown transaction status[" + ut.getStatus() + "]." );
			}
		}
		catch ( Exception e )
		{
			fail( "Failed to end transaciton, " + e );
		}
	}
	
	public void testDeleteNodeWithRel1()
	{
		try
		{
			NodeManager nm = NodeManager.getManager();
			Node node1 = nm.createNode();
			Node node2 = nm.createNode();
			NodeManager.getManager().createRelationship( node1, node2, 
				MyRelTypes.TEST );
			node1.delete();
			try
			{
				TransactionFactory.getUserTransaction().commit();
				fail( "Should not validate" );
			}
			catch (  Exception e )
			{
				// good
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			fail( "" + e );
		}
	}

	public void testDeleteNodeWithRel2()
	{
		try
		{
			NodeManager nm = NodeManager.getManager();
			Node node1 = nm.createNode();
			Node node2 = nm.createNode();
			NodeManager.getManager().createRelationship( node1, node2, 
				MyRelTypes.TEST );
			node2.delete();
			node1.delete();
			try
			{
				TransactionFactory.getUserTransaction().commit();
				fail( "Should not validate" );
			}
			catch (  Exception e )
			{
				// good
			}
		}
		catch ( Exception e )
		{
			fail( "" + e );
		}
	}

	public void testDeleteNodeWithRel3()
	{
		try
		{
			// make sure we can delete in wrong order
			NodeManager nm = NodeManager.getManager();
			Node node0 = nm.createNode();
			Node node1 = nm.createNode();
			Node node2 = nm.createNode();
			Relationship rel0 = 
				NodeManager.getManager().createRelationship( node0, node1, 
					MyRelTypes.TEST );
			Relationship rel1 = 
				NodeManager.getManager().createRelationship( node0, node2, 
					MyRelTypes.TEST );
			node1.delete();
			rel0.delete();
			TransactionFactory.getUserTransaction().commit();
			TransactionFactory.getUserTransaction().begin();
			node2.delete();
			rel1.delete();
			node0.delete();
		}
		catch ( Exception e )
		{
			fail( "" + e );
		}
	}

	public void testCreateRelOnDeletedNode()
	{
		try
		{
			NodeManager nm = NodeManager.getManager();
			Node node1 = nm.createNode();
			Node node2 = nm.createNode();
			TransactionFactory.getUserTransaction().commit();
			TransactionFactory.getUserTransaction().begin();
			node1.delete();
			try
			{
				NodeManager.getManager().createRelationship( node1, node2, 
					MyRelTypes.TEST );
			}
			catch ( Exception e )
			{ // ok
			}
			try
			{
				TransactionFactory.getUserTransaction().commit();
				fail( "Transaction should be marked rollback" );
			}
			catch ( Exception e )
			{ // good
			}
			TransactionFactory.getUserTransaction().begin();
			node2.delete();
			node1.delete();
		}
		catch ( Exception e )
		{
			// e.printStackTrace();
			fail( "" + e );
		}
	}
	
	private enum IllegalRelType implements RelationshipType
	{
		TEST_ILLEGAL;
	}
	
	public void testCreateRelWithIllegalRelationshipType()
	{
		try
		{
			NodeManager nm = NodeManager.getManager();
			Node node1 = nm.createNode();
			Node node2 = nm.createNode();
			try
			{
				NodeManager.getManager().createRelationship( node1, node2, 
					IllegalRelType.TEST_ILLEGAL );
			}
			catch ( IllegalArgumentException e )
			{ // good
			}
			try
			{
				TransactionFactory.getUserTransaction().commit();
				// fail( "Should not validate" ); will validate now
			}
			catch (  Exception e )
			{
				// good
			}
		}
		catch ( Exception e )
		{
			fail( "" + e );
		}
	}		
	
	public void testAddPropertyDeletedNode()
	{
		NodeManager nm = NodeManager.getManager();
		Node node = nm.createNode();
		node.delete();
		try
		{
			node.setProperty( key, new Integer( 1 ) );
			fail( "Add property on deleted node should not validate" );
		}
		catch ( Exception e )
		{
			// good
		}
	}

	public void testRemovePropertyDeletedNode()
	{
		try
		{
			NodeManager nm = NodeManager.getManager();
			Node node = nm.createNode();
			node.setProperty( key, new Integer( 1 ) );
			node.delete();
			try
			{
				node.removeProperty( key );
				TransactionFactory.getUserTransaction().commit();
				fail( "Remove property on deleted node should not validate" );
			}
			catch ( Exception e )
			{
				// ok
			}
		}
		catch ( Exception e )
		{
			fail( "" + e );
		}
	}

	public void testChangePropertyDeletedNode()
	{
		try
		{
			NodeManager nm = NodeManager.getManager();
			Node node = nm.createNode();
			node.setProperty( key, new Integer( 1 ) );
			node.delete();
			try
			{
				node.setProperty( key, new Integer( 2 ) );
				TransactionFactory.getUserTransaction().commit();
				fail( "Change property on deleted node should not validate" );
			}
			catch ( Exception e )
			{
				// ok
			}
		}
		catch ( Exception e )
		{
			fail( "" + e );
		}
	}

	public void testAddPropertyDeletedRelationship()
	{
		try
		{
			NodeManager nm = NodeManager.getManager();
			Node node1 = nm.createNode();
			Node node2 = nm.createNode();
			Relationship rel = nm.createRelationship( node1, node2, 
				MyRelTypes.TEST );
			rel.delete();
			try
			{
				rel.setProperty( key, new Integer( 1 ) );
				TransactionFactory.getUserTransaction().commit();
				fail( "Add property on deleted rel should not validate" );
			}
			catch ( Exception e )
			{ // good
			}
			node1.delete();
			node2.delete();
		}
		catch ( Exception e )
		{
			fail( "" + e );
		}
	}

	public void testRemovePropertyDeletedRelationship()
	{
		try
		{
			NodeManager nm = NodeManager.getManager();
			Node node1 = nm.createNode();
			Node node2 = nm.createNode();
			Relationship rel = nm.createRelationship( node1, node2, 
				MyRelTypes.TEST );
			rel.setProperty( key, new Integer( 1 ) );
			rel.delete();
			try
			{
				rel.removeProperty( key );
				TransactionFactory.getUserTransaction().commit();
				fail( "Remove property on deleted rel should not validate" );
			}
			catch ( Exception e )
			{
				// ok
			}
			node1.delete();
			node2.delete();
		}
		catch ( Exception e )
		{
			fail( "" + e );
		}
	}

	public void testChangePropertyDeletedRelationship()
	{
		try
		{
			NodeManager nm = NodeManager.getManager();
			Node node1 = nm.createNode();
			Node node2 = nm.createNode();
			Relationship rel = nm.createRelationship( node1, node2, 
				MyRelTypes.TEST );
			rel.setProperty( key, new Integer( 1 ) );
			rel.delete();
			try
			{
				rel.setProperty( key, new Integer( 2 ) );
				TransactionFactory.getUserTransaction().commit();
				fail( "Change property on deleted rel should not validate" );
			}
			catch ( Exception e )
			{
				// ok
			}
			node1.delete();
			node2.delete();
		}
		catch ( Exception e )
		{
			fail( "" + e );
		}
	}
	
	public void testMultipleDeleteNode()
	{
		try
		{
			NodeManager nm = NodeManager.getManager();
			Node node1 = nm.createNode();
			node1.delete();
			try
			{
				node1.delete();
				TransactionFactory.getUserTransaction().commit();
				fail( "Should not validate" );
			}
			catch ( Exception e )
			{
				// ok
			}
		}
		catch ( Exception e )
		{
			fail( "" + e );
		}
	}

	public void testMultipleDeleteRelationship()
	{
		try
		{
			NodeManager nm = NodeManager.getManager();
			Node node1 = nm.createNode();
			Node node2 = nm.createNode();
			Relationship rel = 
				NodeManager.getManager().createRelationship( node1, node2, 
					MyRelTypes.TEST );
			rel.delete();
			node1.delete();
			node2.delete();
			try
			{
				rel.delete();
				TransactionFactory.getUserTransaction().commit();
				fail( "Should not validate" );
			}
			catch ( Exception e )
			{
				// ok
			}
		}
		catch ( Exception e )
		{
			fail( "" + e );
		}
	}
	
	public void testIllegalPropertyType()
	{
		Logger log = Logger.getLogger( NodeManager.class.getName() );
		Level level = log.getLevel();
		log.setLevel( Level.OFF );
		try
		{
			NodeManager nm = NodeManager.getManager();
			Node node1 = nm.createNode();
			try
			{
				node1.setProperty( key, new Object() );
			}
			catch ( IllegalValueException e )
			{ // good
			}
			try
			{
				TransactionFactory.getUserTransaction().commit();
				fail( "Shouldn't validate" );
			}
			catch ( Exception e )
			{ } // good
			TransactionFactory.getUserTransaction().begin();
			try
			{
				nm.getNodeById( (int) node1.getId() );
				fail( "Node should not exist, previous tx didn't rollback" );
			}
			catch ( NotFoundException e )
			{
				// good 
			}
			node1 = nm.createNode();
			Node node2 = nm.createNode();
			Relationship rel = nm.createRelationship( node1, node2, 
				MyRelTypes.TEST );
			try
			{
				rel.setProperty( key, new Object() );
			}
			catch ( IllegalValueException e )
			{ // good
			}
			try
			{
				TransactionFactory.getUserTransaction().commit();
				fail( "Shouldn't validate" );
			}
			catch ( Exception e )
			{ } // good
			TransactionFactory.getUserTransaction().begin();
			try
			{
				nm.getNodeById( (int) node1.getId() );
				fail( "Node should not exist, previous tx didn't rollback" );
			}
			catch ( NotFoundException e )
			{
				// good 
			}
			try
			{
				nm.getNodeById( (int) node2.getId() );
				fail( "Node should not exist, previous tx didn't rollback" );
			}
			catch ( NotFoundException e )
			{
				// good 
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			fail( "" + e );
		}
		finally
		{
			log.setLevel( level );
		}
	}
}
