package org.neo4j.index.lucene;

import static org.neo4j.index.Neo4jTestCase.assertCollection;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexProvider;
import org.neo4j.index.Neo4jTestCase;
import org.neo4j.index.lucene.LuceneIndexProvider;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public class TestNewLuceneIndex
{
    private static GraphDatabaseService graphDb;
    private static IndexProvider index;
    private Transaction tx;
    
    @BeforeClass
    public static void setUpStuff()
    {
        String storeDir = "target/var/freshindex";
        Neo4jTestCase.deleteFileOrDirectory( new File( storeDir ) );
        graphDb = new EmbeddedGraphDatabase( storeDir );
        index = new LuceneIndexProvider( graphDb );
    }
    
    @AfterClass
    public static void tearDownStuff()
    {
        graphDb.shutdown();
    }
    
    @After
    public void finishTransaction()
    {
        if ( tx != null )
        {
            tx.success();
            tx.finish();
            tx = null;
        }
    }
    
    private Transaction beginTx()
    {
        if ( tx == null )
        {
            tx = graphDb.beginTx();
        }
        return tx;
    }
    
    @Test
    public void testDefaultNodeIndex()
    {
        String name = "name";
        String mattias = "Mattias Persson";
        String title = "title";
        String hacker = "Hacker";
        
        Index<Node> nodeIndex = index.nodeIndex( "default" );
        assertCollection( nodeIndex.get( name, mattias ) );
        
        beginTx();
        Node node1 = graphDb.createNode();
        Node node2 = graphDb.createNode();
        
        nodeIndex.add( node1, name, mattias );
        assertCollection( nodeIndex.get( name, mattias ), node1 );
        assertCollection( nodeIndex.query( name, "\"" + mattias + "\"" ), node1 );
        assertCollection( nodeIndex.query( "name:\"" + mattias + "\"" ), node1 );
        finishTransaction();
        assertCollection( nodeIndex.get( name, mattias ), node1 );
        assertCollection( nodeIndex.query( name, "\"" + mattias + "\"" ), node1 );
        assertCollection( nodeIndex.query( "name:\"" + mattias + "\"" ), node1 );
        
        beginTx();
        nodeIndex.add( node2, title, hacker );
        assertCollection( nodeIndex.get( name, mattias ), node1 );
        assertCollection( nodeIndex.get( title, hacker ), node2 );
        assertCollection( nodeIndex.query( "name:\"" + mattias + "\" OR title:\"" +
                hacker + "\"" ), node1, node2 );
        finishTransaction();
        assertCollection( nodeIndex.get( name, mattias ), node1 );
        assertCollection( nodeIndex.get( title, hacker ), node2 );
        assertCollection( nodeIndex.query( "name:\"" + mattias + "\" OR title:\"" +
                hacker + "\"" ), node1, node2 );
        
        beginTx();
        nodeIndex.remove( node2, title, hacker );
        assertCollection( nodeIndex.get( name, mattias ), node1 );
        assertCollection( nodeIndex.get( title, hacker ) );
        assertCollection( nodeIndex.query( "name:\"" + mattias + "\" OR title:\"" +
                hacker + "\"" ), node1 );
        finishTransaction();
        assertCollection( nodeIndex.get( name, mattias ), node1 );
        assertCollection( nodeIndex.get( title, hacker ) );
        assertCollection( nodeIndex.query( "name:\"" + mattias + "\" OR title:\"" +
                hacker + "\"" ), node1 );
        
        beginTx();
        nodeIndex.remove( node1, name, mattias );
        finishTransaction();
    }
}
