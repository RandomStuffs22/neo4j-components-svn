package org.neo4j.sandbox.event;

public interface EventFilter
{
	boolean accept( Event event, EventData data, CancellationStatus status );
}
