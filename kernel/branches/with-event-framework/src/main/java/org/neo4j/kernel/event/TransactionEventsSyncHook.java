package org.neo4j.kernel.event;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.kernel.impl.core.NodeManager;
import org.neo4j.kernel.impl.util.SynchronizedWriteSet;

public class TransactionEventsSyncHook<T> implements Synchronization
{
    private final SynchronizedWriteSet<TransactionEventHandler<T>> handlers;
    private final List<HandlerAndState> states = new ArrayList<HandlerAndState>();
    private final NodeManager nodeManager;
    
    /**
     * This is null at construction time, then populated in beforeCompletion
     * and used in afterCompletion.
     */
    private TransactionData transactionData;

    public TransactionEventsSyncHook( NodeManager nodeManager,
            SynchronizedWriteSet<TransactionEventHandler<T>> transactionEventHandlers )
    {
        this.nodeManager = nodeManager;
        this.handlers = transactionEventHandlers;
    }

    public void beforeCompletion()
    {
        System.out.println( "beforeCompletion" );
        TransactionData data = null;
        try
        {
            data = nodeManager.getTransactionData();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        for ( TransactionEventHandler<T> handler : this.handlers )
        {
            System.out.println( "passing to " + handler );
            try
            {
                T state = handler.beforeCommit( data );
                states.add( new HandlerAndState( handler, state ) );
            }
            catch ( Exception e )
            {
                // TODO
            }
        }
    }
    
    public void afterCompletion( int status )
    {
        if ( status == Status.STATUS_COMMITTED )
        {
            for ( HandlerAndState state : this.states )
            {
                state.handler.afterCommit( this.transactionData, state.state );
            }
        }
        // TODO
    }
    
    private class HandlerAndState
    {
        private final TransactionEventHandler<T> handler;
        private final T state;
        
        public HandlerAndState( TransactionEventHandler<T> handler, T state )
        {
            this.handler = handler;
            this.state = state;
        }
    }
}
