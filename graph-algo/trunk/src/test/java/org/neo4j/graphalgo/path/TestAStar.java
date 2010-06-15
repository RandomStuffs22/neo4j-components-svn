package org.neo4j.graphalgo.path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphalgo.EstimateEvaluator;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.util.DoubleEvaluator;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.TraversalFactory;

import common.Neo4jAlgoTestCase;

public class TestAStar extends Neo4jAlgoTestCase
{
    static EstimateEvaluator<Double> ESTIMATE_EVALUATOR = new EstimateEvaluator<Double>()
    {
        public Double getCost( Node node, Node goal )
        {
            double dx = (Double) node.getProperty( "x" )
                        - (Double) goal.getProperty( "x" );
            double dy = (Double) node.getProperty( "y" )
                        - (Double) goal.getProperty( "y" );
            double result = Math.sqrt( Math.pow( dx, 2 ) + Math.pow( dy, 2 ) );
            return result;
        }
    };
    
    private PathFinder<WeightedPath> newFinder()
    {
        return GraphAlgoFactory.aStar( TraversalFactory.expanderForAllTypes(),
                new DoubleEvaluator( "length" ), ESTIMATE_EVALUATOR );
    }

    @Test
    public void testSimplest()
    {
        Node nodeA = graph.makeNode( "A", "x", 0d, "y", 0d );
        Node nodeB = graph.makeNode( "B", "x", 2d, "y", 1d );
        Node nodeC = graph.makeNode( "C", "x", 7d, "y", 0d );
        Relationship relAB = graph.makeEdge( "A", "B", "length", 2d );
        Relationship relAB2 = graph.makeEdge( "A", "B", "length", 2d );
        Relationship relBC = graph.makeEdge( "B", "C", "length", 3d );
        Relationship relAC = graph.makeEdge( "A", "C", "length", 10d );

        PathFinder<WeightedPath> astar = newFinder();
        int counter = 0;
        for ( WeightedPath path : astar.findAllPaths( nodeA, nodeC ) )
        {
            assertEquals( (Double)5d, (Double)path.weight() );
            assertPath( path, nodeA, nodeB, nodeC );
            counter++;
        }
//        assertEquals( 2, counter );
    }

    @Ignore
    @Test
    public void canGetMultiplePathsInTriangleGraph() throws Exception
    {
        Node nodeA = graph.makeNode( "A", "x", 0d, "y", 0d );
        Node nodeB = graph.makeNode( "B", "x", 2d, "y", 1d );
        Node nodeC = graph.makeNode( "C", "x", 7d, "y", 0d );
        Set<Relationship> expectedFirsts = new HashSet<Relationship>();
        expectedFirsts.add( graph.makeEdge( "A", "B", "length", 1d ) );
        expectedFirsts.add( graph.makeEdge( "A", "B", "length", 1d ) );
        Relationship expectedSecond = graph.makeEdge( "B", "C", "length", 2d );
        graph.makeEdge( "A", "C", "length", 5d );

        PathFinder<WeightedPath> algo = newFinder();
        Iterator<WeightedPath> paths = algo.findAllPaths( nodeA, nodeC ).iterator();
        for ( int i = 0; i < 2; i++ )
        {
            assertTrue( "expected more paths (i=" + i + ")", paths.hasNext() );
            Path path = paths.next();
            assertPath( path, nodeA, nodeB, nodeC );

            Iterator<Relationship> relationships = path.relationships().iterator();
            assertTrue( "found shorter path than expected",
                    relationships.hasNext() );
            assertTrue( "path contained unexpected relationship",
                    expectedFirsts.remove( relationships.next() ) );
            assertTrue( "found shorter path than expected",
                    relationships.hasNext() );
            assertEquals( expectedSecond, relationships.next() );
            assertFalse( "found longer path than expected",
                    relationships.hasNext() );
        }
        assertFalse( "expected at most two paths", paths.hasNext() );
    }

    @Ignore
    @Test
    public void canGetMultiplePathsInASmallRoadNetwork() throws Exception
    {
        Node nodeA = graph.makeNode( "A", "x", 1d, "y", 1d );
        Node nodeB = graph.makeNode( "B", "x", 2d, "y", 2d );
        Node nodeC = graph.makeNode( "C", "x", 0d, "y", 3d );
        Node nodeD = graph.makeNode( "D", "x", 1d, "y", 4d );
        Node nodeE = graph.makeNode( "E", "x", 1d, "y", 4d );
        Node nodeF = graph.makeNode( "F", "x", 1d, "y", 4d );
        graph.makeEdge( "A", "B", "length", 2d );
        graph.makeEdge( "A", "C", "length", 2.5d );
        graph.makeEdge( "C", "D", "length", 7.3d );
        graph.makeEdge( "B", "D", "length", 2.5d );
        graph.makeEdge( "D", "E", "length", 3d );
        graph.makeEdge( "C", "E", "length", 5d );
        graph.makeEdge( "E", "F", "length", 5d );
        graph.makeEdge( "C", "F", "length", 12d );
        graph.makeEdge( "A", "F", "length", 25d );

        PathFinder<WeightedPath> algo = newFinder();

        // Try the search in both directions.
        for ( Node[] nodes : new Node[][] { { nodeA, nodeF }, { nodeF, nodeA } } )
        {
            int found = 0;
            Iterator<WeightedPath> paths = algo.findAllPaths( nodes[0], nodes[1] ).iterator();
            for ( int i = 0; i < 2; i++ )
            {
                assertTrue( "expected more paths (i=" + i + ")", paths.hasNext() );
                Path path = paths.next();
                if ( path.length() != found && path.length() == 3 )
                {
                    assertContains( path.nodes(), nodeA, nodeC, nodeE, nodeF );
                }
                else if ( path.length() != found && path.length() == 4 )
                {
                    assertContains( path.nodes(), nodeA, nodeB, nodeD, nodeE,
                            nodeF );
                }
                else
                {
                    fail( "unexpected path length: " + path.length() );
                }
                found = path.length();
            }
            assertFalse( "expected at most two paths", paths.hasNext() );
        }
    }
}
