package org.neo4j.graphdb.index;

import org.neo4j.graphdb.PropertyContainer;

public interface Index<T extends PropertyContainer>
{
    void add( T entity, String key, Object value );
    
    void remove( T entity, String key, Object value );
    
    /**
     * Provides hits for exact matches of key and value.
     */
    IndexHits<T> get( String key, Object value );
    
    /**
     * Provides hits for fulltext matches of query (implementation
     * specific syntax) or impl. specific query object. The query can in this
     * case query many different keys in the same query.
     */
    
    IndexHits<T> query( String key, Object queryOrQueryObject );
    
    IndexHits<T> query( Object queryOrQueryObject );
}
