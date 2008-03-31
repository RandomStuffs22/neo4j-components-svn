package org.neo4j.neometa.input.rdfs;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;
import org.neo4j.neometa.structure.DatatypeClassRange;
import org.neo4j.neometa.structure.MetaStructureClassRange;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureClass;
import org.neo4j.neometa.structure.MetaStructureNamespace;
import org.neo4j.neometa.structure.MetaStructureProperty;
import org.neo4j.neometa.structure.MetaStructureThing;
import org.neo4j.neometa.structure.PropertyRange;
import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.Literal;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.UriOrVariable;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.vocabulary.RDF;
import org.ontoware.rdf2go.vocabulary.RDFS;

public class RdfsImporter
{
	private static enum RelTypes implements RelationshipType
	{
		HEJSAN,
	}
	
	private MetaStructureNamespace meta;
	
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
		
		Transaction tx = neo.beginTx();
		Node rootNode = null;
		try
		{
			rootNode = neo.createNode();
			neo.getReferenceNode().createRelationshipTo( rootNode,
				RelTypes.HEJSAN );
			tx.success();
		}
		finally
		{
			tx.finish();
		}
		
		try
		{
			MetaStructure meta = new MetaStructure( neo, rootNode );
			new RdfsImporter( meta.getGlobalNamespace() ).doImport(
				new File( "test.rdfs" ) );
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
	
	private RdfsImporter( MetaStructureNamespace meta )
	{
		this.meta = meta;
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
	
//	private String resourceType( Resource resource, Model model )
//	{
//		ClosableIterator<? extends Statement> itr = model.findStatements(
//			resource, RDF.type, Variable.ANY );
//		try
//		{
//			return resourceUri( itr.next().getObject() );
//		}
//		finally
//		{
//			itr.close();
//		}
//	}
	
	private void trySetLabelAndComment( MetaStructureThing thing,
		Model model, Resource resource )
	{
		String label = tryGetLiteral( model, resource, RDFS.label );
		String comment = tryGetLiteral( model, resource, RDFS.comment );
		System.out.println( "\tlabel: " + label );
		System.out.println( "\tcomment: " + comment );
		if ( label != null )
		{
			thing.setLabel( label );
		}
		if ( comment != null )
		{
			thing.setComment( comment );
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
					meta.getMetaClass( className, true );
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
				meta.getMetaClass( subName, true ).getDirectSupers().add(
					meta.getMetaClass( superName, true ) );
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
					meta.getMetaProperty( propertyName, true );
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
				meta.getMetaProperty( subName, true ).getDirectSupers().add(
					meta.getMetaProperty( superName, true ) );
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
				meta.getMetaClass( domainClass, true ).getProperties().add(
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
					meta.getMetaClass( rangeType, false );
				PropertyRange propertyRange = null;
				if ( metaClass != null )
				{
					propertyRange = new MetaStructureClassRange( metaClass );
				}
				else if ( rangeType.equals( RDFS.Container.toString() ) ||
					rangeType.equals( RDF.Seq.toString() ) ||
					rangeType.equals( RDF.Bag.toString() ) ||
					rangeType.equals( RDF.Alt.toString() ) )
				{
//					throw new UnsupportedOperationException( "range type " +
//						rangeType + " not supported" );
				}
				else if ( rangeType.equals( RDFS.Literal.toString() ) ||
					rangeType.equals( RDFS.Datatype.toString() ) )
				{
					propertyRange = new DatatypeClassRange( String.class );
				}
				
				if ( propertyRange != null )
				{
					metaProperty.setRange( propertyRange );
				}
				System.out.println( "\trange: " + rangeType );
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
	
	private String tryGetLiteral( Model model,
		Resource resource, UriOrVariable predicate )
	{
		ClosableIterator<? extends Statement> itr =
			model.findStatements( resource, predicate, Variable.ANY );
		try
		{
			if ( itr.hasNext() )
			{
				org.ontoware.rdf2go.model.node.Node object =
					itr.next().getObject();
				if ( object instanceof Literal )
				{
					return ( ( Literal ) object ).getValue();
				}
			}
			return null;
		}
		finally
		{
			itr.close();
		}
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
