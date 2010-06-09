package org.neo4j.index.lucene;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.BatchInserterIndex;
import org.neo4j.graphdb.index.BatchInserterIndexProvider;
import org.neo4j.kernel.impl.batchinsert.BatchInserter;

public class LuceneBatchInserterIndexProvider implements BatchInserterIndexProvider
{
    private final BatchInserter inserter;
    private final Map<IndexIdentifier, LuceneBatchInserterIndex> indexes =
            new HashMap<IndexIdentifier, LuceneBatchInserterIndex>();

    public LuceneBatchInserterIndexProvider( BatchInserter inserter )
    {
        this.inserter = inserter;
    }
    
    public BatchInserterIndex nodeIndex( String indexName )
    {
        return index( new IndexIdentifier( Node.class, indexName ) );
    }

    public BatchInserterIndex relationshipIndex( String indexName )
    {
        return index( new IndexIdentifier( Relationship.class, indexName ) );
    }

    private BatchInserterIndex index( IndexIdentifier identifier )
    {
        // TODO really synchronization?
        synchronized ( indexes )
        {
            LuceneBatchInserterIndex index = indexes.get( identifier );
            if ( index == null )
            {
                index = new LuceneBatchInserterIndex( inserter, identifier );
                indexes.put( identifier, index );
            }
            return index;
        }
    }
    
    public void shutdown()
    {
        for ( LuceneBatchInserterIndex index : indexes.values() )
        {
            index.shutdown();
        }
    }
}
