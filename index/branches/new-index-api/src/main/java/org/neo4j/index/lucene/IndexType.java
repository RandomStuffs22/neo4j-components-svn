package org.neo4j.index.lucene;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.util.Version;

abstract class IndexType
{
    private static final Map<String, IndexType> TYPES =
            Collections.synchronizedMap( new HashMap<String, IndexType>() );
    
    private static final IndexType EXACT = new IndexType( LuceneDataSource.KEYWORD_ANALYZER )
    {
        @Override
        public Query deletionQuery( long entityId, String key, Object value )
        {
            BooleanQuery q = new BooleanQuery();
            q.add( new TermQuery( new Term( LuceneIndex.KEY_DOC_ID, "" + entityId ) ),
                    Occur.MUST );
            q.add( new TermQuery( new Term( key, value.toString() ) ), Occur.MUST );
            return q;
        }

        @Override
        public Query get( String key, Object value )
        {
            return new TermQuery( new Term( key, value.toString() ) );
        }

        @Override
        public void fillDocument( Document document, long entityId, String key,
                Object value )
        {
            addIdToDocument( document, entityId );
            document.add( new Field( key, value.toString(), Store.NO, Index.NOT_ANALYZED ) );
        }
        
        public TxData newTxData(LuceneIndex index)
        {
            return new ExactTxData( index );
        }
    };
    
    private static class FulltextType extends IndexType
    {
        FulltextType( Analyzer analyzer )
        {
            super( analyzer );
        }
        
        @Override
        public Query deletionQuery( long entityId, String key, Object value )
        {
            BooleanQuery q = new BooleanQuery();
            q.add( new TermQuery( new Term( LuceneIndex.KEY_DOC_ID, "" + entityId ) ),
                    Occur.MUST );
            q.add( new TermQuery( new Term( exactKey( key ), value.toString() ) ), Occur.MUST );
            return q;
        }

        @Override
        public Query get( String key, Object value )
        {
            return new TermQuery( new Term( exactKey( key ), value.toString() ) );
        }

        private String exactKey( String key )
        {
            return key + "_e";
        }

        @Override
        public void fillDocument( Document document, long entityId, String key,
                Object value )
        {
            String valueAsString = value.toString();
            addIdToDocument( document, entityId );
            document.add( new Field( exactKey( key ), valueAsString, Store.NO,
                    Index.NOT_ANALYZED ) );
            document.add( new Field( key, valueAsString, Store.NO, Index.ANALYZED ) );
        }
        
        @Override
        public TxData newTxData( LuceneIndex index )
        {
            return new FullTxData( index );
        }
    };
    
    final Analyzer analyzer;
    
    private IndexType( Analyzer analyzer )
    {
        this.analyzer = analyzer;
    }
    
    private static String configKey( String indexName, String property )
    {
        return property;
    }
    
    static IndexType getIndexType( Map<String, Map<String, String>> storedConfig,
            Map<String, String> customConfig, String indexName )
    {
        IndexType existingType = TYPES.get( indexName );
        if ( existingType != null )
        {
            return existingType;
        }
        
        Map<String, String> prioConfig = storedConfig.containsKey( indexName ) ?
                storedConfig.get( indexName ) : customConfig;
        prioConfig = prioConfig != null ? prioConfig : Collections.<String, String>emptyMap();
        
        // TODO If it exists in the storedConfig then verify against customConfig
        // and tell user to f-ck off if they differ?
        
        String type = prioConfig.get( configKey( indexName, "type" ) );
        IndexType result = null;
        if ( type == null || type.equals( "exact" ) )
        {
            result = EXACT;
        }
        else if ( type.equals( "fulltext" ) )
        {
            result = new FulltextType( getAnalyzer( prioConfig, indexName ) );
        }
        else
        {
            throw new RuntimeException( "Unknown type '" + type + "' for index '" +
                    indexName + "'" );
        }
        // Two or more threads might instantiate and put an IndexType
        // representing the same type more than once (if done simultaneously),
        // and it's OK.
        TYPES.put( indexName, result );
        if ( prioConfig == customConfig )
        {
            storedConfig.put( indexName, new HashMap<String, String>( customConfig ) );
        }
        return result;
    }
    
    private static Analyzer getAnalyzer( Map<String, String> config, String indexName )
    {
        String analyzerClass = config.get( configKey( indexName, "analyzer" ) );
        if ( analyzerClass != null )
        {
            try
            {
                return Class.forName( analyzerClass ).asSubclass( Analyzer.class ).newInstance();
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e );
            }
        }
        
        String lowerCase = config.get( configKey( indexName, "to_lower_case" ) );
        if ( lowerCase == null || Boolean.parseBoolean( lowerCase ) )
        {
            return LuceneDataSource.LOWER_CASE_WHITESPACE_ANALYZER;
        }
        else
        {
            return LuceneDataSource.WHITESPACE_ANALYZER;
        }
    }

    abstract Query deletionQuery( long entityId, String key, Object value );
    
    abstract Query get( String key, Object value );
    
    abstract TxData newTxData( LuceneIndex index );
    
    Query query( String keyOrNull, Object value )
    {
        if ( value instanceof Query )
        {
            return (Query) value;
        }
        
        QueryParser parser = new QueryParser( Version.LUCENE_30, keyOrNull, analyzer );
        parser.setAllowLeadingWildcard( true );
        try
        {
            return parser.parse( value.toString() );
        }
        catch ( ParseException e )
        {
            throw new RuntimeException( e );
        }
    }

    abstract void fillDocument( Document document, long entityId, String key,
            Object value );
    
    void addIdToDocument( Document document, long id )
    {
        document.add( new Field( LuceneIndex.KEY_DOC_ID, "" + id, Store.YES, Index.NOT_ANALYZED ) );
    }
}
