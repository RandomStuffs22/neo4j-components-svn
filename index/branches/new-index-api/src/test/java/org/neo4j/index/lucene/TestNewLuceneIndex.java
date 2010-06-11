package org.neo4j.index.lucene;

import static org.neo4j.index.Neo4jTestCase.assertCollection;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.index.Neo4jTestCase;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public class TestNewLuceneIndex
{
    private static GraphDatabaseService graphDb;
    private Transaction tx;
    
    @BeforeClass
    public static void setUpStuff()
    {
        String storeDir = "target/var/freshindex";
        Neo4jTestCase.deleteFileOrDirectory( new File( storeDir ) );
        graphDb = new EmbeddedGraphDatabase( storeDir );
    }
    
    @AfterClass
    public static void tearDownStuff()
    {
        graphDb.shutdown();
    }
    
    @After
    public void commitTx()
    {
        if ( tx != null )
        {
            tx.success();
            tx.finish();
            tx = null;
        }
    }
    
    @Before
    public void beginTx()
    {
        if ( tx == null )
        {
            tx = graphDb.beginTx();
        }
    }
    
    void restartTx()
    {
        commitTx();
        beginTx();
    }
    
    @Test
    public void testDefaultNodeIndex()
    {
        String name = "name";
        String mattias = "Mattias Persson";
        String title = "title";
        String hacker = "Hacker";
        
        Index<Node> nodeIndex = graphDb.nodeIndex( "default" );
        assertCollection( nodeIndex.get( name, mattias ) );
        
        Node node1 = graphDb.createNode();
        Node node2 = graphDb.createNode();
        
        nodeIndex.add( node1, name, mattias );
        assertCollection( nodeIndex.get( name, mattias ), node1 );
        assertCollection( nodeIndex.query( name, "\"" + mattias + "\"" ), node1 );
        assertCollection( nodeIndex.query( "name:\"" + mattias + "\"" ), node1 );
        commitTx();
        assertCollection( nodeIndex.get( name, mattias ), node1 );
        assertCollection( nodeIndex.query( name, "\"" + mattias + "\"" ), node1 );
        assertCollection( nodeIndex.query( "name:\"" + mattias + "\"" ), node1 );
        
        beginTx();
        nodeIndex.add( node2, title, hacker );
        assertCollection( nodeIndex.get( name, mattias ), node1 );
        assertCollection( nodeIndex.get( title, hacker ), node2 );
        assertCollection( nodeIndex.query( "name:\"" + mattias + "\" OR title:\"" +
                hacker + "\"" ), node1, node2 );
        commitTx();
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
        commitTx();
        assertCollection( nodeIndex.get( name, mattias ), node1 );
        assertCollection( nodeIndex.get( title, hacker ) );
        assertCollection( nodeIndex.query( "name:\"" + mattias + "\" OR title:\"" +
                hacker + "\"" ), node1 );
        
        beginTx();
        nodeIndex.remove( node1, name, mattias );
        commitTx();
    }
    
    @Test
    public void testFulltextNodeIndex()
    {
        Index<Node> index = ((EmbeddedGraphDatabase) graphDb).nodeIndex( "fulltext",
                LuceneIndexProvider.FULLTEXT_CONFIG );
        Node node1 = graphDb.createNode();
        Node node2 = graphDb.createNode();
        
        String key = "name";
        index.add( node1, key, "The quick brown fox" );
        index.add( node2, key, "brown fox jumped over" );
        
        assertCollection( index.get( key, "The quick brown fox" ), node1 );
        assertCollection( index.get( key, "brown fox jumped over" ), node2 );
        assertCollection( index.query( key, "quick" ), node1 );
        assertCollection( index.query( key, "brown" ), node1, node2 );
        assertCollection( index.query( key, "quick OR jumped" ), node1, node2 );
        assertCollection( index.query( key, "brown AND fox" ), node1, node2 );
        restartTx();
        assertCollection( index.get( key, "The quick brown fox" ), node1 );
        assertCollection( index.get( key, "brown fox jumped over" ), node2 );
        assertCollection( index.query( key, "quick" ), node1 );
        assertCollection( index.query( key, "brown" ), node1, node2 );
        assertCollection( index.query( key, "quick OR jumped" ), node1, node2 );
        assertCollection( index.query( key, "brown AND fox" ), node1, node2 );
        
        index.remove( node1, null );
        index.remove( node2, null );
        node1.delete();
        node2.delete();
    }
    
    @Test
    public void testFulltextRelationshipIndex()
    {
        Index<Relationship> index = ((EmbeddedGraphDatabase) graphDb).relationshipIndex( "fulltext",
                LuceneIndexProvider.FULLTEXT_CONFIG );
        Node node1 = graphDb.createNode();
        Node node2 = graphDb.createNode();
        RelationshipType type = DynamicRelationshipType.withName( "mytype" );
        Relationship rel1 = node1.createRelationshipTo( node2, type );
        Relationship rel2 = node1.createRelationshipTo( node2, type );
        
        String key = "name";
        index.add( rel1, key, "The quick brown fox" );
        index.add( rel2, key, "brown fox jumped over" );
        restartTx();
        
        assertCollection( index.get( key, "The quick brown fox" ), rel1 );
        assertCollection( index.get( key, "brown fox jumped over" ), rel2 );
        assertCollection( index.query( key, "quick" ), rel1 );
        assertCollection( index.query( key, "brown" ), rel1, rel2 );
        assertCollection( index.query( key, "quick OR jumped" ), rel1, rel2 );
        assertCollection( index.query( key, "brown AND fox" ), rel1, rel2 );
        
        index.remove( rel1, null );
        index.remove( rel2, null );
        rel1.delete();
        rel2.delete();
        node1.delete();
        node2.delete();
    }
}
