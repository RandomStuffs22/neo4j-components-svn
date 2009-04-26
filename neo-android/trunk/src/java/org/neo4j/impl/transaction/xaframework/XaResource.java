package org.neo4j.impl.transaction.xaframework;

import neo.javax.transaction.xa.XAResource;

public interface XaResource extends XAResource
{
    public void setBranchId( byte branchId[] );

    public byte[] getBranchId();
}
