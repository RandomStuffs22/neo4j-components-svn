package org.neo4j.meta.input.rdfs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.neo4j.graphdb.Transaction;
import org.neo4j.meta.model.ClassRange;
import org.neo4j.meta.model.DataRange;
import org.neo4j.meta.model.DatatypeClassRange;
import org.neo4j.meta.model.MetaModel;
import org.neo4j.meta.model.MetaModelClass;
import org.neo4j.meta.model.MetaModelImpl;
import org.neo4j.meta.model.MetaModelNamespace;
import org.neo4j.meta.model.MetaModelProperty;
import org.neo4j.meta.model.MetaModelRestrictable;
import org.neo4j.meta.model.MetaModelThing;
import org.neo4j.meta.model.PropertyRange;
import org.neo4j.meta.model.RdfDatatypeRange;
import org.neo4j.meta.model.RdfUtil;
import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.Literal;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.vocabulary.OWL;
import org.ontoware.rdf2go.vocabulary.RDF;
import org.ontoware.rdf2go.vocabulary.RDFS;

/**
 * Imports RDF schema graphs and creates a meta model representation of it.
 */
public class RdfsImporter
{
	private MetaModel meta;
	
	/**
	 * @param model the {@link MetaModel} instance to use.
	 */
	public RdfsImporter( MetaModel model )
	{
		this.meta = model;
	}
	
	/**
	 * Since all is happening in the global namespace when importing from RDFS
	 * here's a method for that namespace.
	 * @return the global namespace.
	 */
	protected MetaModelNamespace meta()
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
		Model model = RdfHelper.readModel( file );
		Transaction tx = ( ( MetaModelImpl ) meta ).graphDb().beginTx();
		try
		{
			readFrom( model );
			tx.success();
		}
		finally
		{
			model.close();
			tx.finish();
		}
	}
	
	private void debug( String message )
	{
		System.out.println( message );
	}
	
	private void readFrom( Model model )
	{
		readClasses( model );
		readProperties( model );
		readRestrictions( model );
	}
	
	private void trySetLabelAndComment( MetaModelThing thing,
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
	
	private void trySetFromLiteral( MetaModelThing thing, Model model,
		Resource resource, String property, String key )
	{
		String value = RdfHelper.tryGetLiteral( model, resource, property );
		if ( value != null )
		{
			debug( "\t" + key + ": " + value );
			thing.setAdditionalProperty( key, value );
		}
	}
	
	private void trySetFromResource( MetaModelThing thing, Model model,
		Resource resource, String property, String key )
	{
		Node node = RdfHelper.subNode( model, resource, property );
		if ( node != null )
		{
			String value = RdfHelper.resourceUri( node );
			debug( "\t" + key + ": " + value );
			thing.setAdditionalProperty( key, value );
		}
	}

	private abstract class ThingReader<T extends MetaModelThing>
	{
		abstract T get( String name );
		
		abstract T readThing( Model model, Resource resource, String name );

		abstract void couple( String superName, String subName );
	}
	
	private class ClassReader extends ThingReader<MetaModelClass>
	{
		@Override
		MetaModelClass get( String name )
		{
			return meta().getMetaClass( name, true );
		}
		
		@Override
		MetaModelClass readThing( Model model, Resource resource,
			String name )
		{
			MetaModelClass metaClass = get( name );
			trySetLabelAndComment( metaClass, model, resource );
			return metaClass;
		}
		
		@Override
		void couple( String superName, String subName )
		{
			get( superName ).getDirectSubs().add( get( subName ) );
		}
	}
	
	private class PropertyReader extends ThingReader<MetaModelProperty>
	{
		@Override
		MetaModelProperty get( String name )
		{
			return meta().getMetaProperty( name, true );
		}
		
		@Override
		MetaModelProperty readThing( Model model, Resource resource,
			String name )
		{
			MetaModelProperty metaProperty = get( name );
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

	private <T extends MetaModelThing> void readThings( Model model,
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
					debug( "Skipping blank " +
						RdfHelper.local( type.toString() ) );
					continue;
				}
				String className = RdfHelper.resourceUri( subject );
				debug( RdfHelper.local( type.toString() ) + ": " + className );
				reader.readThing( model, subject, className );
			}
		}
		finally
		{
			itr.close();
		}
	}
	
	private <T extends MetaModelThing> void coupleThings( Model model,
		org.ontoware.rdf2go.model.node.URI type, ThingReader<T> reader )
	{
		ClosableIterator<? extends Statement> itr = model.findStatements(
			Variable.ANY, type, Variable.ANY );
		try
		{
			while ( itr.hasNext() )
			{
				Statement statement = itr.next();
				if ( RdfHelper.resourceIsType( model, statement.getObject().
					asResource(), OWL.Restriction.toString() ) )
				{
//					debug( "Skipping restriction" );
					continue;
				}
				String superName =
					RdfHelper.resourceUri( statement.getObject() );
//				if ( RdfHelper.isW3Uri( superName ) )
//				{
//					continue;
//				}
				String subName =
					RdfHelper.resourceUri( statement.getSubject() );
				reader.couple( superName, subName );
				debug( subName + " " + RdfHelper.local( type.toString() ) +
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
	
	private void trySetPropertyDomain( MetaModelProperty metaProperty,
		Model model, Resource property )
	{
		for ( Node domainNode :
			RdfHelper.subNodes( model, property, RDFS.domain.toString() ) )
		{
			for ( Node classNode : RdfHelper.getClassOrUnionOfClasses( model,
				domainNode.asResource() ) )
			{
				String domainClass = RdfHelper.resourceUri( classNode );
				meta().getMetaClass( domainClass, true ).getDirectProperties().
					add( metaProperty );
				debug( "\tdomain: " + domainClass );
			}
		}
	}
	
	private MetaModelClass[] nodesToClasses( Collection<Node> classNodes )
	{
		MetaModelClass[] classes =
			new MetaModelClass[ classNodes.size() ];
		int i = 0;
		for ( Node classNode : classNodes )
		{
			classes[ i++ ] = meta().getMetaClass(
				RdfHelper.resourceUri( classNode ), true );
		}
		return classes;
	}
	
	private PropertyRange mergeRange( PropertyRange previousRange,
		PropertyRange newRange )
	{
		if ( previousRange == null )
		{
			return newRange;
		}
		
		PropertyRange result = null;
		if ( newRange instanceof DataRange )
		{
			DataRange previousDataRange = ( DataRange ) previousRange;
			DataRange newDataRange = ( DataRange ) newRange;
			Collection<Object> values = new ArrayList<Object>(
				previousDataRange.getValues() );
			values.addAll( newDataRange.getValues() );
			result = new DataRange( newDataRange.getRdfDatatype(),
				values.toArray() );
		}
		else if ( newRange instanceof ClassRange )
		{
			ClassRange previousMetaRange =
				( ClassRange ) previousRange;
			ClassRange newMetaRange =
				( ClassRange ) newRange;
			Collection<MetaModelClass> classes =
				new ArrayList<MetaModelClass>();
			classes.addAll( Arrays.asList(
				previousMetaRange.getRangeClasses() ) );
			classes.addAll( Arrays.asList(
				newMetaRange.getRangeClasses() ) );
			result = new ClassRange(
				classes.toArray( new MetaModelClass[ classes.size() ] ) );
		}
		else
		{
			throw new RuntimeException( "Can't merge property range type " +
				newRange );
		}
		return result;
	}
	
	private PropertyRange buildCollectionRange( Model model, Resource resource )
	{
		PropertyRange propertyRange = null;
		String rangeType = RdfHelper.resourceType( model, resource );
		if ( RdfHelper.smartMatchThese( rangeType,
			OWL.DataRange.toString() ) )
		{
			Node collectionNode = RdfHelper.subNode( model,
				resource.asResource(), OWL.oneOf.toString() );
			if ( collectionNode == null )
			{
				throw new RuntimeException( "No collection" );
			}
			
			Node first = RdfHelper.getFirstInCollection( model,
				collectionNode.asResource() );
			String datatype =
				first.asDatatypeLiteral().getDatatype().toString();
			Collection<Node> nodes = RdfHelper.parseCollection( model,
				collectionNode.asResource() );
			Collection<Object> values =
				RdfHelper.nodesToLiterals( nodes, datatype );
			propertyRange = new DataRange( datatype, values.toArray() );
		}
		else if ( RdfHelper.smartMatchThese( rangeType,
			OWL.Class.toString() ) )
		{
			Collection<Node> classNodes =
				RdfHelper.getClassOrUnionOfClasses( model, resource );
			propertyRange = new ClassRange(
				nodesToClasses( classNodes ) );
		}
		else
		{
			throw new RuntimeException( "Unknown blank range " +
				rangeType );
		}
		return propertyRange;
	}
	
	private PropertyRange buildOneValueRange( Model model, Node rangeNode )
	{
		PropertyRange propertyRange = null;
		String rangeType = null;
		if ( rangeNode instanceof Resource )
		{
			rangeType = RdfHelper.resourceUri( rangeNode.asResource() );
		}
		else
		{
			rangeType = RdfHelper.literalDatatype( model,
				( Literal ) rangeNode );
		}
		
		MetaModelClass metaClass =
			meta().getMetaClass( rangeType, false );
		if ( rangeNode instanceof Literal ||
			RdfUtil.recognizesDatatype( rangeType ) )
		{
			if ( rangeNode instanceof Literal )
			{
				Literal literal = ( Literal ) rangeNode;
				String literalValue = literal.getValue();
				propertyRange = new DataRange( rangeType, literalValue );
			}
			else if ( rangeNode instanceof URI )
			{
				propertyRange = new RdfDatatypeRange(
					( ( URI ) rangeNode ).toString() );
			}
			else
			{
				throw new RuntimeException( "Unrecognized type '" +
					rangeNode + "'" );
			}
		}
		else if ( metaClass != null )
		{
			propertyRange = new ClassRange( metaClass );
		}
		else if ( RdfHelper.smartMatchThese( rangeType,
			RDFS.Container.toString(), RDF.Seq.toString(),
			RDF.Bag.toString(), RDF.Alt.toString() ) )
		{
			// TODO
		}
		else if ( RdfHelper.smartMatchThese( rangeType,
			RDFS.Literal.toString(), RDFS.Datatype.toString(),
			RDFS.XMLLiteral.toString() ) )
		{
			propertyRange = new DatatypeClassRange( String.class );
		}
		else if ( RdfHelper.smartMatchThese( rangeType,
			OWL.Thing.toString() ) )
		{
			// TODO Skip?
		}
		else
		{
			// TODO Throw something to let em know?
			throw new RuntimeException( "Unknown property range type " +
				rangeType );
		}
		return propertyRange;
	}
	
	private PropertyRange buildPropertyRange( Model model, Node rangeNode )
	{
		PropertyRange propertyRange = null;
		if ( rangeNode instanceof BlankNode )
		{
			propertyRange = buildCollectionRange( model,
				rangeNode.asResource() );
		}
		else
		{
			propertyRange = buildOneValueRange( model, rangeNode );
		}
		return propertyRange;
	}
	
	private void trySetPropertyRange( MetaModelRestrictable restrictable,
		Model model, Resource property )
	{
		PropertyRange propertyRange = null;
		for ( Node rangeNode :
			RdfHelper.subNodes( model, property, RDFS.range.toString() ) )
		{
			PropertyRange newRange = buildPropertyRange( model, rangeNode );
			newRange = mergeRange( propertyRange, newRange );
			propertyRange = newRange;
		}
		
		if ( propertyRange != null )
		{
			restrictable.setRange( propertyRange );
			debug( "\trange: " + propertyRange );
		}
		
		if ( restrictable instanceof MetaModelProperty )
		{
			trySetPropertyFunctionality( model, property,
				( MetaModelProperty ) restrictable );
		}
	}
	
	private void trySetPropertyFunctionality( Model model, Resource property,
		MetaModelProperty metaProperty )
	{
		String propertyFunctionality = null;
		if ( RdfHelper.resourceIsType( model, property,
			OWL.FunctionalProperty.toString() ) )
		{
			propertyFunctionality = "functional";
			metaProperty.setMinCardinality( 0 );
			metaProperty.setMaxCardinality( 1 );
		}
		else if ( RdfHelper.resourceIsType( model, property,
			OWL.InverseFunctionalProperty.toString() ) )
		{
			propertyFunctionality = "inverseFunctional";
			// TODO cardinality here?
		}
		if ( propertyFunctionality != null )
		{
			metaProperty.setAdditionalProperty( "functionality",
				propertyFunctionality );
			debug( "\t" + propertyFunctionality );
		}
	}
	
	private void trySetPropertyInverseOf( MetaModelProperty metaProperty,
		Model model, Resource property )
	{
		for ( Node inverseOfNode :
			RdfHelper.subNodes( model, property, OWL.inverseOf.toString() ) )
		{
			String inverseProperty = RdfHelper.resourceUri( inverseOfNode );
			metaProperty.setInverseOf( meta().getMetaProperty(
				inverseProperty, true ) );
			debug( "\tinverseOf: " + inverseProperty );
		}
	}
	
	private void readRestrictions( Model model )
	{
		ClosableIterator<? extends Statement> itr = model.findStatements(
			Variable.ANY, RDFS.subClassOf, Variable.ANY );
		try
		{
			while ( itr.hasNext() )
			{
				Statement statement = itr.next();
				Resource restriction = statement.getObject().asResource();
				if ( !RdfHelper.resourceIsType( model, restriction,
					OWL.Restriction.toString() ) )
				{
					continue;
				}
				
				Resource ownerClass = statement.getSubject();
				Resource onProperty = RdfHelper.subNode( model, restriction,
					OWL.onProperty.toString() ).asResource();
				MetaModelClass metaClass = meta().getMetaClass(
					RdfHelper.resourceUri( ownerClass ), true );
				MetaModelProperty metaProperty = meta().getMetaProperty(
					RdfHelper.resourceUri( onProperty ), true );
				MetaModelRestrictable restrictable =
					metaClass.getRestriction( metaProperty, true );
				debug( "Created a restriction " + metaClass +
					" ----> " + metaProperty );
				trySetCardinality( restrictable, model, restriction );
				
				// Try get the values, owl:allValuesFrom etc.
				Resource rangeResource = findResourceOutOf( model, restriction,
					OWL.allValuesFrom.toString(),
					OWL.someValuesFrom.toString(),
					OWL.hasValue.toString() );
				if ( rangeResource != null )
				{
					PropertyRange propertyRange =
						buildPropertyRange( model, rangeResource );
					if ( propertyRange != null )
					{
						restrictable.setRange( propertyRange );
						debug( "\trange: " + propertyRange );
					}
				}
			}
		}
		finally
		{
			itr.close();
		}
	}
	
	private Resource findResourceOutOf( Model model, Resource resource,
		String... predicatesToTest )
	{
		for ( String predicate : predicatesToTest )
		{
			Node subNode = RdfHelper.subNode( model, resource, predicate );
			if ( subNode != null )
			{
				return subNode.asResource();
			}
		}
		return null;
	}
	
	private void trySetCardinality( MetaModelRestrictable restrictable,
		Model model, Resource restriction )
	{
		Integer cardinality = tryGetInteger( model, restriction,
			OWL.cardinality.toString() );
		Integer maxCardinality = tryGetInteger( model, restriction,
			OWL.maxCardinality.toString() );
		Integer minCardinality = tryGetInteger( model, restriction,
			OWL.minCardinality.toString() );
		if ( cardinality != null )
		{
			restrictable.setCardinality( cardinality );
			debug( "\tcardinality: " + cardinality );
		}
		if ( maxCardinality != null )
		{
			restrictable.setMaxCardinality( maxCardinality );
			debug( "\tmaxCardinality: " + maxCardinality );
		}
		if ( minCardinality != null )
		{
			restrictable.setMinCardinality( minCardinality );
			debug( "\tminCardinality: " + minCardinality );
		}
	}
	
	private Integer tryGetInteger( Model model, Resource resource,
		String predicate )
	{
		String literal = RdfHelper.tryGetLiteral( model, resource, predicate );
		return literal == null ? null : Integer.parseInt( literal );
	}
}
