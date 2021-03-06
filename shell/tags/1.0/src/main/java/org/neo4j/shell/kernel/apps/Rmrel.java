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
package org.neo4j.shell.kernel.apps;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.Traverser.Order;
import org.neo4j.shell.AppCommandParser;
import org.neo4j.shell.OptionValueType;
import org.neo4j.shell.Output;
import org.neo4j.shell.Session;
import org.neo4j.shell.ShellException;

/**
 * Mimics the POSIX application "rmdir", but neo4j has relationships instead of
 * directories (if you look at neo in a certain perspective).
 */
public class Rmrel extends GraphDatabaseApp
{
    /**
     * Constructs a new application which can delete relationships in neo.
     */
    public Rmrel()
    {
        this.addValueType( "d", new OptionContext( OptionValueType.NONE,
            "Must be supplied if the affected other node gets decoupled\n" +
            "after this operation so that it gets deleted." ) );
    }

    @Override
    public String getDescription()
    {
        return "Deletes a relationship\nUsage: rmrel <relationship id>";
    }

    @Override
    protected String exec( AppCommandParser parser, Session session,
        Output out ) throws ShellException
    {
        assertCurrentIsNode( session );
        
        if ( parser.arguments().isEmpty() )
        {
            throw new ShellException(
                "Must supply relationship id to delete as the first argument" );
        }

        Node currentNode = this.getCurrent( session ).asNode();
        Relationship rel = findRel( currentNode, Long.parseLong(
            parser.arguments().get( 0 ) ) );
        rel.delete();
        if ( !currentNode.equals(
            getServer().getDb().getReferenceNode() ) &&
            !currentNode.getRelationships().iterator().hasNext() )
        {
            throw new ShellException( "It would result in the current node " +
                currentNode + " to be decoupled (no relationships left)" );
        }
        Node otherNode = rel.getOtherNode( currentNode );
        if ( !otherNode.getRelationships().iterator().hasNext() )
        {
            boolean deleteOtherNodeWhenEmpty = parser.options().containsKey(
                "d" );
            if ( !deleteOtherNodeWhenEmpty )
            {
                throw new ShellException( "Since the node " +
                    getDisplayName( getServer(), session, otherNode, false ) +
                    " would be decoupled after this, you must supply the" +
                    " -d (for delete-when-decoupled) so that the other node " +
                    "(" + otherNode + ") may be deleted" );
            }
            otherNode.delete();
        }
        else
        {
            if ( !this.hasPathToRefNode( otherNode ) )
            {
                throw new ShellException( "It would result in " + otherNode +
                    " to be recursively decoupled with the reference node" );
            }
            if ( !this.hasPathToRefNode( currentNode ) )
            {
                throw new ShellException( "It would result in " + currentNode +
                    " to be recursively decoupled with the reference node" );
            }
        }
        return null;
    }

    private Relationship findRel( Node currentNode, long relId )
        throws ShellException
    {
        for ( Relationship rel : currentNode.getRelationships() )
        {
            if ( rel.getId() == relId )
            {
                return rel;
            }
        }
        throw new ShellException( "No relationship " + relId +
            " connected to " + currentNode );
    }

    private Iterable<RelationshipType> getAllRelationshipTypes()
    {
        return this.getServer().getDb().getRelationshipTypes();
    }

    private boolean hasPathToRefNode( Node node )
    {
        List<Object> filterList = new ArrayList<Object>();
        for ( RelationshipType rel : this.getAllRelationshipTypes() )
        {
            filterList.add( rel );
            filterList.add( Direction.BOTH );
        }

        Node refNode = getServer().getDb().getReferenceNode();
        Traverser traverser = node.traverse( Order.DEPTH_FIRST,
            StopEvaluator.END_OF_GRAPH, ReturnableEvaluator.ALL,
            filterList.toArray() );
        for ( Node testNode : traverser )
        {
            if ( refNode.equals( testNode ) )
            {
                return true;
            }
        }
        return false;
    }
}
