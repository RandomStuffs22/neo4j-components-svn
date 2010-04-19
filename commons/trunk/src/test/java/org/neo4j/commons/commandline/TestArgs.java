package org.neo4j.commons.commandline;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.neo4j.commons.collection.MapUtil;

public class TestArgs
{
    @Test
    public void testArgsParser()
    {
        Args args = new Args( new String[] { "-port=9999", "-type", "test-type",
                "--longer-name=the name", "--some-option", "hi there" } );
        assertEquals( MapUtil.of( "port", "9999", "type", "test-type",
                "longer-name", "the name", "some-option", "hi there" ), args.asMap() );
    }
}
