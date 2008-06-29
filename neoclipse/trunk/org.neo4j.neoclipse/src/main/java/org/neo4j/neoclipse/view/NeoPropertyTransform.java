/*
 * Licensed to "Neo Technology," Network Engine for Objects in Lund AB
 * (http://neotechnology.com) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Neo Technology licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at (http://www.apache.org/licenses/LICENSE-2.0). Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.neo4j.neoclipse.view;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transform between property values and representations for editors.
 * @author Anders Nawroth
 */
@SuppressWarnings( "serial" )
public class NeoPropertyTransform
{
    /**
     * Transform between editor representation and property value.
     */
    protected interface PropertyHandler
    {
        /**
         * Transform from editor representation to property value.
         * @param o
         *            editor representation (mostly a String)
         * @return property value or null
         * @throws IOException
         */
        Object parse( Object o ) throws IOException;

        /**
         * Transform from property value to editor representation.
         * @param o
         *            property value
         * @return editor representation of the value
         */
        Object render( Object o );

        /**
         * Editor type for this property type.
         * @return editor type
         */
        NeoPropertyEditor getEditorType();
    }

    /**
     * Transform a String to a List of Strings.
     * @param input
     *            editor data for array
     * @return list containing the separated strings
     */
    protected static List<String> arrayToCollection( Object input )
    {
        String in = removeBrackets( input );
        List<String> out = new ArrayList<String>();
        for ( String item : in.split( "," ) )
        {
            item = item.trim();
            if ( item.length() > 0 )
            {
                out.add( item );
            }
        }
        return out;
    }

    /**
     * Transform a String to a list of Strings that are used as Strings in the
     * property value. Adds a little more features like being able to have
     * spaces inside a string. Citation signs (") are escaped by a backslash
     * (\).
     * @param input
     *            editor data for String array
     * @return list containing the separated strings
     * @throws IOException
     */
    protected static List<String> stringArrayToCollection( Object input )
        throws IOException
    {
        String in = removeBrackets( input );
        List<String> out = new ArrayList<String>();
        StreamTokenizer tokenizer = new StreamTokenizer( new StringReader( in ) );
        tokenizer.wordChars( '0', '9' );
        while ( tokenizer.nextToken() != StreamTokenizer.TT_EOF )
        {
            if ( tokenizer.sval != null )
            {
                out.add( tokenizer.sval );
            }
        }
        return out;
    }

    /**
     * Simple utility to remove [brackets] used as markers for an array.
     * @param input
     *            user data
     * @return user data minus brackets and leading/trailing whitespace
     */
    private static String removeBrackets( Object input )
    {
        String in = input.toString().trim();
        if ( in.charAt( 0 ) == '[' )
        {
            in = in.substring( 1 );
        }
        if ( in.charAt( in.length() - 1 ) == ']' )
        {
            in = in.substring( 0, in.length() - 1 );
        }
        return in;
    }

    /**
     * A Map from property type classes to property handlers. Use get() on the
     * map to retrieve the correct property handler with parse(), render() nd
     * getEditorType() methods.
     */
    public static final Map<Class<?>,PropertyHandler> handlerMap = new HashMap<Class<?>,PropertyHandler>()
    {
        {
            put( String.class, new PropertyHandler()
            {
                public Object parse( Object o )
                {
                    return o;
                }

                public Object render( Object o )
                {
                    return (String) o;
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
            put( String[].class, new PropertyHandler()
            {
                public Object parse( Object o ) throws IOException
                {
                    List<String> items = stringArrayToCollection( o );
                    String[] res = new String[items.size()];
                    for ( int i = 0; i < res.length; i++ )
                    {
                        res[i] = items.get( i );
                    }
                    return res;
                }

                public Object render( Object o )
                {
                    String[] res = new String[((String[]) o).length];
                    for ( int i = 0; i < res.length; i++ )
                    {
                        String s = ((String[]) o)[i];
                        s = s.replaceAll( "\"", "\\\\\"" );
                        res[i] = '"' + s + '"';
                    }
                    return Arrays.toString( res );
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
            put( Integer.class, new PropertyHandler()
            {
                public Object parse( Object o )
                {
                    return Integer.parseInt( (String) o );
                }

                public Object render( Object o )
                {
                    return ((Integer) o).toString();
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
            put( int[].class, new PropertyHandler()
            {
                public Object parse( Object o )
                {
                    List<String> items = arrayToCollection( o );
                    int[] res = new int[items.size()];
                    for ( int i = 0; i < res.length; i++ )
                    {
                        res[i] = Integer.parseInt( items.get( i ) );
                    }
                    return res;
                }

                public Object render( Object o )
                {
                    return Arrays.toString( (int[]) o );
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
            put( Double.class, new PropertyHandler()
            {
                public Object parse( Object o )
                {
                    return Double.parseDouble( (String) o );
                }

                public Object render( Object o )
                {
                    return ((Double) o).toString();
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
            put( double[].class, new PropertyHandler()
            {
                public Object parse( Object o )
                {
                    List<String> items = arrayToCollection( o );
                    double[] res = new double[items.size()];
                    for ( int i = 0; i < res.length; i++ )
                    {
                        res[i] = Double.parseDouble( items.get( i ) );
                    }
                    return res;
                }

                public Object render( Object o )
                {
                    return Arrays.toString( (double[]) o );
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
            put( Float.class, new PropertyHandler()
            {
                public Object parse( Object o )
                {
                    return Float.parseFloat( (String) o );
                }

                public Object render( Object o )
                {
                    return ((Float) o).toString();
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
            put( float[].class, new PropertyHandler()
            {
                public Object parse( Object o )
                {
                    List<String> items = arrayToCollection( o );
                    float[] res = new float[items.size()];
                    for ( int i = 0; i < res.length; i++ )
                    {
                        res[i] = Float.parseFloat( items.get( i ) );
                    }
                    return res;
                }

                public Object render( Object o )
                {
                    return Arrays.toString( (float[]) o );
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
            put( Boolean.class, new PropertyHandler()
            {
                // has it's dedicated editor, handling transforms,
                // so we just pass things through here
                public Object parse( Object o )
                {
                    return o;
                }

                public Object render( Object o )
                {
                    return o;
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.BOOLEAN;
                }
            } );
            put( boolean[].class, new PropertyHandler()
            {
                public Object parse( Object o )
                {
                    List<String> items = arrayToCollection( o );
                    boolean[] res = new boolean[items.size()];
                    for ( int i = 0; i < res.length; i++ )
                    {
                        res[i] = Boolean.parseBoolean( items.get( i ) );
                    }
                    return res;
                }

                public Object render( Object o )
                {
                    return Arrays.toString( (boolean[]) o );
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
            put( Byte.class, new PropertyHandler()
            {
                public Object parse( Object o )
                {
                    return Byte.parseByte( (String) o );
                }

                public Object render( Object o )
                {
                    return ((Byte) o).toString();
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
            put( byte[].class, new PropertyHandler()
            {
                public Object parse( Object o )
                {
                    List<String> items = arrayToCollection( o );
                    byte[] res = new byte[items.size()];
                    for ( int i = 0; i < res.length; i++ )
                    {
                        res[i] = Byte.parseByte( items.get( i ) );
                    }
                    return res;
                }

                public Object render( Object o )
                {
                    return Arrays.toString( (byte[]) o );
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
            put( Short.class, new PropertyHandler()
            {
                public Object parse( Object o )
                {
                    return Short.parseShort( (String) o );
                }

                public Object render( Object o )
                {
                    return ((Short) o).toString();
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
            put( short[].class, new PropertyHandler()
            {
                public Object parse( Object o )
                {
                    List<String> items = arrayToCollection( o );
                    short[] res = new short[items.size()];
                    for ( int i = 0; i < res.length; i++ )
                    {
                        res[i] = Short.parseShort( items.get( i ) );
                    }
                    return res;
                }

                public Object render( Object o )
                {
                    return Arrays.toString( (short[]) o );
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
            put( Long.class, new PropertyHandler()
            {
                public Object parse( Object o )
                {
                    return Long.parseLong( (String) o );
                }

                public Object render( Object o )
                {
                    return ((Long) o).toString();
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
            put( long[].class, new PropertyHandler()
            {
                public Object parse( Object o )
                {
                    List<String> items = arrayToCollection( o );
                    long[] res = new long[items.size()];
                    for ( int i = 0; i < res.length; i++ )
                    {
                        res[i] = Long.parseLong( items.get( i ) );
                    }
                    return res;
                }

                public Object render( Object o )
                {
                    return Arrays.toString( (long[]) o );
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
            put( Character.class, new PropertyHandler()
            {
                public Object parse( Object o )
                {
                    String s = (String) o;
                    if ( s.length() > 0 )
                    {
                        return ((String) o).charAt( 0 );
                    }
                    return null;
                }

                public Object render( Object o )
                {
                    return ((Character) o).toString();
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
            put( char[].class, new PropertyHandler()
            {
                public Object parse( Object o )
                {
                    List<String> items = arrayToCollection( o );
                    char[] res = new char[items.size()];
                    for ( int i = 0; i < res.length; i++ )
                    {
                        res[i] = items.get( i ).charAt( 0 );
                    }
                    return res;
                }

                public Object render( Object o )
                {
                    return Arrays.toString( (char[]) o );
                }

                public NeoPropertyEditor getEditorType()
                {
                    return NeoPropertyEditor.TEXT;
                }
            } );
        }
    };
}
