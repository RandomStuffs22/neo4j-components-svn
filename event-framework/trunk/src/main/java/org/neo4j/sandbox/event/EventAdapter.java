package org.neo4j.sandbox.event;

import org.neo4j.sandbox.event.EventData.TargetType;

public abstract class EventAdapter implements EventListener,
    CancelableEventListener
{
	@Override
	public final void eventReceived( Event event, EventData data )
	{
		event.dispatchEvent( event, data, this );
	}

	@Override
	public final void handleCancelableEvent( Event event, EventData data,
	    CancellationStatus status )
	{
		event.dispatchCancelableEvent( event, data, status, this );
	}

	public void receiveNodeAdd( TransactionId txId, int targetId )
	{
	}

	public void handleNodeAdd( TransactionId txId, int targetId,
	    CancellationStatus status )
	{
	}

	public void receiveNodeRemove( TransactionId transactionId, int targetId )
	{
	}

	public void handleNodeRemove( TransactionId transactionId, int targetId,
	    CancellationStatus status )
	{
	}

	public void receiveRelationshipAdd( TransactionId transactionId,
	    int targetId )
	{
	}

	public void handleRelationshipAdd( TransactionId transactionId,
	    int targetId, CancellationStatus status )
	{
	}

	public void receiveRelationshipRemove( TransactionId transactionId,
	    int targetId )
	{
	}

	public void handleRelationshipRemove( TransactionId transactionId,
	    int targetId, CancellationStatus status )
	{
	}

	public void receivePropertyAdd( TransactionId transactionId,
	    TargetType targetType, int targetId, String propertyName,
	    Object newPropertyValue )
	{
	}

	public void handlePropertyAdd( TransactionId transactionId,
	    TargetType targetType, int targetId, String propertyName,
	    Object newPropertyValue, CancellationStatus status )
	{
	}

	public void receivePropertyRemove( TransactionId transactionId,
	    TargetType targetType, int targetId, String propertyName,
	    Object oldPropertyValue )
	{
	}

	public void handlePropertyRemove( TransactionId transactionId,
	    TargetType targetType, int targetId, String propertyName,
	    Object oldPropertyValue, CancellationStatus status )
	{
	}

	public void receivePropertyChange( TransactionId transactionId,
	    TargetType targetType, int targetId, String propertyName,
	    Object oldPropertyValue, Object newPropertyValue )
	{
	}

	public void handlePropertyChange( TransactionId transactionId,
	    TargetType targetType, int targetId, String propertyName,
	    Object oldPropertyValue, Object newPropertyValue,
	    CancellationStatus status )
	{
	}

	public void receiveTransactionBegin( TransactionId transactionId )
	{
	}

	public void handleTransactionBegin( TransactionId transactionId )
	{
	}

	public void receiveTransactionCommit( TransactionId transactionId )
	{
	}

	public void handleTransactionCommit( TransactionId transactionId )
	{
	}

	public void receiveTransactionRollback( TransactionId transactionId )
	{
	}

	public void handleTransactionRollback( TransactionId transactionId )
	{
	}
}
