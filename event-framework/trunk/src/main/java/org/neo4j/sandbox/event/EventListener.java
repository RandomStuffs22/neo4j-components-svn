package org.neo4j.sandbox.event;

public interface EventListener
{
	void eventReceived( Event event, EventData data );
}
