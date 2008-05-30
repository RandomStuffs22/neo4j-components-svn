package org.neo4j.util.index;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.impl.cache.AdaptiveCacheManager;
import org.neo4j.impl.cache.LruCache;
import org.neo4j.impl.core.NotFoundException;
import org.neo4j.impl.transaction.LockManager;
import org.neo4j.impl.util.ArrayMap;

// TODO: 
// o Move LuceneTransaction to its own file and do general code cleanup
// o Run optimize when starting up
// o Abort all writers (if any) on shutdown
public class LuceneIndexService extends GenericIndexService
{
    final ArrayMap<String,IndexSearcher> indexSearchers = 
        new ArrayMap<String,IndexSearcher>( 6, true, true );
    
    final ArrayMap<Transaction,LuceneTransaction> luceneTransactions
        = new ArrayMap<Transaction,LuceneTransaction>( 5, true, true );
    final LockManager lockManager;
    private final TransactionManager txManager;
    private final String luceneDirectory;
    
    private Map<String, LruCache<Object, Iterable<Long>>> caching =
    	Collections.synchronizedMap(
    		new HashMap<String, LruCache<Object, Iterable<Long>>>() );
    
    private static class WriterLock
    {
        private final String key;
        
        WriterLock( String key )
        {
            this.key = key;
        }
        
        String getKey()
        {
            return key;
        }
        
        @Override
        public int hashCode()
        {
            return key.hashCode();
        }
        
        @Override
        public boolean equals( Object o )
        {
            if ( !(o instanceof WriterLock) )
            {
                return false;
            }
            return this.key.equals( ((WriterLock) o).getKey() );
        }
    }
    
    class LuceneTransaction implements Synchronization
    {
        private final Set<WriterLock> writers = 
            new HashSet<WriterLock>();
        
        private final Map<String,Map<Object,Set<Long>>> txIndexed = 
            new HashMap<String,Map<Object,Set<Long>>>();

        private final Map<String,Map<Object,Set<Long>>> txRemoved = 
            new HashMap<String,Map<Object,Set<Long>>>();
        
        private final Transaction tx;
        
        LuceneTransaction( Transaction tx )
        {
        	this.tx = tx;
        }
        
        boolean hasWriter( WriterLock lock )
        {
            return writers.contains( lock );
        }

        void addWriter( WriterLock lock )
        {
            writers.add( lock );
        }

        void index( Node node, String key, Object value )
        {
        	insert( node, key, value, txRemoved, txIndexed );
        }
        
        void removeIndex( Node node, String key, Object value )
        {
        	insert( node, key, value, txIndexed, txRemoved );
        }
        
        void insert( Node node, String key, Object value,
        	Map<String, Map<Object, Set<Long>>> toRemoveFrom,
        	Map<String, Map<Object, Set<Long>>> toInsertInto )
        {
            delFromIndex( node, key, value, toRemoveFrom );
            Map<Object,Set<Long>> keyIndex = toInsertInto.get( key );
            if ( keyIndex == null )
            {
                keyIndex = new HashMap<Object,Set<Long>>();
                toInsertInto.put( key, keyIndex );
            }
            Set<Long> nodeIds = keyIndex.get( value );
            if ( nodeIds == null )
            {
                nodeIds = new HashSet<Long>();
            }
            nodeIds.add( node.getId() );
            keyIndex.put( value, nodeIds );
        }
        
        boolean delFromIndex( Node node, String key, Object value,
        	Map<String, Map<Object, Set<Long>>> map )
        {
            Map<Object,Set<Long>> keyIndex = map.get( key );
            if ( keyIndex == null )
            {
                return false;
            }
            Set<Long> nodeIds = keyIndex.get( value );
            if ( nodeIds != null )
            {
                return nodeIds.remove( node.getId() );
            }
            return false;
        }
        
        Set<Long> getDeletedNodesFor( String key, Object value )
        {
            Map<Object,Set<Long>> keyIndex = txRemoved.get( key );
            if ( keyIndex != null )
            {
                Set<Long> nodeIds = keyIndex.get( value );
                if ( nodeIds != null )
                {
                    return nodeIds;
                }
            }
            return Collections.emptySet();
        }
        
        Set<Long> getNodesFor( String key, Object value )
        {
            Map<Object,Set<Long>> keyIndex = txIndexed.get( key );
            if ( keyIndex != null )
            {
                Set<Long> nodeIds = keyIndex.get( value );
                if ( nodeIds != null )
                {
                    return nodeIds;
                }
            }
            return Collections.emptySet();
        }
        
        public void afterCompletion( int status )
        {
            luceneTransactions.remove( tx );
            for ( WriterLock lock : writers )
            {
                String key = lock.getKey();
                if ( status == Status.STATUS_COMMITTED )
                {
                    Map<Object,Set<Long>> deleteMap = txRemoved.get( key );
                    if ( deleteMap != null )
                    {
                        IndexSearcher searcher = getIndexSearcher( key );
                        boolean closeAndRemove = false;
                        for ( Entry<Object,Set<Long>> deleteEntry : 
                            deleteMap.entrySet() )
                        {
                            Object value = deleteEntry.getKey();
                            Collection<Long> ids = deleteEntry.getValue();
                            for ( Long id : ids )
                            {
                                closeAndRemove = true;
                                deleteDocumentUsingReader( searcher, id, 
                                    value );
                            }
                            invalidateCache( key, value );
                        }
                        try
                        {
                            if ( closeAndRemove && searcher != null )
                            {
                                indexSearchers.remove( key );
                                searcher.close();
                            }
                        }
                        catch ( IOException e )
                        {
                            e.printStackTrace();
                        }
                    }
                    IndexWriter writer = getIndexWriter( key );
                    Map<Object,Set<Long>> indexMap = txIndexed.get( key );
                    try
                    {
                        if ( indexMap != null )
                        {
                            for ( Entry<Object,Set<Long>> indexEntry : 
                                indexMap.entrySet() )
                            {
                                Object value = indexEntry.getKey();
                                Collection<Long> ids = indexEntry.getValue();
                                for ( Long id : ids )
                                {
                                    indexWriter( writer, id, value );
                                }
                                invalidateCache( key, value );
                            }
                        }
                        writer.close();
                        
                    }
                    catch ( IOException e )
                    {
                        e.printStackTrace();
                    }
                    IndexSearcher searcher = indexSearchers.remove( key );
                    if ( searcher != null )
                    {
                        try
                        {
                            searcher.close();
                        }
                        catch ( IOException e )
                        {
                            e.printStackTrace();
                        }
                    }
                }
                lockManager.releaseWriteLock( lock );
            }
        }


        private void indexWriter( IndexWriter writer, long nodeId, 
            Object value )
        {
            Document document = new Document();
            document.add( new Field( "id", 
                String.valueOf( nodeId ),
                Field.Store.YES, Field.Index.UN_TOKENIZED ) );
            document.add( new Field( "index", value.toString(),
                Field.Store.NO, Field.Index.UN_TOKENIZED ) );
            try
            {
                writer.addDocument( document );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
        
        public void beforeCompletion()
        {
        }
    }
    
    private static class DefaultAnalyzer extends Analyzer
    {
        @Override
        public TokenStream tokenStream( String fieldName, Reader reader )
        {
            return new LowerCaseFilter( new WhitespaceTokenizer( reader ) );
        }
    }
    
    private static final Analyzer DEFAULT_ANALYZER = new DefaultAnalyzer();
    
    public LuceneIndexService( NeoService neo )
    {
        super ( neo );
        EmbeddedNeo embeddedNeo = ((EmbeddedNeo) neo);
        luceneDirectory = 
            embeddedNeo.getConfig().getTxModule().getTxLogDirectory();
        lockManager = embeddedNeo.getConfig().getLockManager();
        txManager = embeddedNeo.getConfig().getTxModule().getTxManager();
    }
    
    synchronized IndexWriter getIndexWriter( String key )
    {
        try
        {
            Directory dir = FSDirectory.getDirectory( 
                luceneDirectory + "/lucene/" + key );
            return new IndexWriter( dir, false, DEFAULT_ANALYZER );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    protected void enableCache( String key, int maxNumberOfCachedEntries )
    {
    	this.caching.put( key, new LruCache<Object, Iterable<Long>>(
    		key, maxNumberOfCachedEntries, new AdaptiveCacheManager() ) );
    }
    
    @Override
    protected void indexThisTx( Node node, String key, 
        Object value )
    {
        try
        {
	        Transaction tx = txManager.getTransaction();
	        LuceneTransaction luceneTx = luceneTransactions.get( tx );
	        if ( luceneTx == null )
	        {
	            luceneTx = new LuceneTransaction( tx );
	            luceneTransactions.put( tx, luceneTx );
	                tx.registerSynchronization( luceneTx );
	        }
	        WriterLock lock = new WriterLock( key );
	        if ( !luceneTx.hasWriter( lock ) )
	        {
	            lockManager.getWriteLock( lock );
	            luceneTx.addWriter( lock );
	        }
	        luceneTx.index( node, key, value );
        }
        catch ( SystemException e )
        {
            throw new IllegalStateException( "No transaction running?", e );
        }
        catch ( RollbackException e )
        {
            throw new IllegalStateException( 
                "Unable to register synchronization hook", e );
        }
    }
    
    IndexSearcher getIndexSearcher( String key )
    {
        IndexSearcher searcher = indexSearchers.get( key );
        if ( searcher == null )
        {
            try
            {
                Directory dir = FSDirectory.getDirectory( 
                    luceneDirectory + "/lucene/" + key );
                if ( dir.list().length == 0 )
                {
                    return null;
                }
                searcher = new IndexSearcher( dir );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
            indexSearchers.put( key, searcher );
        }
        return searcher;
    }
    
    public Iterable<Node> getNodes( String key, Object value )
    {
        IndexSearcher searcher = getIndexSearcher( key );
        List<Node> nodes = new ArrayList<Node>();
        Transaction tx = null;
        try
        {
        	tx = txManager.getTransaction();
        }
        catch ( SystemException e )
        {
            throw new IllegalStateException( "No transaction running?", e );
        }
        LuceneTransaction luceneTx = luceneTransactions.get( tx );
        Set<Long> addedNodes = Collections.emptySet();
        Set<Long> deletedNodes = Collections.emptySet();
        if ( luceneTx != null )
        {
            addedNodes = luceneTx.getNodesFor( key, value );
            for ( long id : addedNodes )
            {
                nodes.add( getNeo().getNodeById( id ) );
            }
            deletedNodes = luceneTx.getDeletedNodesFor( key, value );
        }
        if ( searcher != null )
        {
        	LruCache<Object, Iterable<Long>> cachedNodesMap =
        		caching.get( key );
        	boolean foundInCache = false;
        	if ( cachedNodesMap != null )
        	{
        		Iterable<Long> cachedNodes = cachedNodesMap.get( value );
        		if ( cachedNodes != null )
        		{
        			foundInCache = true;
        			for ( Long cachedNodeId : cachedNodes )
        			{
        				nodes.add( getNeo().getNodeById( cachedNodeId ) );
        			}
        		}
        	}
        	
        	if ( !foundInCache )
        	{
        		Iterable<Node> readNodes =
        			searchForNodes( key, value, deletedNodes );
        		ArrayList<Long> readNodeIds = new ArrayList<Long>();
        		for ( Node readNode : readNodes )
        		{
        			nodes.add( readNode );
        			readNodeIds.add( readNode.getId() );
        		}
        		if ( cachedNodesMap != null )
        		{
        			cachedNodesMap.add( value, readNodeIds );
        		}
        	}
        }
        return nodes;
    }
    
    private Iterable<Node> searchForNodes( String key, Object value,
    	Set<Long> deletedNodes )
    {
    	IndexSearcher searcher = getIndexSearcher( key );
        Query query = new TermQuery( 
            new Term( "index", value.toString() ) );
        try
        {
        	ArrayList<Node> nodes = new ArrayList<Node>();
            Hits hits = searcher.search( query );
            for ( int i = 0; i < hits.length(); i++ )
            {
                Document document = hits.doc( i );
                try
                {
                    long id = Integer.parseInt(
                        document.getField( "id" ).stringValue() );
                    if ( !deletedNodes.contains( id ) )
                    {
                        nodes.add( getNeo().getNodeById( id ) );
                    }
                }
                catch ( NotFoundException e )
                {
                    // deleted in this tx
                }
            }
            return nodes;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Unable to search for " + key +
                "," + value, e );
        }
    }
    
    public Node getSingleNode( String key, Object value )
    {
/*        LuceneTransaction luceneTx = luceneTransactions.get();
        Set<Long> deletedNodes = luceneTx != null ? 
            luceneTx.getDeletedNodesFor( key, value ) : null;
        Set<Long> addedNodes = luceneTx != null ? 
            luceneTx.getNodesFor( key, value ) : null;
        Node node = null;
        IndexSearcher searcher = getIndexSearcher( key );
        if ( searcher == null )
        {
            if ( addedNodes != null ) 
            {
                if ( addedNodes.size() == 1 )
                {
                    return getNeo().getNodeById( addedNodes.iterator().next() );
                }
                else if ( addedNodes.size() > 1 )
                {
                    throw new RuntimeException( "More than one node " + 
                        "found for: " + key + "," + value );
                }
            }
            return null;
        }
        else
        {
            Query query = new TermQuery( 
                new Term( "index", value.toString() ) );
            try
            {
                Hits hits = searcher.search( query );
                for ( int i = 0; i < hits.length(); i++ )
                {
                    Document document = hits.doc( i );
                    long id = Integer.parseInt(
                        document.getField( "id" ).stringValue() );
                    if ( deletedNodes != null && deletedNodes.contains( id ) )
                    {
                        continue;
                    }
                    if ( node != null )
                    {
                    	if ( node.getId() != id )
                    	{
	                        throw new RuntimeException( "More than one " + 
	                        	"node found for: " + key + "," + value );
                    	}
                    }
                    else
                    {
                    	node = getNeo().getNodeById( id );
                    }
                }
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "Unable to search for " + key + 
                    "," + value, e );
            }
        }
        return node;*/
    	
    	// TODO Temporary code since the commented code (above) doesn't work.
    	Iterator<Node> nodes = getNodes( key, value ).iterator();
    	Node node = nodes.hasNext() ? nodes.next() : null;
    	if ( nodes.hasNext() )
    	{
    		throw new RuntimeException( "More than one node for " + key + "=" +
    			value );
    	}
    	return node;
    }

    boolean documentExist( Long id, String key, Object value )
    {
        IndexSearcher searcher = getIndexSearcher( key );
        if ( searcher == null )
        {
            return false;
        }
        Query query = new TermQuery( new Term( "index", value.toString() ) );
        try
        {
            Hits hits = searcher.search( query );
            for ( int i = 0; i < hits.length(); i++ )
            {
                Document document = hits.doc( 0 );
                int foundId = Integer.parseInt(
                    document.getField( "id" ).stringValue() );
                if ( id == foundId )
                {
                    return true;
                }
            }
            return false;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Problem checking for " + id +"," + 
                key + "," + value, e );
        }
    }
    
    private void invalidateCache( String key, Object value )
    {
    	LruCache<Object, Iterable<Long>> cache = caching.get( key );
    	if ( cache != null )
    	{
    		cache.remove( value );
    	}
    }
    
    void deleteDocumentUsingReader( IndexSearcher searcher, long nodeId, 
        Object value )
    {
        if ( searcher == null )
        {
            return;
        }
        Query query = new TermQuery( new Term( "index", value.toString() ) );
        try
        {
            Hits hits = searcher.search( query );
            for ( int i = 0; i < hits.length(); i++ )
            {
                Document document = hits.doc( 0 );
                int foundId = Integer.parseInt(
                    document.getField( "id" ).stringValue() );
                if ( nodeId == foundId )
                {
                    int docNum = hits.id( i );
                    searcher.getIndexReader().deleteDocument( docNum );
                }
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Unable to delete for " + nodeId +"," + 
                "," + value + " using" + searcher, e );
        }
    }
    
    @Override
    protected void removeIndexThisTx( Node node, String key, Object value )
    {
        try
        {
            Transaction tx = txManager.getTransaction();
            LuceneTransaction luceneTx = luceneTransactions.get( tx );
	        if ( luceneTx == null )
	        {
	            luceneTx = new LuceneTransaction( tx );
	            luceneTransactions.put( tx, luceneTx );
	                tx.registerSynchronization( luceneTx );
	        }
	        WriterLock lock = new WriterLock( key );
	        if ( !luceneTx.hasWriter( lock ) )
	        {
	            lockManager.getWriteLock( lock );
	            luceneTx.addWriter( lock );
	        }
	        luceneTx.removeIndex( node, key, value );
        }
        catch ( SystemException e )
        {
            throw new IllegalStateException( "No transaction running?", e );
        }
        catch ( RollbackException e )
        {
            throw new IllegalStateException( 
                "Unable to register synchronization hook", e );
        }
    }
    
    @Override
    public synchronized void shutdown()
    {
        super.shutdown();
        for ( IndexSearcher searcher : indexSearchers.values() )
        {
            try
            {
                searcher.close();
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
        }
    }
}