package org.neo4j.index.lucene;

import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;

abstract class TxData
{
    final LuceneIndex index;
    
    TxData( LuceneIndex index )
    {
        this.index = index;
    }

    abstract TxData add( Long entityId, String key, Object value );

    abstract TxData add( Document document );

    abstract TxData remove( Long entityId, String key, Object value );

    abstract TxData remove( Query query );

    abstract Map.Entry<Set<Long>, TxData> getEntityIds( Query query );

    abstract Map.Entry<Set<Long>, TxData> getEntityIds( String key, Object value );
    
    abstract void close();
}
