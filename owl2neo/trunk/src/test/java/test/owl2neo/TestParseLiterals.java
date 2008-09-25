package test.owl2neo;

import org.neo4j.owl2neo.RdfUtil;

import junit.framework.TestCase;

public class TestParseLiterals extends TestCase
{
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
