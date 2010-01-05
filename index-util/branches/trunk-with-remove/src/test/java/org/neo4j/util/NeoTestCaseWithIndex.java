package org.neo4j.util;

import org.neo4j.util.index.IndexService;

public abstract class NeoTestCaseWithIndex extends NeoTestCase
{
    private IndexService indexService;
    
    protected abstract IndexService instantiateIndexService();
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        indexService = instantiateIndexService();
    }
    
    protected IndexService indexService()
    {
        return indexService;
    }
    
    @Override
    protected void beforeNeoShutdown()
    {
        indexService().shutdown();
    }
}
