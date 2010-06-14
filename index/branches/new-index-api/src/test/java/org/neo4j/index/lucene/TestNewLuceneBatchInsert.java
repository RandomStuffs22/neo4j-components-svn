package org.neo4j.index.lucene;

import static org.neo4j.index.Neo4jTestCase.assertCollection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.BatchInserterIndex;
import org.neo4j.graphdb.index.BatchInserterIndexProvider;
import org.neo4j.graphdb.index.Index;
import org.neo4j.index.Neo4jTestCase;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.impl.batchinsert.BatchInserter;
import org.neo4j.kernel.impl.batchinsert.BatchInserterImpl;

public class TestNewLuceneBatchInsert
{
    private static final String PATH = "target/var/batch";
    
    @Before
    public void cleanDirectory()
    {
        Neo4jTestCase.deleteFileOrDirectory( new File( PATH ) );
    }
    
    @Test
    public void testSome() throws Exception
    {
        BatchInserter inserter = new BatchInserterImpl( PATH );
        BatchInserterIndexProvider provider = new LuceneBatchInserterIndexProvider( inserter );
        BatchInserterIndex index = provider.nodeIndex( "users", null );
        Map<Integer, Long> ids = new HashMap<Integer, Long>();
        for ( int i = 0; i < 100; i++ )
        {
            long id = inserter.createNode( null );
            index.add( id, "name", "Joe" + i );
            ids.put( i, id );
        }
        
        for ( int i = 0; i < 100; i++ )
        {
            assertCollection( index.get( "name", "Joe" + i ), ids.get( i ) );
        }
        
        assertCollection( index.query( "name", "Joe*" ),
                ids.values().toArray( new Long[ids.size()] ) );
        provider.shutdown();
        inserter.shutdown();
        
        GraphDatabaseService db = new EmbeddedGraphDatabase( PATH );
        Index<Node> dbIndex = db.nodeIndex( "users" );
        for ( int i = 0; i < 100; i++ )
        {
            assertCollection( dbIndex.get( "name", "Joe" + i ), db.getNodeById(
                    ids.get( i ) ) );
        }
        
        Collection<Node> nodes = new ArrayList<Node>();
        for ( long id : ids.values() )
        {
            nodes.add( db.getNodeById( id ) );
        }
        assertCollection( dbIndex.query( "name", "Joe*" ),
                nodes.toArray( new Node[nodes.size()] ) );
        db.shutdown();
    }
    
    @Test
    public void testFulltext()
    {
        BatchInserter inserter = new BatchInserterImpl( PATH );
        BatchInserterIndexProvider provider = new LuceneBatchInserterIndexProvider( inserter );
        String name = "users2";
        BatchInserterIndex index = provider.nodeIndex( name,
                LuceneIndexProvider.FULLTEXT_CONFIG );

        long id1 = inserter.createNode( null );
        index.add( id1, "name", "Mattias Persson" );
        index.add( id1, "email", "something@somewhere" );
        long id2 = inserter.createNode( null );
        index.add( id2, "name", "Lars PerssoN" );
        assertCollection( index.get( "name", "Mattias Persson" ), id1 );
        assertCollection( index.query( "name", "mattias" ), id1 );
        assertCollection( index.query( "name", "bla" ) );
        assertCollection( index.query( "name", "persson" ), id1, id2 );
        
        provider.shutdown();
        inserter.shutdown();
        
        GraphDatabaseService db = new EmbeddedGraphDatabase( PATH );
        Index<Node> dbIndex = ((EmbeddedGraphDatabase) db).nodeIndex( name,
                LuceneIndexProvider.FULLTEXT_CONFIG );
        Node node1 = db.getNodeById( id1 );
        Node node2 = db.getNodeById( id2 );
        assertCollection( dbIndex.query( "name", "persson" ), node1, node2 );
        db.shutdown();
    }
}
