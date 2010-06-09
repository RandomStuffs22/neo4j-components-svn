package org.neo4j.index.lucene;

import org.neo4j.commons.iterator.IteratorUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.IndexService;

public class LuceneIndexService implements IndexService
{
    public static final int DEFAULT_LAZY_THRESHOLD = LuceneIndexProvider.DEFAULT_LAZY_THRESHOLD;
    
    private final GraphDatabaseService graphDb;

    public LuceneIndexService( GraphDatabaseService graphDb )
    {
        this.graphDb = graphDb;
    }
    
    protected LuceneIndex<Node> getIndex( String key )
    {
        return (LuceneIndex<Node>) this.graphDb.nodeIndex( key );
    }

    public IndexHits<Node> getNodes( String key, Object value )
    {
        return getIndex( key ).get( key, value );
    }

    public IndexHits<Node> getNodesExactMatch( String key, Object value )
    {
        return getIndex( key ).get( key, value );
    }
    
    public Node getSingleNode( String key, Object value )
    {
        return single( getNodes( key, value ) );
    }
    
    private Node single( IndexHits<Node> hits )
    {
        try
        {
            return IteratorUtil.singleValueOrNull( hits );
        }
        finally
        {
            hits.close();
        }
    }
    
    public Node getSingleNodeExactMatch( String key, Object value )
    {
        return single( getNodesExactMatch( key, value ) );
    }

    public void index( Node node, String key, Object value )
    {
        getIndex( key ).add( node, key, value );
    }

    public void removeIndex( Node node, String key, Object value )
    {
        getIndex( key ).remove( node, key, value );
    }

    public void removeIndex( Node node, String key )
    {
        getIndex( key ).remove( node, "\"" + key + "\"" );
    }

    public void removeIndex( String key )
    {
        getIndex( key ).remove( "\"" + key + "\"" );
    }
    
    public void enableCache( String key, int size )
    {
        getIndex( key ).enableCache( size );
    }
    
    public int getEnabledCacheSize( String key )
    {
        return getIndex( key ).getCacheSize();
    }

    public void shutdown()
    {
    }
}
