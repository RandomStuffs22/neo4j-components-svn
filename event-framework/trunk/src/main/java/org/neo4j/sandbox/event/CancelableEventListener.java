package org.neo4j.sandbox.event;

public interface CancelableEventListener
{
	void handleCancelableEvent( Event event, EventData data,
	    CancellationStatus status );
}
