package org.neo4j.graphdb.event;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;


/**
 * Represents a changed property.
 *
 * Instances of this interface represent the property as it is after the
 * transaction when returned from
 * {@link TransactionData#assignedNodeProperties()} or
 * {@link TransactionData#assignedRelationshipProperties()}. Instances of this
 * interface represent the property as it was before the transaction when
 * returned from {@link TransactionData#removedNodeProperties()} or
 * {@link TransactionData#removedRelationshipProperties()}.
 *
 * @author Tobias Ivarsson
 *
 * @param <T> The type of the entity the property belongs to, either
 *            {@link Node} or {@link Relationship}.
 */
public interface PropertyEntry<T extends PropertyContainer>
{
    /**
     * Get the entity that this property was modified on. The entity is either a
     * {@link Node} or a {@link Relationship}, depending on the generic type of
     * this instance.
     *
     * @return the {@link Node} or {@link Relationship} that the property was
     *         modified on.
     */
    T entity();

    /**
     * Get the key of the modified property.
     *
     * @return the key of the modified property.
     */
    String key();

    /**
     * Get the value of the modified property. If this {@link PropertyEntry} was
     * returned from {@link TransactionData#assignedNodeProperties()} or
     * {@link TransactionData#assignedRelationshipProperties()}, the value
     * returned from this method is the value that will be assigned to the
     * property after the transaction is committed. If this
     * {@link PropertyEntry} was returned from
     * {@link TransactionData#removedNodeProperties()} or
     * {@link TransactionData#removedRelationshipProperties()}, the value
     * returned from this method is the value that was stored at this property
     * before the transaction started.
     * 
     * @return The value of the modified property.
     */
    Object value();
}
