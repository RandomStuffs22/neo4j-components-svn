package org.neo4j.api.template;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;
import static org.neo4j.api.template.NeoGraphDescriptionTest.Type.HAS;
import org.neo4j.api.template.graph.GraphDescription;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

public class NeoGraphDescriptionTest extends NeoApiTest
{

    enum Type implements RelationshipType
    {
        HAS
    }

    @Test
    public void testLoadGraph()
    {
        final NeoTemplate template = new NeoTemplate(neo);
        template.execute(new NeoCallback()
        {
            public void neo(final Status status, final Graph graph) throws Exception
            {
                final GraphDescription heaven = new GraphDescription();
                heaven.add("adam", "age", 1);
                heaven.add("eve", "age", 0);
                heaven.relate("adam", HAS, "eve");
                graph.load(heaven);

                checkHeaven(graph);
            }
        });
    }

    private void checkHeaven(final Graph graph)
    {
        final Node adam = graph.getReferenceNode();
        assertEquals("adam", adam.getProperty("name"));
        assertEquals(1, adam.getProperty("age"));
        final Node eve = adam.getSingleRelationship(HAS, Direction.OUTGOING).getEndNode();
        assertEquals("eve", eve.getProperty("name"));
        assertEquals(0, eve.getProperty("age"));
    }

    @Test
    public void testLoadGraphProps()
    {
        final NeoTemplate template = new NeoTemplate(neo);
        template.execute(new NeoCallback()
        {
            public void neo(final Status status, final Graph graph) throws Exception
            {
                final GraphDescription heaven = new GraphDescription(createGraphProperties());
                graph.load(heaven);
                checkHeaven(graph);
            }
        });
    }

    private Properties createGraphProperties() throws IOException
    {
        Properties props = new Properties();
        props.load(new ByteArrayInputStream((
                "adam.age=Integer:1\n" +
                "eve.age=Integer:0\n" +
                "adam->HAS=eve"
        ).getBytes("UTF-8")));
        return props;
    }
}