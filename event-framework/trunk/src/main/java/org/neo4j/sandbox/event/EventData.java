package org.neo4j.sandbox.event;

import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.PropertyContainer;
import org.neo4j.api.core.Relationship;

/**
 * An object that holds the data associated with an event.
 */
public abstract class EventData
{
	/**
	 * @return the id of the transaction that this event was part of.
	 */
	public final TransactionId getTransactionId()
	{
		return txId;
	}

	/**
	 * @return the target type for the event, Node or Relationship.
	 * @throws UnsupportedOperationException
	 *             for data objects associated with transaction events.
	 */
	public TargetType getTargetType()
	{
		return type;
	}

	/**
	 * @return the id of the target Node or Relationship.
	 * @throws UnsupportedOperationException
	 *             for data objects associated with transaction events.
	 */
	public int getTargetId()
	{
		return target;
	}

	/**
	 * @return the key of the property this event was related to.
	 * @throws UnsupportedOperationException
	 *             if this event was not a property related event.
	 */
	public abstract String getPropertyKey();

	/**
	 * @return the new value of the property this event was related to.
	 * @throws UnsupportedOperationException
	 *             if this event was not a property related event, or if this
	 *             event was a {@link Event#PROPERTY_REMOVE remove} event.
	 */
	public abstract Object getNewPropertyValue();

	/**
	 * @return the previous value of the property this event was related to.
	 * @throws UnsupportedOperationException
	 *             if this event was not a property related event, or if this
	 *             event was an {@link Event#PROPERTY_ADD add} event.
	 */
	public abstract Object getOldPropertyValue();

	/**
	 * A representation of the type of the target for an event, {@link Node} or
	 * {@link Relationship}.
	 */
	public static enum TargetType
	{
		/**
		 * Represents a {@link Node} target.
		 */
		NODE
		{
			@Override
			public PropertyContainer get( NeoService neo, int id )
			{
				return neo.getNodeById( id );
			}
		},
		/**
		 * Represents a {@link Relationship} target.
		 */
		RELATIONSHIP
		{
			@Override
			public PropertyContainer get( NeoService neo, int id )
			{
				return neo.getRelationshipById( id );
			}
		};
		/**
		 * A method for getting the object of the type that this represents from
		 * the underlying store.
		 * @param neo
		 *            The store from where to get the object.
		 * @param id
		 *            The identifier for the object.
		 * @return The {@link Node} or {@link Relationship} with the given id
		 *         from the underlying store.
		 */
		public abstract PropertyContainer get( NeoService neo, int id );

		private UnsupportedOperationException noProperty()
		{
			String name = "";
			switch ( this )
			{
				case NODE:
					name = "Node";
					break;
				case RELATIONSHIP:
					name = "Ralationship";
					break;
			}
			return EventData.noProperty( name );
		}
	}

	// Implementation
	private final TransactionId txId;
	final TargetType type;
	private final int target;

	private EventData( TransactionId txId, TargetType type, int target )
	{
		this.txId = txId;
		this.type = type;
		this.target = target;
	}

	static final class TransactionData extends EventData
	{
		TransactionData( TransactionId txId )
		{
			super( txId, null, 0 );
		}

		@Override
		public Object getNewPropertyValue()
		{
			throw noProperty( "Transaction" );
		}

		@Override
		public Object getOldPropertyValue()
		{
			throw noProperty( "Transaction" );
		}

		@Override
		public String getPropertyKey()
		{
			throw noProperty( "Transaction" );
		}

		@Override
		public int getTargetId()
		{
			throw noTarget();
		}

		@Override
		public TargetType getTargetType()
		{
			throw noTarget();
		}

		private static UnsupportedOperationException noTarget()
		{
			return new UnsupportedOperationException(
			    "Transaction events don't have associated targets." );
		}
	}
	static final class PropertyData extends EventData
	{
		private final String key;
		private final Object before;
		private final Object after;

		PropertyData( TransactionId txId, TargetType type, int target,
		    String key, Object before, Object after )
		{
			super( txId, type, target );
			this.key = key;
			this.before = before;
			this.after = after;
		}

		@Override
		public Object getNewPropertyValue()
		{
			if ( after != null )
			{
				return after;
			}
			else
			{
				throw new UnsupportedOperationException(
				    "Cannot get the new value of a removed property." );
			}
		}

		@Override
		public Object getOldPropertyValue()
		{
			if ( before != null )
			{
				return before;
			}
			else
			{
				throw new UnsupportedOperationException(
				    "Cannot get the old value of an added property." );
			}
		}

		@Override
		public String getPropertyKey()
		{
			return key;
		}
	}
	static final class ElementData extends EventData
	{
		ElementData( TransactionId txId, TargetType type, int target )
		{
			super( txId, type, target );
		}

		@Override
		public Object getNewPropertyValue()
		{
			throw type.noProperty();
		}

		@Override
		public Object getOldPropertyValue()
		{
			throw type.noProperty();
		}

		@Override
		public String getPropertyKey()
		{
			throw type.noProperty();
		}
	}

	private static UnsupportedOperationException noProperty( String type )
	{
		return new UnsupportedOperationException( type
		    + " events don't have associated property data." );
	}
}
