package org.neo4j.graphdb.index;

public interface BatchInserterIndex
{
    void add( long entityId, String key, Object value );
    
    void remove( long entityId, String key, Object value );
    
    /**
     * Provides hits for exact matches of key and value.
     */
    IndexHits<Long> get( String key, Object value );
    
    /**
     * Provides hits for fulltext matches of query (implementation
     * specific syntax) or impl. specific query object. The query can in this
     * case query many different keys in the same query.
     */
    
    IndexHits<Long> query( String key, Object queryOrQueryObject );
    
    IndexHits<Long> query( Object queryOrQueryObject );
}
