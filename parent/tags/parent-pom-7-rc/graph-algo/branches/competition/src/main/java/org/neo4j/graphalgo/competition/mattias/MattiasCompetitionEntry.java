/*
 * Copyright 2010 Network Engine for Objects in Lund AB [neotechnology.com]
 * 
 * This program is free software: you can redistribute it and/or modify
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.graphalgo.competition.mattias;

import org.neo4j.graphalgo.competition.CompetitionEntry;
import org.neo4j.graphalgo.competition.ShortestPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;

public class MattiasCompetitionEntry implements CompetitionEntry
{
    public ShortestPath shortestPath( int maxlength, RelationshipType type,
            Direction dir ) throws NotImplementedException
    {
        throw new NotImplementedException();
    }

    public ShortestPath shortestPath( int maxlength, RelationshipType type1,
            Direction dir1, RelationshipType type2, Direction dir2 )
            throws NotImplementedException
    {
        throw new NotImplementedException();
    }

    public ShortestPath shortestPath( int maxlength, RelationshipType type1,
            Direction dir1, RelationshipType type2, Direction dir2,
            Object... more ) throws NotImplementedException
    {
        throw new NotImplementedException();
    }
}
