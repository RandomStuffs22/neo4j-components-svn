/*
 * Copyright (c) 2002-2009 "Neo Technology,"
 *     Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 * 
 * Neo4j is free software: you can redistribute it and/or modify
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.RelationshipType;

class IntArrayIterator<T> implements Iterable<T>, Iterator<T>
{
//    private Logger log = Logger
//        .getLogger( IntArrayIterator.class.getName() );

    private Iterator<RelTypeElementIterator> typeIterator;
    private RelTypeElementIterator currentTypeIterator = null;
    private final NodeImpl fromNode;
    private final Direction direction;
    private T nextElement;
    private final NodeManager nodeManager;
    private final RelationshipType types[];

    private Set<String> visitedTypes = new HashSet<String>();
    private final RelationshipGetter<T> relGetter;
    
    IntArrayIterator( List<RelTypeElementIterator> rels, NodeImpl fromNode,
        Direction direction, NodeManager nodeManager, RelationshipType[] types,
        RelationshipGetter<T> relGetter )
    {
        this.relGetter = relGetter;
        this.typeIterator = rels.iterator();
        if ( typeIterator.hasNext() )
        {
            currentTypeIterator = typeIterator.next();
            visitedTypes.add( currentTypeIterator.getType() );
        }
        else
        {
            currentTypeIterator = new NullRelTypeElement();
        }
        this.fromNode = fromNode;
        this.direction = direction;
        this.nodeManager = nodeManager;
        this.types = types;
    }

//    IntArrayIterator( Iterator<RelTypeElementIterator> rels, NodeImpl fromNode,
//        Direction direction, NodeManager nodeManager )
//    {
//        this.typeIterator = rels;
//        this.fromNode = fromNode;
//        this.direction = direction;
//        this.nodeManager = nodeManager;
//    }
    
    public Iterator<T> iterator()
    {
        return this;
    }

    public boolean hasNext()
    {
        if ( nextElement != null )
        {
            return true;
        }
        do
        {
            if ( currentTypeIterator.hasNext( nodeManager ) )
            {
                int nextId = currentTypeIterator.next( nodeManager );
                try
                {
                    T possibleElement = relGetter.getRelationship( nodeManager, nextId );
                    if ( direction == Direction.INCOMING
                         && relGetter.getEndNodeId( possibleElement )== fromNode.id )
                    {
                        nextElement = possibleElement;
                        return true;
                    }
                    else if ( direction == Direction.OUTGOING
                            && relGetter.getStartNodeId( possibleElement ) == fromNode.id )
                    {
                        nextElement = possibleElement;
                        return true;
                    }
                    else if ( direction == Direction.BOTH )
                    {
                        nextElement = possibleElement;
                        return true;
                    }
                }
                catch ( NotFoundException e )
                {
//                    log.log( Level.FINE,
//                        "Unable to get relationship " + nextId, e );
                }
            }
            while ( !currentTypeIterator.hasNext( nodeManager ) )
            {
                if ( typeIterator.hasNext() )
                {
                    currentTypeIterator = typeIterator.next();
                    visitedTypes.add( currentTypeIterator.getType() );
                }
                else 
                {
                    boolean gotMore = fromNode.getMoreRelationships( nodeManager );
                    List<RelTypeElementIterator> list = Collections.EMPTY_LIST;
                    if ( types.length == 0 )
                    {
                        list = fromNode.getAllRelationships( nodeManager );
                    }
                    else
                    {
                        list = fromNode.getAllRelationshipsOfType( nodeManager, types );
                    }
                    Iterator<RelTypeElementIterator> itr = list.iterator();
                    while ( itr.hasNext() )
                    {
                        RelTypeElementIterator element = itr.next();
                        if ( visitedTypes.contains( element.getType() ) )
                        {
                            itr.remove();
                        }
                    }
                    typeIterator = list.iterator();
                    if ( typeIterator.hasNext() )
                    {
                        currentTypeIterator = typeIterator.next();
                        visitedTypes.add( currentTypeIterator.getType() );
                    }
                    if ( !gotMore )
                    {
                        break;
                    }
                }
            }
         } while ( currentTypeIterator.hasNext( nodeManager ) );
        // no next element found
        return false;
    }

    public T next()
    {
        hasNext();
        if ( nextElement != null )
        {
            T elementToReturn = nextElement;
            nextElement = null;
            return elementToReturn;
        }
        throw new NoSuchElementException();
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
