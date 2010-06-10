package org.neo4j.graphdb.index;

import java.util.Map;

import org.neo4j.commons.Service;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public abstract class IndexProvider extends Service
{
    protected IndexProvider( String key, String... altKeys )
    {
        super( key, altKeys );
    }

    public abstract Index<Node> nodeIndex( String indexName, Map<String, String> config );
    
    public abstract Index<Relationship> relationshipIndex( String indexName,
            Map<String, String> config );
}
