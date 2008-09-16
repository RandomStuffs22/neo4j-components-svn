package org.neo4j.weaver.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.neo4j.api.core.Direction;

/**
 * Specifies that a method should return basic search method relationship
 * 
 * @author Magnus Robertsson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FindRelationship {
	// TODO Get rid of this soft dependency
	// We can't use interface in annotations and we can't use application specific enums such as RelTypes
	// Tricky, indeed... Could at least be improved by checking that the enumeration exists in the
	// application specific enum.
	String type();
	//org.neo4j.imdb.domain.RelTypes type();
	Direction direction();
}
