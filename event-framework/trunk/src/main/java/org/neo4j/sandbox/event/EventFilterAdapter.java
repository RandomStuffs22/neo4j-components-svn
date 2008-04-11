package org.neo4j.sandbox.event;

public abstract class EventFilterAdapter implements EventFilter
{
	@Override
	public final boolean accept( Event event, EventData data,
	    CancellationStatus status )
	{
		return acceptStatus( status ) && acceptEvent( event, data );
	}

	protected boolean acceptStatus( CancellationStatus status )
    {
	    return status == null || !status.isCanceled();
    }

	protected abstract boolean acceptEvent( Event event, EventData data );
}
