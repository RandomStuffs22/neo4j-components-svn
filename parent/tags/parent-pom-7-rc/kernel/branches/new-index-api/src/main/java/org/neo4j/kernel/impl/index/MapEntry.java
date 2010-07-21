package org.neo4j.kernel.impl.index;

import java.util.Map;

public class MapEntry<K, V> implements Map.Entry<K, V>
{
    private final K key;
    private final V value;

    public MapEntry( K key, V value )
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
