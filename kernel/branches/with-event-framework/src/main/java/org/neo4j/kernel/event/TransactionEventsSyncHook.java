package org.neo4j.kernel.event;

import java.util.HashMap;
import java.util.Map;

import javax.transaction.Synchronization;

import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.kernel.impl.util.SynchronizedWriteSet;

public class TransactionEventsSyncHook<T> implements Synchronization
{
    private final SynchronizedWriteSet<TransactionEventHandler<T>> handlers;
    private final Map<TransactionEventHandler<T>, T> stateObjects =
            new HashMap<TransactionEventHandler<T>, T>();

    public TransactionEventsSyncHook(
            SynchronizedWriteSet<TransactionEventHandler<T>> transactionEventHandlers )
    {
        this.handlers = transactionEventHandlers;
    }

    
    public void beforeCompletion()
    {
        TransactionData data = null;
        for ( TransactionEventHandler<T> handler : this.handlers )
        {
            try
            {
                T state = handler.beforeCommit( data );
                stateObjects.put( handler, state );
            }
            catch ( Exception e )
            {
                // TODO
            }
        }
    }
    
    public void afterCompletion( int status )
    {
    }
}
