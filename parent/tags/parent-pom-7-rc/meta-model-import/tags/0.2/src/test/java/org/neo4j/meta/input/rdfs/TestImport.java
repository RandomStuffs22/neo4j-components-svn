package org.neo4j.meta.input.rdfs;

import java.io.File;

import org.neo4j.graphdb.Transaction;
import org.neo4j.meta.input.rdfs.RdfsImporter;
import org.neo4j.meta.model.MetaModel;
import org.neo4j.meta.model.MetaModelClass;
import org.neo4j.meta.model.MetaModelImpl;
import org.neo4j.meta.model.MetaModelNamespace;
import org.neo4j.meta.model.MetaModelProperty;

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
		MetaModel meta = new MetaModelImpl( neo() );
		new RdfsImporter( meta ).doImport( new File( "test.rdfs" ) );
		
		Transaction tx = neo().beginTx();
		try
		{
			String baseName = "http://test.org#";
			MetaModelNamespace global = meta.getGlobalNamespace();
			MetaModelClass cPerson =
				global.getMetaClass( baseName + "Person", false );
			MetaModelClass cStudent =
				global.getMetaClass( baseName + "Student", false );
			MetaModelClass cTeacher =
				global.getMetaClass( baseName + "Teacher", false );
			MetaModelClass cCourse =
				global.getMetaClass( baseName + "Course", false );
			MetaModelProperty pTeacher =
				global.getMetaProperty( baseName + "teacher", false );
			MetaModelProperty pStudents =
				global.getMetaProperty( baseName + "students", false );
			MetaModelProperty pName =
				global.getMetaProperty( baseName + "name", false );
			MetaModelProperty pGivenName =
				global.getMetaProperty( baseName + "givenName", false );
			MetaModelProperty pBirthDate =
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
		MetaModel meta = new MetaModelImpl( neo() );
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
