package org.neo4j.meta.input.owl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestParseLiterals
{
    @Test
	public void testParseSomeLiterals() throws Exception
	{
		String booleanUri = RdfUtil.NS_XML_SCHEMA + "boolean";
		assertTrue( ( Boolean ) RdfUtil.getRealValue( booleanUri, "True" ) );
		assertTrue( ( Boolean ) RdfUtil.getRealValue(
			booleanUri, Boolean.TRUE.toString() ) );
		assertFalse( ( Boolean ) RdfUtil.getRealValue( booleanUri, "FALSE" ) );
		assertFalse( ( Boolean ) RdfUtil.getRealValue( booleanUri,
			Boolean.FALSE.toString() ) );
	}
}
