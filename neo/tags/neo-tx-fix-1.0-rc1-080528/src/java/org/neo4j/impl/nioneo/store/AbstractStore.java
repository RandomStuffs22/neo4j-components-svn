/*
 * Copyright 2002-2007 Network Engine for Objects in Lund AB [neotechnology.com]
 * 
 * This program is free software: you can redistribute it and/or modify
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.impl.nioneo.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.Map;

/**
 * An abstract representation of a store. A store is a file that contains 
 * records. Each record has a fixed size (<CODE>getRecordSize()</CODE>) so the 
 * position for a record can be calculated by <CODE>id * getRecordSize()</CODE>.
 * <p>
 * A store has an {@link IdGenerator} managing the records that are free or 
 * in use.
 */
public abstract class AbstractStore extends CommonAbstractStore
{
	/**
	 * Returnes the fixed size of each record in this store.
	 *
	 * @return The record size
	 */
	public abstract int getRecordSize();
	
	/**
	 * Creates a new empty store. The factory method returning an 
	 * implementation of some store type should make use of this method 
	 * to initialize an empty store.
	 * <p>
	 * This method will create a empty store containing the descriptor 
	 * returned by the <CODE>getTypeAndVersionDescriptor()</CODE>. The id 
	 * generator used by this store will also be created
	 *
	 * @param fileName The file name of the store that will be created
	 * @param typeAndVersionDescriptor The type and version descriptor that 
	 * identifies this store
	 * @throws IOException If fileName is null or if file exists
	 */
	protected static void createEmptyStore( String fileName, 
		String typeAndVersionDescriptor ) 
	{
		// sanity checks
		if ( fileName == null )
		{
			throw new IllegalArgumentException( "Null filename" );
		}
		File file = new File( fileName );
		if ( file.exists() )
		{
			throw new IllegalStateException( "Can't create store[" + fileName + 
				"], file already exists" );
		}

		// write the header
        try
        {
    		FileChannel channel = new FileOutputStream( fileName ).getChannel();
    		int endHeaderSize = typeAndVersionDescriptor.getBytes().length;
    		ByteBuffer buffer = ByteBuffer.allocate( endHeaderSize );
    		buffer.put( typeAndVersionDescriptor.getBytes() ).flip();
    		channel.write( buffer );
    		channel.force( false );
    		channel.close();
        }
        catch ( IOException e )
        {
            throw new StoreFailureException( "Unable to create store " + 
                fileName, e );
        }
		IdGenerator.createGenerator( fileName + ".id" ); 
	}
	
	public AbstractStore( String fileName, Map<?,?> config )
	{
		super( fileName, config );
	}
	
	public AbstractStore( String fileName )
	{
		super( fileName );
	}
	
	protected void loadStorage()
	{
        try
        {
    		long fileSize = getFileChannel().size();
    		String expectedVersion = getTypeAndVersionDescriptor();
    		byte version[] = new byte[ expectedVersion.getBytes().length ];
    		ByteBuffer buffer = ByteBuffer.wrap( version );
    		if ( fileSize >= version.length )
    		{
    			getFileChannel().position( fileSize - version.length );
    		}
    		else
    		{
    			setStoreNotOk();
    		}
    		getFileChannel().read( buffer );
    		if ( !expectedVersion.equals( new String( version ) ) )
    		{
    			versionFound( new String( version ) );
    			setStoreNotOk();
    		}
    		if ( getRecordSize() != 0 && 
    			( fileSize - version.length ) % getRecordSize() != 0 )
    		{
    			setStoreNotOk();
    		}
    		if ( getStoreOk() )
    		{
    			getFileChannel().truncate( fileSize - version.length );
    		}
        }
        catch ( IOException e )
        {
            throw new StoreFailureException( "Unable to load store " + 
                getStorageFileName(), e );
        }
		try
		{
			openIdGenerator();
		}
		catch ( StoreFailureException e )
		{
			setStoreNotOk();
		}
		setWindowPool( new PersistenceWindowPool( getStorageFileName(), 
			getRecordSize(), getFileChannel(), getMappedMem() ) );
	}
	
	/**
	 * Returns the next free id from {@link IdGenerator} used by this storage. 
	 *
	 * @return The id generator for this storage
	 */
	public int nextId()
	{
		return super.nextId();
	}
	
	/**
	 * Returns the highest id in use by this store.
	 * 
	 * @return The highest id in use
	 */
	public int getHighId()
	{
		return super.getHighId();
	}
	
	/**
	 * Sets the high id of {@link IdGenerator}.
	 * 
	 * @param id The high id
	 */
	protected void setHighId( int id )
	{
		super.setHighId( id );
	}

	/**
	 * Makes a id previously acquired from <CODE>nextId()</CODE> method 
	 * available again.
	 *
	 * @param id The id to free
	 */
	public void freeId( int id )
	{
		super.freeId( id );
	}
	
	/**
	 * Rebuilds the {@link IdGenerator} by looping through all records
	 * and checking if record in use or not.
	 * 
	 * @throws IOException if unable to rebuild the id generator
	 */
	protected void rebuildIdGenerator()
	{
		// TODO: fix this hardcoding
		final byte RECORD_NOT_IN_USE = 0;
		
		logger.fine( "Rebuilding id generator for[" + getStorageFileName() + 
			"] ..." );
		closeIdGenerator();
		File file = new File( getStorageFileName() + ".id" );
		if ( file.exists() )
		{
			file.delete();
		}
		IdGenerator.createGenerator( getStorageFileName() + ".id" );
		openIdGenerator();
		FileChannel fileChannel = getFileChannel();
        int highId = 1;
        long defraggedCount = 0;
        try
        {
    		long fileSize = fileChannel.size();
    		int recordSize = getRecordSize();
    		ByteBuffer byteBuffer = ByteBuffer.wrap( new byte[1] );
    		LinkedList<Integer> freeIdList = new LinkedList<Integer>();
    		for ( long i = 0; i * recordSize < fileSize && recordSize > 0; i++ )
    		{
    			fileChannel.position( i * recordSize );
    			fileChannel.read( byteBuffer );
    			byteBuffer.flip();
    			byte inUse = byteBuffer.get();
    			byteBuffer.flip();
    			nextId();
    			if ( inUse == RECORD_NOT_IN_USE )
    			{
    				freeIdList.add( (int) i );
    			}
    			else
    			{
    				highId = (int) i;
    				while ( !freeIdList.isEmpty() )
    				{
    					freeId( freeIdList.removeFirst() );
    					defraggedCount++;
    				}
    			}
    		}
        }
        catch ( IOException e )
        {
            throw new StoreFailureException( "Unable to rebuild id generator " + 
                getStorageFileName(), e );
        }
		setHighId( highId + 1 );
		logger.fine( "[" + getStorageFileName() + "] high id=" + getHighId() + 
			" (defragged=" + defraggedCount + ")" );  
		closeIdGenerator();
		openIdGenerator();
	}	

    protected boolean transferToBuffer( int id, ReadFromBuffer buffer )
    {
        buffer.makeReadyForTransfer();
        try
        {
            if ( getRecordSize() != getFileChannel().transferTo( 
                ((long) id) * getRecordSize(), getRecordSize(), 
                buffer.getFileChannel() ) )
            {
                return false;
            }
            return true;
        }
        catch ( IOException e )
        {
            throw new StoreFailureException( 
                "Failed to transfer data to read from buffer", e );
        }
    }
    
    protected boolean transferRecord( AbstractRecord record )
    {
        long id = record.getId();
        long count = record.getTransferCount();
        FileChannel fileChannel = getFileChannel();
        try
        {
            fileChannel.position( id * getRecordSize() );
            if ( count != record.getFromChannel().transferTo( 
                record.getTransferStartPosition(), count, fileChannel ) )
            {
                return false;
            }
            return true;
        }
        catch ( IOException e )
        {
            throw new StoreFailureException( "Unable to transfer record from " +
                "other file channel, " + getStorageFileName(), e );
        }
    }
}