package org.neo4j.index.lucene;

import java.io.IOException;

import org.apache.lucene.AllDocs;
import org.apache.lucene.document.Document;
import org.neo4j.commons.iterator.PrefetchingIterator;

class HitsIterator extends PrefetchingIterator<Document>
{
    private final AllDocs hits;
    private final int size;
    private int index;
    
    public HitsIterator( AllDocs hits )
    {
        this.hits = hits;
        this.size = hits.length();
    }

    @Override
    protected Document fetchNextOrNull()
    {
        int i = index++;
        try
        {
            return i < size ? hits.doc( i ) : null;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
}
