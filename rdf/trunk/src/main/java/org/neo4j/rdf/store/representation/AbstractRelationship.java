package org.neo4j.rdf.store.representation;

import org.neo4j.graphdb.Relationship;

/**
 * Represents a more simple abstraction of a {@link Relationship}.
 */
public class AbstractRelationship extends AbstractElement
{
    private final String RELATIONSHIP_TYPE_NAME;
    private final AbstractNode startNode, endNode;

    /**
     * @param startNode the start node.
     * @param relTypeName the relationship type name.
     * @param endNode the end node.
     */
    public AbstractRelationship( AbstractNode startNode, String relTypeName,
        AbstractNode endNode )
    {
        this.RELATIONSHIP_TYPE_NAME = relTypeName;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    /**
     * @return the name of the type of this relationship.
     */
    public String getRelationshipTypeName()
    {
        return RELATIONSHIP_TYPE_NAME;
    }

    /**
     * @return the start node of this relationship.
     */
    public AbstractNode getStartNode()
    {
        return startNode;
    }

    /**
     * @return the end node of this relationship.
     */
    public AbstractNode getEndNode()
    {
        return endNode;
    }

    /**
     * @param oneNode the node to get the opposite node of.
     * @return the opposite node of the supplied node, so if the start node
     * is supplied the end node is returned and vice versa.
     * @throws IllegalArgumentException if the supplied node is neither the
     * start nor the end node.
     */
    public AbstractNode getOtherNode( AbstractNode oneNode )
    {
        if ( oneNode != startNode && oneNode != endNode )
        {
            throw new IllegalArgumentException( "Neither start nor end node" );
        }
        return oneNode == startNode ? endNode : startNode;
    }

    /**
     * @return an array of the start and end nodes.
     */
    public AbstractNode[] getBothNodes()
    {
        return new AbstractNode[] { startNode, endNode };
    }
    
    @Override
    public int hashCode()
    {
        int result = 0;
        result += this.startNode.hashCode() * 32;
        result += this.endNode.hashCode() * 32;
        result += this.RELATIONSHIP_TYPE_NAME.hashCode() * 32;
        return result;
    }
    
    @Override
    public boolean equals( Object o )
    {
        if ( !( o instanceof AbstractRelationship ) )
        {
            return false;
        }
        AbstractRelationship r = ( AbstractRelationship ) o;
        return r.startNode.equals( startNode ) && r.endNode.equals( endNode ) &&
            r.RELATIONSHIP_TYPE_NAME.equals( RELATIONSHIP_TYPE_NAME );
    }
}
