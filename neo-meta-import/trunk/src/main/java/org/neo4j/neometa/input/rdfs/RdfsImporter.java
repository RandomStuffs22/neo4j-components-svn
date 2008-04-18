package org.neo4j.neometa.input.rdfs;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.neo4j.api.core.Transaction;
import org.neo4j.neometa.structure.DataRange;
import org.neo4j.neometa.structure.DatatypeClassRange;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureClass;
import org.neo4j.neometa.structure.MetaStructureClassRange;
import org.neo4j.neometa.structure.MetaStructureImpl;
import org.neo4j.neometa.structure.MetaStructureNamespace;
import org.neo4j.neometa.structure.MetaStructureProperty;
import org.neo4j.neometa.structure.MetaStructureRestrictable;
import org.neo4j.neometa.structure.MetaStructureThing;
import org.neo4j.neometa.structure.PropertyRange;
import org.neo4j.neometa.structure.RdfDatatypeRange;
import org.neo4j.neometa.structure.RdfUtil;
import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.Variable;
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
		Model model = RdfHelper.readModel( file );
		Transaction tx = ( ( MetaStructureImpl ) meta ).neo().beginTx();
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
		String value = RdfHelper.tryGetLiteral( model, resource, property );
		if ( value != null )
		{
			debug( "\t" + key + ": " + value );
			thing.setAdditionalProperty( key, value );
		}
	}
	
	private void trySetFromResource( MetaStructureThing thing, Model model,
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
	
	private void trySetPropertyDomain( MetaStructureProperty metaProperty,
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
	
	private MetaStructureClass[] nodesToClasses( Collection<Node> classNodes )
	{
		MetaStructureClass[] classes =
			new MetaStructureClass[ classNodes.size() ];
		int i = 0;
		for ( Node classNode : classNodes )
		{
			classes[ i++ ] = meta().getMetaClass(
				RdfHelper.resourceUri( classNode ), true );
		}
		return classes;
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
			propertyRange = new MetaStructureClassRange(
				nodesToClasses( classNodes ) );
		}
		else
		{
			throw new RuntimeException( "Unknown blank range " +
				rangeType );
		}
		return propertyRange;
	}
	
	private PropertyRange buildOneValueRange( Model model, Resource resource )
	{
		PropertyRange propertyRange = null;
		String rangeType = RdfHelper.resourceUri( resource );
		MetaStructureClass metaClass =
			meta().getMetaClass( rangeType, false );
		if ( metaClass != null )
		{
			propertyRange = new MetaStructureClassRange( metaClass );
		}
		else if ( RdfHelper.smartMatchThese( rangeType,
			RDFS.Container.toString(), RDF.Seq.toString(),
			RDF.Bag.toString(), RDF.Alt.toString() ) )
		{
			// TODO
		}
		else if ( RdfUtil.recognizesDatatype( rangeType ) )
		{
			propertyRange = new RdfDatatypeRange( rangeType );
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
	
	private PropertyRange buildPropertyRange( Model model, Resource resource )
	{
		PropertyRange propertyRange = null;
		if ( resource instanceof BlankNode )
		{
			propertyRange = buildCollectionRange( model, resource );
		}
		else
		{
			propertyRange = buildOneValueRange( model, resource );
		}
		return propertyRange;
	}
	
	private void trySetPropertyRange( MetaStructureRestrictable restrictable,
		Model model, Resource property )
	{
		for ( Node rangeNode :
			RdfHelper.subNodes( model, property, RDFS.range.toString() ) )
		{
			Resource range = rangeNode.asResource();
			PropertyRange propertyRange = buildPropertyRange( model, range );
			if ( propertyRange != null )
			{
				restrictable.setRange( propertyRange );
				debug( "\trange: " + propertyRange );
			}
			
			if ( restrictable instanceof MetaStructureProperty )
			{
				trySetPropertyFunctionality( model, property,
					( MetaStructureProperty ) restrictable );
			}
		}
	}
	
	private void trySetPropertyFunctionality( Model model, Resource property,
		MetaStructureProperty metaProperty )
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
	
	private void trySetPropertyInverseOf( MetaStructureProperty metaProperty,
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
				MetaStructureClass metaClass = meta().getMetaClass(
					RdfHelper.resourceUri( ownerClass ), true );
				MetaStructureProperty metaProperty = meta().getMetaProperty(
					RdfHelper.resourceUri( onProperty ), true );
				MetaStructureRestrictable restrictable =
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
	
	private void trySetCardinality( MetaStructureRestrictable restrictable,
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
