package org.neo4j.shell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a String[] argument from a main-method. It expects values to be either
 * key/value pairs or just "orphan" values (w/o a key associated).
 * <p>
 * A key is defined with one or more dashes in the beginning, for example:
 * 
 * <pre>
 *   '-path'
 *   '--path'
 * </pre>
 * 
 * A key/value pair can be either one single String from the array where there's
 * a '=' delimiter between the key and value, like so:
 * 
 * <pre>
 *   '--path=/my/path/to/something'
 * </pre>
 * ...or consist of two (consecutive) strings from the array, like so:
 * <pre>
 *   '-path' '/my/path/to/something'
 * </pre>
 */
class Args
{
    private final String[] args;
    private final Map<String, String> map = new HashMap<String, String>();
    private final List<String> orphans = new ArrayList<String>();
    
    public Args( String[] args )
    {
        this.args = args;
        parseArgs( args );
    }
    
    public String[] source()
    {
        return this.args;
    }
    
    public Map<String, String> asMap()
    {
        return new HashMap<String, String>( this.map );
    }
    
    public boolean has( String  key )
    {
        return this.map.containsKey( key );
    }
    
    public String get( String key, String defaultValue )
    {
        return this.map.containsKey( key ) ? this.map.get( key ) : defaultValue;
    }
    
    public Number getNumber( String key, Number defaultValue )
    {
        String value = this.map.get( key );
        return value != null ? Double.parseDouble( value ) : defaultValue;
    }
    
    public Boolean getBoolean( String key, Boolean defaultValue )
    {
        String value = this.map.get( key );
        return value != null ? Boolean.parseBoolean( value ) : defaultValue;
    }
    
    public Boolean getBoolean( String key, Boolean defaultValueIfNotFound,
            Boolean defaultValueIfNoValue )
    {
        String value = this.map.get( key );
        if ( value != null )
        {
            return Boolean.parseBoolean( value );
        }
        return this.map.containsKey( key ) ? defaultValueIfNoValue : defaultValueIfNotFound;
    }
    
    public List<String> orphans()
    {
        return new ArrayList<String>( this.orphans );
    }
    
    private static boolean isOption( String arg )
    {
        return arg.startsWith( "-" );
    }
    
    private static String stripOption( String arg )
    {
        while ( arg.length() > 0 && arg.charAt( 0 ) == '-' )
        {
            arg = arg.substring( 1 );
        }
        return arg;
    }

    private void parseArgs( String[] args )
    {
        for ( int i = 0; i < args.length; i++ )
        {
            String arg = args[ i ];
            if ( isOption( arg ) )
            {
                arg = stripOption( arg );
                int equalIndex = arg.indexOf( '=' );
                if ( equalIndex != -1 )
                {
                    String key = arg.substring( 0, equalIndex );
                    String value = arg.substring( equalIndex + 1 );
                    if ( value.length() > 0 )
                    {
                        map.put( key, value );
                    }
                }
                else
                {
                    String key = arg;
                    int nextIndex = ++i;
                    String value = nextIndex < args.length ?
                        args[ nextIndex ] : null;
                    value = value == null || isOption( value ) ? null : value;
                    map.put( key, value );
                }
            }
            else
            {
                orphans.add( arg );
            }
        }
    }
}
