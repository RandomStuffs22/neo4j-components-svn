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

import java.util.Collections;
import java.util.Map;

import org.neo4j.commons.collection.MapUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexProvider;
import org.neo4j.kernel.Config;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.EmbeddedReadOnlyGraphDatabase;
import org.neo4j.kernel.impl.transaction.TxModule;

public class LuceneIndexProvider extends IndexProvider
{
    public static final Map<String, String> EXACT_CONFIG =
            Collections.unmodifiableMap( MapUtil.<String, String>genericOf(
                    "provider", LuceneIndexProvider.class.getName(), "type", "exact" ) );
    
    public static final Map<String, String> FULLTEXT_CONFIG =
            Collections.unmodifiableMap( MapUtil.<String, String>genericOf(
                    "provider", LuceneIndexProvider.class.getName(), "type", "fulltext" ) );
    
    public static final int DEFAULT_LAZY_THRESHOLD = 100;
    private static final String DATA_SOURCE_NAME = "lucene";
    
    final ConnectionBroker broker;
    final LuceneDataSource dataSource;
    final int lazynessThreshold = DEFAULT_LAZY_THRESHOLD;
    final GraphDatabaseService graphDb;
    
    public LuceneIndexProvider( GraphDatabaseService graphDb )
    {
        super( "lucene" );
        new Exception( "skdjskjdk" ).printStackTrace();
        this.graphDb = graphDb;

        Config config = getGraphDbConfig();
        TxModule txModule = config.getTxModule();
        dataSource = (LuceneDataSource) txModule.registerDataSource( DATA_SOURCE_NAME,
                LuceneDataSource.class.getName(), LuceneDataSource.DEFAULT_BRANCH_ID,
                config.getParams(), true );
        broker = EmbeddedGraphDatabase.isReadOnly( graphDb ) ?
                new ReadOnlyConnectionBroker( txModule.getTxManager(), dataSource ) :
                new ConnectionBroker( txModule.getTxManager(), dataSource );
    }
    
    private Config getGraphDbConfig()
    {
        return EmbeddedGraphDatabase.isReadOnly( graphDb ) ?
                ((EmbeddedReadOnlyGraphDatabase) graphDb).getConfig() :
                ((EmbeddedGraphDatabase) graphDb).getConfig();
    }
    
    public Index<Node> nodeIndex( String indexName, Map<String, String> config )
    {
        return new LuceneIndex.NodeIndex( this, new IndexIdentifier(
                Node.class, indexName, config ) );
    }
    
    public Index<Relationship> relationshipIndex( String indexName, Map<String, String> config )
    {
        return new LuceneIndex.RelationshipIndex( this, new IndexIdentifier(
                Relationship.class, indexName, config ) );
    }
}
