package org.neo4j.onlinebackup;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.impl.transaction.XaDataSourceManager;
import org.neo4j.kernel.impl.transaction.xaframework.XaDataSource;

public class TestCacheInvalidation
{
    private static final String FILE_SEP = System
        .getProperty( "file.separator" );
    private static final String TARGET_DIR = "target";
    private static final String VAR = TARGET_DIR + FILE_SEP + "var";
    private static final String STORE_LOCATION_DIR = VAR + FILE_SEP + "neo-db";
    private static final String BACKUP_LOCATION_DIR = VAR + FILE_SEP
        + "neo-backup";

    private enum RelTypes implements RelationshipType
    {
        TEST
    }
    
    @Before
    public void clean() throws IOException
    {
        Util.deleteDir( new File( VAR ) );

        EmbeddedGraphDatabase graphDb = 
            new EmbeddedGraphDatabase( STORE_LOCATION_DIR );
        graphDb.shutdown();
        Util.copyDir( STORE_LOCATION_DIR, BACKUP_LOCATION_DIR );
        graphDb = new EmbeddedGraphDatabase( STORE_LOCATION_DIR );
        XaDataSourceManager xaDsMgr = 
            graphDb.getConfig().getTxModule().getXaDataSourceManager();
        for ( XaDataSource xaDs : xaDsMgr.getAllRegisteredDataSources() )
        {
            xaDs.keepLogicalLogs( true );
        }
        Transaction tx = graphDb.beginTx();
        Node node2;
        Relationship rel2;
        try
        {
            Node node1 = graphDb.createNode();
            Relationship rel1 = graphDb.getReferenceNode().createRelationshipTo( 
                    node1,RelTypes.TEST );
            node2 = graphDb.createNode();
            rel2 = graphDb.getReferenceNode().createRelationshipTo( node2, 
                    RelTypes.TEST );
            node1.setProperty( "test", 1 );
            rel1.setProperty( "test", 1 );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        for ( XaDataSource xaDs : xaDsMgr.getAllRegisteredDataSources() )
        {
            xaDs.rotateLogicalLog();
        }
        tx = graphDb.beginTx();
        try
        {
            node2.setProperty( "test", 2 );
            rel2.setProperty( "test", 2 );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        graphDb.shutdown();
        
        Util.copyLogs( STORE_LOCATION_DIR, BACKUP_LOCATION_DIR );
    }

    @Test
    public void applyAndTest() throws IOException
    {
        String destDir = "target/var/neo-backup";
        Map<String,String> params = new HashMap<String, String>();
        params.put( "backup_slave", "true" );
        EmbeddedGraphDatabase graphDb = new EmbeddedGraphDatabase( destDir );
        XaDataSourceManager xaDsMgr = 
            graphDb.getConfig().getTxModule().getXaDataSourceManager();
        XaDataSource xaDs = xaDsMgr.getXaDataSource( "nioneodb" );
        xaDs.makeBackupSlave();
        // first version
        long nextVersion = xaDs.getCurrentLogVersion();
        xaDs.applyLog( xaDs.getLogicalLog( nextVersion ) );
        Transaction tx = graphDb.beginTx();
        Node node2 = null;
        for ( Relationship rel : 
            graphDb.getReferenceNode().getRelationships( RelTypes.TEST ) )
        {
            if ( !rel.getEndNode().hasProperty( "test" ) )
            {
                node2 = rel.getEndNode();
                break;
            }
        }
        tx.finish();
        nextVersion = xaDs.getCurrentLogVersion();
        xaDs.applyLog( xaDs.getLogicalLog( nextVersion ) );
        tx = graphDb.beginTx();
        Assert.assertEquals( 2, node2.getProperty( "test" ) );
        Assert.assertEquals( 2, node2.getSingleRelationship( 
                RelTypes.TEST, Direction.INCOMING ).getProperty( "test" ) );
        tx.finish();

        graphDb.shutdown();
    }
}
