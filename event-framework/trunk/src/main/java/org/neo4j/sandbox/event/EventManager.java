package org.neo4j.sandbox.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;

public class EventManager
{
	private ConcurrentMap<EventListener, EventFilter> reactivePostCommitted = new ConcurrentHashMap<EventListener, EventFilter>();
	private ConcurrentMap<CancelableEventListener, EventFilter> proactivePreCommitted = new ConcurrentHashMap<CancelableEventListener, EventFilter>();

	private EventManager( EmbeddedNeo neo )
	{
		new EventFrameworkImplementationBridge( this ).register( neo
		    .getConfig().getEventModule().getEventManager() );
	}

	public void registerProactivePreCommittedEventListener(
	    CancelableEventListener listener, EventFilter filter )
	{
		EventFilter oldFilter = proactivePreCommitted.putIfAbsent( listener,
		    filter );
		if ( oldFilter != null )
		{
			// TODO: already registered, but with another filter, what to do?
			// throw an exception?
			// replace the filter?
			// combine the filters?
			// - if so, how should it be combined? least or most restricting?
			// do nothing?
		}
	}

	public void unregisterProactivePreCommittedEventListener(
	    CancelableEventListener listener )
	{
		proactivePreCommitted.remove( listener );
	}

	public void registerReactivePostCommittedEventListener(
	    EventListener listener, EventFilter filter )
	{
		EventFilter oldFilter = reactivePostCommitted.putIfAbsent( listener,
		    filter );
		if ( oldFilter != null )
		{
			// TODO: already registered, but with another filter, what to do?
			// see above for suggestions...
		}
	}

	public void unregisterReactivePostCommittedEventListener(
	    EventListener listener )
	{
		reactivePostCommitted.remove( listener );
	}

	private static ConcurrentMap<NeoService, EventManager> managers = new ConcurrentHashMap<NeoService, EventManager>();
	private static Lock creationLock = new ReentrantLock();

	public static EventManager getEventManagerFor( NeoService neo )
	{
		EventManager manager = managers.get( neo );
		if ( null == manager )
		{
			creationLock.lock();
			try
			{
				if ( null == ( manager = managers.get( neo ) ) )
				{
					managers.put( neo, manager = new EventManager(
					    ( EmbeddedNeo ) neo ) );
				}
			}
			finally
			{
				creationLock.unlock();
			}
		}
		return manager;
	}

	boolean fireCancelableEvent( Event event, EventData data )
	{
		CancellationStatus status = new CancellationStatus();
		for ( Map.Entry<CancelableEventListener, EventFilter> pair : proactivePreCommitted
		    .entrySet() )
		{
			if ( pair.getValue().accept( event, data, status ) )
			{
				pair.getKey().handleCancelableEvent( event, data, status );
			}
		}
		return status.isCanceled();
	}

	void fireEvent( Event event, EventData data )
	{
		for ( Map.Entry<EventListener, EventFilter> pair : reactivePostCommitted
		    .entrySet() )
		{
			if ( pair.getValue().accept( event, data, null ) )
			{
				pair.getKey().eventReceived( event, data );
			}
		}
	}
}
