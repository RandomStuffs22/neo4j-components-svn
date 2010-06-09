package org.neo4j.index.lucene;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads/stores index config from a file associated with the lucene index
 * provider. If an index exists its config is read from it, else from neo4j
 * config parameters.
 * 
 * Store in some weird binary format so that people generally don't want to
 * tamper with it.
 */
class IndexConfig
{
    private final Map<String, Map<String, String>> config;
    private final Map<String, String> defaultParams;
    
    private IndexConfig( Map<String, Map<String, String>> readConfig,
            Map<String, String> defaultParams )
    {
        this.config = readConfig;
        this.defaultParams = defaultParams;
    }
    
    static IndexConfig load( String baseStorePath, Map<String, String> defaultParams )
    {
        // TODO
        return null;
    }
    
    void store()
    {
        // TODO
    }
    
    void set( String indexName, String key, String value )
    {
        inner( indexName ).put( key, value );
    }
    
    String get( String indexName, String key, String defaultValue )
    {
        String value = inner( indexName ).get( key );
        if ( value != null )
        {
            return value;
        }
        value = getFromDefault( indexName, key );
        return value != null ? value : defaultValue;
    }
    
    private String getFromDefault( String indexName, String key )
    {
        String paramsKey = "index." + indexName + "." + key;
        return defaultParams.get( paramsKey );
    }

    Map<String, String> getAll( String indexName )
    {
        Map<String, String> map = inner( indexName );
        return map == null ? Collections.<String, String>emptyMap() :
                Collections.unmodifiableMap( map );
    }

    private Map<String, String> inner( String indexName )
    {
        Map<String, String> map = config.get( indexName );
        if ( map == null )
        {
            map = new HashMap<String, String>();
            config.put( indexName, map );
        }
        return map;
    }
}
