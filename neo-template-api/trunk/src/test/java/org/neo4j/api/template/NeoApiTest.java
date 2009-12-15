package org.neo4j.api.template;

import org.junit.After;
import org.junit.Before;
import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.Traverser;
import org.neo4j.api.template.traversal.Traversal;
import org.neo4j.api.template.util.NamedRelationshipType;

public abstract class NeoApiTest
{
    protected NeoService neo;

    @Before
    public void setUp()
    {
        neo = new EmbeddedNeo("target/var/neo");
    }

    @After
    public void tearDown()
    {
        if (neo != null)
        {
            clear();
            neo.shutdown();
        }
    }

    private void clear()
    {
        new NeoTemplate(neo).execute(new NeoCallback()
        {
            public void neo(final Status status, final Graph graph) throws Exception
            {
                final NamedRelationshipType relationshipType = new NamedRelationshipType("HAS");
                final Traverser allNodes = graph.traverse(Traversal.walk().both(relationshipType));
                for (Node node : allNodes)
                {
                    for (Relationship relationship : node.getRelationships())
                    {
                        relationship.delete();
                    }
                    node.delete();
                }
            }
        });
    }
}
