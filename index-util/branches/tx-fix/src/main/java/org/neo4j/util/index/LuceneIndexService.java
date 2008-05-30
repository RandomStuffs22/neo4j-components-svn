package org.neo4j.util.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.impl.cache.LruCache;
import org.neo4j.impl.core.NotFoundException;
import org.neo4j.impl.transaction.LockManager;
import org.neo4j.impl.transaction.NotInTransactionException;
import org.neo4j.impl.transaction.TxModule;
import org.neo4j.impl.util.ArrayMap;

// TODO: 
// o Run optimize when starting up
public class LuceneIndexService extends GenericIndexService
{
    
    private final TransactionManager txManager;
    private final String luceneDirectory;
    private final ConnectionBroker broker;
    
    private final LuceneDataSource xaDs; 
    
    public LuceneIndexService( NeoService neo )
    {
        super ( neo );
        EmbeddedNeo embeddedNeo = ((EmbeddedNeo) neo);
        luceneDirectory = 
            embeddedNeo.getConfig().getTxModule().getTxLogDirectory() + 
            "/lucene";
        TxModule txModule = embeddedNeo.getConfig().getTxModule();
        txManager = txModule.getTxManager();
        byte resourceId[] = "162373".getBytes();
        Map<Object,Object> params = getDefaultParams();
        params.put( "dir", luceneDirectory );
        params.put( LockManager.class, 
            embeddedNeo.getConfig().getLockManager() ); 
        xaDs = (LuceneDataSource) txModule.registerDataSource( "lucene", 
            LuceneDataSource.class.getName(), resourceId, params, true );
        broker = new ConnectionBroker( txManager, xaDs );
    }
    
    private Map<Object,Object> getDefaultParams()
    {
        Map<Object,Object> params = new HashMap<Object,Object>();
        params.put( LuceneIndexService.class, this );
        return params;
    }
    
    
    protected void enableCache( String key, int maxNumberOfCachedEntries )
    {
        xaDs.enableCache( key, maxNumberOfCachedEntries );
    }
    
    @Override
    protected void indexThisTx( Node node, String key, 
        Object value )
    {
        getConnection().index( node, key, value );
    }
    
    
    public Iterable<Node> getNodes( String key, Object value )
    {
        IndexSearcher searcher = xaDs.getIndexSearcher( key );
        List<Node> nodes = new ArrayList<Node>();
        LuceneTransaction luceneTx = getConnection().getLuceneTx();
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
        		xaDs.getFromCache( key );
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
    	IndexSearcher searcher = xaDs.getIndexSearcher( key );
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

    @Override
    protected void removeIndexThisTx( Node node, String key, Object value )
    {
        getConnection().removeIndex( node, key, value );
    }
    
    @Override
    public synchronized void shutdown()
    {
        super.shutdown();
        xaDs.close();
    }

    LuceneXaConnection getConnection()
    {
        return broker.acquireResourceConnection();
    }
    
    private static class ConnectionBroker
    {
        private final ArrayMap<Transaction,LuceneXaConnection> txConnectionMap =
            new ArrayMap<Transaction,LuceneXaConnection>( 5, true, true );

        private final TransactionManager transactionManager;
        private final LuceneDataSource xaDs;
        
        ConnectionBroker( TransactionManager transactionManager, 
            LuceneDataSource xaDs ) 
        {
            this.transactionManager = transactionManager;
            this.xaDs = xaDs;
        }

        LuceneXaConnection acquireResourceConnection()
        {
            LuceneXaConnection con      = null;
            Transaction tx = this.getCurrentTransaction();        
            con = txConnectionMap.get( tx );
            if ( con == null )
            {
                try
                {
                    con = (LuceneXaConnection) xaDs.getXaConnection();
                    if ( !tx.enlistResource( con.getXaResource() ) )
                    {
                        throw new RuntimeException( "Unable to enlist '" + 
                            con.getXaResource() + "' in " + tx );
                    }
                    tx.registerSynchronization( new TxCommitHook( tx ) );
                    txConnectionMap.put( tx, con );
                }
                catch ( javax.transaction.RollbackException re )
                {
                    String msg = "The transaction is marked for rollback only.";
                    throw new RuntimeException( msg, re );
                }
                catch ( javax.transaction.SystemException se )
                {
                    String msg = 
                        "TM encountered an unexpected error condition.";
                    throw new RuntimeException( msg, se );
                }
            }
            return con;
        }
        
        void releaseResourceConnectionsForTransaction( Transaction tx ) 
            throws NotInTransactionException
        {
            LuceneXaConnection con = txConnectionMap.remove( tx );
            if ( con != null )
            {
                con.destroy();
            }
        }
        
        void delistResourcesForTransaction() throws NotInTransactionException
        {
            Transaction tx = this.getCurrentTransaction();
            LuceneXaConnection con = txConnectionMap.get( tx ); 
            if ( con != null )
            {
                try
                {
                    tx.delistResource( con.getXaResource(), 
                        XAResource.TMSUCCESS );
                }
                catch ( IllegalStateException e )
                {
                    throw new RuntimeException( 
                        "Unable to delist lucene resource from tx", e );
                }
                catch ( SystemException e )
                {
                    throw new RuntimeException( 
                        "Unable to delist lucene resource from tx", e );
                }
            }
        }
        
        private Transaction getCurrentTransaction()
            throws NotInTransactionException
        {
            try
            {
                Transaction tx = transactionManager.getTransaction();
                if ( tx == null )
                {
                    throw new NotInTransactionException( 
                        "No transaction found for current thread" );
                }
                return tx;
            }
            catch ( SystemException se )
            {
                throw new NotInTransactionException( 
                    "Error fetching transaction for current thread", se );
            }
        }
        
        private class TxCommitHook implements Synchronization
        {
            private final Transaction tx;
            
            TxCommitHook( Transaction tx )
            {
                this.tx = tx;
            }
            
            public void afterCompletion( int param )
            {
                releaseResourceConnectionsForTransaction( tx );
            }

            public void beforeCompletion()
            {
                delistResourcesForTransaction();
            }
        }
    }
}