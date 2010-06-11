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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.Query;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.index.lucene.LuceneCommand.AddCommand;
import org.neo4j.index.lucene.LuceneCommand.RemoveCommand;
import org.neo4j.index.lucene.LuceneCommand.RemoveQueryCommand;
import org.neo4j.kernel.impl.transaction.xaframework.XaCommand;
import org.neo4j.kernel.impl.transaction.xaframework.XaLogicalLog;
import org.neo4j.kernel.impl.transaction.xaframework.XaTransaction;

class LuceneTransaction extends XaTransaction
{
    private final Map<IndexIdentifier, TxDataBoth> txData =
            new HashMap<IndexIdentifier, TxDataBoth>();
    private final LuceneDataSource dataSource;

    private final Map<IndexIdentifier,List<LuceneCommand>> commandMap = 
            new HashMap<IndexIdentifier,List<LuceneCommand>>();

    LuceneTransaction( int identifier, XaLogicalLog xaLog,
        LuceneDataSource luceneDs )
    {
        super( identifier, xaLog );
        this.dataSource = luceneDs;
    }

    <T extends PropertyContainer> void add( LuceneIndex<T> index, T entity,
            String key, Object value )
    {
        TxDataBoth data = getTxData( index, true );
        insert( index, entity, key, value, data.added( true ), data.removed( false ) );
        queueCommand( new AddCommand( index.identifier,
                getEntityId( entity ), key, value.toString() ) );
    }
    
    private long getEntityId( PropertyContainer entity )
    {
        return entity instanceof Node ? ((Node) entity).getId() :
                ((Relationship) entity).getId();
    }
    
    <T extends PropertyContainer> TxDataBoth getTxData( LuceneIndex<T> index,
            boolean createIfNotExists )
    {
        IndexIdentifier identifier = index.identifier;
        TxDataBoth data = txData.get( identifier );
        if ( data == null && createIfNotExists )
        {
            data = new TxDataBoth( index.type );
            txData.put( identifier, data );
        }
        return data;
    }

    <T extends PropertyContainer> void remove( LuceneIndex<T> index, T entity,
            String key, Object value )
    {
        TxDataBoth data = getTxData( index, true );
        insert( index, entity, key, value, data.removed( true ), data.added( false ) );
        queueCommand( new RemoveCommand( index.identifier,
                getEntityId( entity ), key, value.toString() ) );
    }
    
    <T extends PropertyContainer> void remove( LuceneIndex<T> index,
            Query query )
    {
        TxDataBoth data = getTxData( index, true );
        TxData added = data.added( false );
        if ( added != null )
        {
            added.remove( query );
        }
        TxData removed = data.removed( true );
        IndexSearcherRef searcher = dataSource.getIndexSearcher( index.identifier );
        if ( searcher != null )
        {
            Iterator<Document> docs = index.search( searcher, query ).documents;
            while ( docs.hasNext() )
            {
                removed.add( docs.next() );
            }
        }
        queueCommand( new RemoveQueryCommand( index.identifier, -1, "", query.toString() ) );
    }
    
    private void queueCommand( LuceneCommand command )
    {
        IndexIdentifier indexId = command.getIndexIdentifier();
        List<LuceneCommand> commands = commandMap.get( indexId );
        if ( commands == null )
        {
            commands = new ArrayList<LuceneCommand>();
            commandMap.put( indexId, commands );
        }
        commands.add( command );
    }
    
    private <T extends PropertyContainer> void insert( LuceneIndex<T> index,
            T entity, String key, Object value, TxData insertInto, TxData removeFrom )
    {
        long id = getEntityId( entity );
        if ( removeFrom != null )
        {
            removeFrom.remove( id, key, value );
        }
        insertInto.add( id, key, value );
    }

    <T extends PropertyContainer> Set<Long> getRemovedIds( LuceneIndex<T> index,
            Query query )
    {
        TxDataBoth data = getTxData( index, false );
        if ( data == null )
        {
            return Collections.emptySet();
        }
        TxData removed = data.removed( false );
        if ( removed == null )
        {
            return Collections.emptySet();
        }
        Set<Long> ids = removed.getEntityIds( query );
        return ids != null ? ids : Collections.<Long>emptySet();
    }
    
    <T extends PropertyContainer> Set<Long> getAddedIds( LuceneIndex<T> index,
            Query query )
    {
        TxDataBoth data = getTxData( index, false );
        if ( data == null )
        {
            return Collections.emptySet();
        }
        TxData added = data.added( false );
        if ( added == null )
        {
            return Collections.emptySet();
        }
        Set<Long> ids = added.getEntityIds( query );
        return ids != null ? ids : Collections.<Long>emptySet();
    }
    
    private void writeToIndex( IndexWriter writer, IndexIdentifier identifier,
            long entityId, String key, Object value )
    {
        Document document = new Document();
        identifier.getType( dataSource.store.indexConfig ).fillDocument(
                document, entityId, key, value );
        try
        {
            writer.addDocument( document );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected void doAddCommand( XaCommand command )
    { // we override inject command and manage our own in memory command list
    }
    
    @Override
    protected void injectCommand( XaCommand command )
    {
        queueCommand( ( LuceneCommand ) command );
    }

    @Override
    protected void doCommit()
    {
        dataSource.getWriteLock();
        try
        {
            for ( Map.Entry<IndexIdentifier, List<LuceneCommand>> entry :
                this.commandMap.entrySet() )
            {
                IndexIdentifier identifier = entry.getKey();
                IndexWriter writer = dataSource.getIndexWriter( identifier );
                for ( LuceneCommand command : entry.getValue() )
                {
                    long entityId = command.getEntityId();
                    String key = command.getKey();
                    String value = command.getValue();
                    if ( command instanceof AddCommand )
                    {
                        writeToIndex( writer, identifier, entityId, key, value );
                    }
                    else if ( command instanceof RemoveCommand )
                    {
                        dataSource.deleteDocuments( writer, identifier,
                                entityId, key, value );
                    }
                    else if ( command instanceof RemoveQueryCommand )
                    {
                        dataSource.deleteDocuments( writer, identifier, command.getValue() );
                    }
                    else
                    {
                        throw new RuntimeException( "Unknown command type " +
                            command + ", " + command.getClass() );
                    }
                }
                dataSource.removeWriter( writer );
                dataSource.invalidateIndexSearcher( identifier );
            }
            closeTxData();
        }
        finally
        {
            dataSource.releaseWriteLock();
        }
    }

    private void closeTxData()
    {
        for ( TxDataBoth data : this.txData.values() )
        {
            data.close();
        }
        this.txData.clear();
    }

    @Override
    protected void doPrepare()
    {
        for ( List<LuceneCommand> list : commandMap.values() )
        {
            for ( LuceneCommand command : list )
            {
                addCommand( command );
            }
        }
    }

    @Override
    protected void doRollback()
    {
        // TODO Auto-generated method stub
        commandMap.clear();
        closeTxData();
    }

    @Override
    public boolean isReadOnly()
    {
        return false;
    }
    
    // Bad name
    private class TxDataBoth
    {
        private final IndexType indexType;
        private TxData add;
        private TxData remove;
        
        public TxDataBoth( IndexType indexType )
        {
            this.indexType = indexType;
        }
        
        TxData added( boolean createIfNotExists )
        {
            if ( this.add == null && createIfNotExists )
            {
                this.add = new TxData( indexType );
            }
            return this.add;
        }
        
        TxData removed( boolean createIfNotExists )
        {
            if ( this.remove == null && createIfNotExists )
            {
                this.remove = new TxData( indexType );
            }
            return this.remove;
        }
        
        void close()
        {
            safeClose( add );
            safeClose( remove );
        }

        private void safeClose( TxData data )
        {
            if ( data != null )
            {
                data.close();
            }
        }
    }
}
