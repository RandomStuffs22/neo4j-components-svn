/*
 * Copyright (c) 2002-2008 "Neo Technology,"
 *     Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 * 
 * Neo4j is free software: you can redistribute it and/or modify
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.util.timeline;

import java.util.Iterator;
import java.util.LinkedList;

import org.neo4j.api.core.Node;
import org.neo4j.util.NeoTestCase;
import org.neo4j.util.timeline.Timeline;

public class TestTimeline extends NeoTestCase
{
	private Timeline timeline;
	
	@Override
	public void setUp() throws Exception
	{
	    super.setUp();
		Node node = neo().createNode();
		timeline = new Timeline( "test_timeline", node, false, neo() ); 
	}
	
	@Override
	public void tearDown() throws Exception
	{
		timeline.delete();
		super.tearDown();
	}
	
	private long getStamp()
	{
		long stamp = System.currentTimeMillis();
		try
		{
			Thread.sleep( 20 );
		}
		catch ( InterruptedException e )
		{
			Thread.interrupted();
		}
		return stamp;
	}
	
	public void testTimelineBasic()
	{
		Node node1 = neo().createNode();
		long stamp1 = getStamp();
		node1.setProperty( "timestamp", stamp1 );
		
		assertTrue( !timeline.getAllNodes().iterator().hasNext() );
		assertTrue( !timeline.getAllNodesAfter( 0 ).iterator().hasNext() );
		assertTrue( !timeline.getAllNodesBefore( 0 ).iterator().hasNext() );
		assertTrue( !timeline.getAllNodesBetween( 0, 
			1 ).iterator().hasNext() );
		assertTrue( timeline.getFirstNode() == null );
		assertTrue( timeline.getLastNode() == null );

		timeline.addNode( node1, stamp1 );
		assertEquals( stamp1, timeline.getTimestampForNode( node1 ) );
		
		Iterator<Node> itr = timeline.getAllNodes().iterator();
		assertEquals( node1, itr.next() );
		assertTrue( !itr.hasNext() );
		itr = timeline.getAllNodesAfter( 0 ).iterator(); 
		assertEquals( node1, itr.next() );
		assertTrue( !itr.hasNext() );
		itr = timeline.getAllNodesBefore( stamp1 + 1 ).iterator(); 
		assertEquals( node1, itr.next() );
		assertTrue( !itr.hasNext() );
		itr = timeline.getAllNodesBetween( 0, stamp1 + 1 ).iterator();
		assertEquals( node1, itr.next() );
		assertTrue( !itr.hasNext() );
		assertEquals( node1, timeline.getFirstNode() );
		assertEquals( node1, timeline.getLastNode() );
		
		timeline.removeNode( node1 );
		assertTrue( !timeline.getAllNodes().iterator().hasNext() );
		assertTrue( !timeline.getAllNodesAfter( 0 ).iterator().hasNext() );
		assertTrue( !timeline.getAllNodesBefore( 0 ).iterator().hasNext() );
		assertTrue( !timeline.getAllNodesBetween( 0, 
			1 ).iterator().hasNext() );
		assertTrue( timeline.getFirstNode() == null );
		assertTrue( timeline.getLastNode() == null );

		timeline.addNode( node1, stamp1 );
		assertEquals( stamp1, timeline.getTimestampForNode( node1 ) );
		Node node2 = neo().createNode();
		long stamp2 = getStamp();
		node2.setProperty( "timestamp", stamp2 );
		timeline.addNode( node2, stamp2 );
		assertEquals( stamp2, timeline.getTimestampForNode( node2 ) );
		
		itr = timeline.getAllNodes().iterator();
		assertEquals( node1, itr.next() );
		assertEquals( node2, itr.next() );
		assertTrue( !itr.hasNext() );
		itr = timeline.getAllNodesAfter( 0 ).iterator(); 
		assertEquals( node1, itr.next() );
		assertEquals( node2, itr.next() );
		assertTrue( !itr.hasNext() );
		itr = timeline.getAllNodesBefore( stamp2 + 1 ).iterator(); 
		assertEquals( node1, itr.next() );
		assertEquals( node2, itr.next() );
		assertTrue( !itr.hasNext() );
		itr = timeline.getAllNodesBetween( 0, stamp2 + 1 ).iterator();
		assertEquals( node1, itr.next() );
		assertEquals( node2, itr.next() );
		assertTrue( !itr.hasNext() );
		assertEquals( node1, timeline.getFirstNode() );
		assertEquals( node2, timeline.getLastNode() );		
		
        itr = timeline.getAllNodes( null, null ).iterator();
        assertEquals( node1, itr.next() );
        assertEquals( node2, itr.next() );
        assertTrue( !itr.hasNext() );
        itr = timeline.getAllNodes( 0L, null ).iterator(); 
        assertEquals( node1, itr.next() );
        assertEquals( node2, itr.next() );
        assertTrue( !itr.hasNext() );
        itr = timeline.getAllNodes( null, stamp2 + 1 ).iterator(); 
        assertEquals( node1, itr.next() );
        assertEquals( node2, itr.next() );
        assertTrue( !itr.hasNext() );
        itr = timeline.getAllNodes( 0L, stamp2 + 1 ).iterator();
        assertEquals( node1, itr.next() );
        assertEquals( node2, itr.next() );
        assertTrue( !itr.hasNext() );

		timeline.removeNode( node1 );
		timeline.removeNode( node2 );
		assertTrue( !timeline.getAllNodes().iterator().hasNext() );
		assertTrue( !timeline.getAllNodesAfter( 0 ).iterator().hasNext() );
		assertTrue( !timeline.getAllNodesBefore( 0 ).iterator().hasNext() );
		assertTrue( !timeline.getAllNodesBetween( 0, 
			1 ).iterator().hasNext() );
		assertTrue( timeline.getFirstNode() == null );
		assertTrue( timeline.getLastNode() == null );
		
		
		timeline.addNode( node1, stamp1 );
		timeline.addNode( node2, stamp2 );
		Node node3 = neo().createNode();
		long stamp3 = getStamp();
		node3.setProperty( "timestamp", stamp3 );
		timeline.addNode( node3, stamp3 );
		
		itr = timeline.getAllNodes().iterator();
		assertEquals( node1, itr.next() );
		assertEquals( node2, itr.next() );
		assertEquals( node3, itr.next() );
		assertTrue( !itr.hasNext() );
		itr = timeline.getAllNodesAfter( 0 ).iterator();
		assertEquals( node1, itr.next() );
		assertEquals( node2, itr.next() );
		assertEquals( node3, itr.next() );
		assertTrue( !itr.hasNext() );
		itr = timeline.getAllNodesBefore( stamp3 + 1 ).iterator(); 
		assertEquals( node1, itr.next() );
		assertEquals( node2, itr.next() );
		assertEquals( node3, itr.next() );
		assertTrue( !itr.hasNext() );
		itr = timeline.getAllNodesBetween( 0, stamp3 + 1 ).iterator();
		assertEquals( node1, itr.next() );
		assertEquals( node2, itr.next() );
		assertEquals( node3, itr.next() );
		assertTrue( !itr.hasNext() );
		assertEquals( node1, timeline.getFirstNode() );
		assertEquals( node3, timeline.getLastNode() );		
		
		itr = timeline.getAllNodesAfter( stamp1 ).iterator(); 
		assertEquals( node2, itr.next() );
		assertEquals( node3, itr.next() );
		assertTrue( !itr.hasNext() );
		itr = timeline.getAllNodesBefore( stamp3 ).iterator(); 
		assertEquals( node1, itr.next() );
		assertEquals( node2, itr.next() );
		assertTrue( !itr.hasNext() );
		itr = timeline.getAllNodesBetween( stamp1, stamp3 ).iterator();
		assertEquals( node2, itr.next() );
		assertTrue( !itr.hasNext() );
		
		timeline.removeNode( node2 );
		itr = timeline.getAllNodesAfter( stamp1 ).iterator(); 
		assertEquals( node3, itr.next() );
		assertTrue( !itr.hasNext() );
		itr = timeline.getAllNodesBefore( stamp3 ).iterator(); 
		assertEquals( node1, itr.next() );
		assertTrue( !itr.hasNext() );
		itr = timeline.getAllNodesBetween( stamp1, stamp3 ).iterator();
		assertTrue( !itr.hasNext() );
		assertEquals( node1, timeline.getFirstNode() );
		assertEquals( node3, timeline.getLastNode() );
		
		timeline.removeNode( node3 );
		itr = timeline.getAllNodes().iterator();
		assertEquals( node1, itr.next() );
		assertTrue( !itr.hasNext() );
		itr = timeline.getAllNodesAfter( 0 ).iterator(); 
		assertEquals( node1, itr.next() );
		assertTrue( !itr.hasNext() );
		itr = timeline.getAllNodesBefore( stamp1 + 1 ).iterator(); 
		assertEquals( node1, itr.next() );
		assertTrue( !itr.hasNext() );
		itr = timeline.getAllNodesBetween( 0, stamp1 + 1 ).iterator();
		assertEquals( node1, itr.next() );
		assertTrue( !itr.hasNext() );
		assertEquals( node1, timeline.getFirstNode() );
		assertEquals( node1, timeline.getLastNode() );
		
		timeline.removeNode( node1 );
		
		node1.delete();
		node2.delete();
		node3.delete();
	}
	
	public void testIllegalStuff()
	{
		Node node1 = neo().createNode();
		long stamp1 = System.currentTimeMillis();
		try 
		{ 
			new Timeline( "blabla", null, true, neo() );
			fail( "Null parameter should throw exception" );
		} 
		catch ( IllegalArgumentException e ) { // good
		}
		try 
		{ 
			new Timeline( "blabla", node1, false, null );
			fail( "Null parameter should throw exception" );
		} 
		catch ( IllegalArgumentException e ) { // good
		}

		node1.setProperty( "timestamp", stamp1 );
		timeline.addNode( node1, stamp1 );
		try 
		{ 
			timeline.addNode( node1, stamp1 );
			fail( "Re-adding node should throw exception" );
		} 
		catch ( IllegalArgumentException e ) { // good
		}
		try 
		{ 
			timeline.removeNode( timeline.getUnderlyingNode() );
			fail( "Removing underlying node should throw exception" );
		} 
		catch ( IllegalArgumentException e ) { // good
		}
		timeline.removeNode( node1 );
		try 
		{ 
			timeline.removeNode( node1 );
			fail( "Removing non added node should throw exception" );
		} 
		catch ( IllegalArgumentException e ) { // good
		}
		
		node1.delete();
	}
	
	public void testIndexedTimeline()
	{
		Node tlNode = neo().createNode();
		Timeline timeline = new Timeline( "test", tlNode, true, neo() ); 
		LinkedList<Node> after = new LinkedList<Node>();
		LinkedList<Node> before = new LinkedList<Node>();
		for ( long i = 1; i < 128; i++ )
		{
			Node node = neo().createNode();
			timeline.addNode( node, i );
			if ( i > 64 )
			{
				after.add( node );
			}
			else
			{
				before.add( node );
			}
		}
		Iterable<Node> nodes = timeline.getAllNodesAfter( 64 );
		while ( nodes.iterator().hasNext() )
		{
			Node nodeToRemove = nodes.iterator().next();
			assert nodeToRemove.equals( after.removeFirst() );
			timeline.removeNode( nodeToRemove );
			nodes = timeline.getAllNodesBefore( 65 );
			if ( nodes.iterator().hasNext() )
			{
				nodeToRemove = nodes.iterator().next();
				assert nodeToRemove.equals( before.removeFirst() );
				timeline.removeNode( nodeToRemove );
			}
			nodes = timeline.getAllNodesAfter( 64 );
		}
		nodes = timeline.getAllNodesBefore( 65 );
		if ( nodes.iterator().hasNext() )
		{
			Node nodeToRemove = nodes.iterator().next();
			assert nodeToRemove.equals( before.removeLast() );
			timeline.removeNode( nodeToRemove );
		}
		assert !tlNode.getRelationships( 
			Timeline.RelTypes.TIMELINE_NEXT_ENTRY ).iterator().hasNext();
		timeline.delete();
	}
	
	public void testTimelineSameTimestamp()
	{
		Node tlNode = neo().createNode();
		Timeline timeline = new Timeline( "test", tlNode, true, neo() );
		Node node0 = neo().createNode();
		Node node1_1 = neo().createNode();
		Node node1_2 = neo().createNode();
		Node node2 = neo().createNode();
		timeline.addNode( node1_1, 1 );
		timeline.addNode( node1_2, 1 );
		timeline.addNode( node0, 0 );
		timeline.addNode( node2, 2 );
		Iterator<Node> itr = timeline.getAllNodes().iterator();
		assertEquals( node0, itr.next() );
		Node node1 = itr.next();
		if ( node1.equals( node1_1 ) )
		{
			assertEquals( node1_2, itr.next() );
		}
		else if ( node1.equals( node1_2 ) )
		{
			assertEquals( node1_1, itr.next() );
		}
		else
		{
			fail( "should return node1_1 or node1_2" );
		}
		assertEquals( node2, itr.next() );
		assertTrue( !itr.hasNext() );
        itr = timeline.getNodes( 1 ).iterator();
        node1 = itr.next();
        if ( node1.equals( node1_1 ) )
        {
            assertEquals( node1_2, itr.next() );
        }
        else if ( node1.equals( node1_2 ) )
        {
            assertEquals( node1_1, itr.next() );
        }
        else
        {
            fail( "should return node1_1 or node1_2" );
        }
		node0.delete();
		node1_1.delete();
		node1_2.delete();
		node2.delete();
		timeline.delete();
	}
	
	public void testMultipleTimelines()
	{
		Node tlNode1 = neo().createNode();
		Timeline timeline1 = new Timeline( "test1", tlNode1, true, neo() );
		Node tlNode2 = neo().createNode();
		Timeline timeline2 = new Timeline( "test2", tlNode2, true, neo() );
		Node node1 = neo().createNode();
		Node node2 = neo().createNode();
		Node node3 = neo().createNode();
		Node node4 = neo().createNode();
		
		timeline1.addNode( node1, 1 );
		timeline1.addNode( node2, 2 );
		timeline2.addNode( node3, 1 );
		timeline2.addNode( node4, 2 );
		
		assertEquals( node2, timeline1.getLastNode() );
		assertEquals( node4, timeline2.getLastNode() );
		assertEquals( node1, timeline1.getFirstNode() );
		assertEquals( node3, timeline2.getFirstNode() );
		
		timeline1.addNode( node3, 3 );
		Iterator<Node> itr = timeline1.getAllNodes().iterator();
		assertEquals( node1, itr.next() );
		assertEquals( node2, itr.next() );
		assertEquals( node3, itr.next() );
		assertTrue( !itr.hasNext() );
		
		itr = timeline2.getAllNodes().iterator();
		assertEquals( node3, itr.next() );
		assertEquals( node4, itr.next() );
		assertTrue( !itr.hasNext() );
		
		timeline1.delete();
		timeline2.delete();
		node1.delete(); node2.delete(); node3.delete(); node4.delete();
	}

    public void testTimelineRemoveNode()
    {
        Node tlNode = neo().createNode();
        Timeline indexedTimeline = new Timeline( "test", tlNode, true, neo() ); 
        for ( long i = 1; i < 128; i++ )
        {
            Node node = neo().createNode();
            indexedTimeline.addNode( node, i );
        }
        for ( Node node : indexedTimeline.getAllNodes() )
        {
            indexedTimeline.removeNode( node );
            node.delete();
        }
        assertFalse( indexedTimeline.getAllNodes().iterator().hasNext() );
        LinkedList<Node> nodes = new LinkedList<Node>();
        for ( long i = 1; i < 128; i++ )
        {
            Node node = neo().createNode();
            indexedTimeline.addNode( node, i );
            nodes.add( node );
        }
        indexedTimeline.delete();
        for ( Node node : nodes )
        {
            node.delete();
        }
    }
}
