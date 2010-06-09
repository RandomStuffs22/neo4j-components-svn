package org.neo4j.index.lucene;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

public class LuceneFulltextQueryIndexService extends
        LuceneFulltextIndexService
{
    public LuceneFulltextQueryIndexService( GraphDatabaseService graphDb )
    {
        super( graphDb );
    }

    @Override
    public IndexHits<Node> getNodes( String key, Object value )
    {
        return getIndex( key ).query( key, value );
    }
}
