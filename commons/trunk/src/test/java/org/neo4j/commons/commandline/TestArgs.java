package org.neo4j.commons.commandline;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.neo4j.commons.collection.MapUtil;

public class TestArgs
{
    @Test
    public void testArgsParser()
    {
        Args args = new Args( new String[] { "-port=9999", "-type", "test-type", "orphan1",
                "--longer-name=the name", "--some-option", "hi there", "orphan2" } );
        assertEquals( MapUtil.of( "port", "9999", "type", "test-type",
                "longer-name", "the name", "some-option", "hi there" ), args.asMap() );
        assertEquals( Arrays.asList( "orphan1", "orphan2" ), args.orphans() );
    }
}
