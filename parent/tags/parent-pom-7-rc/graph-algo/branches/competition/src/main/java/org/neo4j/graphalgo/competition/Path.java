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

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public interface Path
{
    Node getStartNode();

    Node getEndNode();

    Iterable<Relationship> getRelationships();

    Iterable<Node> getNodes();

    int length();

    /**
     * Return a natural string representation of this Path.
     * 
     * The string representation should show all the nodes and relationships in
     * the path with relationship directions represented.
     * 
     * @return A string representation of the path.
     */
    @Override
    public String toString();
}
