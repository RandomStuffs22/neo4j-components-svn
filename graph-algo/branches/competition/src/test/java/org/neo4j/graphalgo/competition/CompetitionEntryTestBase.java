package org.neo4j.graphalgo.competition;

import java.lang.reflect.Method;

import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.RunWith;
import org.neo4j.graphalgo.competition.CompetitionEntry.NotImplementedException;
import org.neo4j.graphalgo.competition.PhaseRunner.Phase;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * This is the base class for the competition test suite. To enter the
 * competition create a subclass of this class with your name and implement the
 * {@link #initialize(GraphDatabaseService) initialize method}.
 */
@RunWith( PhaseRunner.class )
public abstract class CompetitionEntryTestBase
{
    protected abstract CompetitionEntry initialize( GraphDatabaseService graphdb );

    final CompetitionEntry create( GraphDatabaseService graphdb )
    {
        return new ExceptionConverter<NotImplementedException>(
                NotImplementedException.class )
        {
            @Override
            protected Object convert( Method cause,
                    NotImplementedException exception )
            {
                throw new AssumptionViolatedException( cause.getName()
                                                       + " is not implemented." );
            }
        }.proxy( CompetitionEntry.class, initialize( graphdb ) );
    }

    public @Phase( 1 )
    CorrectnessTest correctnessShortestPath()
    {
        return new ShortestPathCorrectness( this );
    }

    public @Phase( 2 )
    PerformanceTest performanceShortestPath()
    {
        return new ShortestPathPerformance( this );
    }
}
