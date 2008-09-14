package org.neo4j.weaver.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that a method returns the start node of a relationship. Only used
 * on classes representing a relationship.
 * 
 * @author Magnus Robertsson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface StartNode {

}
