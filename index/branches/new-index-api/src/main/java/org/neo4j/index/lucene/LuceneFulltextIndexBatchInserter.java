/*
 * Copyright (c) 2002-2009 "Neo Technology,"
 *     Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 * 
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.index.lucene;

import org.neo4j.graphdb.index.BatchInserterIndex;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.kernel.impl.batchinsert.BatchInserter;

/**
 * The "batch inserter" version of {@link LuceneFulltextIndexService}. It should
 * be used with a BatchInserter and stores the indexes in the same format as
 * {@link LuceneFulltextIndexService}.
 * 
 * It's optimized for large chunks of either reads or writes. So try to avoid
 * mixed reads and writes because there's a slight overhead to go from read mode
 * to write mode (the "mode" is per key and will not affect other keys)
 * 
 * See more information at {link
 * http://wiki.neo4j.org/content/Indexing_with_BatchInserter the Indexing with
 * BatchInserter wiki page}.
 */
public class LuceneFulltextIndexBatchInserter extends
        LuceneIndexBatchInserterImpl
{
    /**
     * @param inserter the {@link BatchInserter} to use.
     */
    public LuceneFulltextIndexBatchInserter( BatchInserter inserter )
    {
        super( inserter );
    }
    
    @Override
    protected BatchInserterIndex getIndex( String indexName )
    {
        // TODO Make sure the index is a fulltext index
        return super.getIndex( indexName );
    }
    
    @Override
    public IndexHits<Long> getNodes( String key, Object value )
    {
        return getIndex( key ).query( LuceneFulltextIndexService.toQuery( key, value ) );
    }
}
