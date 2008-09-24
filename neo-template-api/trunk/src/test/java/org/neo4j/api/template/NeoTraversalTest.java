package org.neo4j.api.template;

import static org.junit.Assert.assertEquals;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.StopEvaluator;
import static org.neo4j.api.template.NeoTemplateTest.Type.HAS;
import static org.neo4j.api.template.NeoTraversalTest.Type.HUSBAND;
import org.neo4j.api.template.graph.GraphDescription;
import org.neo4j.api.template.traversal.Traversal;
import static org.neo4j.api.template.traversal.Traversal.walk;
import org.neo4j.api.template.util.Mapper;

import static java.util.Arrays.asList;

public class NeoTraversalTest extends NeoApiTest
{

    enum Type implements RelationshipType
    {
        MARRIED, CHILD, GRANDSON, GRANDDAUGHTER, WIFE, HUSBAND, HAS
    }

    @Test
    public void testSimpleTraverse()
    {
        runAndCheckTraverse(walk().both(HAS), "grandpa", "grandma", "daughter", "son", "man", "wife");
    }


    @Ignore
    @Test
    public void testComplexTraversal()
    {
        final Traversal traversal = Traversal.walk().breadthFirst().depthFirst()
                .stopOn(StopEvaluator.DEPTH_ONE).first().all()
                .incoming(HAS).outgoing(HAS).twoway(HAS);
        runAndCheckTraverse(traversal, "grandpa", "grandma", "daughter", "son", "man", "wife");
    }

    private void runAndCheckTraverse(final Traversal traversal, final String... names)
    {
        final NeoTemplate template = new NeoTemplate(neo);
        template.execute(new NeoCallback()
        {
            public void neo(final Status status, final Graph graph) throws Exception
            {
                createFamily(graph);
                assertEquals("all members", asList(names),
                        graph.traverse(graph.getReferenceNode(), traversal,
                                new Mapper<Node, String>()
                                {
                                    public String map(final Node node)
                                    {
                                        return (String) node.getProperty("name", "");
                                    }
                                }));
            }
        });
    }

    private void createFamily(final Graph graph)
    {
        final GraphDescription family = new GraphDescription();
        family.add("family", "type", "small");
        family.relate("family", Type.HAS, "wife");
        family.relate("family", Type.HAS, "man");


        family.add("man", "age", 35);
        family.add("wife", "age", 30);
        family.relate("man", Type.MARRIED, "wife");
        family.relate("wife", Type.MARRIED, "man");
        family.relate("man", Type.WIFE, "wife");
        family.relate("wife", HUSBAND, "man");

        family.add("daughter", "age", 10);
        family.add("son", "age", 8);

        family.relate("family", Type.HAS, "son");
        family.relate("family", Type.HAS, "daughter");

        family.relate("man", Type.CHILD, "son");
        family.relate("wife", Type.CHILD, "son");
        family.relate("man", Type.CHILD, "daughter");
        family.relate("wife", Type.CHILD, "daughter");

        family.add("grandma", "age", 60);
        family.add("grandpa", "age", 75);

        family.relate("family", Type.HAS, "grandma");
        family.relate("family", Type.HAS, "grandpa");

        family.relate("grandpa", Type.CHILD, "man");
        family.relate("grandma", Type.CHILD, "man");

        family.relate("grandpa", Type.GRANDSON, "son");
        family.relate("grandma", Type.GRANDSON, "son");
        family.relate("grandpa", Type.GRANDDAUGHTER, "daughter");
        family.relate("grandma", Type.GRANDDAUGHTER, "daughter");

        // todo even more relationships
        graph.load(family);
    }
}