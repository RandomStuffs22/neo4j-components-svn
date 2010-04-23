package org.neo4j.kernel.event;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.neo4j.graphdb.Transaction;
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
     * This is null at construction time, then populated in beforeCompletion and
     * used in afterCompletion.
     */
    private TransactionData transactionData;
    private final Transaction transaction;

    public TransactionEventsSyncHook(
            NodeManager nodeManager, Transaction transaction,
            SynchronizedWriteSet<TransactionEventHandler<T>> transactionEventHandlers )
    {
        this.nodeManager = nodeManager;
        this.transaction = transaction;
        this.handlers = transactionEventHandlers;
    }

    public void beforeCompletion()
    {
        TransactionData data = null;
        data = nodeManager.getTransactionData();
        for ( TransactionEventHandler<T> handler : this.handlers )
        {
            try
            {
                T state = handler.beforeCommit( data );
                states.add( new HandlerAndState( handler, state ) );
            }
            catch ( Throwable t )
            {
                // TODO
                transaction.failure();
                throw new RuntimeException( t );
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
