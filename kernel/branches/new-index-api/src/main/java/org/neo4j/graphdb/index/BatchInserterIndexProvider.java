package org.neo4j.graphdb.index;

public interface BatchInserterIndexProvider
{
    BatchInserterIndex nodeIndex( String indexName );
    
    BatchInserterIndex relationshipIndex( String indexName );
}
