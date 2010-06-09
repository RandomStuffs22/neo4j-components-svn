package org.neo4j.index.lucene;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.index.IndexService;
import org.neo4j.index.Neo4jWithIndexTestCase;

public abstract class TestLuceneIndexLazyness extends Neo4jWithIndexTestCase
{
    @Override
    protected IndexService instantiateIndex()
    {
        return new LuceneIndexService( graphDb() );
    }
    
    @Test
    public void testIt() throws Exception
    {
        String key1 = "key1";
        String key2 = "key2";
        String value = "myvalue";
        Collection<Node> nodes1 = new ArrayList<Node>();
        Collection<Node> nodes2 = new ArrayList<Node>();
        for ( int i = 0; i < 10000; i++ )
        {
            Node node = graphDb().createNode();
            index().index( node, key1, value );
            nodes1.add( node );
        }
        restartTx();
        for ( int i = 0; i < LuceneIndexProvider.DEFAULT_LAZY_THRESHOLD - 1; i++ )
        {
            Node node = graphDb().createNode();
            index().index( node, key2, value );
            nodes2.add( node );
        }
        
        Node[] nodeArray1 = nodes1.toArray( new Node[ 0 ] );
        Node[] nodeArray2 = nodes2.toArray( new Node[ 0 ] );
        long total = 0;
        long totalTotal = 0;
        int counter = 0;
        for ( int i = 0; i < 10; i++ )
        {
            long t = System.currentTimeMillis();
            Iterable<Node> itr = index().getNodes( key1, value );
            long time1 = System.currentTimeMillis() - t;
            assertCollection( asCollection( itr ), nodeArray1 );
            long totalTime1 = System.currentTimeMillis() - t;
            
            t = System.currentTimeMillis();
            itr = index().getNodes( key2, value );
            long time2 = System.currentTimeMillis() - t;
            assertCollection( asCollection( itr ), nodeArray2 );
            long totalTime2 = System.currentTimeMillis() - t;
            System.out.println( "time1:" + time1 + " (" + totalTime1 +
                "), time2:" + time2 + " (" + totalTime2 + ")" );
            
//            if ( i > 0 )
//            {
//                total += syncTime;
//                totalTotal += syncTotalTime;
//                counter++;
//            }
            
            // At the very least
//            assertTrue( lazyTime < syncTime / 3 );
        }
        
//        System.out.println( "avg:" + ( total / counter ) + ", " +
//            ( totalTotal / counter ) );
        
        for ( Node node : nodes1 )
        {
            index().removeIndex( node, key1, value );
            node.delete();
        }
        for ( Node node : nodes2 )
        {
            index().removeIndex( node, key2, value );
            node.delete();
        }
    }
}
