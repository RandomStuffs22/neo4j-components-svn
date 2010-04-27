package org.neo4j.kernel.impl.manage;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.kernel.impl.core.NodeManager;
import org.neo4j.kernel.impl.nioneo.store.PropertyStore;
import org.neo4j.kernel.manage.PrimitiveMXBean;

public final class PrimitiveMonitor extends Neo4jMonitor implements
        PrimitiveMXBean
{
    private final NodeManager nodeManager;

    public PrimitiveMonitor( int instanceId, NodeManager nodeManager )
    {
        super( instanceId );
        this.nodeManager = nodeManager;
    }

    @Override
    public long getNumberOfNodeIdsInUse()
    {
        return nodeManager.getNumberOfIdsInUse( Node.class );
    }

    @Override
    public long getNumberOfRelationshipIdsInUse()
    {
        return nodeManager.getNumberOfIdsInUse( Relationship.class );
    }


    @Override
    public long getNumberOfPropertyIdsInUse()
    {
        return nodeManager.getNumberOfIdsInUse( PropertyStore.class );
    }

    @Override
    public long getNumberOfRelationshipTypeIdsInUse()
    {
        return nodeManager.getNumberOfIdsInUse( RelationshipType.class );
    }
}
