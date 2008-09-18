/*
 * Copyright 2008 Network Engine for Objects in Lund AB [neotechnology.com]
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
package org.neo4j.graphalgo.shortestPath;

import java.util.HashMap;
import java.util.List;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.graphalgo.shortestPath.Dijkstra;
import org.neo4j.graphalgo.shortestPath.std.DoubleAdder;
import org.neo4j.graphalgo.shortestPath.std.DoubleComparator;
import org.neo4j.graphalgo.shortestPath.std.DoubleEvaluator;
import org.neo4j.graphalgo.testUtil.NeoAlgoTestCase;

public class DijkstraIteratorTest extends NeoAlgoTestCase
{
    public DijkstraIteratorTest( String arg0 )
    {
        super( arg0 );
    }

    public void testRun()
    {
        new TestDijkstra().runTest();
    }

    protected class TestDijkstra extends Dijkstra<Double>
    {
        public TestDijkstra()
        {
            super( 0.0, null, null, new DoubleEvaluator( "cost" ),
                new DoubleAdder(), new DoubleComparator(), Direction.BOTH,
                MyRelTypes.R1 );
        }

        protected class TestIterator extends Dijkstra<Double>.DijstraIterator
        {
            public TestIterator( Node startNode,
                HashMap<Node,List<Relationship>> predecessors,
                HashMap<Node,Double> mySeen, HashMap<Node,Double> otherSeen,
                HashMap<Node,Double> myDistances,
                HashMap<Node,Double> otherDistances, boolean backwards )
            {
                super( startNode, predecessors, mySeen, otherSeen, myDistances,
                    otherDistances, backwards );
            }
        }

        public void runTest()
        {
            graph.makeEdge( "start", "a", "cost", (double) 1 );
            graph.makeEdge( "a", "x", "cost", (double) 9 );
            graph.makeEdge( "a", "b", "cost", (double) 1 );
            graph.makeEdge( "b", "x", "cost", (double) 7 );
            graph.makeEdge( "b", "c", "cost", (double) 1 );
            graph.makeEdge( "c", "x", "cost", (double) 5 );
            graph.makeEdge( "c", "d", "cost", (double) 1 );
            graph.makeEdge( "d", "x", "cost", (double) 3 );
            graph.makeEdge( "d", "e", "cost", (double) 1 );
            graph.makeEdge( "e", "x", "cost", (double) 1 );
            HashMap<Node,Double> seen1, seen2, dists1, dists2;
            seen1 = new HashMap<Node,Double>();
            seen2 = new HashMap<Node,Double>();
            dists1 = new HashMap<Node,Double>();
            dists2 = new HashMap<Node,Double>();
            DijstraIterator iter1 = new TestIterator( graph.getNode( "start" ),
                predecessors1, seen1, seen2, dists1, dists2, false );
            // while ( iter1.hasNext() && !limitReached() && !iter1.isDone() )
            assertTrue( iter1.next().equals( graph.getNode( "start" ) ) );
            assertTrue( iter1.next().equals( graph.getNode( "a" ) ) );
            assertTrue( seen1.get( graph.getNode( "x" ) ) == 10.0 );
            assertTrue( iter1.next().equals( graph.getNode( "b" ) ) );
            assertTrue( seen1.get( graph.getNode( "x" ) ) == 9.0 );
            assertTrue( iter1.next().equals( graph.getNode( "c" ) ) );
            assertTrue( seen1.get( graph.getNode( "x" ) ) == 8.0 );
            assertTrue( iter1.next().equals( graph.getNode( "d" ) ) );
            assertTrue( seen1.get( graph.getNode( "x" ) ) == 7.0 );
            assertTrue( iter1.next().equals( graph.getNode( "e" ) ) );
            assertTrue( seen1.get( graph.getNode( "x" ) ) == 6.0 );
            assertTrue( iter1.next().equals( graph.getNode( "x" ) ) );
            assertTrue( seen1.get( graph.getNode( "x" ) ) == 6.0 );
            assertFalse( iter1.hasNext() );
            int count = 0;
            // This code below is correct for the alternative priority queue
            // while ( iter1.hasNext() )
            // {
            // iter1.next();
            // ++count;
            // }
            // assertTrue( count == 4 );
            // assertTrue( seen1.get( graph.getNode( "x" ) ) == 6.0 );
            // Now test node limit
            seen1 = new HashMap<Node,Double>();
            seen2 = new HashMap<Node,Double>();
            dists1 = new HashMap<Node,Double>();
            dists2 = new HashMap<Node,Double>();
            iter1 = new TestIterator( graph.getNode( "start" ), predecessors1,
                seen1, seen2, dists1, dists2, false );
            this.numberOfNodesTraversed = 0;
            this.limitMaxNodesToTraverse( 3 );
            count = 0;
            while ( iter1.hasNext() )
            {
                iter1.next();
                ++count;
            }
            assertTrue( count == 3 );
        }
    }
}
