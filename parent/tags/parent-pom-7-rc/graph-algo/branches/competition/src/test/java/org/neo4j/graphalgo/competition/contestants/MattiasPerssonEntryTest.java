package org.neo4j.graphalgo.competition.contestants;

import org.neo4j.graphalgo.competition.CompetitionEntry;
import org.neo4j.graphalgo.competition.CompetitionEntryTestBase;
import org.neo4j.graphalgo.competition.mattias.MattiasCompetitionEntry;
import org.neo4j.graphdb.GraphDatabaseService;

public class MattiasPerssonEntryTest extends CompetitionEntryTestBase
{
    @Override
    protected CompetitionEntry initialize( GraphDatabaseService graphdb )
    {
        return new MattiasCompetitionEntry();
    }
}
