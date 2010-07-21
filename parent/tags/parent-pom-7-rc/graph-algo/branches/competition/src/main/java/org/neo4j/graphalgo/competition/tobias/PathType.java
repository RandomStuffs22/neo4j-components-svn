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
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.neo4j.graphalgo.competition.Path;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

final class PathType implements Path
{
    public static final class PathBuilder
    {
        private final PathBuilder previous;
        private final Node start;
        private final Relationship relationship;
        private final int size;

        public PathBuilder( Node start )
        {
            if ( start == null )
            {
                throw new NullPointerException();
            }
            this.start = start;
            this.previous = null;
            this.relationship = null;
            this.size = 0;
        }

        private PathBuilder( PathBuilder prev, Relationship rel )
        {
            this.start = prev.start;
            this.previous = prev;
            this.relationship = rel;
            this.size = prev.size + 1;
        }

        public Node getStartNode()
        {
            return start;
        }

        public Path build()
        {
            return new PathType( this, null );
        }

        public PathBuilder push(
                @SuppressWarnings( "hiding" ) Relationship relationship )
        {
            if ( relationship == null )
            {
                throw new NullPointerException();
            }
            return new PathBuilder( this, relationship );
        }

        public Path build( PathBuilder other )
        {
            return new PathType( this, other );
        }

        @Override
        public String toString()
        {
            if ( previous == null )
            {
                return start.toString();
            }
            else
            {
                return relToString( relationship ) + ":" + previous.toString();
            }
        }
    }

    private static String relToString( Relationship rel )
    {
        return rel.getStartNode() + "--" + rel.getType() + "-->"
               + rel.getEndNode();
    }

    private final Node start;
    private final Relationship[] path;

    private PathType( PathBuilder left, PathBuilder right )
    {
        path = new Relationship[left.size + ( right == null ? 0 : right.size )];
        if ( right != null )
        {
            for ( int i = left.size, total = i + right.size; i < total; i++ )
            {
                path[i] = right.relationship;
                right = right.previous;
            }
            assert right.relationship == null : "right Path.Builder size error";
        }
        for ( int i = left.size - 1; i >= 0; i-- )
        {
            path[i] = left.relationship;
            left = left.previous;
        }
        assert left.relationship == null : "left Path.Builder size error";
        start = left.start;
    }

    public static Path singular( Node start )
    {
        return new PathBuilder( start ).build();
    }

    public Node getStartNode()
    {
        return start;
    }

    public Node getEndNode()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Iterable<Node> getNodes()
    {
        return new Iterable<Node>()
        {
            public Iterator<Node> iterator()
            {
                return new Iterator<Node>()
                {
                    Node current = start;
                    int index = 0;

                    public boolean hasNext()
                    {
                        return index <= path.length;
                    }

                    public Node next()
                    {
                        if ( current == null )
                        {
                            throw new NoSuchElementException();
                        }
                        Node next = null;
                        if ( index < path.length )
                        {
                            next = path[index].getOtherNode( current );
                        }
                        index += 1;
                        try
                        {
                            return current;
                        }
                        finally
                        {
                            current = next;
                        }
                    }

                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public Iterable<Relationship> getRelationships()
    {
        return Collections.unmodifiableCollection( Arrays.asList( path ) );
    }

    public int length()
    {
        return path.length;
    }

    @Override
    public String toString()
    {
        Node current = start;
        StringBuilder result = new StringBuilder();
        for ( Relationship rel : path )
        {
            result.append( current );
            String prefix = "--", suffix = "--";
            if ( current.equals( rel.getEndNode() ) )
                prefix = "<--";
            else
                suffix = "-->";
            result.append( prefix );
            result.append( rel.getType() );
            result.append( ".[" );
            result.append( rel.getId() );
            result.append( "]" );
            result.append( suffix );
            current = rel.getOtherNode( current );
        }
        result.append( current );
        return result.toString();
    }
}
