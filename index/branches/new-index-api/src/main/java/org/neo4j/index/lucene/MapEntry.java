package org.neo4j.index.lucene;

import java.util.Map;

class MapEntry<K, V> implements Map.Entry<K, V>
{
    private final K key;
    private final V value;

    MapEntry( K key, V value )
    {
        this.key = key;
        this.value = value;
    }
    
    public K getKey()
    {
        return key;
    }

    public V getValue()
    {
        return value;
    }

    public V setValue( V value )
    {
        throw new UnsupportedOperationException();
    }
}
