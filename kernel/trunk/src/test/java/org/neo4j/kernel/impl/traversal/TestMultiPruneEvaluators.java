/**
 * Copyright (c) 2002-2010 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
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

package org.neo4j.kernel.impl.traversal;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.PruneEvaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.kernel.Traversal;

public class TestMultiPruneEvaluators extends AbstractTestBase
{
    @BeforeClass
    public static void setupGraph()
    {
        createGraph( "a to b", "a to c", "a to d", "a to e",
                "b to f", "b to g", "b to h",
                "c to i",
                "d to j", "d to k", "d to l",
                "e to m", "e to n",
                "k to o", "k to p", "k to q", "k to r" );
    }

    @Test
    public void testMaxDepthAndCustomPruneEvaluatorCombined()
    {
        TraversalDescription description = Traversal.description().filter( Traversal.returnAll() )
                .prune( Traversal.pruneAfterDepth( 1 ) ).prune( new PruneEvaluator()
                {
                    public boolean pruneAfter( Path position )
                    {
                        return IteratorUtil.count( position.endNode().getRelationships(
                                Direction.OUTGOING ).iterator() ) < 3;
                    }
                } );
        Set<String> expectedNodes = new HashSet<String>(
                Arrays.asList( "a", "b", "c", "d", "e" ) );
        for ( Path position : description.traverse( referenceNode() ) )
        {
            String name = (String) position.endNode().getProperty( "name" );
            assertTrue( name + " shouldn't have been returned", expectedNodes.remove( name ) );
        }
        assertTrue( expectedNodes.isEmpty() );
    }
}
