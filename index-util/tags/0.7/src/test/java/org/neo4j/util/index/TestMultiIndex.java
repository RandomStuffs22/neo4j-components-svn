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
package org.neo4j.util.index;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.neo4j.api.core.Node;
import org.neo4j.util.NeoTestCase;
import org.neo4j.util.index.Index;
import org.neo4j.util.index.MultiValueIndex;
import org.neo4j.util.index.SingleValueIndex;

public class TestMultiIndex extends NeoTestCase
{
	private MultiValueIndex index;
	
	@Override
	public void setUp() throws Exception
	{
	    super.setUp();
		Node node = neo().createNode();
		index = new MultiValueIndex( "test_simple", node, neo() ); 
	}
	
	public void testSimpleIndexBasic()
	{
		Node node1 = neo().createNode();
		Object key1 = 1;
		
		assertTrue( !index.getNodesFor( key1 ).iterator().hasNext() );

		index.index( node1, key1 );
		
		Iterator<Node> itr = index.getNodesFor( key1 ).iterator();
		assertEquals( node1, itr.next() );
		assertTrue( !itr.hasNext() );
		
		index.remove( node1, key1 );
		assertTrue( !index.getNodesFor( key1 ).iterator().hasNext() );

		index.index( node1, key1 );
		Node node2 = neo().createNode();
		index.index( node2, key1 );
		
		itr = index.getNodesFor( key1 ).iterator();
		assertTrue( itr.next() != null );
		assertTrue( itr.next() != null );
		assertTrue( !itr.hasNext() );
		assertTrue( !itr.hasNext() );		
		
		index.remove( node1, key1 );
		index.remove( node2, key1 );
		assertTrue( !index.getNodesFor( key1 ).iterator().hasNext() );
		
		node1.delete();
		node2.delete();
	}
	
	public void testIllegalStuff()
	{
		Node node1 = neo().createNode();
		try 
		{ 
			new MultiValueIndex( "blabla", null, neo() );
			fail( "Null parameter should throw exception" );
		} 
		catch ( IllegalArgumentException e ) { // good
		}
		try 
		{ 
			new MultiValueIndex( "blabla", node1, null );
			fail( "Null parameter should throw exception" );
		} 
		catch ( IllegalArgumentException e ) { // good
		}
		Index sIndex = new SingleValueIndex( "multi", node1, neo() );
		try 
		{ 
			new MultiValueIndex( "blabla", node1, neo() );
			fail( "Wrong index type should throw exception" );
		} 
		catch ( IllegalArgumentException e ) { // good
		}
		sIndex.drop();
	}	

    public void testValues()
    {
        Set<Node> nodes = new HashSet<Node>();
        for ( int i = 0; i < 100; i++ )
        {
            Node node1 = neo().createNode();
            Node node2 = neo().createNode();
            nodes.add( node1 );
            nodes.add( node2 );
            index.index( node1, i );
            index.index( node2, i );
        }
        for ( Node node : index.values() )
        {
            assertTrue( nodes.remove( node ) );
        }
        assertTrue( nodes.isEmpty() );
    }
}