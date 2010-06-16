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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexProvider;

/**
 * An implementation of {@link GraphDatabaseService} that is used to embed Neo4j
 * in an application. You typically instantiate it by invoking the
 * {@link #EmbeddedGraphDatabase(String) single argument constructor} that takes
 * a path to a directory where Neo4j will store its data files, as such:
 *
 * <pre>
 * <code>
 * GraphDatabaseService graphDb = new EmbeddedGraphDatabase( &quot;var/graphdb&quot; );
 * // ... use Neo4j
 * graphDb.shutdown();
 * </code>
 * </pre>
 *
 * For more information, see {@link GraphDatabaseService}.
 */
public final class EmbeddedGraphDatabase implements GraphDatabaseService
{
    private final EmbeddedGraphDbImpl graphDbImpl;

    /**
     * Creates an embedded {@link GraphDatabaseService} with a store located in
     * <code>storeDir</code>, which will be created if it doesn't already exist.
     *
     * @param storeDir the store directory for the Neo4j store files
     */
    public EmbeddedGraphDatabase( String storeDir )
    {
        this( storeDir, new HashMap<String, String>() );
    }

    /**
     * A non-standard way of creating an embedded {@link GraphDatabaseService}
     * with a set of configuration parameters. Will most likely be removed in
     * future releases.
     * <p>
     * Creates an embedded {@link GraphDatabaseService} with a store located in
     * <code>storeDir</code>, which will be created if it doesn't already exist.
     *
     * @param storeDir the store directory for the db files
     * @param params configuration parameters
     */
    public EmbeddedGraphDatabase( String storeDir, Map<String,String> params )
    {
        this.graphDbImpl = new EmbeddedGraphDbImpl( storeDir, params, this );
    }

    /**
     * A non-standard convenience method that loads a standard property file and
     * converts it into a generic <Code>Map<String,String></CODE>. Will most
     * likely be removed in future releases.
     *
     * @param file the property file to load
     * @return a map containing the properties from the file
     * @throws IllegalArgumentException if file does not exist
     */
    public static Map<String,String> loadConfigurations( String file )
    {
        return EmbeddedGraphDbImpl.loadConfigurations( file );
    }

    public Node createNode()
    {
        return graphDbImpl.createNode();
    }

    public Node getNodeById( long id )
    {
        return graphDbImpl.getNodeById( id );
    }

    public Relationship getRelationshipById( long id )
    {
        return graphDbImpl.getRelationshipById( id );
    }

    public Node getReferenceNode()
    {
        return graphDbImpl.getReferenceNode();
    }

    public void shutdown()
    {
        graphDbImpl.shutdown();
    }

    public boolean enableRemoteShell()
    {
        return graphDbImpl.enableRemoteShell();
    }

    public boolean enableRemoteShell(
        final Map<String,Serializable> initialProperties )
    {
        return graphDbImpl.enableRemoteShell( initialProperties );
    }

    public Iterable<RelationshipType> getRelationshipTypes()
    {
        return graphDbImpl.getRelationshipTypes();
    }

    /**
     * @throws TransactionFailureException if unable to start transaction
     */
    public Transaction beginTx()
    {
        return graphDbImpl.beginTx();
    }

    /**
     * Returns a non-standard configuration object. Will most likely be removed
     * in future releases.
     *
     * @return a configuration object
     */
    public Config getConfig()
    {
        return graphDbImpl.getConfig();
    }

    public <T> T getManagementBean( Class<T> type )
    {
        return graphDbImpl.getManagementBean( type );
    }

    @Override
    public String toString()
    {
        return super.toString() + " [" + graphDbImpl.getStoreDir() + "]";
    }

    public String getStoreDir()
    {
        return graphDbImpl.getStoreDir();
    }

    public Iterable<Node> getAllNodes()
    {
        return graphDbImpl.getAllNodes();
    }

    public void registerKernelEventHandler(
            KernelEventHandler handler )
    {
        this.graphDbImpl.registerKernelEventHandler( handler );
    }

    public <T> void registerTransactionEventHandler(
            TransactionEventHandler<T> handler )
    {
        this.graphDbImpl.registerTransactionEventHandler( handler );
    }

    public void unregisterKernelEventHandler(
            KernelEventHandler handler )
    {
        this.graphDbImpl.unregisterKernelEventHandler( handler );
    }

    public <T> void unregisterTransactionEventHandler(
            TransactionEventHandler<T> handler )
    {
        this.graphDbImpl.unregisterTransactionEventHandler( handler );
    }
    
    public static boolean isReadOnly( GraphDatabaseService graphDb )
    {
        return graphDb instanceof EmbeddedReadOnlyGraphDatabase;
    }

    public Index<Node> nodeIndex( String indexName )
    {
        return this.graphDbImpl.nodeIndex( indexName, null );
    }
    
    /**
     * Returns an {@link Index} for {@link Node}s by the name {@code name}.
     * If that index exists (has been requested before) and the configuration
     * was the same as {@code config} it's returned, else if the configuration
     * differs an {@link IllegalArgumentException} is thrown. If the index
     * doesn't exist it's created with the given configuration. For example:
     * 
     * <code>
     * Map<String, String> config = new HashMap<String, String>();
     * config.put( "provider", "lucene" );
     * config.put( "type", "fulltext" );
     * Index<Node> index = embeddedGraphDb.nodeIndex( "users", config );
     * ....
     * Index<Node> index = embeddedGraphDb.nodeIndex( "users" );
     * </code>
     * 
     * Would result in the second call to nodeIndex would return a lucene
     * fulltext index for "users".
     * 
     * An index comes from an
     * {@link IndexProvider} and which index provider to use is decided via:
     * 
     * <ul>
     * <li>Look at stored configuration for the given index</li>
     * <li>Look at given configuration map for "provider" key</li>
     * <li>Look at configuration parameter
     * <b>index.node.[name]</b>, f.ex. <b>index.node.users</b></li>
     * <li>Look at configuration parameter <b>index.node</b></li>
     * <li>Look at configuration parameter <b>index</b></li>
     * <li>Default to lucene index provider</li>
     * </ul>
     * 
     * The index provider value can be a class name pointing to an
     * {@link IndexProvider} implementation, or a service name specified
     * by that provider, f.ex. "lucene". Once an index has be created that same
     * index will be returned for the same {@code name}, even if configuration
     * changes between runs.
     * 
     * @param name the name of the index to return.
     * @return an {@link Index} for {@link Node}s corresponding to the
     * {@code name}.
     */
    public Index<Node> nodeIndex( String indexName, Map<String, String> config )
    {
        return this.graphDbImpl.nodeIndex( indexName, config );
    }

    public Index<Relationship> relationshipIndex( String indexName )
    {
        return this.graphDbImpl.relationshipIndex( indexName, null );
    }

    /**
     * Returns an {@link Index} for {@link Relationship}s by the name
     * {@code name}. If that index exists (has been requested before) and the
     * configuration was the same as {@code config} it's returned, else if the
     * configuration differs an {@link IllegalArgumentException} is thrown.
     * If the index doesn't exist it's created with the given configuration.
     * For example:
     * 
     * <code>
     * Map<String, String> config = new HashMap<String, String>();
     * config.put( "provider", "lucene" );
     * config.put( "type", "fulltext" );
     * Index<Relationship> index = embeddedGraphDb.nodeIndex( "users", config );
     * ....
     * Index<Relationship> index = embeddedGraphDb.nodeIndex( "users" );
     * </code>
     * 
     * Would result in the second call to nodeIndex would return a lucene
     * fulltext index for "users".
     * 
     * An index comes from an
     * {@link IndexProvider} and which index provider to use is decided via:
     * 
     * <ul>
     * <li>Look at stored configuration for the given index</li>
     * <li>Look at given configuration map for "provider" key</li>
     * <li>Look at configuration parameter
     * <b>index.relationship.[name]</b>, f.ex.
     * <b>index.relationship.users</b></li>
     * <li>Look at configuration parameter <b>index.relationship</b></li>
     * <li>Look at configuration parameter <b>index</b></li>
     * <li>Default to lucene index provider</li>
     * </ul>
     * 
     * The index provider value can be a class name pointing to an
     * {@link IndexProvider} implementation, or a service name specified
     * by that provider, f.ex. "lucene". Once an index has be created that same
     * index will be returned for the same {@code name}, even if configuration
     * changes between runs.
     * 
     * @param name the name of the index to return.
     * @return an {@link Index} for {@link Relationship}s corresponding to the
     * {@code name}.
     */
    public Index<Relationship> relationshipIndex( String indexName, Map<String, String> config )
    {
        return this.graphDbImpl.relationshipIndex( indexName, config );
    }
}
