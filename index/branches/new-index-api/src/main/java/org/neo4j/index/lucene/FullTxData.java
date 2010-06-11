package org.neo4j.index.lucene;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.AllDocs;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

class FullTxData extends TxData
{
    private DirectoryAndWorkers luceneData;
    
    FullTxData( LuceneIndex index )
    {
        super( index );
    }
    
    TxData add( Long entityId, String key, Object value )
    {
        ensureLuceneDataInstantiated();
        Document document = new Document();
        index.type.fillDocument( document, entityId, key, value );
        return add( document );
    }
    
    private void ensureLuceneDataInstantiated()
    {
        if ( luceneData == null )
        {
            luceneData = newDirectory();
        }
    }

    TxData add( Document document )
    {
        try
        {
            ensureLuceneDataInstantiated();
            luceneData.writer.addDocument( document );
            return this;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    TxData remove( Long entityId, String key, Object value )
    {
        return remove( index.type.deletionQuery( entityId, key, value ) );
    }
    
    TxData remove( Query query )
    {
        ensureLuceneDataInstantiated();
        index.service.getDataSource().deleteDocuments( luceneData.writer, query );
        luceneData.invalidateSearcher();
        return this;
    }
    
    Map.Entry<Set<Long>, TxData> getEntityIds( Query query )
    {
        if ( luceneData == null )
        {
            return new MapEntry<Set<Long>, TxData>( Collections.<Long>emptySet(), this );
        }
        
        try
        {
            AllDocs hits = new AllDocs( luceneData.getSearcher(), query, null );
            HashSet<Long> result = new HashSet<Long>();
            for ( int i = 0; i < hits.length(); i++ )
            {
                result.add( Long.parseLong( hits.doc( i ).getField(
                    LuceneIndex.KEY_DOC_ID ).stringValue() ) );
            }
            return new MapEntry<Set<Long>, TxData>( result, this );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    void close()
    {
        if ( luceneData != null )
        {
            luceneData.close();
        }
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
        return new IndexWriter( directory, index.type.analyzer, MaxFieldLength.UNLIMITED );
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
                if ( this.searcher == null )
                {
                    this.writer.commit();
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

    @Override
    Map.Entry<Set<Long>, TxData> getEntityIds( String key, Object value )
    {
        return getEntityIds( this.index.type.get( key, value ) );
    }
}
