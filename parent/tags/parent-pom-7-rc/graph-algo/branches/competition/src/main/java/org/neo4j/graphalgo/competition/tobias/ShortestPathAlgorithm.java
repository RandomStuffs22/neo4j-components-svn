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
package org.neo4j.graphalgo.competition.tobias;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.neo4j.graphalgo.competition.Path;
import org.neo4j.graphalgo.competition.ShortestPath;
import org.neo4j.graphdb.Node;

class ShortestPathAlgorithm implements ShortestPath
{
    private final int maxlength;
    private final RelationshipExpander expander;

    ShortestPathAlgorithm( int maxlength, RelationshipExpander expander )
    {
        this.maxlength = maxlength;
        this.expander = expander;
    }

    public Path onePath( Node start, Node end )
    {
        Collection<Path> paths = paths( true, start, end );
        if ( paths.size() == 0 )
        {
            return null;
        }
        assert paths.size() == 1 : "Returned more than one shortest path";
        return paths.iterator().next();
    }

    public Collection<Path> allPaths( Node start, Node end )
    {
        return paths( false, start, end );
    }

    private Collection<Path> paths( final boolean single, Node start, Node end )
    {
        if ( start.equals( end ) )
        {
            return Arrays.asList( PathType.singular( start ) );
        }
        else
        {
            final EvaluationState sharedState = new EvaluationState( single );
            EvaluationStage source, target;
            source = new InitialStage( sharedState, true, start );
            target = new InitialStage( sharedState, false, end );
            Collection<Path> result = new LinkedList<Path>();
            for ( int depth = 0; depth < maxlength && result.isEmpty(); depth++ )
            {
                source = source.evaluate( result, target );
                if ( source == null || !result.isEmpty() ) break;
                if ( source.size > target.size ) // swap
                {
                    EvaluationStage stage = source;
                    source = target;
                    target = stage;
                }
            }
            return Collections.unmodifiableCollection( result );
        }
    }

    final class EvaluationState
    {
        private final boolean single;

        EvaluationState( boolean single )
        {
            this.single = single;
        }
    }

    static abstract class EvaluationStage
    {
        final EvaluationState state;
        private final boolean isStart;
        final int size;

        EvaluationStage( EvaluationState state, boolean isStart, int size )
        {
            this.state = state;
            this.isStart = isStart;
            this.size = size;
        }

        /*
         * Returns the new stage to replace this or null if no further
         * expansions can be made.
         */
        EvaluationStage evaluate( Collection<Path> result,
                EvaluationStage target )
        {
            throw new UnsupportedOperationException( "Not implemented yet." );
        }
    }

    static class InitialStage extends EvaluationStage
    {
        final Node node;

        InitialStage( EvaluationState state, boolean isStart, Node node )
        {
            super( state, isStart, 1 );
            this.node = node;
        }
    }

    static class IncompleteStage extends EvaluationStage
    {
        IncompleteStage( EvaluationState state, boolean isStart )
        {
            super( state, isStart, 0 ); // TODO: implement this
        }
    }

    static class FullStage extends EvaluationStage
    {
        FullStage( EvaluationState state, boolean isStart )
        {
            super( state, isStart, 0 ); // TODO: implement this
        }
    }
}
