package org.neo4j.graphalgo.competition;

import org.junit.After;
import org.junit.Before;
import org.junit.internal.builders.IgnoredClassRunner;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

@RunWith( IgnoredClassRunner.class )
public abstract class TestBase
{
    private final CompetitionEntryTestBase base;
    private GraphDatabaseService graphdb;
    private CompetitionEntry entry;

    TestBase( CompetitionEntryTestBase base )
    {
        this.base = base;
    }

    protected final Transaction beginTx()
    {
        return graphdb.beginTx();
    }

    protected final Node getNodeById( long id )
    {
        return graphdb.getNodeById( id );
    }

    @Before
    public final void createCompetitionEntry()
    {
        graphdb = createGraphDatabase();
        entry = base.create( graphdb );
    }

    @After
    public final void shutdownGraphDatabase()
    {
        entry = null;
        if ( graphdb != null )
        {
            graphdb.shutdown();
            graphdb = null;
        }
    }

    abstract GraphDatabaseService createGraphDatabase();
}
