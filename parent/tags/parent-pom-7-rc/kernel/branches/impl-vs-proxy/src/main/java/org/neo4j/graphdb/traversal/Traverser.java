package org.neo4j.graphdb.traversal;

import java.util.Iterator;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

/**
 * The interface which represents the traverser which is used to step through
 * the results of a traversal. Each step can be represented in different ways.
 * The default is as {@link Position} objects which all over representations
 * can be derived from, i.e {@link Node}, {@link Relationship}, {@link Path}.
 * Or each step can be represented in one of those representations directly.
 */
public interface Traverser extends Iterable<Position>
{
    /**
     * Represents the traversal in the form of {@link Node}s. This is a
     * convenient way of iterating over {@link Position}s and getting the
     * {@link Position#node()} for each position.
     * 
     * @return the traversal in the form of {@link Node} objects.
     */
    Iterable<Node> nodes();
    
    /**
     * Represents the traversal in the form of {@link Relationship}s. This is a
     * convenient way of iterating over {@link Position}s and getting the
     * {@link Position#lastRelationship()} for each position.
     * 
     * @return the traversal in the form of {@link Relationship} objects.
     */
    Iterable<Relationship> relationships();
    
    /**
     * Represents the traversal in the form of {@link Path}s. This is a
     * convenient way of iterating over {@link Position}s and getting the
     * {@link Position#path()} for each position.
     * 
     * @return the traversal in the form of {@link Path} objects.
     */
    Iterable<Path> paths();
    
    /**
     * Represents the traversal in the default form, i.e {@link Position}s.
     * @return the iterator of this traversal in the form of {@link Position}s.
     */
    Iterator<Position> iterator();
}