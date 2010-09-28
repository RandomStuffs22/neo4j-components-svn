package org.neo4j.kernel.impl.core;

import org.neo4j.graphdb.Relationship;

public interface RelationshipGetter<T>
{
    public static final RelationshipGetter<Relationship> DEFAULT = new RelationshipGetter<Relationship>()
    {
        public Relationship getRelationship( NodeManager nodeManager, long id )
        {
            return nodeManager.getRelationshipById( (int) id );
        }

        public long getStartNodeId( Relationship relationship )
        {
            return relationship.getStartNode().getId();
        }

        public long getEndNodeId( Relationship relationship )
        {
            return relationship.getEndNode().getId();
        }
    };
    
    public static final RelationshipGetter<RelationshipImpl> INTERNAL = new RelationshipGetter<RelationshipImpl>()
    {
        public RelationshipImpl getRelationship( NodeManager nodeManager, long id )
        {
            return nodeManager.getRelForProxy( (int ) id );
        }

        public long getStartNodeId( RelationshipImpl relationship )
        {
            return relationship.getStartNodeId();
        }

        public long getEndNodeId( RelationshipImpl relationship )
        {
            return relationship.getEndNodeId();
        }
    };
    
    T getRelationship( NodeManager nodeManager, long id );
    
    long getStartNodeId( T relationship );

    long getEndNodeId( T relationship );
}
