/*
 * Copyright (c) 2002-2010 "Neo Technology,"
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
package org.neo4j.kernel;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.TransactionManager;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.kernel.impl.core.KernelPanicEventGenerator;
import org.neo4j.kernel.impl.core.LockReleaser;
import org.neo4j.kernel.impl.core.TxEventSyncHookFactory;
import org.neo4j.kernel.impl.nioneo.xa.NioNeoDbPersistenceSource;
import org.neo4j.kernel.impl.transaction.LockManager;
import org.neo4j.kernel.impl.transaction.TxModule;
import org.neo4j.kernel.impl.transaction.xaframework.XaDataSource;
import org.neo4j.kernel.impl.util.FileUtils;

class GraphDbInstance
{
    private boolean started = false;
    private boolean create;
    private String storeDir;

    GraphDbInstance( String storeDir, boolean create )
    {
        this.storeDir = storeDir;
        this.create = create;
    }

    private Config config = null;

    private NioNeoDbPersistenceSource persistenceSource = null;

    public Config getConfig()
    {
        return config;
    }

    private Map<Object, Object> getDefaultParams()
    {
        Map<Object, Object> params = new HashMap<Object, Object>();
        params.put( "neostore.nodestore.db.mapped_memory", "20M" );
        params.put( "neostore.propertystore.db.mapped_memory", "90M" );
        params.put( "neostore.propertystore.db.index.mapped_memory", "1M" );
        params.put( "neostore.propertystore.db.index.keys.mapped_memory", "1M" );
        params.put( "neostore.propertystore.db.strings.mapped_memory", "130M" );
        params.put( "neostore.propertystore.db.arrays.mapped_memory", "130M" );
        params.put( "neostore.relationshipstore.db.mapped_memory", "100M" );
        // if on windows, default no memory mapping
        String nameOs = System.getProperty( "os.name" );
        if ( nameOs.startsWith( "Windows" ) )
        {
            params.put( Config.USE_MEMORY_MAPPED_BUFFERS, "false" );
        }
        return params;
    }

    /**
     * Starts Neo4j with default configuration
     * @param graphDb The graph database service.
     *
     * @param storeDir path to directory where Neo4j store is located
     * @param create if true a new Neo4j store will be created if no store exist
     *            at <CODE>storeDir</CODE>
     * @param configuration parameters
     * @throws StartupFailedException if unable to start
     */
    public synchronized Map<Object, Object> start(
            GraphDatabaseService graphDb,
            Map<String, String> stringParams, KernelPanicEventGenerator kpe,
            TxEventSyncHookFactory syncHookFactory )
    {
        if ( started )
        {
            throw new IllegalStateException( "Neo4j instance already started" );
        }
        Map<Object, Object> params = getDefaultParams();
        boolean useMemoryMapped = true;
        if ( stringParams.containsKey( Config.USE_MEMORY_MAPPED_BUFFERS ) )
        {
            params.put( Config.USE_MEMORY_MAPPED_BUFFERS, 
                    stringParams.get( Config.USE_MEMORY_MAPPED_BUFFERS ) );
        }
        if ( "false".equals( params.get( Config.USE_MEMORY_MAPPED_BUFFERS ) ) )
        {
            useMemoryMapped = false;
        }
        boolean dump = false;
        if ( "true".equals( stringParams.get( Config.DUMP_CONFIGURATION ) ) )
        {
            dump = true;
        }
        storeDir = FileUtils.fixSeparatorsInPath( storeDir );
        new AutoConfigurator( storeDir, useMemoryMapped, dump ).configure( params );
        for ( Map.Entry<String, String> entry : stringParams.entrySet() )
        {
            params.put( entry.getKey(), entry.getValue() );
        }
        config = new Config( graphDb, storeDir, params, kpe );

        String separator = System.getProperty( "file.separator" );
        String store = storeDir + separator + "neostore";
        params.put( "store_dir", storeDir );
        params.put( "neo_store", store );
        params.put( "create", String.valueOf( create ) );
        String logicalLog = storeDir + separator + "nioneo_logical.log";
        params.put( "logical_log", logicalLog );
        byte resourceId[] = "414141".getBytes();
        params.put( LockManager.class, config.getLockManager() );
        params.put( LockReleaser.class, config.getLockReleaser() );
        config.getTxModule().registerDataSource( Config.DEFAULT_DATA_SOURCE_NAME,
                Config.NIO_NEO_DB_CLASS, resourceId, params );
        // hack for lucene index recovery if in path
        if ( !config.isReadOnly() || config.isBackupSlave() )
        {
            try
            {
                Class clazz = Class.forName( Config.LUCENE_DS_CLASS );
                cleanWriteLocksInLuceneDirectory( storeDir + "/lucene" );
                byte luceneId[] = "162373".getBytes();
                registerLuceneDataSource( "lucene", clazz.getName(),
                        config.getTxModule(), storeDir + "/lucene",
                        config.getLockManager(), luceneId, params );
            }
            catch ( ClassNotFoundException e )
            { // ok index util not on class path
            }
            
            try
            {
                Class clazz = Class.forName( Config.LUCENE_FULLTEXT_DS_CLASS );
                cleanWriteLocksInLuceneDirectory( storeDir + "/lucene-fulltext" );
                byte[] luceneId = "262374".getBytes();
                registerLuceneDataSource( "lucene-fulltext",
                        clazz.getName(), config.getTxModule(),
                        storeDir + "/lucene-fulltext", config.getLockManager(),
                        luceneId, params );
            }
            catch ( ClassNotFoundException e )
            { // ok index util not on class path
            }
            
            try
            {
                Class<?> cls = Class.forName( "org.neo4j.index.impl.lucene.LuceneDataSource" );
                config.getTxModule().registerDataSource( "lucene-index", cls.getName(),
                        "162374".getBytes(), config.getParams() );
            }
            catch ( ClassNotFoundException e )
            { // ok new lucene index not on classpath
            }
        }
        persistenceSource = new NioNeoDbPersistenceSource();
        config.setPersistenceSource( Config.DEFAULT_DATA_SOURCE_NAME, create );
        config.getIdGeneratorModule().setPersistenceSourceInstance(
                persistenceSource );
        config.getTxModule().init();
        config.getPersistenceModule().init();
        persistenceSource.init();
        config.getIdGeneratorModule().init();
        config.getGraphDbModule().init();

        config.getTxModule().start();
        config.getPersistenceModule().start(
                config.getTxModule().getTxManager(), persistenceSource,
                syncHookFactory );
        persistenceSource.start( config.getTxModule().getXaDataSourceManager() );
        config.getIdGeneratorModule().start();
        config.getGraphDbModule().start( config.getLockReleaser(),
                config.getPersistenceModule().getPersistenceManager(), params );
        if ( "true".equals( params.get( Config.DUMP_CONFIGURATION ) ) )
        {
            for ( Object key : params.keySet() )
            {
                if ( key instanceof String )
                {
                    Object value = params.get( key );
                    if ( value instanceof String )
                    {
                        System.out.println( key + "=" + value );
                    }
                }
            }
        }
        
        if ( config.getTxModule().getXaDataSourceManager().hasDataSource( "lucene-index" ) )
        {
            config.getTxModule().getXaDataSourceManager().unregisterDataSource( "lucene-index" );
        }
        
        started = true;
        return Collections.unmodifiableMap( params );
    }

    private void cleanWriteLocksInLuceneDirectory( String luceneDir )
    {
        File dir = new File( luceneDir );
        if ( !dir.isDirectory() )
        {
            return;
        }
        for ( File file : dir.listFiles() )
        {
            if ( file.isDirectory() )
            {
                cleanWriteLocksInLuceneDirectory( file.getAbsolutePath() );
            }
            else if ( file.getName().equals( "write.lock" ) )
            {
                boolean success = file.delete();
                assert success;
            }
        }
    }

    private XaDataSource registerLuceneDataSource( String name,
            String className, TxModule txModule, String luceneDirectory,
            LockManager lockManager, byte[] resourceId, 
            Map<Object,Object> params )
    {
        params.put( "dir", luceneDirectory );
        params.put( LockManager.class, lockManager );
        return txModule.registerDataSource( name, className, resourceId,
                params, true );
    }

    /**
     * Returns true if Neo4j is started.
     *
     * @return True if Neo4j started
     */
    public boolean started()
    {
        return started;
    }

    /**
     * Shut down Neo4j.
     */
    public synchronized void shutdown()
    {
        if ( started )
        {
            config.getGraphDbModule().stop();
            config.getIdGeneratorModule().stop();
            persistenceSource.stop();
            config.getPersistenceModule().stop();
            config.getTxModule().stop();
            config.getGraphDbModule().destroy();
            config.getIdGeneratorModule().destroy();
            persistenceSource.destroy();
            config.getPersistenceModule().destroy();
            config.getTxModule().destroy();
        }
        started = false;
    }

    public Iterable<RelationshipType> getRelationshipTypes()
    {
        return config.getGraphDbModule().getRelationshipTypes();
    }

    public boolean transactionRunning()
    {
        try
        {
            return config.getTxModule().getTxManager().getTransaction() != null;
        }
        catch ( Exception e )
        {
            throw new TransactionFailureException(
                    "Unable to get transaction.", e );
        }
    }

    public TransactionManager getTransactionManager()
    {
        return config.getTxModule().getTxManager();
    }
}