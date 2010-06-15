package org.neo4j.index.lucene;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.BatchInserterIndex;
import org.neo4j.graphdb.index.BatchInserterIndexProvider;
import org.neo4j.kernel.impl.batchinsert.BatchInserter;
import org.neo4j.kernel.impl.batchinsert.BatchInserterImpl;

public class LuceneBatchInserterIndexProvider implements BatchInserterIndexProvider
{
    private final BatchInserter inserter;
    private final Map<IndexIdentifier, LuceneBatchInserterIndex> indexes =
            new HashMap<IndexIdentifier, LuceneBatchInserterIndex>();
    final LuceneIndexStore store;
    final IndexTypeCache typeCache;

    public LuceneBatchInserterIndexProvider( BatchInserter inserter )
    {
        String dbStoreDir = ((BatchInserterImpl) inserter).getStore();
        this.inserter = inserter;
        this.store = LuceneDataSource.newIndexStore( dbStoreDir );
        this.typeCache = new IndexTypeCache( store );
    }
    
    public BatchInserterIndex nodeIndex( String indexName, Map<String, String> config )
    {
        return index( new IndexIdentifier( Node.class, indexName, config ) );
    }

    public BatchInserterIndex relationshipIndex( String indexName, Map<String, String> config )
    {
        return index( new IndexIdentifier( Relationship.class, indexName, config ) );
    }

    private BatchInserterIndex index( IndexIdentifier identifier )
    {
        // We don't care about threads here... c'mon... it's a
        // single-threaded batch inserter
        LuceneBatchInserterIndex index = indexes.get( identifier );
        if ( index == null )
        {
            index = new LuceneBatchInserterIndex( this, inserter, identifier );
            indexes.put( identifier, index );
        }
        return index;
    }
    
    public void shutdown()
    {
        for ( LuceneBatchInserterIndex index : indexes.values() )
        {
            index.shutdown();
        }
    }
}
