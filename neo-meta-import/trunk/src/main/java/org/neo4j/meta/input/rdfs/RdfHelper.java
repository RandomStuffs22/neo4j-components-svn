package org.neo4j.meta.input.rdfs;

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

import org.neo4j.meta.model.RdfUtil;
import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.DatatypeLiteral;
import org.ontoware.rdf2go.model.node.Literal;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.ontoware.rdf2go.vocabulary.OWL;
import org.ontoware.rdf2go.vocabulary.RDF;

/**
 * Some fine helper methods for RDF models.
 */
abstract class RdfHelper
{
	static String local( String uri )
	{
		int index = uri.lastIndexOf( '#' );
		return index == -1 ? uri : uri.substring( index + 1 );
	}

	static String resourceUri( Node node )
	{
		return node.asURI().asJavaURI().toString();
	}

	static String resourceType( Model model, Resource resource )
	{
		Node[] nodes = subNodes( model, resource, RDF.type.toString() );
		return nodes.length == 0 ? null : resourceUri( nodes[ 0 ] );
	}
	
	static String literalDatatype( Model model, Literal literal )
	{
		if ( literal instanceof DatatypeLiteral )
		{
			return literal.asDatatypeLiteral().getDatatype().toString();
		}
		return RdfUtil.NS_XML_SCHEMA + "string";
	}

	static Node[] subNodes( Model model, Resource resource,
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
	
	static Node subNode( Model model, Resource resource, String predicate )
	{
		Node[] nodes = subNodes( model, resource, predicate );
		if ( nodes.length > 1 )
		{
			throw new RuntimeException( "More than one " + predicate +
				" for " + resourceUri( resource ) );
		}
		return nodes.length == 0 ? null : nodes[ 0 ];
	}
	
	static boolean resourceIsType( Model model, Resource resource,
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
	
	static boolean smartMatch( String type1, String type2 )
	{
		if ( type1.equals( type2 ) )
		{
			return true;
		}
		if ( isW3Uri( type1 ) && isW3Uri( type2 ) &&
			RdfHelper.local( type1 ).equals( RdfHelper.local( type2 ) ) )
		{
			return true;
		}
		return false;
	}
	
	static boolean smartMatchThese( String uri, String... matchWith )
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

	static boolean isW3Uri( String uriString )
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
	
	static String tryGetLiteral( Model model, Resource resource,
		String predicate )
	{
		Node node = subNode( model, resource, predicate );
		if ( node == null )
		{
			return null;
		}
		return node instanceof Literal ? ( ( Literal ) node ).getValue() : null;
	}

	static Collection<Node> getClassOrUnionOfClasses( Model model,
		Resource theClassOrUnion )
	{
		Collection<Node> result = null;
		if ( RdfHelper.resourceIsType( model, theClassOrUnion,
			OWL.Class.toString() ) && theClassOrUnion instanceof BlankNode )
		{
			// TODO Also support intersectionOf and complementOf
			result = new ArrayList<Node>();
			for ( Node classNode : RdfHelper.subNodes( model, theClassOrUnion,
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
	
	static Node getFirstInCollection( Model model, Resource resource )
	{
		return subNode( model, resource, RDF.first.toString() );
	}
	
	static Collection<Node> parseCollection( Model model,
		Resource resourceWhichIsACollection )
	{
		ArrayList<Node> collection = new ArrayList<Node>();
		collectFromCollection( model, resourceWhichIsACollection, collection );
		return collection;
	}
	
	static void collectFromCollection( Model model,
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
	
	static void printStatementsAbout( Model model, Node node )
	{
		ClosableIterator<? extends Statement> itr = model.findStatements(
			Variable.ANY, Variable.ANY, node );
		printStatements( itr );
		
		if ( node instanceof Resource )
		{
			itr = model.findStatements( node.asResource(), Variable.ANY,
				Variable.ANY );
			printStatements( itr );
		}
	}
	
	static void printStatements( ClosableIterator<? extends Statement> itr )
	{
		try
		{
			while ( itr.hasNext() )
			{
				printStatement( itr.next() );
			}
		}
		finally
		{
			itr.close();
		}
	}
	
	static void printStatement( Statement statement )
	{
		System.out.println( statement.getSubject() + ":" +
			statement.getPredicate() + ":" + statement.getObject() );
	}
	
	static Collection<Object> nodesToLiterals( Collection<Node> nodes,
		String datatype )
	{
		ArrayList<Object> list = new ArrayList<Object>();
		for ( Node node : nodes )
		{
			try
			{
				String type = datatype != null ? datatype :
					node.asDatatypeLiteral().getDatatype().toString();
				list.add( RdfUtil.getRealValue(
					type, ( ( Literal ) node ).getValue() ) );
			}
			catch ( ParseException e )
			{
				throw new RuntimeException( e );
			}
		}
		return list;
	}

	static void safeClose( Closeable closeable )
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

	static Model readModel( File file ) throws IOException
	{
		FileInputStream in = null;
		try
		{
			in = new FileInputStream( file );
			return readModel( in );
		}
		finally
		{
			RdfHelper.safeClose( in );
		}
	}
	
	static Model readModel( InputStream stream ) throws IOException
	{
		Model model = RDF2Go.getModelFactory().createModel();
		model.open();
		model.readFrom( stream );
		return model;
	}
}
