package org.neo4j.weaver.impl;

import org.neo4j.api.core.Relationship;


public interface RelationshipAware {
	Relationship getUnderlyingRelationship();
}
