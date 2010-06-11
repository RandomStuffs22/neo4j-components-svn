/*
 * Copyright (c) 2002-2009 "Neo Technology,"
 *     Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 * 
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.index.lucene;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

class LuceneIndexStore
{
    private static final int SIZEOF_ID_DATA = 24;
    
    private long creationTime;
    private long randomIdentifier;
    private long version;
    
    private final FileChannel fileChannel;
    private ByteBuffer dontUseBuffer = ByteBuffer.allocate( SIZEOF_ID_DATA );
    final Map<String, Map<String, String>> indexConfig;
    
    private ByteBuffer buffer( int size )
    {
        if ( dontUseBuffer.capacity() < size )
        {
            dontUseBuffer = ByteBuffer.allocate( size*2 );
        }
        return dontUseBuffer;
    }
    
    public LuceneIndexStore( String store )
    {
        if ( !new File( store ).exists() )
        {
            create( store );
        }
        try
        {
            fileChannel = new RandomAccessFile( store, "rw" ).getChannel();
            ByteBuffer buffer = buffer( SIZEOF_ID_DATA );
            if ( fileChannel.read( buffer ) != SIZEOF_ID_DATA )
            {
                throw new RuntimeException( "Expected to read " + SIZEOF_ID_DATA + " bytes" );
            }
            buffer.flip();
            creationTime = buffer.getLong();
            randomIdentifier = buffer.getLong();
            version = buffer.getLong();
            indexConfig = readIndexConfig();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    private Map<String, Map<String, String>> readIndexConfig() throws IOException
    {
        Map<String, Map<String, String>> map = new HashMap<String, Map<String,String>>();
        while ( true )
        {
            String indexName = readNextString();
            if ( indexName == null )
            {
                break;
            }
            Integer propertyCount = readNextInt();
            if ( propertyCount == null )
            {
                break;
            }
            Map<String, String> properties = new HashMap<String, String>();
            for ( int i = 0; i < propertyCount; i++ )
            {
                String key = readNextString();
                if ( key == null )
                {
                    break;
                }
                String value = readNextString();
                if ( value == null )
                {
                    break;
                }
                properties.put( key, value );
            }
            map.put( indexName, properties );
        }
        return map;
    }
    
    private Integer readNextInt() throws IOException
    {
        ByteBuffer buffer = buffer( 4 );
        buffer.clear();
        buffer.limit( 4 );
        int read = fileChannel.read( buffer );
        if ( read < 4 )
        {
            return null;
        }
        return buffer.getInt();
    }

    private String readNextString() throws IOException
    {
        Integer length = readNextInt();
        if ( length == null )
        {
            return null;
        }
        ByteBuffer buffer = buffer( length );
        buffer = buffer( length );
        buffer.clear();
        buffer.limit( length );
        if ( fileChannel.read( buffer ) != length )
        {
            return null;
        }
        return buffer.asCharBuffer().toString();
    }

    void create( String store )
    {
        if ( new File( store ).exists() )
        {
            throw new IllegalArgumentException( store + " already exist" );
        }
        try
        {
            FileChannel fileChannel = 
                new RandomAccessFile( store, "rw" ).getChannel();
            ByteBuffer buf = ByteBuffer.allocate( SIZEOF_ID_DATA );
            long time = System.currentTimeMillis();
            long identifier = new Random( time ).nextLong();
            buf.putLong( time ).putLong( identifier ).putLong( 0 );
            buf.flip();
            writeIdData( fileChannel, buf );
            fileChannel.close();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    private static void writeIdData( FileChannel channel, ByteBuffer buffer ) throws IOException
    {
        if ( channel.write( buffer, 0 ) != SIZEOF_ID_DATA )
        {
            throw new RuntimeException( "Expected to write " + SIZEOF_ID_DATA + " bytes" );
        }
    }

    public long getCreationTime()
    {
        return creationTime;
    }

    public long getRandomNumber()
    {
        return randomIdentifier;
    }

    public long getVersion()
    {
        return version;
    }

    public synchronized long incrementVersion()
    {
        long current = getVersion();
        version++;
        writeOut();
        return current;
    }

    public synchronized void setVersion( long version )
    {
        this.version = version;
        writeOut();
    }
    
    private void writeOut()
    {
        ByteBuffer buffer = buffer( SIZEOF_ID_DATA );
        buffer.clear();
        buffer.putLong( creationTime ).putLong( randomIdentifier ).putLong( version );
        buffer.flip();
        try
        {
            writeIdData( fileChannel, buffer );
            writeIndexConfig();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    private void writeIndexConfig() throws IOException
    {
        for ( Map.Entry<String, Map<String, String>> entry : indexConfig.entrySet() )
        {
            writeString( entry.getKey() );
            writeInt( entry.getValue().size() );
            for ( Map.Entry<String, String> propertyEntry : entry.getValue().entrySet() )
            {
                writeString( propertyEntry.getKey() );
                writeString( propertyEntry.getValue() );
            }
        }
    }

    private void writeString( String value ) throws IOException
    {
        char[] chars = value.toCharArray();
        int length = chars.length*2;
        writeInt( length );
        ByteBuffer buffer = buffer( length );
        buffer.asCharBuffer().put( chars );
        fileChannel.write( buffer );
    }
    
    private void writeInt( int value ) throws IOException
    {
        ByteBuffer buffer = buffer( 4 );
        buffer.clear();
        buffer.putInt( value );
        buffer.flip();
        fileChannel.write( buffer );
    }

    public void close()
    {
        if ( !fileChannel.isOpen() )
        {
            return;
        }
        
        try
        {
            fileChannel.close();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
}