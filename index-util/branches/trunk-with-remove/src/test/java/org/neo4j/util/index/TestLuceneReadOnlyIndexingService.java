/*
 * Copyright (c) 2002-2009 "Neo Technology,"
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

import java.util.Iterator;

import org.neo4j.api.core.EmbeddedReadOnlyNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Transaction;
import org.neo4j.util.NeoTestCaseWithIndex;

public class TestLuceneReadOnlyIndexingService extends NeoTestCaseWithIndex
{
	protected IndexService instantiateIndexService()
	{
	    return new LuceneIndexService( neo() );
	}
	
    public void testSimple()
    {
        Node node1 = neo().createNode();
        
        assertTrue( !indexService().getNodes( "a_property", 
            1 ).iterator().hasNext() );
        assertEquals( 0, indexService().getNodes( "a_property", 1 ).size() );

        indexService().index( node1, "a_property", 1 );
        
        IndexHits hits = indexService().getNodes( "a_property", 1 );
        Iterator<Node> itr = hits.iterator();
        assertEquals( node1, itr.next() );
        assertTrue( !itr.hasNext() );
        assertEquals( 1, hits.size() );
        restartTx();
        
        NeoService readOnlyNeo = new EmbeddedReadOnlyNeo(
            getNeoPath().getAbsolutePath() );
        IndexService readOnlyIndex =
            new LuceneReadOnlyIndexService( readOnlyNeo );
        Transaction tx = readOnlyNeo.beginTx();
        itr = readOnlyIndex.getNodes( "a_property", 1 ).iterator();
        assertEquals( node1, itr.next() );
        assertTrue( !itr.hasNext() );
        tx.finish();
        readOnlyIndex.shutdown();
        readOnlyNeo.shutdown();
        
        indexService().removeIndex( node1, "a_property", 1 );
        node1.delete();
    }
}
