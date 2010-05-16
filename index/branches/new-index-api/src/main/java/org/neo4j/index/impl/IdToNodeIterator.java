package org.neo4j.index.impl;

import java.util.Iterator;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

/**
 * Converts an Iterator<Long> of node ids to an Iterator<Node> where the
 * {@link GraphDatabaseService#getNodeById(long)} is used to look up the nodes,
 * one call per step in the iterator.
 */
public class IdToNodeIterator extends IdToEntityIterator<Node>
{
    private final GraphDatabaseService graphDb;
    
    /**
     * @param ids the node ids to use as underlying iterator.
     * @param graphDb the {@link GraphDatabaseService} to use for node lookups.
     */
    public IdToNodeIterator( Iterator<Long> ids, GraphDatabaseService graphDb )
    {
        super( ids );
        this.graphDb = graphDb;
    }

    @Override
    protected Node underlyingObjectToObject( Long id )
    {
        return graphDb.getNodeById( id );
    }
}
