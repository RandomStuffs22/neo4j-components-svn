/*
 * Licensed to "Neo Technology," Network Engine for Objects in Lund AB
 * (http://neotechnology.com) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Neo Technology licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at (http://www.apache.org/licenses/LICENSE-2.0). Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.neo4j.neoclipse.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.ReturnableEvaluator;
import org.neo4j.api.core.StopEvaluator;
import org.neo4j.api.core.TraversalPosition;
import org.neo4j.api.core.Traverser;
import org.neo4j.api.core.Traverser.Order;
import org.neo4j.neoclipse.Activator;
import org.neo4j.neoclipse.reltype.RelationshipTypesProviderWrapper;
import org.neo4j.neoclipse.view.NeoGraphViewPart;

/**
 * This class represents a search query for Neo objects.
 * @author Peter H&auml;nsgen
 */
public class NeoSearchQuery implements ISearchQuery
{
    /**
     * Dummy list to return an empty iterable&lt;Node&gt; when search can't find
     * anything.
     */
    private static final List<Node> EMPTY_NODE_LIST = Arrays
        .asList( new Node[0] );

    /**
     * The search expression.
     */
    private final NeoSearchExpression expression;

    /**
     * The found matches.
     */
    private final NeoSearchResult result;

    private NeoService neoService;

    private final NeoGraphViewPart graphView;

    /**
     * The constructor.
     * @param graphView
     *            the current graph view
     */
    public NeoSearchQuery( final NeoSearchExpression expression,
        final NeoGraphViewPart graphView )
    {
        this.expression = expression;
        this.graphView = graphView;

        // initialize an empty result
        result = new NeoSearchResult( this );
    }

    /**
     * Returns a String form of the search expression.
     */
    public String getExpression()
    {
        return expression.getExpression();
    }

    /**
     * Returns true.
     */
    public boolean canRerun()
    {
        return true;
    }

    /**
     * Returns true.
     */
    public boolean canRunInBackground()
    {
        return true;
    }

    /**
     * Returns a label.
     */
    public String getLabel()
    {
        return "Neo4j Search";
    }

    /**
     * Returns the search result.
     */
    public ISearchResult getSearchResult()
    {
        return result;
    }

    /**
     * Executes the search.
     */
    public IStatus run( final IProgressMonitor monitor )
        throws OperationCanceledException
    {
        neoService = Activator.getDefault().getNeoServiceSafely();
        if ( neoService == null )
        {
            return new Status( IStatus.ERROR, Activator.PLUGIN_ID,
                "There is no active Neo4j service." );
        }

        // TODO here we should do some real search using Neo's index service
        // for now simply navigate along the graph
        Node start = null;
        if ( graphView != null )
        {
            start = graphView.getCurrentNode();
        }
        else
        {
            start = Activator.getDefault().getReferenceNode();
        }

        Iterable<Node> matches = getMatchingNodesByTraversing( start, monitor );
        result.setMatches( matches );

        if ( monitor.isCanceled() )
        {
            return new Status( IStatus.CANCEL, Activator.PLUGIN_ID,
                "Cancelled." );
        }
        else
        {
            return new Status( IStatus.OK, Activator.PLUGIN_ID, "OK" );
        }
    }

    /**
     * Finds all nodes matching the search criteria.
     */
    protected Iterable<Node> getMatchingNodesByTraversing( final Node node,
        final IProgressMonitor monitor )
    {
        // monitor.beginTask( "Neo4j search operation started.",
        // IProgressMonitor.UNKNOWN );
        final List<Object> relDirList = new ArrayList<Object>();

        Iterable<RelationshipType> relationshipTypes = RelationshipTypesProviderWrapper
            .getInstance().getRelationshipTypesFromNeo();
        for ( RelationshipType relType : relationshipTypes )
        {
            relDirList.add( relType );
            relDirList.add( Direction.BOTH );
        }

        if ( relDirList.isEmpty() )
        {
            // there's no relationships, so we can't search
            return EMPTY_NODE_LIST;
        }

        Traverser trav = node.traverse( Order.DEPTH_FIRST,
            StopEvaluator.END_OF_GRAPH, new ReturnableEvaluator()
            {
                public boolean isReturnableNode(
                    final TraversalPosition currentPos )
                {
                    // monitor.worked( 1 );
                    Node currentNode = currentPos.currentNode();
                    // for completeness, also check the id of the node
                    if ( expression.matches( currentNode.getId() ) )
                    {
                        return true;
                    }
                    else
                    {
                        // find at least one property whose value matches the
                        // given
                        // expression
                        // for ( Object value : currentNode.getPropertyValues()
                        // )
                        // changed due to strange problem in b7
                        // TODO: change back to old code when OK
                        for ( String key : currentNode.getPropertyKeys() )
                        {
                            Object value = currentNode.getProperty( key );
                            if ( expression.matches( value ) )
                            {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            }, relDirList.toArray() );
        // try using as id, if possible
        Node nodeFromId = null;
        if ( expression.isPossibleId() )
        {
            try
            {
                long id = Long.parseLong( expression.getExpression() );
                nodeFromId = neoService.getNodeById( id );
            }
            catch ( RuntimeException e ) // NumberFormatException included
            {
                // do nothing
            }
        }
        if ( nodeFromId != null )
        {
            return new IterableMerger( nodeFromId, trav );
        }
        return trav;
    }

    /**
     * Lots of stuff to just add one node to an Iterable.
     * @author Anders Nawroth
     */
    private static class IterableMerger implements Iterable<Node>
    {
        private final MergeIterator iter;

        public IterableMerger( final Node node, final Iterable<Node> traverser )
        {
            this.iter = new MergeIterator( node, traverser );
        }

        private static class MergeIterator implements Iterator<Node>
        {
            private final Node node;
            private Node nextNode;
            private final Iterator<Node> travIter;
            private boolean usedNode = false;

            public MergeIterator( final Node node,
                final Iterable<Node> traverser )
            {
                this.node = node;
                this.travIter = traverser.iterator();
            }

            public boolean hasNext()
            {
                if ( !usedNode )
                {
                    return true;
                }

                if ( travIter.hasNext() )
                {
                    nextNode = travIter.next();
                    if ( node.equals( nextNode ) )
                    {
                        if ( travIter.hasNext() )
                        {
                            nextNode = travIter.next();
                            return true;
                        }
                        else
                        {
                            return false;
                        }
                    }
                    else
                    {
                        return true;
                    }
                }
                else
                {
                    return false;
                }
            }

            public Node next()
            {
                if ( !usedNode )
                {
                    usedNode = true;
                    return node;
                }
                return nextNode;
            }

            public void remove()
            {
                if ( usedNode )
                {
                    travIter.remove();
                }
            }
        }

        public Iterator<Node> iterator()
        {
            return iter;
        }
    }

    /**
     * Finds all nodes matching the search criteria.
     */
    protected Iterable<Node> getMatchingNodesByRecursion( final Node node,
        final IProgressMonitor monitor )
    {
        // TODO the Neo traverser API is not sufficient as it does not allow to
        // find ALL connected
        // nodes regardless of their relationship types
        // we have to implement a similar functionality ourselves...

        Set<Node> visitedNodes = new HashSet<Node>();
        List<Node> matches = new ArrayList<Node>();

        // try using as id, if possible
        if ( expression.isPossibleId() )
        {
            try
            {
                long id = Long.parseLong( expression.getExpression() );
                Node nodeFromId = neoService.getNodeById( id );
                matches.add( nodeFromId );
                visitedNodes.add( nodeFromId );
            }
            catch ( RuntimeException e ) // this also covers
            // NumberFormatException
            {
                // do nothing
            }
        }

        checkNode( node, visitedNodes, matches, monitor );

        return matches;
    }

    /**
     * Checks if a node matches the search criteria and visits all connected
     * nodes.
     */
    protected void checkNode( final Node node, final Set<Node> visitedNodes,
        final List<Node> matches, final IProgressMonitor monitor )
    {
        if ( monitor.isCanceled() )
        {
            return;
        }

        if ( !visitedNodes.add( node ) )
        {
            // we have already been here
            return;
        }

        // for completeness, also check the id of the node
        if ( expression.matches( node.getId() ) )
        {
            matches.add( node );
        }
        else
        {
            // find at least one property whose value matches the given
            // expression
            for ( Object value : node.getPropertyValues() )
            {
                if ( expression.matches( value ) )
                {
                    matches.add( node );
                    break;
                }
            }
        }

        // recursively follow all connections
        for ( Relationship r : node.getRelationships( Direction.BOTH ) )
        {
            Node end = r.getOtherNode( node );

            checkNode( end, visitedNodes, matches, monitor );
        }
    }
}
