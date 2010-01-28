package org.neo4j.graphalgo.competition.contestants;

import org.neo4j.graphalgo.competition.CompetitionEntry;
import org.neo4j.graphalgo.competition.CompetitionEntryTestBase;
import org.neo4j.graphalgo.competition.tobias.TobiasCompetitionEntry;
import org.neo4j.graphdb.GraphDatabaseService;

public class TobiasIvarssonEntryTest extends CompetitionEntryTestBase
{
    @Override
    protected CompetitionEntry initialize( GraphDatabaseService graphdb )
    {
        return new TobiasCompetitionEntry();
    }
}
