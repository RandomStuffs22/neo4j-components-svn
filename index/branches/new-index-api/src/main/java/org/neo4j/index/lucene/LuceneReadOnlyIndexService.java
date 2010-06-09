package org.neo4j.index.lucene;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.index.ReadOnlyIndexException;

public class LuceneReadOnlyIndexService extends LuceneIndexService
{
    public LuceneReadOnlyIndexService( GraphDatabaseService graphDb )
    {
        super( graphDb );
    }
    
    @Override
    public void index( Node node, String key, Object value )
    {
        throw new ReadOnlyIndexException();
    }
    
    @Override
    public void removeIndex( Node node, String key, Object value )
    {
        throw new ReadOnlyIndexException();
    }
    
    @Override
    public void removeIndex( Node node, String key )
    {
        throw new ReadOnlyIndexException();
    }
    
    @Override
    public void removeIndex( String key )
    {
        throw new ReadOnlyIndexException();
    }
}
