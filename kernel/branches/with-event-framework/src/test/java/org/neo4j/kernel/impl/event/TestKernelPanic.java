package org.neo4j.kernel.impl.event;

import static org.junit.Assert.*;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.junit.Test;
import org.neo4j.graphdb.event.ErrorState;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.impl.AbstractNeo4jTestCase;
import org.neo4j.kernel.impl.transaction.XaDataSourceManager;

public class TestKernelPanic extends AbstractNeo4jTestCase
{
    @Test
    public void noobTest() throws Exception
    {
        EmbeddedGraphDatabase graphDb = getEmbeddedGraphDb();
        
        XaDataSourceManager xaDs =
            graphDb.getConfig().getTxModule().getXaDataSourceManager();
        
        DummyXaDataSource noob = new DummyXaDataSource();
        xaDs.registerDataSource( "noob", noob, "554342".getBytes() );
        
        Panic panic = new Panic();
        graphDb.registerKernelEventHandler( panic );
     
        TransactionManager txMgr = graphDb.getConfig().getTxModule().getTxManager();
        
        Transaction tx = txMgr.getTransaction();
        
        graphDb.createNode();
        tx.enlistResource( noob.getXaConnection().getXaResource() );
        try
        {
            newTransaction();
            fail( "Should fail" );
        }
        catch ( Throwable t )
        {
            // ok
            for ( int i = 0; i < 10 && panic.panic == false; i++ )
            {
                Thread.sleep( 1000 );
            }
        }
        assertTrue( panic.panic );
    }
    
    private static class Panic implements KernelEventHandler
    {
        boolean panic = false;
        
        public void beforeShutdown()
        {
            // TODO Auto-generated method stub
            
        }

        public Object getResource()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public void kernelPanic( ErrorState error )
        {
            panic = true;
        }

        public ExecutionOrder orderComparedTo( KernelEventHandler other )
        {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
}
