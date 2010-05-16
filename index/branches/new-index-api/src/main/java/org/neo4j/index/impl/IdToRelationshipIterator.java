package org.neo4j.index.impl;

import java.util.Iterator;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;

public class IdToRelationshipIterator extends IdToEntityIterator<Relationship>
{
    private final GraphDatabaseService graphDb;

    public IdToRelationshipIterator( Iterator<Long> ids,
            GraphDatabaseService graphDb )
    {
        super( ids );
        this.graphDb = graphDb;
    }

    @Override
    protected Relationship underlyingObjectToObject( Long id )
    {
        return graphDb.getRelationshipById( id );
    }
}
