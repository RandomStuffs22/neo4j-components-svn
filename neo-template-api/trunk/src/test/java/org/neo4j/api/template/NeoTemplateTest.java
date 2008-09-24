package org.neo4j.api.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import static org.neo4j.api.template.NeoTemplateTest.Type.HAS;
import static org.neo4j.api.template.graph.Property._;

public class NeoTemplateTest extends NeoApiTest
{

    enum Type implements RelationshipType
    {
        HAS
    }

    @Test
    public void testRefNode()
    {
        new NeoTemplate(neo).execute(new NeoCallback()
        {
            public void neo(final Status status, final Graph graph) throws Exception
            {
                Node refNode = graph.getReferenceNode();
                Node refNodeById = graph.getNodeById(refNode.getId());
                assertEquals("same ref node", refNode, refNodeById);
            }
        });
    }

    @Test
    public void testSingleNode()
    {
        final NeoTemplate template = new NeoTemplate(neo);
        template.execute(new NeoCallback()
        {
            public void neo(final Status status, final Graph graph) throws Exception
            {
                Node refNode = graph.getReferenceNode();
                Node node = graph.createNode(_("name", "Test"), _("size", 100));
                refNode.createRelationshipTo(node, HAS);

                final Relationship toTestNode = refNode.getSingleRelationship(HAS, Direction.OUTGOING);
                final Node nodeByRelationship = toTestNode.getEndNode();
                assertEquals("Test", nodeByRelationship.getProperty("name"));
                assertEquals(100, nodeByRelationship.getProperty("size"));
            }
        });
        template.execute(new NeoCallback()
        {
            public void neo(final Status status, final Graph graph) throws Exception
            {
                Node refNode = graph.getReferenceNode();
                final Relationship toTestNode = refNode.getSingleRelationship(HAS, Direction.OUTGOING);
                final Node nodeByRelationship = toTestNode.getEndNode();
                assertEquals("Test", nodeByRelationship.getProperty("name"));
                assertEquals(100, nodeByRelationship.getProperty("size"));
            }
        });
    }

    @Test
    public void testRollback()
    {
        final NeoTemplate template = new NeoTemplate(neo);
        template.execute(new NeoCallback()
        {
            public void neo(final Status status, final Graph graph) throws Exception
            {
                Node node = graph.getReferenceNode();
                node.setProperty("test", "test");
                assertEquals("test", node.getProperty("test"));
                status.mustRollback();
            }
        });
        template.execute(new NeoCallback()
        {
            public void neo(final Status status, final Graph graph) throws Exception
            {
                Node node = graph.getReferenceNode();
                assertFalse(node.hasProperty("test"));
            }
        });
    }
}
