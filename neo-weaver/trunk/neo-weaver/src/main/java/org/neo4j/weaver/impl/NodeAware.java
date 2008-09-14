package org.neo4j.weaver.impl;

import org.neo4j.api.core.Node;


public interface NodeAware {
	Node getUnderlyingNode();
}
