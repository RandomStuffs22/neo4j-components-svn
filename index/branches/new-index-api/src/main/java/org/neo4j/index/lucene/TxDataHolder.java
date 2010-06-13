package org.neo4j.index.lucene;

import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.Query;

class TxDataHolder
{
    final LuceneIndex index;
    private TxData data;
    
    TxDataHolder( LuceneIndex index, TxData initialData )
    {
        this.index = index;
        this.data = initialData;
    }

    void add( Long entityId, String key, Object value )
    {
        this.data = this.data.add( entityId, key, value );
    }
    
    /**
     * Only for the tx data representing removal.
     */
    void add( Query query )
    {
        this.data = this.data.add( query );
    }

    void remove( Long entityId, String key, Object value )
    {
        this.data = this.data.remove( entityId, key, value );
    }

    void remove( Query query )
    {
        this.data = this.data.remove( query );
    }

    Set<Long> getEntityIds( Query query )
    {
        Map.Entry<Set<Long>, TxData> entry = this.data.getEntityIds( query );
        this.data = entry.getValue();
        return entry.getKey();
    }

    Set<Long> getEntityIds( String key, Object value )
    {
        Map.Entry<Set<Long>, TxData> entry = this.data.getEntityIds( key, value );
        this.data = entry.getValue();
        return entry.getKey();
    }
    
    void close()
    {
        this.data.close();
    }

    Query getExtraQuery()
    {
        return this.data.getExtraQuery();
    }
}
