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
package org.neo4j.kernel.impl.nioneo.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.kernel.impl.AbstractNeo4jTestCase;
import org.neo4j.kernel.impl.core.LockReleaser;
import org.neo4j.kernel.impl.core.PropertyIndex;
import org.neo4j.kernel.impl.nioneo.xa.NeoStoreXaConnection;
import org.neo4j.kernel.impl.nioneo.xa.NeoStoreXaDataSource;
import org.neo4j.kernel.impl.transaction.LockManager;
import org.neo4j.kernel.impl.transaction.XidImpl;

public class TestXa extends AbstractNeo4jTestCase
{
    private NeoStoreXaDataSource ds;
    private NeoStoreXaConnection xaCon;
    private Logger log;
    private Level level;

    private static class MyPropertyIndex extends
        org.neo4j.kernel.impl.core.PropertyIndex
    {
        private static Map<String,PropertyIndex> stringToIndex = new HashMap<String,PropertyIndex>();
        private static Map<Integer,PropertyIndex> intToIndex = new HashMap<Integer,PropertyIndex>();

        protected MyPropertyIndex( String key, int keyId )
        {
            super( key, keyId );
        }

        public static Iterable<PropertyIndex> index( String key )
        {
            if ( stringToIndex.containsKey( key ) )
            {
                return Arrays.asList( new PropertyIndex[] { stringToIndex
                    .get( key ) } );
            }
            return Collections.emptyList();
        }

//        public static PropertyIndex getIndexFor( int index )
//        {
//            return intToIndex.get( index );
//        }

        public static void add( MyPropertyIndex index )
        {
            // TODO Auto-generated method stub
            stringToIndex.put( index.getKey(), index );
            intToIndex.put( index.getKeyId(), index );
        }
    }
    
    @Override
    protected boolean restartGraphDbBetweenTests()
    {
        return true;
    }

    private PropertyIndex createDummyIndex( int id, String key )
    {
        MyPropertyIndex index = new MyPropertyIndex( key, id );
        MyPropertyIndex.add( index );
        return index;
    }

    private LockManager lockManager;
    private LockReleaser lockReleaser;
    
    private String path()
    {
        String path = getStorePath( "xatest" );
        new File( path ).mkdirs();
        return path;
    }
    
    private String file( String name )
    {
        return path() + File.separator + name;
    }

    @Before
    public void setUpNeoStore() throws Exception
    {
        log = Logger
            .getLogger( "org.neo4j.kernel.impl.transaction.xaframework.XaLogicalLog/"
                + "nioneo_logical.log" );
        level = log.getLevel();
        log.setLevel( Level.OFF );
        log = Logger
            .getLogger( "org.neo4j.kernel.impl.nioneo.xa.NeoStoreXaDataSource" );
        log.setLevel( Level.OFF );
        NeoStore.createStore( file( "neo" ), Collections.EMPTY_MAP );
        lockManager = getEmbeddedGraphDb().getConfig().getLockManager();
        lockReleaser = getEmbeddedGraphDb().getConfig().getLockReleaser();
        ds = new NeoStoreXaDataSource( file( "neo" ), file( "nioneo_logical.log" ),
            lockManager, lockReleaser );
        xaCon = (NeoStoreXaConnection) ds.getXaConnection();
    }

    @After
    public void tearDownNeoStore()
    {
        ds.close();
        log.setLevel( level );
        log = Logger
            .getLogger( "org.neo4j.kernel.impl.transaction.xaframework.XaLogicalLog/"
                + "nioneo_logical.log" );
        log.setLevel( level );
        log = Logger
            .getLogger( "org.neo4j.kernel.impl.nioneo.xa.NeoStoreXaDataSource" );
        log.setLevel( level );
        File file = new File( file( "neo" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.id" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.nodestore.db" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.nodestore.db.id" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.propertystore.db" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.propertystore.db.id" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.propertystore.db.index" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.propertystore.db.index.id" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.propertystore.db.index.keys" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.propertystore.db.index.keys.id" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.propertystore.db.strings" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.propertystore.db.strings.id" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.propertystore.db.arrays" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.propertystore.db.arrays.id" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.relationshipstore.db" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.relationshipstore.db.id" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.relationshiptypestore.db" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.relationshiptypestore.db.id" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.relationshiptypestore.db.names" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "neo.relationshiptypestore.db.names.id" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( path() );
        for ( File nioFile : file.listFiles() )
        {
            if ( nioFile.getName().startsWith( "nioneo_logical.log" ) )
            {
                assertTrue( nioFile.delete() );
            }
        }
    }

    private void deleteLogicalLogIfExist()
    {
        File file = new File( file( "nioneo_logical.log.1" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "nioneo_logical.log.2" ) );
        if ( file.exists() )
        {
            assertTrue( file.delete() );
        }
        file = new File( file( "nioneo_logical.log.active" ) );
        assertTrue( file.delete() );
    }

    private void renameCopiedLogicalLog()
    {
        File file = new File( file( "nioneo_logical.log.bak.1" ) );
        if ( file.exists() )
        {
            assertTrue( file.renameTo( new File( file( "nioneo_logical.log.1" ) ) ) );
        }
        else
        {
            file = new File( file( "nioneo_logical.log.bak.2" ) );
            assertTrue( file.renameTo( new File( file( "nioneo_logical.log.2" ) ) ) );
        }
        file = new File( file( "nioneo_logical.log.bak.active" ) );
        assertTrue( file.renameTo( new File( file( "nioneo_logical.log.active" ) ) ) );
    }

    private void truncateLogicalLog( int size ) throws IOException
    {
        char active = '1';
        FileChannel af = new RandomAccessFile( file( "nioneo_logical.log.active" ), 
            "r" ).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate( 1024 );
        af.read( buffer );
        af.close();
        buffer.flip();
        active = buffer.asCharBuffer().get();
        buffer.clear();
        FileChannel fileChannel = new RandomAccessFile( file( "nioneo_logical.log." + 
            active ), "rw" ).getChannel();
        if ( fileChannel.size() > size )
        {
            fileChannel.truncate( size );
        }
        else
        {
            fileChannel.position( size );
            ByteBuffer buf = ByteBuffer.allocate( 1 );
            buf.put( (byte) 0 ).flip();
            fileChannel.write( buf );
        }
        fileChannel.force( false );
        fileChannel.close();
    }
    
    private void copyLogicalLog() throws IOException
    {
        char active = '1';
        FileChannel af = new RandomAccessFile( file( "nioneo_logical.log.active" ), 
            "r" ).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate( 1024 );
        af.read( buffer );
        buffer.flip();
        FileChannel activeCopy = new RandomAccessFile( 
                file( "nioneo_logical.log.bak.active" ), "rw" ).getChannel();
        activeCopy.write( buffer );
        activeCopy.close();
        af.close();
        buffer.flip();
        active = buffer.asCharBuffer().get();
        buffer.clear();
        FileChannel source = new RandomAccessFile( file( "nioneo_logical.log." + 
            active ), "r" ).getChannel();
        FileChannel dest = new RandomAccessFile( file( "nioneo_logical.log.bak." + 
            active ), "rw" ).getChannel();
        int read = -1;
        do
        {
            read = source.read( buffer );
            buffer.flip();
            dest.write( buffer );
            buffer.clear();
        }
        while ( read == 1024 );
        source.close();
        dest.close();
    }

    private PropertyIndex index( String key )
    {
        Iterator<PropertyIndex> itr = MyPropertyIndex.index( key ).iterator();
        if ( !itr.hasNext() )
        {
            int id = ds.nextId( PropertyIndex.class );
            PropertyIndex index = createDummyIndex( id, key );
            xaCon.getPropertyIndexConsumer().createPropertyIndex( id, key );
            return index;
        }
        return itr.next();
    }

    @Test
    public void testLogicalLog() throws Exception
    {
        Xid xid = new XidImpl( new byte[1], new byte[1] );
        XAResource xaRes = xaCon.getXaResource();
        xaRes.start( xid, XAResource.TMNOFLAGS );
        int node1 = ds.nextId( Node.class );
        xaCon.getNodeConsumer().createNode( node1 );
        int node2 = ds.nextId( Node.class );
        xaCon.getNodeConsumer().createNode( node2 );
        int n1prop1 = ds.nextId( PropertyStore.class );
        xaCon.getNodeConsumer().addProperty( node1, n1prop1,
            index( "prop1" ), "string1" );
        xaCon.getNodeConsumer().getProperties( node1, false );
        int relType1 = ds.nextId( RelationshipType.class );
        xaCon.getRelationshipTypeConsumer().addRelationshipType( relType1,
            "relationshiptype1" );
        int rel1 = ds.nextId( Relationship.class );
        xaCon.getRelationshipConsumer().createRelationship( rel1, node1,
            node2, relType1 );
        int r1prop1 = ds.nextId( PropertyStore.class );
        xaCon.getRelationshipConsumer().addProperty( rel1, r1prop1,
            index( "prop1" ), "string1" );
        xaCon.getNodeConsumer().changeProperty( node1, n1prop1, "string2" );
        xaCon.getRelationshipConsumer().changeProperty( rel1, r1prop1,
            "string2" );
        xaCon.getNodeConsumer().removeProperty( node1, n1prop1 );
        xaCon.getRelationshipConsumer().removeProperty( rel1, r1prop1 );
        xaCon.getRelationshipConsumer().deleteRelationship( rel1 );
        xaCon.getNodeConsumer().deleteNode( node1 );
        xaCon.getNodeConsumer().deleteNode( node2 );
        xaRes.end( xid, XAResource.TMSUCCESS );
        xaRes.commit( xid, true );
        copyLogicalLog();
        xaCon.clearAllTransactions();
        ds.close();
        deleteLogicalLogIfExist();
        renameCopiedLogicalLog();
        ds = new NeoStoreXaDataSource( file( "neo" ), file( "nioneo_logical.log" ),
            lockManager, lockReleaser );
        xaCon = (NeoStoreXaConnection) ds.getXaConnection();
        xaRes = xaCon.getXaResource();
        assertEquals( 0, xaRes.recover( XAResource.TMNOFLAGS ).length );
        xaCon.clearAllTransactions();
    }

    @Test
    public void testLogicalLogPrepared() throws Exception
    {
        Xid xid = new XidImpl( new byte[2], new byte[2] );
        XAResource xaRes = xaCon.getXaResource();
        xaRes.start( xid, XAResource.TMNOFLAGS );
        int node1 = ds.nextId( Node.class );
        xaCon.getNodeConsumer().createNode( node1 );
        int node2 = ds.nextId( Node.class );
        xaCon.getNodeConsumer().createNode( node2 );
        int n1prop1 = ds.nextId( PropertyStore.class );
        xaCon.getNodeConsumer().addProperty( node1, n1prop1,
            index( "prop1" ), "string1" );
        int relType1 = ds.nextId( RelationshipType.class );
        xaCon.getRelationshipTypeConsumer().addRelationshipType( relType1,
            "relationshiptype1" );
        int rel1 = ds.nextId( Relationship.class );
        xaCon.getRelationshipConsumer().createRelationship( rel1, node1,
            node2, relType1 );
        int r1prop1 = ds.nextId( PropertyStore.class );
        xaCon.getRelationshipConsumer().addProperty( rel1, r1prop1,
            index( "prop1" ), "string1" );
        xaCon.getNodeConsumer().changeProperty( node1, n1prop1, "string2" );
        xaCon.getRelationshipConsumer().changeProperty( rel1, r1prop1,
            "string2" );
        xaRes.end( xid, XAResource.TMSUCCESS );
        xaRes.prepare( xid );
        copyLogicalLog();
        xaCon.clearAllTransactions();
        ds.close();
        deleteLogicalLogIfExist();
        renameCopiedLogicalLog();
        ds = new NeoStoreXaDataSource( file( "neo" ), file( "nioneo_logical.log" ),
            lockManager, lockReleaser );
        xaCon = (NeoStoreXaConnection) ds.getXaConnection();
        xaRes = xaCon.getXaResource();
        assertEquals( 1, xaRes.recover( XAResource.TMNOFLAGS ).length );
        xaRes.commit( xid, true );
        xaCon.clearAllTransactions();
    }

    @Test
    public void testLogicalLogPrePrepared() throws Exception
    {
        Xid xid = new XidImpl( new byte[3], new byte[3] );
        XAResource xaRes = xaCon.getXaResource();
        xaRes.start( xid, XAResource.TMNOFLAGS );
        int node1 = ds.nextId( Node.class );
        xaCon.getNodeConsumer().createNode( node1 );
        int node2 = ds.nextId( Node.class );
        xaCon.getNodeConsumer().createNode( node2 );
        int n1prop1 = ds.nextId( PropertyStore.class );
        xaCon.getNodeConsumer().addProperty( node1, n1prop1,
            index( "prop1" ), "string1" );
        int relType1 = ds.nextId( RelationshipType.class );
        xaCon.getRelationshipTypeConsumer().addRelationshipType( relType1,
            "relationshiptype1" );
        int rel1 = ds.nextId( Relationship.class );
        xaCon.getRelationshipConsumer().createRelationship( rel1, node1,
            node2, relType1 );
        int r1prop1 = ds.nextId( PropertyStore.class );
        xaCon.getRelationshipConsumer().addProperty( rel1, r1prop1,
            index( "prop1" ), "string1" );
        xaCon.getNodeConsumer().changeProperty( node1, n1prop1, "string2" );
        xaCon.getRelationshipConsumer().changeProperty( rel1, r1prop1,
            "string2" );
        xaRes.end( xid, XAResource.TMSUCCESS );
        xaCon.clearAllTransactions();
        copyLogicalLog();
        ds.close();
        deleteLogicalLogIfExist();
        renameCopiedLogicalLog();
        ds = new NeoStoreXaDataSource( file( "neo" ), file( "nioneo_logical.log" ),
            lockManager, lockReleaser );
        xaCon = (NeoStoreXaConnection) ds.getXaConnection();
        xaRes = xaCon.getXaResource();
        assertEquals( 0, xaRes.recover( XAResource.TMNOFLAGS ).length );
    }

    @Test
    public void testBrokenNodeCommand() throws Exception
    {
        Xid xid = new XidImpl( new byte[4], new byte[4] );
        XAResource xaRes = xaCon.getXaResource();
        xaRes.start( xid, XAResource.TMNOFLAGS );
        int node1 = ds.nextId( Node.class );
        xaCon.getNodeConsumer().createNode( node1 );
        xaRes.end( xid, XAResource.TMSUCCESS );
        xaRes.prepare( xid );
        xaCon.clearAllTransactions();
        copyLogicalLog();
        xaCon.clearAllTransactions();
        ds.close();
        deleteLogicalLogIfExist();
        renameCopiedLogicalLog();
        truncateLogicalLog( 39 );
        truncateLogicalLog( 40 );
        ds = new NeoStoreXaDataSource( file( "neo" ), file( "nioneo_logical.log" ),
            lockManager, lockReleaser );
        xaCon = (NeoStoreXaConnection) ds.getXaConnection();
        xaRes = xaCon.getXaResource();
        assertEquals( 0, xaRes.recover( XAResource.TMNOFLAGS ).length );
        xaCon.clearAllTransactions();
    } 

    @Test
    public void testBrokenCommand() throws Exception
    {
        Xid xid = new XidImpl( new byte[4], new byte[4] );
        XAResource xaRes = xaCon.getXaResource();
        xaRes.start( xid, XAResource.TMNOFLAGS );
        int node1 = ds.nextId( Node.class );
        xaCon.getNodeConsumer().createNode( node1 );
        xaRes.end( xid, XAResource.TMSUCCESS );
        xaRes.prepare( xid );
        xaCon.clearAllTransactions();
        copyLogicalLog();
        xaCon.clearAllTransactions();
        ds.close();
        deleteLogicalLogIfExist();
        renameCopiedLogicalLog();
        truncateLogicalLog( 32 );
        truncateLogicalLog( 40 );
        ds = new NeoStoreXaDataSource( file( "neo" ), file( "nioneo_logical.log" ),
            lockManager, lockReleaser );
        xaCon = (NeoStoreXaConnection) ds.getXaConnection();
        xaRes = xaCon.getXaResource();
        assertEquals( 0, xaRes.recover( XAResource.TMNOFLAGS ).length );
        xaCon.clearAllTransactions();
    }
    
    @Test
    public void testBrokenPrepare() throws Exception
    {
        Xid xid = new XidImpl( new byte[4], new byte[4] );
        XAResource xaRes = xaCon.getXaResource();
        xaRes.start( xid, XAResource.TMNOFLAGS );
        int node1 = ds.nextId( Node.class );
        xaCon.getNodeConsumer().createNode( node1 );
        int node2 = ds.nextId( Node.class );
        xaCon.getNodeConsumer().createNode( node2 );
        int n1prop1 = ds.nextId( PropertyStore.class );
        xaCon.getNodeConsumer().addProperty( node1, n1prop1,
            index( "prop1" ), "string1" );
        xaRes.end( xid, XAResource.TMSUCCESS );
        xaRes.prepare( xid );
        copyLogicalLog();
        xaCon.clearAllTransactions();
        ds.close();
        deleteLogicalLogIfExist();
        renameCopiedLogicalLog();
        truncateLogicalLog( 141 );
        ds = new NeoStoreXaDataSource( file( "neo" ), file( "nioneo_logical.log" ),
            lockManager, lockReleaser );
        xaCon = (NeoStoreXaConnection) ds.getXaConnection();
        xaRes = xaCon.getXaResource();
        assertEquals( 0, xaRes.recover( XAResource.TMNOFLAGS ).length );
        xaCon.clearAllTransactions();
    }

    @Test
    public void testBrokenDone() throws Exception
    {
        Xid xid = new XidImpl( new byte[4], new byte[4] );
        XAResource xaRes = xaCon.getXaResource();
        xaRes.start( xid, XAResource.TMNOFLAGS );
        int node1 = ds.nextId( Node.class );
        xaCon.getNodeConsumer().createNode( node1 );
        int node2 = ds.nextId( Node.class );
        xaCon.getNodeConsumer().createNode( node2 );
        int n1prop1 = ds.nextId( PropertyStore.class );
        xaCon.getNodeConsumer().addProperty( node1, n1prop1,
            index( "prop1" ), "string1" );
        xaRes.end( xid, XAResource.TMSUCCESS );
        xaRes.prepare( xid );
        xaRes.commit( xid, false );
        copyLogicalLog();
        ds.close();
        deleteLogicalLogIfExist();
        renameCopiedLogicalLog();
        truncateLogicalLog( 157 );
        ds = new NeoStoreXaDataSource( file( "neo" ), file( "nioneo_logical.log" ),
             lockManager, lockReleaser );
        xaCon = (NeoStoreXaConnection) ds.getXaConnection();
        xaRes = xaCon.getXaResource();
        assertEquals( 1, xaRes.recover( XAResource.TMNOFLAGS ).length );
        xaCon.clearAllTransactions();
    }
    
    @Test
    public void testLogVersion()
    {
        long creationTime = ds.getCreationTime();
        long randomIdentifier = ds.getRandomIdentifier();
        long currentVersion = ds.getCurrentLogVersion();
        assertEquals( currentVersion, ds.incrementAndGetLogVersion() );
        assertEquals( currentVersion + 1, ds.incrementAndGetLogVersion() );
        assertEquals( creationTime, ds.getCreationTime() );
        assertEquals( randomIdentifier, ds.getRandomIdentifier() );
    }

    @Test
    public void testLogicalLogRotation() throws Exception
    {
        ds.keepLogicalLogs( true );
        Xid xid = new XidImpl( new byte[1], new byte[1] );
        XAResource xaRes = xaCon.getXaResource();
        xaRes.start( xid, XAResource.TMNOFLAGS );
        int node1 = ds.nextId( Node.class );
        xaCon.getNodeConsumer().createNode( node1 );
        int node2 = ds.nextId( Node.class );
        xaCon.getNodeConsumer().createNode( node2 );
        int n1prop1 = ds.nextId( PropertyStore.class );
        xaCon.getNodeConsumer().addProperty( node1, n1prop1,
            index( "prop1" ), "string1" );
        xaCon.getNodeConsumer().getProperties( node1, false );
        int relType1 = ds.nextId( RelationshipType.class );
        xaCon.getRelationshipTypeConsumer().addRelationshipType( relType1,
            "relationshiptype1" );
        int rel1 = ds.nextId( Relationship.class );
        xaCon.getRelationshipConsumer().createRelationship( rel1, node1,
            node2, relType1 );
        int r1prop1 = ds.nextId( PropertyStore.class );
        xaCon.getRelationshipConsumer().addProperty( rel1, r1prop1,
            index( "prop1" ), "string1" );
        xaCon.getNodeConsumer().changeProperty( node1, n1prop1, "string2" );
        xaCon.getRelationshipConsumer().changeProperty( rel1, r1prop1,
            "string2" );
        xaCon.getNodeConsumer().removeProperty( node1, n1prop1 );
        xaCon.getRelationshipConsumer().removeProperty( rel1, r1prop1 );
        xaCon.getRelationshipConsumer().deleteRelationship( rel1 );
        xaCon.getNodeConsumer().deleteNode( node1 );
        xaCon.getNodeConsumer().deleteNode( node2 );
        xaRes.end( xid, XAResource.TMSUCCESS );
        xaRes.commit( xid, true );
        long currentVersion = ds.getCurrentLogVersion();
        ds.rotateLogicalLog();
        assertTrue( ds.getLogicalLog( currentVersion ) != null );
        ds.rotateLogicalLog();
        assertTrue( ds.getLogicalLog( currentVersion ) != null );
        assertTrue( ds.getLogicalLog( currentVersion + 1 ) != null );
    }


    @Test
    public void testApplyLogicalLog() throws Exception
    {
        ds.keepLogicalLogs( true );
        Xid xid = new XidImpl( new byte[1], new byte[1] );
        XAResource xaRes = xaCon.getXaResource();
        xaRes.start( xid, XAResource.TMNOFLAGS );
        int node1 = ds.nextId( Node.class );
        xaCon.getNodeConsumer().createNode( node1 );
        int node2 = ds.nextId( Node.class );
        xaCon.getNodeConsumer().createNode( node2 );
        int n1prop1 = ds.nextId( PropertyStore.class );
        xaCon.getNodeConsumer().addProperty( node1, n1prop1,
            index( "prop1" ), "string1" );
        xaCon.getNodeConsumer().getProperties( node1, false );
        int relType1 = ds.nextId( RelationshipType.class );
        xaCon.getRelationshipTypeConsumer().addRelationshipType( relType1,
            "relationshiptype1" );
        int rel1 = ds.nextId( Relationship.class );
        xaCon.getRelationshipConsumer().createRelationship( rel1, node1,
            node2, relType1 );
        int r1prop1 = ds.nextId( PropertyStore.class );
        xaCon.getRelationshipConsumer().addProperty( rel1, r1prop1,
            index( "prop1" ), "string1" );
        xaCon.getNodeConsumer().changeProperty( node1, n1prop1, "string2" );
        xaCon.getRelationshipConsumer().changeProperty( rel1, r1prop1,
            "string2" );
        xaCon.getNodeConsumer().removeProperty( node1, n1prop1 );
        xaCon.getRelationshipConsumer().removeProperty( rel1, r1prop1 );
        xaCon.getRelationshipConsumer().deleteRelationship( rel1 );
        xaCon.getNodeConsumer().deleteNode( node1 );
        xaCon.getNodeConsumer().deleteNode( node2 );
        xaRes.end( xid, XAResource.TMSUCCESS );
        xaRes.commit( xid, true );
        long currentVersion = ds.getCurrentLogVersion();
        ds.keepLogicalLogs( true );
        ds.rotateLogicalLog();
        ds.rotateLogicalLog();
        ds.rotateLogicalLog();
        ds.setCurrentLogVersion( currentVersion );
        ds.makeBackupSlave();
        ds.applyLog( ds.getLogicalLog( currentVersion ) );
        ds.applyLog( ds.getLogicalLog( currentVersion + 1 ) );
        ds.applyLog( ds.getLogicalLog( currentVersion + 2 ) );
        ds.keepLogicalLogs( false );
    }
}
