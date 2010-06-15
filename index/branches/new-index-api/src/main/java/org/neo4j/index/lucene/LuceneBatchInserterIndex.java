package org.neo4j.index.lucene;

import java.io.IOException;

import org.apache.lucene.AllDocs;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.neo4j.graphdb.index.BatchInserterIndex;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.kernel.impl.batchinsert.BatchInserter;
import org.neo4j.kernel.impl.batchinsert.BatchInserterImpl;

public class LuceneBatchInserterIndex implements BatchInserterIndex
{
    private final BatchInserter inserter;
    private final String storeDir;
    private final IndexIdentifier identifier;
    private final IndexType type;
    
    private IndexWriter writer;
    private boolean writerModified;
    private IndexSearcher searcher;

    LuceneBatchInserterIndex( LuceneBatchInserterIndexProvider provider,
            BatchInserter inserter, IndexIdentifier identifier )
    {
        String dbStoreDir = ((BatchInserterImpl) inserter).getStore();
        this.storeDir = LuceneDataSource.getStoreDir( dbStoreDir );
        this.inserter = inserter;
        this.identifier = identifier;
        this.type = provider.typeCache.getIndexType( identifier );
    }
    
    public void add( long entityId, String key, Object value )
    {
        Document doc = new Document();
        type.fillDocument( doc, entityId, key, value );
        LuceneUtil.strictAddDocument( writer(), doc );
        setModified();
    }
    
    private void setModified()
    {
        writerModified = true;
    }

    private IndexWriter writer()
    {
        if ( this.writer != null )
        {
            return this.writer;
        }
        try
        {
            this.writer = new IndexWriter( LuceneDataSource.getDirectory( storeDir, identifier ),
                    type.analyzer, MaxFieldLength.UNLIMITED );
            return this.writer;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    private void closeSearcher()
    {
        try
        {
            LuceneUtil.close( this.searcher );
        }
        finally
        {
            this.searcher = null;
        }
    }

    private IndexSearcher searcher()
    {
        IndexSearcher result = this.searcher;
        try
        {
            if ( result == null || writerModified )
            {
                if ( result != null )
                {
                    result.getIndexReader().close();
                    result.close();
                }
                writer().commit();
                IndexReader newReader = writer().getReader();
                result = new IndexSearcher( newReader );
                writerModified = false;
            }
            return result;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            this.searcher = result;
        }
    }
    
    private void closeWriter()
    {
        try
        {
            if ( this.writer != null )
            {
                this.writer.optimize( true );
            }
            LuceneUtil.close( this.writer );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            this.writer = null;
        }
    }

    private IndexHits<Long> query( Query query )
    {
        try
        {
            AllDocs hits = new AllDocs( searcher(), query, null );
            SearchResult result = new SearchResult( new HitsIterator( hits ),
                    hits.length() );
            return new DocToIdIterator( result, null, null );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    public IndexHits<Long> get( String key, Object value )
    {
        return query( type.get( key, value ) );
    }

    public IndexHits<Long> query( String key, Object queryOrQueryObject )
    {
        return query( type.query( key, queryOrQueryObject ) );
    }

    public IndexHits<Long> query( Object queryOrQueryObject )
    {
        return query( type.query( null, queryOrQueryObject ) );
    }

    public void remove( long entityId, String key, Object value )
    {
        LuceneUtil.strictRemoveDocument( writer(), type.deletionQuery( entityId, key, value ) );
        setModified();
    }
    
    public void remove( long entityId, Object queryOrQueryObjectOrNull )
    {
        remove( type.combine( entityId, queryOrQueryObjectOrNull ) );
    }
    
    public void remove( Object queryOrQueryObject )
    {
        remove( type.query( null, queryOrQueryObject ) );
    }
    
    private void remove( Query query )
    {
        try
        {
            writer().deleteDocuments( query );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        setModified();
    }

    public void shutdown()
    {
        closeWriter();
        closeSearcher();
    }
    
    public void flush()
    {
        try
        {
            writer().commit();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
}
