package org.neo4j.graphalgo.competition;

import org.neo4j.graphdb.GraphDatabaseService;

abstract class PerformanceTest extends TestBase
{
    PerformanceTest( CompetitionEntryTestBase base )
    {
        super( base );
    }

    @Override
    GraphDatabaseService createGraphDatabase()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
