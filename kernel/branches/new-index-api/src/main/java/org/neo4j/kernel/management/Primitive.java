package org.neo4j.kernel.management;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.kernel.impl.core.NodeManager;
import org.neo4j.kernel.impl.nioneo.store.PropertyStore;

class Primitive extends Neo4jJmx implements PrimitiveMBean
{
    private final NodeManager nodeManager;

    Primitive( int instanceId, NodeManager nodeManager )
    {
        super( instanceId );
        this.nodeManager = nodeManager;
    }

    public long getNumberOfNodeIdsInUse()
    {
        return nodeManager.getNumberOfIdsInUse( Node.class );
    }

    public long getNumberOfRelationshipIdsInUse()
    {
        return nodeManager.getNumberOfIdsInUse( Relationship.class );
    }


    public long getNumberOfPropertyIdsInUse()
    {
        return nodeManager.getNumberOfIdsInUse( PropertyStore.class );
    }

    public long getNumberOfRelationshipTypeIdsInUse()
    {
        return nodeManager.getNumberOfIdsInUse( RelationshipType.class );
    }
}
