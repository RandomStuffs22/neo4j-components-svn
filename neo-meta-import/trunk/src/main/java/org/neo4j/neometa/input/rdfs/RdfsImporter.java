package org.neo4j.neometa.input.rdfs;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Transaction;
import org.neo4j.neometa.structure.DatatypeClassRange;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureClass;
import org.neo4j.neometa.structure.MetaStructureClassRange;
import org.neo4j.neometa.structure.MetaStructureNamespace;
import org.neo4j.neometa.structure.MetaStructureProperty;
import org.neo4j.neometa.structure.MetaStructureThing;
import org.neo4j.neometa.structure.PropertyRange;
import org.neo4j.neometa.structure.RdfDatatypeRange;
import org.neo4j.neometa.structure.RdfUtil;
import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.Literal;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.UriOrVariable;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.vocabulary.RDF;
import org.ontoware.rdf2go.vocabulary.RDFS;

public class RdfsImporter
{
	private MetaStructure meta;
	
	public static void main( String[] args ) throws Exception
	{
		final NeoService neo = new EmbeddedNeo( "var/neo" );
		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			@Override
			public void run()
			{
				neo.shutdown();
			}
		} );
		
		try
		{
			MetaStructure meta = new MetaStructure( neo );
			new RdfsImporter( meta ).doImport( new File( "test.rdfs" ) );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		finally
		{
			System.exit( 0 );
		}
	}
	
	public RdfsImporter( MetaStructure meta )
	{
		this.meta = meta;
	}
	
	/**
	 * Since all is happening in the global namespace when importing from RDFS
	 * here's a method for that namespace.
	 * @return the global namespace.
	 */
	protected MetaStructureNamespace meta()
	{
		return this.meta.getGlobalNamespace();
	}
	
	public void doImport( File file ) throws IOException
	{
		Model model = readModel( file );
		Transaction tx = meta.neo().beginTx();
		try
		{
			readFrom( model );
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
	
	private void readFrom( Model model )
	{
		readClasses( model );
		readProperties( model );
	}
	
	private String resourceUri( org.ontoware.rdf2go.model.node.Node node )
	{
		return node.asURI().asJavaURI().toString();
	}
	
//	private Resource isSubclassOf( Model model, Resource resource, String uri )
//	{
//		return isSubtypeOf( model, resource, uri, RDFS.subClassOf );
//	}
//	
//	private Resource isSubtypeOf( Model model, Resource resource, String uri,
//		UriOrVariable type )
//	{
//		ClosableIterator<? extends Statement> itr = model.findStatements(
//			resource, type, Variable.ANY );
//		while ( itr.hasNext() )
//		{
//			Statement statement = itr.next();
//			Resource object = statement.getObject().asResource();
//			if ( resourceUri( object ).equals( uri ) )
//			{
//				return object;
//			}
//			else
//			{
//				Resource furtherUp =
//					isSubtypeOf( model, object, uri, type );
//				if ( furtherUp != null )
//				{
//					return furtherUp;
//				}
//			}
//		}
//		return null;
//	}
	
	private void trySetLabelAndComment( MetaStructureThing thing,
		Model model, Resource resource )
	{
		trySetFromLiteral( thing, model, resource, RDFS.label, "label" );
		trySetFromLiteral( thing, model, resource, RDFS.comment, "comment" );
		trySetFromResource( thing, model, resource, RDFS.seeAlso, "seeAlso" );
		trySetFromResource( thing, model, resource, RDFS.isDefinedBy,
			"isDefinedBy" );
	}
	
	private void trySetFromLiteral( MetaStructureThing thing, Model model,
		Resource resource, UriOrVariable property, String key )
	{
		String value = tryGetLiteral( model, resource, property );
		if ( value != null )
		{
			System.out.println( "\t" + key + ": " + value );
			thing.setAdditionalProperty( key, value );
		}
	}
	
	private void trySetFromResource( MetaStructureThing thing, Model model,
		Resource resource, UriOrVariable property, String key )
	{
		Node node = tryGetResource( model, resource, property, Resource.class );
		if ( node != null )
		{
			String value = resourceUri( node );
			System.out.println( "\t" + key + ": " + value );
			thing.setAdditionalProperty( key, value );
		}
	}

	private void readClasses( Model model )
	{
		ClosableIterator<? extends Statement> itr =
			model.findStatements( Variable.ANY, RDF.type, RDFS.Class );
		try
		{
			while ( itr.hasNext() )
			{
				Statement statement = itr.next();
				Resource subject = statement.getSubject();
				String className = resourceUri( subject );
				System.out.println( "Class: " + className );
				MetaStructureClass metaClass =
					meta().getMetaClass( className, true );
				trySetLabelAndComment( metaClass, model, subject );
			}
		}
		finally
		{
			itr.close();
		}
		
		itr = model.findStatements( Variable.ANY, RDFS.subClassOf,
			Variable.ANY );
		try
		{
			while ( itr.hasNext() )
			{
				Statement statement = itr.next();
				String superName = resourceUri( statement.getObject() );
				if ( isW3Uri( superName ) )
				{
					continue;
				}
				String subName = resourceUri( statement.getSubject() );
				meta().getMetaClass( subName, true ).getDirectSupers().add(
					meta().getMetaClass( superName, true ) );
				System.out.println( subName + " subClassOf " + superName );
			}
		}
		finally
		{
			itr.close();
		}
	}
	
	private void readProperties( Model model )
	{
		ClosableIterator<? extends Statement> itr = model.findStatements(
			Variable.ANY, RDF.type, RDF.Property );
		try
		{
			while ( itr.hasNext() )
			{
				Statement statement = itr.next();
				Resource property = statement.getSubject();
				String propertyName = resourceUri( property );
				System.out.println( "Property: " + propertyName );
				MetaStructureProperty metaProperty =
					meta().getMetaProperty( propertyName, true );
				trySetLabelAndComment( metaProperty, model, property );
				trySetPropertyDomain( metaProperty, model, property );
				trySetPropertyRange( metaProperty, model, property );
			}
		}
		finally
		{
			itr.close();
		}

		itr = model.findStatements( Variable.ANY, RDFS.subPropertyOf,
			Variable.ANY );
		try
		{
			while ( itr.hasNext() )
			{
				Statement statement = itr.next();
				String superName = resourceUri( statement.getObject() );
				if ( isW3Uri( superName ) )
				{
					continue;
				}
				String subName = resourceUri( statement.getSubject() );
				meta().getMetaProperty( subName, true ).getDirectSupers().add(
					meta().getMetaProperty( superName, true ) );
				System.out.println( subName + " subPropertyOf " + superName );
			}
		}
		finally
		{
			itr.close();
		}
	}
	
	private void trySetPropertyDomain( MetaStructureProperty metaProperty,
		Model model, Resource property )
	{
		ClosableIterator<? extends Statement> itr =
			model.findStatements( property, RDFS.domain, Variable.ANY );
		try
		{
			while ( itr.hasNext() )
			{
				Statement statement = itr.next();
				String domainClass = resourceUri( statement.getObject() );
				meta().getMetaClass( domainClass, true ).getProperties().add(
					metaProperty );
				System.out.println( "\tdomain: " + domainClass );
			}
		}
		finally
		{
			itr.close();
		}
	}
	
	private void trySetPropertyRange( MetaStructureProperty metaProperty,
		Model model, Resource property )
	{
		ClosableIterator<? extends Statement> itr =
			model.findStatements( property, RDFS.range, Variable.ANY );
		try
		{
			while ( itr.hasNext() )
			{
				Statement statement = itr.next();
				Resource range = statement.getObject().asResource();
				String rangeType = resourceUri( range );
				MetaStructureClass metaClass =
					meta().getMetaClass( rangeType, false );
				PropertyRange propertyRange = null;
				if ( metaClass != null )
				{
					propertyRange = new MetaStructureClassRange( metaClass );
				}
//				else if ( isSubclassOf( model,
//					range, RDFS.Container.toString() ) != null )
				else if ( rangeType.equals( RDFS.Container.toString() ) ||
					rangeType.equals( RDF.Seq.toString() ) ||
					rangeType.equals( RDF.Bag.toString() ) ||
					rangeType.equals( RDF.Alt.toString() ) )
				{
					metaProperty.setMaxCardinality( Integer.MAX_VALUE );
					metaProperty.setCollectionBehaviourClass( List.class );
					// TODO
				}
				else if ( RdfUtil.recognizesDatatype( rangeType ) )
				{
					propertyRange = new RdfDatatypeRange( rangeType );
				}
				else if ( rangeType.equals( RDFS.Literal.toString() ) ||
					rangeType.equals( RDFS.Datatype.toString() ) )
				{
					propertyRange = new DatatypeClassRange( String.class );
				}
				
				if ( propertyRange != null )
				{
					metaProperty.setRange( propertyRange );
					System.out.println( "\trange: " + rangeType + " (" +
						propertyRange.getClass().getName() + ")" );
				}
			}
		}
		finally
		{
			itr.close();
		}
	}
	
	private boolean isW3Uri( String uriString )
	{
		try
		{
			URI uri = new URI( uriString );
			if ( uri.getHost().contains( "www.w3.org" ) )
			{
				return true;
			}
			return false;
		}
		catch ( URISyntaxException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	private Node tryGetResource( Model model, Resource resource,
		UriOrVariable predicate, Class<? extends Node> resourceClass )
	{
		ClosableIterator<? extends Statement> itr =
			model.findStatements( resource, predicate, Variable.ANY );
		try
		{
			if ( itr.hasNext() )
			{
				org.ontoware.rdf2go.model.node.Node object =
					itr.next().getObject();
				if ( resourceClass.isAssignableFrom( object.getClass() ) )
				{
					return object;
				}
			}
			return null;
		}
		finally
		{
			itr.close();
		}
	}
	
	private String tryGetLiteral( Model model,
		Resource resource, UriOrVariable predicate )
	{
		Node node = tryGetResource( model, resource, predicate, Literal.class );
		return node == null ? null : ( ( Literal ) node ).getValue();
	}
	
	private Model readModel( File file ) throws IOException
	{
		FileInputStream in = null;
		try
		{
			in = new FileInputStream( file );
			return readModel( in );
		}
		finally
		{
			safeClose( in );
		}
	}
	
	private Model readModel( InputStream stream ) throws IOException
	{
		Model model = RDF2Go.getModelFactory().createModel();
		model.open();
		model.readFrom( stream );
		return model;
	}
	
	private void safeClose( Closeable closeable )
	{
		if ( closeable != null )
		{
			try
			{
				closeable.close();
			}
			catch ( IOException e )
			{
				// Yep
			}
		}
	}
}
