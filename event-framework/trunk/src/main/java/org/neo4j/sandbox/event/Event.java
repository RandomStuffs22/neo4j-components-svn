package org.neo4j.sandbox.event;

/**
 * Enum representing the different events that can be fired from Neo.
 */
public enum Event
{
	/**
	 * 
	 */
	NODE_ADD
	{
		@Override
		void dispatchEvent( Event event, EventData data, EventAdapter receiver )
		{
			receiver.receiveNodeAdd( data.getTransactionId(), data
			    .getTargetId() );
		}

		@Override
		void dispatchCancelableEvent( Event event, EventData data,
		    CancellationStatus status, EventAdapter receiver )
		{
			receiver.handleNodeAdd( data.getTransactionId(),
			    data.getTargetId(), status );
		}
	},
	/**
	 * 
	 */
	NODE_REMOVE
	{
		@Override
		void dispatchEvent( Event event, EventData data, EventAdapter receiver )
		{
			receiver.receiveNodeRemove( data.getTransactionId(), data
			    .getTargetId() );
		}

		@Override
		void dispatchCancelableEvent( Event event, EventData data,
		    CancellationStatus status, EventAdapter receiver )
		{
			receiver.handleNodeRemove( data.getTransactionId(), data
			    .getTargetId(), status );
		}
	},
	/**
	 * 
	 */
	RELATIONSHIP_ADD
	{
		@Override
		void dispatchEvent( Event event, EventData data, EventAdapter receiver )
		{
			receiver.receiveRelationshipAdd( data.getTransactionId(), data
			    .getTargetId() );
		}

		@Override
		void dispatchCancelableEvent( Event event, EventData data,
		    CancellationStatus status, EventAdapter receiver )
		{
			receiver.handleRelationshipAdd( data.getTransactionId(), data
			    .getTargetId(), status );
		}
	},
	/**
	 * 
	 */
	RELATIONSHIP_REMOVE
	{
		@Override
		void dispatchEvent( Event event, EventData data, EventAdapter receiver )
		{
			receiver.receiveRelationshipRemove( data.getTransactionId(), data
			    .getTargetId() );
		}

		@Override
		void dispatchCancelableEvent( Event event, EventData data,
		    CancellationStatus status, EventAdapter receiver )
		{
			receiver.handleRelationshipRemove( data.getTransactionId(), data
			    .getTargetId(), status );
		}
	},
	/**
	 * 
	 */
	PROPERTY_ADD
	{
		@Override
		void dispatchEvent( Event event, EventData data, EventAdapter receiver )
		{
			receiver.receivePropertyAdd( data.getTransactionId(), data
			    .getTargetType(), data.getTargetId(), data.getPropertyKey(),
			    data.getNewPropertyValue() );
		}

		@Override
		void dispatchCancelableEvent( Event event, EventData data,
		    CancellationStatus status, EventAdapter receiver )
		{
			receiver.handlePropertyAdd( data.getTransactionId(), data
			    .getTargetType(), data.getTargetId(), data.getPropertyKey(),
			    data.getNewPropertyValue(), status );
		}
	},
	/**
	 * 
	 */
	PROPERTY_REMOVE
	{
		@Override
		void dispatchEvent( Event event, EventData data, EventAdapter receiver )
		{
			receiver.receivePropertyRemove( data.getTransactionId(), data
			    .getTargetType(), data.getTargetId(), data.getPropertyKey(),
			    data.getOldPropertyValue() );
		}

		@Override
		void dispatchCancelableEvent( Event event, EventData data,
		    CancellationStatus status, EventAdapter receiver )
		{
			receiver.handlePropertyRemove( data.getTransactionId(), data
			    .getTargetType(), data.getTargetId(), data.getPropertyKey(),
			    data.getOldPropertyValue(), status );
		}
	},
	/**
	 * 
	 */
	PROPERTY_CHANGE
	{
		@Override
		void dispatchEvent( Event event, EventData data, EventAdapter receiver )
		{
			receiver.receivePropertyChange( data.getTransactionId(), data
			    .getTargetType(), data.getTargetId(), data.getPropertyKey(),
			    data.getOldPropertyValue(), data.getNewPropertyValue() );
		}

		@Override
		void dispatchCancelableEvent( Event event, EventData data,
		    CancellationStatus status, EventAdapter receiver )
		{
			receiver.handlePropertyChange( data.getTransactionId(), data
			    .getTargetType(), data.getTargetId(), data.getPropertyKey(),
			    data.getOldPropertyValue(), data.getNewPropertyValue(), status );
		}
	},
	/**
	 * 
	 */
	TX_BEGIN
	{
		@Override
		void dispatchEvent( Event event, EventData data, EventAdapter receiver )
		{
			receiver.receiveTransactionBegin( data.getTransactionId() );
		}

		@Override
		void dispatchCancelableEvent( Event event, EventData data,
		    CancellationStatus status, EventAdapter receiver )
		{
			receiver.handleTransactionBegin( data.getTransactionId() );
		}
	},
	/**
	 * 
	 */
	TX_COMMIT
	{
		@Override
		void dispatchEvent( Event event, EventData data, EventAdapter receiver )
		{
			receiver.receiveTransactionCommit( data.getTransactionId() );
		}

		@Override
		void dispatchCancelableEvent( Event event, EventData data,
		    CancellationStatus status, EventAdapter receiver )
		{
			receiver.handleTransactionCommit( data.getTransactionId() );
		}
	},
	/**
	 * 
	 */
	TX_ROLLBACK
	{
		@Override
		void dispatchEvent( Event event, EventData data, EventAdapter receiver )
		{
			receiver.receiveTransactionRollback( data.getTransactionId() );
		}

		@Override
		void dispatchCancelableEvent( Event event, EventData data,
		    CancellationStatus status, EventAdapter receiver )
		{
			receiver.handleTransactionRollback( data.getTransactionId() );
		}
	};
	abstract void dispatchEvent( Event event, EventData data,
	    EventAdapter receiver );

	abstract void dispatchCancelableEvent( Event event, EventData data,
	    CancellationStatus status, EventAdapter receiver );
}
