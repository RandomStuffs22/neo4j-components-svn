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

package org.neo4j.kernel.impl.event;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.junit.Test;
import org.neo4j.graphdb.event.ErrorState;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.impl.AbstractNeo4jTestCase;
import org.neo4j.kernel.impl.transaction.XaDataSourceManager;

public class TestKernelPanic
{
    @Test
    public void noobTest() throws Exception
    {
        String path = "target/var/testdb";
        AbstractNeo4jTestCase.deleteFileOrDirectory( new File( path ) );
        EmbeddedGraphDatabase graphDb = new EmbeddedGraphDatabase( path );
        XaDataSourceManager xaDs =
            graphDb.getConfig().getTxModule().getXaDataSourceManager();
        
        IllBehavingXaDataSource noob = new IllBehavingXaDataSource();
        xaDs.registerDataSource( "noob", noob, "554342".getBytes() );
        
        Panic panic = new Panic();
        graphDb.registerKernelEventHandler( panic );
     
        org.neo4j.graphdb.Transaction gdbTx = graphDb.beginTx();
        TransactionManager txMgr = graphDb.getConfig().getTxModule().getTxManager();
        Transaction tx = txMgr.getTransaction();
        
        graphDb.createNode();
        tx.enlistResource( noob.getXaConnection().getXaResource() );
        try
        {
            gdbTx.success();
            gdbTx.finish();
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
        finally
        {
            graphDb.unregisterKernelEventHandler( panic );
        }
        assertTrue( panic.panic );
        
        graphDb.shutdown();
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
