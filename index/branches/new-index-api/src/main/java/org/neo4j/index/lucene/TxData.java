package org.neo4j.index.lucene;

import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.Query;

abstract class TxData
{
    final LuceneIndex index;
    
    TxData( LuceneIndex index )
    {
        this.index = index;
    }

    abstract TxData add( Long entityId, String key, Object value );
    
    /**
     * Only for the {@link TxData} representing removal.
     */
    abstract TxData add( Query query );

    abstract TxData remove( Long entityId, String key, Object value );

    abstract TxData remove( Query query );

    abstract Map.Entry<Set<Long>, TxData> query( Query query );

    abstract Map.Entry<Set<Long>, TxData> get( String key, Object value );
    
    abstract void close();

    abstract Query getExtraQuery();
}
