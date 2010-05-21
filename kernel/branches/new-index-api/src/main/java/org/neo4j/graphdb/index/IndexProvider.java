package org.neo4j.graphdb.index;

import org.neo4j.commons.Service;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public abstract class IndexProvider extends Service
{
    protected IndexProvider( String key, String... altKeys )
    {
        super( key, altKeys );
    }

    public abstract Index<Node> nodeIndex( String indexName );
    
    public abstract Index<Relationship> relationshipIndex( String indexName );
}
