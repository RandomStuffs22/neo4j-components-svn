package org.neo4j.weaver.api;

import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;

/**
 * Creates Neo persistent objects from domain interfaces. Instead of
 * implementing your domain interfaces by hand you can let the
 * DomainObjectFactory create the implementations at runtime.
 * 
 * So instead of writing:
 *
 * <pre>
 * Person person = new PersonImpl(underlyingNode);
 * </pre>
 * You write:
 * 
 * <pre>
 * Person person = factory.create(underlyingNode, Person.class);
 * </pre>
 * 
 * As you see there is no need for the PersonImpl class. If you need service
 * methods in your domain interface you can mixin the methods by using an
 * abstract class:
 * 
 * <pre>
 * public abstract class PersonMixin implements Person {
 *     @Override
 *     public String getFullName() {
 *         return getFirstName() + " " + getLastName();
 *     }
 * }
 * </pre>
 * 
 * The DomainObjectFactory need to be aware of the mixins to be able to instantiate
 * them. To let the DomainObjectFactory be aware of your mixins you can either do
 * this at start of your application by using #addMixin(Class<?>,Class<?>)
 * or by adding an annotation to your domain interface:
 * 
 * <pre>
 * @Mixin(PersonMixin.class)
 * public interface Person {
 *     String getFirstName();
 *     String getLastName();
 *     String getFullName();
 * }
 * </pre>
 * 
 * The latter creates a string relationship between the domain interface and
 * the mixin which, of course, if not ideal. Room for improvement here!
 * 
 * @author Magnus Robertsson
 */
public interface DomainObjectFactory {
	/**
	 * Creates a new object with a new underlying node object.
	 * 
	 * @param <T> Any (non final) class or interface.
	 * @param type
	 * @return A proxy object of the specified type.
	 */
	<T> T create(Class<T> type);

	/**
	 * Creates a new object from the specified underlying node.
	 * 
	 * @param <T>				Any (non final) class or interface.
	 * @param underlyingNode 	The node to wrap.
	 * @param type				Any (non final) class or interface.
	 * @return A proxy object of the specified type.
	 */
	<T> T create(Node underlyingNode, Class<T> type);

	/**
	 * Creates a new object from the specified underlying relationship.
	 * 
	 * @param <T>						Any (non final) class or interface.
	 * @param underlyingRelationship	The relationship to wrap.
	 * @param type						Any (non final) class or interface.
	 * @return A proxy object of the specified type.
	 */
	<T> T create(Relationship underlyingRelationship, Class<T> type);

	/**
	 * Returns the underlying node for the specified object.
	 * 
	 * @param obj A proxy object to get the node from.
	 * @return The underlying node.
	 */
	Node getUnderlyingNode(Object obj);
}