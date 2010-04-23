package org.neo4j.graphdb.event;

import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.impl.transaction.TransactionFailureException;

/**
 * The interface of an event handler for Neo4j Transaction events.
 *
 * @author Tobias Ivarsson
 *
 * @param <T> The type of a state object that the transaction handler can use to
 *            pass information from the {@link #beforeCommit(TransactionData)}
 *            event dispatch method to the
 *            {@link #afterCommit(TransactionData, Object)} or
 *            {@link #afterRollback(TransactionData, Object)} method, depending
 *            on whether the transaction succeeded or failed.
 */
public interface TransactionEventHandler<T>
{
    /**
     * Invoked when a transaction is about to be committed.
     *
     * If this method throws an exception the transaction will be rolled back
     * and a {@link TransactionFailureException} will be thrown from
     * {@link Transaction#finish()}.
     *
     * The transaction is still open when this method is invoked, making it
     * possible to perform mutating operations in this method. This is however
     * highly discouraged. Changes made in this method are not guaranteed to be
     * visible by this or other {@link TransactionEventHandler}s.
     *
     * @param data the changes that will be committed in this transaction.
     * @return a state object (or <code>null</code>) that will be passed on to
     *         {@link #afterCommit(TransactionData, Object)} or
     *         {@link #afterRollback(TransactionData, Object)} of this object.
     * @throws Exception to indicate that the transaction should be rolled back.
     */
    T beforeCommit( TransactionData data ) throws Exception;

    /**
     * Invoked after the transaction has been committed successfully.
     * 
     * @param data the changes that were committed in this transaction.
     * @param state the object returned by
     *            {@link #beforeCommit(TransactionData)}.
     */
    void afterCommit( TransactionData data, T state );

    /**
     * Invoked after the transaction has been rolled back if committing the
     * transaction failed for some reason.
     *
     * @param data the changes that were committed in this transaction.
     * @param state the object returned by
     *            {@link #beforeCommit(TransactionData)}.
     */
    // TODO: should this method take a parameter describing WHY the tx failed?
    void afterRollback( TransactionData data, T state );
}
