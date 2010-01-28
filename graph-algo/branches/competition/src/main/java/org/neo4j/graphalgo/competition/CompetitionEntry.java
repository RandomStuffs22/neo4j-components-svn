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
package org.neo4j.graphalgo.competition;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;

/**
 * An entry in the competition.
 */
public interface CompetitionEntry
{
    /**
     * Thrown for methods that are not implemented.
     */
    @SuppressWarnings( "serial" )
    public class NotImplementedException extends Exception
    {
    }

    /**
     * Return an implementation of a shortest path algorithm.
     * 
     * @param maxlength the maximum length to allow searching for shortest paths
     * @param type the {@link RelationshipType} to traverse.
     * @param dir the {@link Direction} to traverse relationships in.
     * @return An implementation of a shortest path algorithm.
     * @throws NotImplementedException if the method is not implemented.
     */
    ShortestPath shortestPath( int maxlength, RelationshipType type,
            Direction dir ) throws NotImplementedException;

    /**
     * Return an implementation of a shortest path algorithm.
     * 
     * @param maxlength the maximum length to allow searching for shortest paths
     * @param type1 the first {@link RelationshipType}
     * @param dir1 the {@link Direction} for the first {@link RelationshipType}
     * @param type2 the second {@link RelationshipType}
     * @param dir2 the {@link Direction} for the second {@link RelationshipType}
     * @return An implementation of a shortest path algorithm.
     * @throws NotImplementedException if the method is not implemented.
     */
    ShortestPath shortestPath( int maxlength, RelationshipType type1,
            Direction dir1, RelationshipType type2, Direction dir2 )
            throws NotImplementedException;

    /**
     * Return an implementation of a shortest path algorithm.
     * 
     * @param maxlength the maximum length to allow searching for shortest paths
     * @param type1 the first {@link RelationshipType}
     * @param dir1 the {@link Direction} for the first {@link RelationshipType}
     * @param type2 the second {@link RelationshipType}
     * @param dir2 the {@link Direction} for the second {@link RelationshipType}
     * @param more more {@link RelationshipType}s and {@link Direction}s
     * @return An implementation of a shortest path algorithm.
     * @throws NotImplementedException if the method is not implemented.
     */
    ShortestPath shortestPath( int maxlength, RelationshipType type1,
            Direction dir1, RelationshipType type2, Direction dir2,
            Object... more ) throws NotImplementedException;
}
