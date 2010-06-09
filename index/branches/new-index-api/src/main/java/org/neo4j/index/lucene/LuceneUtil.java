package org.neo4j.index.lucene;

import java.io.IOException;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;

abstract class LuceneUtil
{
    static void close( IndexWriter writer )
    {
        close( (Object) writer );
    }
    
    static void close( IndexSearcher searcher )
    {
        close( (Object) searcher );
    }
    
    private static void close( Object object )
    {
        if ( object == null )
        {
            return;
        }
        
        try
        {
            if ( object instanceof IndexWriter )
            {
                ((IndexWriter) object).close();
            }
            else if ( object instanceof IndexSearcher )
            {
                ((IndexSearcher) object).close();
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
}
