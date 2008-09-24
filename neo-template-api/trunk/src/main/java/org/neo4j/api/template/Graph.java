package org.neo4j.api.template;

import org.neo4j.api.core.Node;
import org.neo4j.api.core.Traverser;
import org.neo4j.api.template.graph.GraphDescription;
import org.neo4j.api.template.graph.Property;
import org.neo4j.api.template.traversal.Traversal;
import org.neo4j.api.template.util.Mapper;

import java.util.List;

public interface Graph
{
    Node createNode(Property... params);

    Node getReferenceNode();

    Node getNodeById(final long id);

    Traverser traverse(final Traversal traversal);

    Traverser traverse(Node startNode, Traversal traversal);

    <T> List<T> traverse(Node startNode, Traversal traversal, Mapper<Node, T> mapper);

    void load(final GraphDescription graph);
}
