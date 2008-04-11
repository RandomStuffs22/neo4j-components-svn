package org.neo4j.sandbox.event;

import org.neo4j.impl.event.EventListenerAlreadyRegisteredException;
import org.neo4j.impl.event.EventListenerNotRegisteredException;
import org.neo4j.impl.event.ProActiveEventListener;
import org.neo4j.impl.event.ReActiveEventListener;

final class EventFrameworkImplementationBridge
{
	private static enum Dispatcher
	{
		NODE_ADD_from_NODE_CREATE( Event.NODE_ADD,
		    org.neo4j.impl.event.Event.NODE_CREATE )
		{
			@Override
			EventData convertData( org.neo4j.impl.event.EventData data )
			{
				// TODO Implement translation from the old EventData format
				// to the new format.
				return null;
			}
		},
		NODE_REMOVE_from_NODE_DELETE( Event.NODE_REMOVE,
		    org.neo4j.impl.event.Event.NODE_DELETE )
		{
			@Override
			EventData convertData( org.neo4j.impl.event.EventData data )
			{
				// TODO Implement translation from the old EventData format
				// to the new format.
				return null;
			}
		},
		PROPERTY_ADD_from_NODE_ADD_PROPERTY( Event.PROPERTY_ADD,
		    org.neo4j.impl.event.Event.NODE_ADD_PROPERTY )
		{
			@Override
			EventData convertData( org.neo4j.impl.event.EventData data )
			{
				// TODO Implement translation from the old EventData format
				// to the new format.
				return null;
			}
		},
		PROPERTY_REMOVE_from_NODE_REMOVE_PROPERTY( Event.PROPERTY_REMOVE,
		    org.neo4j.impl.event.Event.NODE_REMOVE_PROPERTY )
		{
			@Override
			EventData convertData( org.neo4j.impl.event.EventData data )
			{
				// TODO Implement translation from the old EventData format
				// to the new format.
				return null;
			}
		},
		PROPERTY_CHANGE_from_NODE_CHANGE_PROPERTY( Event.PROPERTY_CHANGE,
		    org.neo4j.impl.event.Event.NODE_CHANGE_PROPERTY )
		{
			@Override
			EventData convertData( org.neo4j.impl.event.EventData data )
			{
				// TODO Implement translation from the old EventData format
				// to the new format.
				return null;
			}
		},
		RELATIONSHIP_ADD_from_RELATIONSHIP_CREATE( Event.RELATIONSHIP_ADD,
		    org.neo4j.impl.event.Event.RELATIONSHIP_CREATE )
		{
			@Override
			EventData convertData( org.neo4j.impl.event.EventData data )
			{
				// TODO Implement translation from the old EventData format
				// to the new format.
				return null;
			}
		},
		RELATIONSHIP_REMOVE_from_RELATIONSHIP_DELETE(
		    Event.RELATIONSHIP_REMOVE,
		    org.neo4j.impl.event.Event.RELATIONSHIP_DELETE )
		{
			@Override
			EventData convertData( org.neo4j.impl.event.EventData data )
			{
				// TODO Implement translation from the old EventData format
				// to the new format.
				return null;
			}
		},
		PROPERTY_ADD_from_RELATIONSHIP_ADD_PROPERTY( Event.PROPERTY_ADD,
		    org.neo4j.impl.event.Event.RELATIONSHIP_ADD_PROPERTY )
		{
			@Override
			EventData convertData( org.neo4j.impl.event.EventData data )
			{
				// TODO Implement translation from the old EventData format
				// to the new format.
				return null;
			}
		},
		PROPERTY_REMOVE_from_RELATIONSHIP_REMOVE_PROPERTY(
		    Event.PROPERTY_REMOVE,
		    org.neo4j.impl.event.Event.RELATIONSHIP_REMOVE_PROPERTY )
		{
			@Override
			EventData convertData( org.neo4j.impl.event.EventData data )
			{
				// TODO Implement translation from the old EventData format
				// to the new format.
				return null;
			}
		},
		PROPERTY_CHANGE_from_RELATIONSHIP_CHANGE_PROPERTY(
		    Event.PROPERTY_CHANGE,
		    org.neo4j.impl.event.Event.RELATIONSHIP_CHANGE_PROPERTY )
		{
			@Override
			EventData convertData( org.neo4j.impl.event.EventData data )
			{
				// TODO Implement translation from the old EventData format
				// to the new format.
				return null;
			}
		},
		TX_BEGIN_from_TX_BEGIN( Event.TX_BEGIN,
		    org.neo4j.impl.event.Event.TX_BEGIN )
		{
			@Override
			EventData convertData( org.neo4j.impl.event.EventData data )
			{
				// TODO Implement translation from the old EventData format
				// to the new format.
				return null;
			}
		},
		TX_COMMIT_from_TX_COMMIT( Event.TX_COMMIT,
		    org.neo4j.impl.event.Event.TX_COMMIT )
		{
			@Override
			EventData convertData( org.neo4j.impl.event.EventData data )
			{
				// TODO Implement translation from the old EventData format
				// to the new format.
				return null;
			}
		},
		TX_ROLLBACK_from_TX_ROLLBACK( Event.TX_ROLLBACK,
		    org.neo4j.impl.event.Event.TX_ROLLBACK )
		{
			@Override
			EventData convertData( org.neo4j.impl.event.EventData data )
			{
				// TODO Implement translation from the old EventData format
				// to the new format.
				return null;
			}
		};
		private final Event newType;
		private final org.neo4j.impl.event.Event oldType;

		private Dispatcher( Event newType, org.neo4j.impl.event.Event oldType )
		{
			this.newType = newType;
			this.oldType = oldType;
		}

		void register( org.neo4j.impl.event.EventManager oldManager,
		    EventManager newManager )
		    throws EventListenerAlreadyRegisteredException,
		    EventListenerNotRegisteredException
		{
			Forwarder handler = new Forwarder( this, newManager );
			oldManager.registerProActiveEventListener( handler, oldType );
			oldManager.registerReActiveEventListener( handler, oldType );
		}

		boolean fireProActive( EventManager manager,
		    org.neo4j.impl.event.EventData data )
		{
			return manager.fireCancelableEvent( newType, convertData( data ) );
		}

		void fireReActive( EventManager manager,
		    org.neo4j.impl.event.EventData data )
		{
			manager.fireEvent( newType, convertData( data ) );
		}

		abstract EventData convertData( org.neo4j.impl.event.EventData data );
	}
	private static class Forwarder implements ProActiveEventListener,
	    ReActiveEventListener
	{
		private final Dispatcher dispatch;
		private final EventManager manger;

		Forwarder( Dispatcher dispatch, EventManager manager )
		{
			this.dispatch = dispatch;
			this.manger = manager;
		}

		@Override
		public boolean proActiveEventReceived(
		    org.neo4j.impl.event.Event event,
		    org.neo4j.impl.event.EventData data )
		{
			return dispatch.fireProActive( manger, data );
		}

		@Override
		public void reActiveEventReceived( org.neo4j.impl.event.Event event,
		    org.neo4j.impl.event.EventData data )
		{
			dispatch.fireReActive( manger, data );
		}
	}

	private final EventManager manager;

	public EventFrameworkImplementationBridge( EventManager manager )
	{
		this.manager = manager;
	}

	void register( org.neo4j.impl.event.EventManager manager )
	{
		for ( Dispatcher dispatch : Dispatcher.values() )
		{
			try
			{
				dispatch.register( manager, this.manager );
			}
			catch ( EventListenerAlreadyRegisteredException e )
			{
				throw new RuntimeException( "This should never happen.", e );
			}
			catch ( EventListenerNotRegisteredException e )
			{
				throw new RuntimeException( "Could not create event bridge.", e );
			}
		}
	}
}
