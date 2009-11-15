package test.owl2neo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import junit.framework.TestCase;

import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.meta.model.MetaModel;
import org.neo4j.meta.model.MetaModelClass;
import org.neo4j.meta.model.MetaModelImpl;
import org.neo4j.meta.model.MetaModelProperty;
import org.neo4j.owl2neo.Owl2Neo;
import org.neo4j.owl2neo.OwlClass;
import org.neo4j.owl2neo.OwlModel;
import org.neo4j.owl2neo.OwlProperty;
import org.neo4j.owl2neo.OwlRestriction;

/**
 * An overall test for the owl2neometa, mostly {@link OwlModel}.
 */
public class TestOwl2Neo extends TestCase
{
	private static final File directory = new File( "var/test" );
	private NeoService neo;
	private Owl2Neo owl2Neo;

	private void clearNeoDirectory()
	{
		if ( directory.exists() )
		{
			for ( File file : directory.listFiles() )
			{
				file.delete();
			}
		}
	}
	
	@Override
	protected void setUp() throws Exception
	{
		clearNeoDirectory();
		neo = new EmbeddedNeo( directory.getAbsolutePath() );
		MetaModel metaManager = new MetaModelImpl( neo );
		owl2Neo = new Owl2Neo( neo, metaManager );
	}

	@Override
	protected void tearDown() throws Exception
	{
		neo.shutdown();
		clearNeoDirectory();
	}

	/**
	 * Tests the use of the {@link OwlModel} class.
	 */
	public void testOwlModel()
	{
		OwlModel model = owl2Neo.getOwlModel();
		MetaModel metaManager = owl2Neo.getMetaManager();
		
		MetaModelClass t1 = metaManager.getGlobalNamespace().getMetaClass(
		    "t1", true );
		MetaModelClass t2 = metaManager.getGlobalNamespace().getMetaClass(
		    "t2", true );
		OwlClass c1 = model.getOwlClass( t1 );
		OwlClass c2 = model.getOwlClass( t2 );
		c2.supers().add( c1 );
		
		assertFalse( model.hasOwlProperty( "something" ) );
		MetaModelProperty nameProp = metaManager.getGlobalNamespace()
		    .getMetaProperty( "name", true );
        MetaModelProperty middleNameProp = metaManager.getGlobalNamespace()
            .getMetaProperty( "middleName", true );
		OwlProperty name = model.getOwlProperty( nameProp );
		OwlProperty middleName = model.getOwlProperty( middleNameProp );
		assertEquals( "name", name.getRdfAbout() );
		assertEquals( "middleName", middleName.getRdfAbout() );
		middleName.supers().add( name );
		assertNull( name.get( OwlModel.RANGE ) );
		assertNull( middleName.get( OwlModel.RANGE ) );
		
		String middleNameRangeValue = "testRange";
		middleName.set( OwlModel.RANGE, middleNameRangeValue );
		assertEquals( middleNameRangeValue, middleName.get( OwlModel.RANGE ) );
		assertNull( name.get( OwlModel.RANGE ) );
		
		String nameRangeValue = "nameRange";
		name.set( OwlModel.RANGE, nameRangeValue );
		assertEquals( nameRangeValue, name.get( OwlModel.RANGE ) );
		assertEquals( middleNameRangeValue, middleName.get( OwlModel.RANGE ) );
		
		middleName.remove( OwlModel.RANGE );
		assertEquals( nameRangeValue, name.get( OwlModel.RANGE ) );
		assertEquals( nameRangeValue, middleName.get( OwlModel.RANGE ) );
		
		OwlRestriction r1 = model.addRestriction( c1, name );
		String domainTestValue = "domain";
		r1.set( OwlModel.DOMAIN, domainTestValue );
		
		Collection<MetaModelClass> nodeTypes =
		    new ArrayList<MetaModelClass>( 
		        Arrays.asList( new MetaModelClass[] { t2 } ) );
		assertEquals( nameRangeValue, model.findPropertyValue(
			middleName.getRdfAbout(), OwlModel.RANGE, nodeTypes ) );
		
		assertEquals( domainTestValue, model.findPropertyValue(
			name.getRdfAbout(), OwlModel.DOMAIN, nodeTypes ) );
		
		assertTrue( model.getPropertiesRegardingClass( c1 ).contains( name ) );
			
		
		// TODO Should a restriction for a specific class on "name" be a
		// restriction in "middleName" as well?
//		assertEquals( domainTestValue, model.findPropertyValue(
//			middleName.getRdfAbout(), OwlModel.DOMAIN, nodeTypes ) );
	}
}
