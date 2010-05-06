package org.neo4j.index.future.lucene;

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
import org.neo4j.graphdb.PropertyContainer;

abstract class IndexType
{
    private static final Map<String, IndexType> TYPES =
            Collections.synchronizedMap( new HashMap<String, IndexType>() );
    
    private static final IndexType EXACT = new IndexType( "e" )
    {
        @Override
        public <T extends PropertyContainer> TxData newTxData(
                LuceneIndex<T> index )
        {
            return new TxData( index, LuceneDataSource.KEYWORD_ANALYZER );
        }

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
        public Query query( String key, Object value )
        {
            return parseQuery( key, value, LuceneDataSource.KEYWORD_ANALYZER );
        }

        @Override
        public void fillDocument( Document document, long entityId, String key,
                Object value )
        {
            document.add( new Field( LuceneIndex.KEY_DOC_ID, "" + entityId, Store.YES,
                    Index.NOT_ANALYZED ) );
            document.add( new Field( key, value.toString(), Store.NO,
                    Index.NOT_ANALYZED ) );
        }
    };
    
    private static class FulltextType extends IndexType
    {
        private final Analyzer analyzer;
        
        FulltextType( String shortName, Analyzer analyzer )
        {
            super( shortName );
            this.analyzer = analyzer;
        }
        
        @Override
        public <T extends PropertyContainer> TxData newTxData(
                LuceneIndex<T> index )
        {
            return new TxData( index, analyzer );
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

        @Override
        public Query query( String key, Object value )
        {
            return parseQuery( key, value, this.analyzer );
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
            document.add( new Field( LuceneIndex.KEY_DOC_ID, "" + entityId,
                    Store.YES, Index.NOT_ANALYZED ) );
            document.add( new Field( exactKey( key ), valueAsString, Store.NO,
                    Index.NOT_ANALYZED ) );
            document.add( new Field( key, valueAsString, Store.NO,
                    Index.ANALYZED ) );
        }
    };
    
    private final String shortName;
    
    private IndexType( String shortName )
    {
        this.shortName = shortName;
    }
    
    public String shortName()
    {
        return this.shortName;
    }
    
    private static String key( String indexName, String property )
    {
        String key = "index." + indexName;
        if ( property != null )
        {
            key += "." + property;
        }
        return key;
    }
    
    static IndexType getIndexType( Map<Object, Object> config, String indexName )
    {
        IndexType existingType = TYPES.get( indexName );
        if ( existingType != null )
        {
            return existingType;
        }
        
        String type = (String) config.get( key( indexName, null ) );
        IndexType result = null;
        if ( type == null )
        {
            result = EXACT;
        }
        else if ( type.equals( "exact" ) )
        {
            result = EXACT;
        }
        else if ( type.equals( "fulltext" ) )
        {
            result = new FulltextType( type, getAnalyzer( config, indexName ) );
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
        return result;
    }
    
    private static Analyzer getAnalyzer( Map<Object, Object> config,
            String indexName )
    {
        String analyzerClass = (String) config.get( key( indexName, "analyzer" ) );
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
        
        String lowerCase = (String) config.get( key( indexName, "to_lower_case" ) );
        if ( lowerCase == null || Boolean.parseBoolean( lowerCase ) )
        {
            return LuceneDataSource.LOWER_CASE_WHITESPACE_ANALYZER;
        }
        else
        {
            return LuceneDataSource.WHITESPACE_ANALYZER;
        }
    }

    abstract <T extends PropertyContainer> TxData newTxData( LuceneIndex<T> index );
    
    abstract Query deletionQuery( long entityId, String key, Object value );
    
    abstract Query get( String key, Object value );
    
    abstract Query query( String key, Object value );

    abstract void fillDocument( Document document, long entityId, String key,
            Object value );
    
    private static Query parseQuery( String key, Object value, Analyzer analyzer )
    {
        if ( value instanceof Query )
        {
            return (Query) value;
        }
        
        QueryParser parser = new QueryParser( Version.LUCENE_29, key,
                analyzer );
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
}
