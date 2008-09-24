package org.neo4j.api.template.util;

import org.neo4j.api.core.Node;

public interface NodeEvaluator
{
    boolean accept(final Node node);
}
