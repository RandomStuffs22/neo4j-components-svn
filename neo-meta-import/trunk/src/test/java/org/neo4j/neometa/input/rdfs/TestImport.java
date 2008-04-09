package org.neo4j.neometa.input.rdfs;

import java.io.File;

import org.neo4j.api.core.Transaction;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureClass;
import org.neo4j.neometa.structure.MetaStructureNamespace;
import org.neo4j.neometa.structure.MetaStructureProperty;

/**
 * Tests to import some graphs and verifies the contents in the meta model
 * afterwards.
 */
public class TestImport extends MetaTestCase
{
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
	}
	
	/**
	 * A simple example.
	 * @throws Exception if something goes wrong.
	 */
	public void testSomeImport() throws Exception
	{
		MetaStructure meta = new MetaStructure( neo() );
		new RdfsImporter( meta ).doImport( new File( "test.rdfs" ) );
		
		Transaction tx = neo().beginTx();
		try
		{
			String baseName = "http://test.org#";
			MetaStructureNamespace global = meta.getGlobalNamespace();
			MetaStructureClass cPerson =
				global.getMetaClass( baseName + "Person", false );
			MetaStructureClass cStudent =
				global.getMetaClass( baseName + "Student", false );
			MetaStructureClass cTeacher =
				global.getMetaClass( baseName + "Teacher", false );
			MetaStructureClass cCourse =
				global.getMetaClass( baseName + "Course", false );
			MetaStructureProperty pTeacher =
				global.getMetaProperty( baseName + "teacher", false );
			MetaStructureProperty pStudents =
				global.getMetaProperty( baseName + "students", false );
			MetaStructureProperty pName =
				global.getMetaProperty( baseName + "name", false );
			MetaStructureProperty pGivenName =
				global.getMetaProperty( baseName + "givenName", false );
			MetaStructureProperty pBirthDate =
				global.getMetaProperty( baseName + "birthDate", false );
			assertAllNotNull( cPerson, cStudent, cTeacher, cCourse,
				pTeacher, pStudents, pName, pGivenName, pBirthDate );
			
			// Person
			assertEquals( "Person Class",
				cPerson.getAdditionalProperty( "comment" ) );
			assertEquals( "http://www.mattias.com/2008#persons",
				cPerson.getAdditionalProperty( "seeAlso" ) );
			assertCollection( cPerson.getDirectSubs(), cStudent, cTeacher );
			assertCollection( cPerson.getAllProperties(), pName,
				pGivenName, pBirthDate );
			
			// Student
			assertCollection( cStudent.getDirectSupers(), cPerson );
			assertCollection( cStudent.getAllProperties(), pName, pGivenName,
				pBirthDate );
			
			// Course
			assertCollection( cStudent.getDirectSubs() );
			assertCollection( cStudent.getDirectSupers(), cPerson );
			assertCollection( cCourse.getAllProperties(), pTeacher, pStudents,
				pName, pGivenName );
			
			// Teacher
			assertCollection( cTeacher.getDirectSubs() );
			assertCollection( cTeacher.getDirectSupers(), cPerson );
			assertEquals( "TeacherYeah",
				cTeacher.getAdditionalProperty( "label" ) );
			
			deleteMetaModel();
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
	
	/**
	 * Imports the FOAF RDF/XML graph.
	 * @throws Exception if something goes wrong.
	 */
	public void testFoafImport() throws Exception
	{
		doAnImportOf(
			"foaf.rdfs"
			,"prim-3.owl"
			,"om2-1.owl"
			,"miro.owl"
			,"wnbasic.rdfs"
			,"wnfull.rdfs"
			);
	}
	
	private void doAnImportOf( String... files ) throws Exception
	{
		for ( String file : files )
		{
			doImport( file );
		}
	}
	
	private void doImport( String file ) throws Exception
	{
		System.out.println( "=========================" );
		MetaStructure meta = new MetaStructure( neo() );
		new RdfsImporter( meta ).doImport( new File( file ) );
		
		Transaction tx = neo().beginTx();
		try
		{
			deleteMetaModel();
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
	
	private void assertAllNotNull( Object... objects )
	{
		for ( Object object : objects )
		{
			assertNotNull( object );
		}
	}
}
