package org.neo4j.index.lucene;

import javax.transaction.TransactionManager;

import org.neo4j.index.ReadOnlyIndexException;

public class ReadOnlyConnectionBroker extends ConnectionBroker
{
    ReadOnlyConnectionBroker( TransactionManager transactionManager,
            LuceneDataSource dataSource )
    {
        super( transactionManager, dataSource );
    }
    
    @Override
    LuceneXaConnection acquireResourceConnection()
    {
        throw new ReadOnlyIndexException();
    }
    
    @Override
    LuceneXaConnection acquireReadOnlyResourceConnection()
    {
        return null;
    }
}
