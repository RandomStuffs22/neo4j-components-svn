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
package org.neo4j.index.future.lucene;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.event.ErrorState;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexProvider;
import org.neo4j.kernel.Config;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.EmbeddedReadOnlyGraphDatabase;
import org.neo4j.kernel.impl.transaction.TxModule;

public class LuceneIndexProvider extends IndexProvider implements
        KernelEventHandler
{
    public static final int DEFAULT_LAZY_SEARCH_RESULT_THRESHOLD = 100;
    private static final String DATA_SOURCE_NAME = "lucene-index";
    
    private ConnectionBroker broker;
    private LuceneDataSource dataSource;
    int lazynessThreshold = DEFAULT_LAZY_SEARCH_RESULT_THRESHOLD;
    final GraphDatabaseService graphDb;
    
    public LuceneIndexProvider( GraphDatabaseService graphDb )
    {
        super( "lucene", "lucene_fulltext" );
        this.graphDb = graphDb;
    }
    
    private Config getGraphDbConfig()
    {
        return EmbeddedGraphDatabase.isReadOnly( graphDb ) ?
                ((EmbeddedReadOnlyGraphDatabase) graphDb).getConfig() :
                ((EmbeddedGraphDatabase) graphDb).getConfig();
    }
    
    private void ensureDataSourceRegistered()
    {
        if ( this.dataSource == null )
        {
            Config config = getGraphDbConfig();
            TxModule txModule = config.getTxModule();
            dataSource = (LuceneDataSource) txModule.registerDataSource( DATA_SOURCE_NAME,
                    LuceneDataSource.class.getName(), LuceneDataSource.DEFAULT_BRANCH_ID,
                    config.getParams(), true );
            broker = EmbeddedGraphDatabase.isReadOnly( graphDb ) ?
                    new ReadOnlyConnectionBroker( txModule.getTxManager(), dataSource ) :
                    new ConnectionBroker( txModule.getTxManager(), dataSource );
        }
    }

    private IndexType getIndexType( String indexName )
    {
        return IndexType.getIndexType( dataSource.config, indexName );
    }
    
    ConnectionBroker getBroker()
    {
        return this.broker;
    }
    
    LuceneDataSource getDataSource()
    {
        return this.dataSource;
    }
    
    public Index<Node> nodeIndex( String indexName )
    {
        ensureDataSourceRegistered();
        return new LuceneIndex.NodeIndex( this, new IndexIdentifier(
                Node.class, indexName, getIndexType( indexName ) ) );
    }
    
    public Index<Relationship> relationshipIndex( String indexName )
    {
        ensureDataSourceRegistered();
        return new LuceneIndex.RelationshipIndex( this, new IndexIdentifier(
                Relationship.class, indexName, getIndexType( indexName ) ) );
    }

    public void beforeShutdown()
    {
        TxModule txModule = ((EmbeddedGraphDatabase) graphDb).getConfig().getTxModule();
        if ( txModule.getXaDataSourceManager().hasDataSource( DATA_SOURCE_NAME ) )
        {
            txModule.getXaDataSourceManager().unregisterDataSource(
                    DATA_SOURCE_NAME );
        }
        dataSource.close();
    }

    public Object getResource()
    {
        return this;
    }

    public void kernelPanic( ErrorState error )
    {
        // Do nothing
    }

    public ExecutionOrder orderComparedTo( KernelEventHandler other )
    {
        return ExecutionOrder.DOESNT_MATTER;
    }
}
