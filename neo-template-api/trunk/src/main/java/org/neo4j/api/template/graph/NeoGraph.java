package org.neo4j.api.template.graph;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Traverser;
import org.neo4j.api.template.Graph;
import org.neo4j.api.template.traversal.Traversal;
import org.neo4j.api.template.util.Mapper;

import java.util.ArrayList;
import java.util.List;

public class NeoGraph implements Graph
{
    private final NeoService neo;

    public NeoGraph(final NeoService neo)
    {
        if (neo == null)
            throw new IllegalArgumentException("NeoService must not be null");
        this.neo = neo;
    }

    public Node createNode(final Property... params)
    {
        final Node node = neo.createNode();
        if (params == null || params.length == 0) return node;
        for (Property property : params)
        {
            node.setProperty(property.getName(), property.getValue());
        }
        return node;
    }

    public Node getReferenceNode()
    {
        return neo.getReferenceNode();
    }

    public Node getNodeById(final long id)
    {
        return neo.getNodeById(id);
    }

    public Traverser traverse(final Traversal traversal)
    {
        return traverse(neo.getReferenceNode(), traversal);
    }

    public Traverser traverse(final Node startNode, final Traversal traversal)
    {
        if (startNode == null)
            throw new IllegalArgumentException("StartNode must not be null");
        if (traversal == null)
            throw new IllegalArgumentException("Traversal must not be null");
        return traversal.from(startNode);
    }

    public <T> List<T> traverse(final Node startNode, final Traversal traversal, Mapper<Node, T> mapper)
    {
        if (mapper == null)
            throw new IllegalArgumentException("Mapper must not be null");

        return map(mapper, traverse(startNode, traversal));
    }

    private <T> List<T> map(final Mapper<Node, T> mapper, final Iterable<Node> nodes)
    {
        final List<T> result = new ArrayList<T>();
        for (final Node node : nodes)
        {
            result.add(mapper.map(node));
        }
        return result;
    }

    public void load(final GraphDescription description)
    {
        if (description == null)
            throw new IllegalArgumentException("GraphDescription must not be null");
        description.addToGraph(this);
    }
}

