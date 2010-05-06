package org.neo4j.index.future.lucene;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

class TxData
{
//    private final Map<String, Map<Object, Set<Long>>> map =
//        new HashMap<String, Map<Object,Set<Long>>>();
    private final DirectoryAndWorkers luceneData;
    private final LuceneIndex index;
    private final Analyzer analyzer;
    
    TxData( LuceneIndex index, Analyzer analyzer )
    {
        this.index = index;
        this.analyzer = analyzer;
        this.luceneData = newDirectory();
    }
    
    void add( Long entityId, String key, Object value )
    {
        try
        {
            Document document = new Document();
            index.getIndexType().fillDocument( document, entityId, key, value );
            luceneData.writer.addDocument( document );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    void remove( Long entityId, String key, Object value )
    {
        try
        {
            IndexType type = index.getIndexType();
            Query deletionQuery = type.deletionQuery( entityId, key, value );
            luceneData.writer.deleteDocuments( deletionQuery );
            luceneData.invalidateSearcher();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    Set<Long> getEntityIds( Query query )
    {
        try
        {
            Hits hits = luceneData.getSearcher().search( query );
            HashSet<Long> result = new HashSet<Long>();
            for ( int i = 0; i < hits.length(); i++ )
            {
                result.add( Long.parseLong( hits.doc( i ).getField(
                    LuceneIndex.KEY_DOC_ID ).stringValue() ) );
            }
            return result;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    void close()
    {
        luceneData.close();
    }

    private DirectoryAndWorkers newDirectory()
    {
        Directory directory = new RAMDirectory();
        try
        {
            return new DirectoryAndWorkers( directory );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    private IndexWriter newIndexWriter( Directory directory )
            throws IOException
    {
        return new IndexWriter( directory, analyzer,
            MaxFieldLength.UNLIMITED );
    }
    
    private class DirectoryAndWorkers
    {
        private final Directory directory;
        private final IndexWriter writer;
        private IndexSearcher searcher;
        
        private DirectoryAndWorkers( Directory directory )
            throws IOException
        {
            this.directory = directory;
            this.writer = newIndexWriter( directory );
        }
        
        private void invalidateSearcher()
        {
            safeClose( this.searcher );
            this.searcher = null;
        }
        
        private void close()
        {
            safeClose( this.writer );
            invalidateSearcher();
        }
        
        private IndexSearcher getSearcher()
        {
            try
            {
                this.writer.commit();
                if ( this.searcher == null )
                {
                    this.searcher = new IndexSearcher( directory, true );
                }
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
            return this.searcher;
        }
    }

    private static void safeClose( Object object )
    {
        if ( object == null )
        {
            return;
        }
        
        try
        {
            if ( object instanceof IndexWriter )
            {
                ( ( IndexWriter ) object ).close();
            }
            else if ( object instanceof IndexSearcher )
            {
                ( ( IndexSearcher ) object ).close();
            }
        }
        catch ( IOException e )
        {
            // Ok
        }
    }
}
