/**
 * Copyright (c) 2002-2010 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
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

package org.neo4j.kernel.impl.traversal;

import java.util.Iterator;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchSelector;
import org.neo4j.graphdb.traversal.TraversalBranch;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.UniquenessFilter;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.PrefetchingIterator;

class TraverserImpl implements Traverser
{
    private final TraversalDescriptionImpl description;
    private final Node startNode;

    TraverserImpl( TraversalDescriptionImpl description, Node startNode )
    {
        this.description = description;
        this.startNode = startNode;
    }

    public Iterator<Path> iterator()
    {
        return new TraverserIterator();
    }

    public Iterable<Node> nodes()
    {
        return new IterableWrapper<Node, Path>( this )
        {
            @Override
            protected Node underlyingObjectToObject( Path position )
            {
                return position.endNode();
            }
        };
    }

    public Iterable<Relationship> relationships()
    {
        return new IterableWrapper<Relationship, Path>( this )
        {
            @Override
            public Iterator<Relationship> iterator()
            {
                Iterator<Relationship> iter = super.iterator();
                iter.next(); // Skip the first, it is null
                return iter;
            }

            @Override
            protected Relationship underlyingObjectToObject( Path position )
            {
                return position.lastRelationship();
            }
        };
    }

    class TraverserIterator extends PrefetchingIterator<Path>
    {
        final UniquenessFilter uniquness;
        private final BranchSelector sourceSelector;
        final TraversalDescriptionImpl description;
        final Node startNode;

        TraverserIterator()
        {
            this.description = TraverserImpl.this.description;
            this.uniquness = description.uniqueness.create( description.uniquenessParameter );
            this.startNode = TraverserImpl.this.startNode;
            this.sourceSelector = description.branchSelector.create(
                    new StartNodeTraversalBranch( this, startNode,
                            description.expander ) );
        }

        boolean okToProceedFirst( TraversalBranch source )
        {
            return this.uniquness.checkFirst( source );
        }
        
        boolean okToProceed( TraversalBranch source )
        {
            return this.uniquness.check( source, true );
        }

        boolean shouldExpandBeyond( TraversalBranch source )
        {
            return this.uniquness.check( source, false ) &&
                    !description.pruning.pruneAfter( source.position() );
        }

        boolean okToReturn( TraversalBranch source )
        {
            return description.filter.accept( source.position() );
        }

        @Override
        protected Path fetchNextOrNull()
        {
            TraversalBranch result = null;
            while ( true )
            {
                result = sourceSelector.next();
                if ( result == null )
                {
                    break;
                }
                if ( okToReturn( result ) )
                {
                    break;
                }
            }
            return result != null ? result.position() : null;
        }
    }
}
