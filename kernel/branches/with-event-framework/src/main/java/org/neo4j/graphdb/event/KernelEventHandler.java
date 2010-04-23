package org.neo4j.graphdb.event;

import org.neo4j.graphdb.GraphDatabaseService;

/**
 * The interface of an event handler for Neo4j Kernel life cycle events.
 *
 * @author Tobias Ivarsson
 */
public interface KernelEventHandler
{
    /**
     * This is invoked during the shutdown process of a Neo4j Graph Database. It
     * is invoked while the {@link GraphDatabaseService} is still in an
     * operating state, after the processing of this event has terminated the
     * Neo4j Graph Database will terminate. This event can be used to shut down
     * other services that depend on the {@link GraphDatabaseService}.
     */
    void beforeShutdown();

    /**
     * This is invoked when the Neo4j Graph Database enters a state from which
     * it cannot continue.
     *
     * @param error an object describing the state that the
     *            {@link GraphDatabaseService} failed to recover from.
     */
    void kernelPanic( ErrorState error );
}
