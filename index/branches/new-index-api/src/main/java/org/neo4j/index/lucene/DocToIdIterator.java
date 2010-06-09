package org.neo4j.index.lucene;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.neo4j.commons.iterator.PrefetchingIterator;
import org.neo4j.graphdb.index.IndexHits;

class DocToIdIterator extends PrefetchingIterator<Long> implements IndexHits<Long>
{
    private final SearchResult searchResult;
    private final Collection<Long> exclude;
    private final IndexSearcherRef searcherOrNull;
    private final Set<Long> alreadyReturnedIds = new HashSet<Long>();
    
    DocToIdIterator( SearchResult searchResult, Collection<Long> exclude,
        IndexSearcherRef searcherOrNull )
    {
        this.searchResult = searchResult;
        this.exclude = exclude;
        this.searcherOrNull = searcherOrNull;
    }

    @Override
    protected Long fetchNextOrNull()
    {
        Long result = null;
        while ( result == null )
        {
            if ( !searchResult.documents.hasNext() )
            {
                endReached();
                break;
            }
            Document doc = searchResult.documents.next();
            long id = Long.parseLong(
                doc.getField( LuceneIndex.KEY_DOC_ID ).stringValue() );
            if ( exclude == null || !exclude.contains( id ) )
            {
                if ( alreadyReturnedIds.add( id ) )
                {
                    result = id;
                }
            }
        }
        return result;
    }
    
    private void endReached()
    {
        if ( this.searcherOrNull != null )
        {
            this.searcherOrNull.closeStrict();
        }
    }

    public int size()
    {
        return searchResult.size;
    }

    public void close()
    {
    }

    public Iterator<Long> iterator()
    {
        return this;
    }
}
