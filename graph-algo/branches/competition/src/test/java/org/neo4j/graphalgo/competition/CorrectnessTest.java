package org.neo4j.graphalgo.competition;

import org.neo4j.graphdb.GraphDatabaseService;

abstract class CorrectnessTest extends TestBase
{
    CorrectnessTest( CompetitionEntryTestBase base )
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
