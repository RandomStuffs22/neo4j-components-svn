package org.neo4j.graphdb.index;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public interface IndexProvider
{
    Index<Node> nodeIndex( String indexName );
    
    Index<Relationship> relationshipIndex( String indexName );
}
