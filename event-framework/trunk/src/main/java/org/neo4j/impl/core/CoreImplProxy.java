package org.neo4j.impl.core;

import org.neo4j.api.core.Relationship;

public class CoreImplProxy
{
	private CoreImplProxy()
	{
	}

	public static Relationship getRelationship( NodeManager nm, int id )
	{
		return nm.getRelationshipById( id );
	}
}
