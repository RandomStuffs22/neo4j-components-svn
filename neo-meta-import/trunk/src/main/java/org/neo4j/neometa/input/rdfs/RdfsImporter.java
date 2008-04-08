package org.neo4j.neometa.input.rdfs;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.neo4j.api.core.Transaction;
import org.neo4j.neometa.structure.DataRange;
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
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.Literal;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.ontoware.rdf2go.vocabulary.OWL;
import org.ontoware.rdf2go.vocabulary.RDF;
import org.ontoware.rdf2go.vocabulary.RDFS;

/**
 * Imports RDF schema graphs and creates a neo meta model representation of it.
 */
public class RdfsImporter
{
	private MetaStructure meta;
	
	/**
	 * @param meta the {@link MetaStructure} instance to use.
	 */
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
	
	/**
	 * Imports an RDF/XML graph from a file.
	 * @param file the file containing the RDF/XML graph.
	 * @throws IOException if there were problems reading the file.
	 */
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
	
	private void debug( String message )
	{
		System.out.println( message );
	}
	
	private static String local( String uri )
	{
		int index = uri.lastIndexOf( '#' );
		return index == -1 ? uri : uri.substring( index + 1 );
	}
	
	private void readFrom( Model model )
	{
		readClasses( model );
		readProperties( model );
	}
	
	private String resourceUri( Node node )
	{
		return node.asURI().asJavaURI().toString();
	}
	
	private String resourceType( Model model, Resource resource )
	{
		Node[] nodes = subNodes( model, resource, RDF.type.toString() );
		return nodes.length == 0 ? null : resourceUri( nodes[ 0 ] );
	}
	
	private Node[] subNodes( Model model, Resource resource,
		String predicate )
	{
		ClosableIterator<? extends Statement> itr = model.findStatements(
			resource, new URIImpl( predicate ), Variable.ANY );
		try
		{
			ArrayList<Node> result = new ArrayList<Node>();
			while ( itr.hasNext() )
			{
				result.add( itr.next().getObject() );
			}
			return result.toArray( new Node[ result.size() ] );
		}
		finally
		{
			itr.close();
		}
	}
	
	private Node subNode( Model model, Resource resource, String predicate )
	{
		Node[] nodes = subNodes( model, resource, predicate );
		if ( nodes.length > 1 )
		{
			throw new RuntimeException( "More than one " + predicate +
				" for " + resourceUri( resource ) );
		}
		return nodes.length == 0 ? null : nodes[ 0 ];
	}
	
	private boolean resourceIsType( Model model, Resource resource,
		String type )
	{
		for ( Node typeNode : subNodes( model, resource, RDF.type.toString() ) )
		{
			if ( smartMatch( resourceUri( typeNode ), type ) )
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean smartMatch( String type1, String type2 )
	{
		if ( type1.equals( type2 ) )
		{
			return true;
		}
		if ( isW3Uri( type1 ) && isW3Uri( type2 ) &&
			local( type1 ).equals( local( type2 ) ) )
		{
			return true;
		}
		return false;
	}
	
	private boolean smartMatchThese( String uri, String... matchWith )
	{
		for ( String match : matchWith )
		{
			if ( smartMatch( uri, match ) )
			{
				return true;
			}
		}
		return false;
	}
	
	private void trySetLabelAndComment( MetaStructureThing thing,
		Model model, Resource resource )
	{
		trySetFromLiteral( thing, model, resource,
			RDFS.label.toString(), "label" );
		trySetFromLiteral( thing, model, resource,
			RDFS.comment.toString(), "comment" );
		trySetFromResource( thing, model, resource,
			RDFS.seeAlso.toString(), "seeAlso" );
		trySetFromResource( thing, model, resource,
			RDFS.isDefinedBy.toString(), "isDefinedBy" );
	}
	
	private void trySetFromLiteral( MetaStructureThing thing, Model model,
		Resource resource, String property, String key )
	{
		String value = tryGetLiteral( model, resource, property );
		if ( value != null )
		{
			debug( "\t" + key + ": " + value );
			thing.setAdditionalProperty( key, value );
		}
	}
	
	private void trySetFromResource( MetaStructureThing thing, Model model,
		Resource resource, String property, String key )
	{
		Node node = subNode( model, resource, property );
		if ( node != null )
		{
			String value = resourceUri( node );
			debug( "\t" + key + ": " + value );
			thing.setAdditionalProperty( key, value );
		}
	}

	private abstract class ThingReader<T extends MetaStructureThing>
	{
		abstract T get( String name );
		
		abstract T readThing( Model model, Resource resource, String name );

		abstract void couple( String superName, String subName );
	}
	
	private class ClassReader extends ThingReader<MetaStructureClass>
	{
		@Override
		MetaStructureClass get( String name )
		{
			return meta().getMetaClass( name, true );
		}
		
		@Override
		MetaStructureClass readThing( Model model, Resource resource,
			String name )
		{
			MetaStructureClass metaClass = get( name );
			trySetLabelAndComment( metaClass, model, resource );
			return metaClass;
		}
		
		@Override
		void couple( String superName, String subName )
		{
			get( superName ).getDirectSubs().add( get( subName ) );
		}
	}
	
	private class PropertyReader extends ThingReader<MetaStructureProperty>
	{
		@Override
		MetaStructureProperty get( String name )
		{
			return meta().getMetaProperty( name, true );
		}
		
		@Override
		MetaStructureProperty readThing( Model model, Resource resource,
			String name )
		{
			MetaStructureProperty metaProperty = get( name );
			trySetLabelAndComment( metaProperty, model, resource );
			trySetPropertyDomain( metaProperty, model, resource );
			trySetPropertyRange( metaProperty, model, resource );
			trySetPropertyInverseOf( metaProperty, model, resource );
			return metaProperty;
		}
		
		@Override
		void couple( String superName, String subName )
		{
			get( superName ).getDirectSubs().add( get( subName ) );
		}
	}

	private <T extends MetaStructureThing> void readThings( Model model,
		org.ontoware.rdf2go.model.node.URI type, ThingReader<T> reader )
	{
		ClosableIterator<? extends Statement> itr =
			model.findStatements( Variable.ANY, RDF.type, type );
		try
		{
			while ( itr.hasNext() )
			{
				Statement statement = itr.next();
				Resource subject = statement.getSubject();
				if ( subject instanceof BlankNode )
				{
					debug( "Skipping blank " + local( type.toString() ) );
					continue;
				}
				String className = resourceUri( subject );
				debug( local( type.toString() ) + ": " + className );
				reader.readThing( model, subject, className );
			}
		}
		finally
		{
			itr.close();
		}
	}
	
	private <T extends MetaStructureThing> void coupleThings( Model model,
		org.ontoware.rdf2go.model.node.URI type, ThingReader<T> reader )
	{
		ClosableIterator<? extends Statement> itr = model.findStatements(
			Variable.ANY, type, Variable.ANY );
		try
		{
			while ( itr.hasNext() )
			{
				Statement statement = itr.next();
				if ( resourceIsType( model, statement.getObject().
					asResource(), OWL.Restriction.toString() ) )
				{
					debug( "Skipping restriction" );
					continue;
				}
				String superName = resourceUri( statement.getObject() );
				if ( isW3Uri( superName ) )
				{
					continue;
				}
				String subName = resourceUri( statement.getSubject() );
				reader.couple( superName, subName );
				debug( subName + " " + local( type.toString() ) +
					" " + superName );
			}
		}
		finally
		{
			itr.close();
		}
	}

	private void readClasses( Model model )
	{
		ClassReader reader = new ClassReader();
		readThings( model, RDFS.Class, reader );
		readThings( model, OWL.Class, reader );
		coupleThings( model, RDFS.subClassOf, reader );
	}
	
	private void readProperties( Model model )
	{
		PropertyReader reader = new PropertyReader();
		readThings( model, RDF.Property, reader );
		readThings( model, OWL.DatatypeProperty, reader );
		readThings( model, OWL.ObjectProperty, reader );
		coupleThings( model, RDFS.subPropertyOf, reader );
	}
	
	private void trySetPropertyDomain( MetaStructureProperty metaProperty,
		Model model, Resource property )
	{
		for ( Node domainNode :
			subNodes( model, property, RDFS.domain.toString() ) )
		{
			for ( Node classNode :
				getClassOrUnionOfClasses( model, domainNode.asResource() ) )
			{
				String domainClass = resourceUri( classNode );
				meta().getMetaClass( domainClass, true ).getDirectProperties().
					add( metaProperty );
				debug( "\tdomain: " + domainClass );
			}
		}
	}
	
	private Collection<Node> getClassOrUnionOfClasses( Model model,
		Resource theClassOrUnion )
	{
		Collection<Node> result = null;
		if ( resourceIsType( model, theClassOrUnion, OWL.Class.toString() ) &&
			theClassOrUnion instanceof BlankNode )
		{
			result = new ArrayList<Node>();
			for ( Node classNode : subNodes( model, theClassOrUnion,
				OWL.unionOf.toString() ) )
			{
				result = parseCollection( model, classNode.asResource() );
			}
		}
		else
		{
			result = Arrays.asList( ( Node ) theClassOrUnion );
		}
		return result;
	}
	
	private Node getFirstInCollection( Model model, Resource resource )
	{
		return subNode( model, resource, RDF.first.toString() );
	}
	
	private Collection<Node> parseCollection( Model model,
		Resource resourceWhichIsACollection )
	{
		ArrayList<Node> collection = new ArrayList<Node>();
		collectFromCollection( model, resourceWhichIsACollection, collection );
		return collection;
	}
	
	private void collectFromCollection( Model model,
		Resource resourceWhichIsACollection, Collection<Node> collection )
	{
		Node firstNode = getFirstInCollection( model,
			resourceWhichIsACollection );
		if ( firstNode != null )
		{
			collection.add( firstNode );
			Node restNode = subNode( model, resourceWhichIsACollection,
				RDF.rest.toString() );
			if ( restNode != null && !resourceIsType( model,
				restNode.asResource(), RDF.nil.toString() ) )
			{
				collectFromCollection( model, restNode.asResource(),
					collection );
			}
		}
	}
	
//	private void printStatementsAbout( Model model, Node node )
//	{
//		ClosableIterator<? extends Statement> itr = model.findStatements(
//			Variable.ANY, Variable.ANY, node );
//		printStatements( itr );
//		
//		if ( node instanceof Resource )
//		{
//			itr = model.findStatements( node.asResource(), Variable.ANY,
//				Variable.ANY );
//			printStatements( itr );
//		}
//	}
//	
//	private void printStatements( ClosableIterator<? extends Statement> itr )
//	{
//		try
//		{
//			while ( itr.hasNext() )
//			{
//				printStatement( itr.next() );
//			}
//		}
//		finally
//		{
//			itr.close();
//		}
//	}
//	
//	private void printStatement( Statement statement )
//	{
//		debug( statement.getSubject() + ":" +
//			statement.getPredicate() + ":" + statement.getObject() );
//	}
	
	private Collection<Object> nodesToLiterals( Collection<Node> nodes,
		String datatype )
	{
		ArrayList<Object> list = new ArrayList<Object>();
		for ( Node node : nodes )
		{
			try
			{
				list.add( RdfUtil.getRealValue(
					datatype, ( ( Literal ) node ).getValue() ) );
			}
			catch ( ParseException e )
			{
				throw new RuntimeException( e );
			}
		}
		return list;
	}
	
	private void trySetPropertyRange( MetaStructureProperty metaProperty,
		Model model, Resource property )
	{
		for ( Node rangeNode :
			subNodes( model, property, RDFS.range.toString() ) )
		{
			Resource range = rangeNode.asResource();
			PropertyRange propertyRange = null;
			if ( range instanceof BlankNode )
			{
				String rangeType = resourceType( model, range );
				if ( smartMatchThese( rangeType, OWL.DataRange.toString() ) )
				{
					Node collectionNode = subNode( model, range.asResource(),
						OWL.oneOf.toString() );
					if ( collectionNode == null )
					{
						throw new RuntimeException( "No collection" );
					}
					
					Node first = getFirstInCollection( model,
						collectionNode.asResource() );
					String datatype =
						first.asDatatypeLiteral().getDatatype().toString();
					Collection<Node> nodes = parseCollection( model,
						collectionNode.asResource() );
					Collection<Object> values =
						nodesToLiterals( nodes, datatype );
					propertyRange = new DataRange( datatype, values.toArray() );
				}
				else if ( smartMatchThese( rangeType, OWL.Class.toString() ) )
				{
					Collection<Node> classNodes =
						getClassOrUnionOfClasses( model, range );
					MetaStructureClass[] classes =
						new MetaStructureClass[ classNodes.size() ];
					int i = 0;
					for ( Node classNode : classNodes )
					{
						classes[ i++ ] = meta().getMetaClass(
							resourceUri( classNode ), true );
					}
					propertyRange = new MetaStructureClassRange( classes );
				}
				else
				{
					throw new RuntimeException( "Unknown blank range " +
						rangeType );
				}
			}
			else
			{
				String rangeType = resourceUri( range );
				MetaStructureClass metaClass =
					meta().getMetaClass( rangeType, false );
				if ( metaClass != null )
				{
					propertyRange = new MetaStructureClassRange( metaClass );
				}
				else if ( smartMatchThese( rangeType, RDFS.Container.toString(),
					RDF.Seq.toString(), RDF.Bag.toString(),
					RDF.Alt.toString() ) )
				{
					metaProperty.setCollectionBehaviourClass( List.class );
					// TODO
				}
				else if ( RdfUtil.recognizesDatatype( rangeType ) )
				{
					propertyRange = new RdfDatatypeRange( rangeType );
				}
				else if ( smartMatchThese( rangeType, RDFS.Literal.toString(),
					RDFS.Datatype.toString(), RDFS.XMLLiteral.toString() ) )
				{
					propertyRange = new DatatypeClassRange( String.class );
				}
				else if ( smartMatchThese( rangeType, OWL.Thing.toString() ) )
				{
					// TODO Skip?
				}
				else
				{
					// TODO Throw something to let em know?
					throw new RuntimeException( "Unknown property range type " +
						rangeType );
				}
			}

			if ( propertyRange != null )
			{
				metaProperty.setRange( propertyRange );
				debug( "\trange: " + propertyRange );
			}
			trySetPropertyFunctionality( model, property );
		}
	}
	
	private void trySetPropertyFunctionality( Model model, Resource property )
	{
		String propertyFunctionality = null;
		if ( resourceIsType( model, property,
			OWL.FunctionalProperty.toString() ) )
		{
			propertyFunctionality = "functional";
		}
		else if ( resourceIsType( model, property,
			OWL.InverseFunctionalProperty.toString() ) )
		{
			propertyFunctionality = "inverseFunctional";
		}
		if ( propertyFunctionality != null )
		{
			debug( "\t" + propertyFunctionality );
		}
	}
	
	private void trySetPropertyInverseOf( MetaStructureProperty metaProperty,
		Model model, Resource property )
	{
		for ( Node inverseOfNode :
			subNodes( model, property, OWL.inverseOf.toString() ) )
		{
			String inverseProperty = resourceUri( inverseOfNode );
			metaProperty.setInverseOf( meta().getMetaProperty(
				inverseProperty, true ) );
			debug( "\tinverseOf: " + inverseProperty );
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
	
	private String tryGetLiteral( Model model, Resource resource,
		String predicate )
	{
		Node node = subNode( model, resource, predicate );
		if ( node == null )
		{
			return null;
		}
		return node instanceof Literal ? ( ( Literal ) node ).getValue() : null;
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
