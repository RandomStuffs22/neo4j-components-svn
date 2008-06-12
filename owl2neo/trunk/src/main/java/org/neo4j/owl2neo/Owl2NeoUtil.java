package org.neo4j.owl2neo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;
import org.neo4j.meta.MetaManager;
import org.neo4j.meta.NodeType;
import org.neo4j.util.NeoStringSet;
import org.neo4j.util.NeoUtil;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.OWLAntiSymmetricObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLAxiomAnnotationAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLClassAssertionAxiom;
import org.semanticweb.owl.model.OWLClassAxiom;
import org.semanticweb.owl.model.OWLConstant;
import org.semanticweb.owl.model.OWLConstantAnnotation;
import org.semanticweb.owl.model.OWLDataAllRestriction;
import org.semanticweb.owl.model.OWLDataComplementOf;
import org.semanticweb.owl.model.OWLDataExactCardinalityRestriction;
import org.semanticweb.owl.model.OWLDataMaxCardinalityRestriction;
import org.semanticweb.owl.model.OWLDataMinCardinalityRestriction;
import org.semanticweb.owl.model.OWLDataOneOf;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLDataPropertyAxiom;
import org.semanticweb.owl.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owl.model.OWLDataPropertyExpression;
import org.semanticweb.owl.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owl.model.OWLDataRange;
import org.semanticweb.owl.model.OWLDataRangeFacetRestriction;
import org.semanticweb.owl.model.OWLDataRangeRestriction;
import org.semanticweb.owl.model.OWLDataSomeRestriction;
import org.semanticweb.owl.model.OWLDataSubPropertyAxiom;
import org.semanticweb.owl.model.OWLDataType;
import org.semanticweb.owl.model.OWLDataValueRestriction;
import org.semanticweb.owl.model.OWLDeclarationAxiom;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owl.model.OWLDisjointClassesAxiom;
import org.semanticweb.owl.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owl.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owl.model.OWLDisjointUnionAxiom;
import org.semanticweb.owl.model.OWLEntityAnnotationAxiom;
import org.semanticweb.owl.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owl.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owl.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owl.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owl.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLImportsDeclaration;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owl.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLObjectAllRestriction;
import org.semanticweb.owl.model.OWLObjectAnnotation;
import org.semanticweb.owl.model.OWLObjectComplementOf;
import org.semanticweb.owl.model.OWLObjectExactCardinalityRestriction;
import org.semanticweb.owl.model.OWLObjectIntersectionOf;
import org.semanticweb.owl.model.OWLObjectMaxCardinalityRestriction;
import org.semanticweb.owl.model.OWLObjectMinCardinalityRestriction;
import org.semanticweb.owl.model.OWLObjectOneOf;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyChainSubPropertyAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyExpression;
import org.semanticweb.owl.model.OWLObjectPropertyInverse;
import org.semanticweb.owl.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owl.model.OWLObjectSelfRestriction;
import org.semanticweb.owl.model.OWLObjectSomeRestriction;
import org.semanticweb.owl.model.OWLObjectSubPropertyAxiom;
import org.semanticweb.owl.model.OWLObjectUnionOf;
import org.semanticweb.owl.model.OWLObjectValueRestriction;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyAnnotationAxiom;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLProperty;
import org.semanticweb.owl.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLSameIndividualsAxiom;
import org.semanticweb.owl.model.OWLSubClassAxiom;
import org.semanticweb.owl.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLTypedConstant;
import org.semanticweb.owl.model.OWLUntypedConstant;
import org.semanticweb.owl.model.SWRLAtomConstantObject;
import org.semanticweb.owl.model.SWRLAtomDVariable;
import org.semanticweb.owl.model.SWRLAtomIVariable;
import org.semanticweb.owl.model.SWRLAtomIndividualObject;
import org.semanticweb.owl.model.SWRLBuiltInAtom;
import org.semanticweb.owl.model.SWRLClassAtom;
import org.semanticweb.owl.model.SWRLDataRangeAtom;
import org.semanticweb.owl.model.SWRLDataValuedPropertyAtom;
import org.semanticweb.owl.model.SWRLDifferentFromAtom;
import org.semanticweb.owl.model.SWRLObjectPropertyAtom;
import org.semanticweb.owl.model.SWRLRule;
import org.semanticweb.owl.model.SWRLSameAsAtom;
import org.semanticweb.owl.util.OWLObjectVisitorAdapter;
import org.semanticweb.owl.vocab.OWLXMLVocabulary;

/**
 * Utility for reading the ontologies an filling the neo meta model
 * {@link MetaManager} and the in-memory representation {@link OwlModel}
 * with all the properties, classes and restrictions from the ontologies.
 */
class Owl2NeoUtil
{
	private static enum Owl2NeoRelTypes implements RelationshipType
	{
		REF_OWL_2_NEO_ONTOLOGIES,
		OWL_2_NEO_ONTOLOGY,
	}
	
	private OWLOntologyManager owl;
	private Owl2Neo owl2Neo;
	private Set<String> ontologyBaseUris = new HashSet<String>();
	
	Owl2NeoUtil( Owl2Neo owl2Neo )
	{
		this.owl2Neo = owl2Neo;
		initialize();
	}
	
	private void initialize()
	{
		this.owl = OWLManager.createOWLOntologyManager();
	}
	
	private void putOwlFile( File owlFile )
	{
		try
		{
			owl.loadOntology( owlFile.toURI() );
			getStoredOntologies().add( readFile( owlFile ) );
		}
		catch ( OWLOntologyCreationException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	private Node getReferenceNode()
	{
		return new NeoUtil( owl2Neo.getNeo() ).getOrCreateSubReferenceNode(
			Owl2NeoRelTypes.REF_OWL_2_NEO_ONTOLOGIES );
	}
	
	private Collection<String> getStoredOntologies()
	{
		return new NeoStringSet( owl2Neo.getNeo(), getReferenceNode(),
			Owl2NeoRelTypes.OWL_2_NEO_ONTOLOGY ); 
	}
	
	private String readFile( File file )
	{
		BufferedReader input = null;
		try
		{
			input = new BufferedReader( new InputStreamReader(
				new FileInputStream( file ) ) );
			StringBuffer buffer = new StringBuffer();
			char[] readBuffer = new char[ 4000 ];
			int charsRead = -1;
			do
			{
				charsRead = input.read( readBuffer );
				if ( charsRead != -1 )
				{
					buffer.append( readBuffer, 0, charsRead );
				}
			}
			while ( charsRead != -1 );
			return buffer.toString();
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
		finally
		{
			IoUtil.safeClose( input );
		}
	}
	
	private File storeToTempFile( String string )
	{
		Writer writer = null;
		try
		{
			File file = File.createTempFile( "owl", "neo" );
			writer = new FileWriter( file );
			writer.write( string );
			return file;
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
		finally
		{
			IoUtil.safeClose( writer );
		}
	}
	
	private String getOntologyUri( String entireOntologyAsString )
	{
		String lookFor = "xmlns=\"";
		int startIndex = entireOntologyAsString.indexOf( lookFor );
		if ( startIndex == -1 )
		{
			throw new RuntimeException( "Missing '" + lookFor + "'" );
		}
		startIndex += lookFor.length();
		int endIndex = entireOntologyAsString.indexOf( "\"", startIndex );
		if ( endIndex == -1 )
		{
			throw new RuntimeException( "No end for '" + lookFor + "'" );
		}
		String result =
			entireOntologyAsString.substring( startIndex, endIndex );
		if ( result.endsWith( "#" ) )
		{
			result = result.substring( 0, result.length() - 1 );
		}
		return result;
	}
	
	/**
	 * Performs the sync from the ontologies to a neo representation,
	 * using {@link Owl2Neo#getMetaManager()} and {@link Owl2Neo#getOwlModel()}.
	 * @param ontologies an array of files, containing ontologies
	 * (in RDF XML format), to sync.
	 * @param clearPreviousOntologies whether or not to remove previously stored
	 * ontologies (ontologies are stored in neo after each sync).
	 */
	public void syncOntologiesWithNeoRepresentation(
		boolean clearPreviousOntologies, File... ontologies )
	{
		Transaction tx = owl2Neo.getNeo().beginTx();
		try
		{
			if ( clearPreviousOntologies )
			{
				getStoredOntologies().clear();
			}
			Map<String, String> updates = checkForNewOntologies( ontologies );
			for ( String ontologyString : getStoredOntologies() )
			{
				// Well, this is a little awkwaard... but the ontology manager
				// will complain if it doesn't get a File as the source of
				// the ontology.
				File ontologyFile = storeToTempFile( ontologyString );
				this.putOwlFile( ontologyFile );
				IoUtil.safeDelete( ontologyFile );
			}
			this.syncOntologies();
			sendUpdateEvents( updates );
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
	
	private void sendUpdateEvents( Map<String, String> updates )
	{
		for ( Map.Entry<String, String> entry : updates.entrySet() )
		{
			for ( OntologyChangeHandler handler :
				owl2Neo.getOntologyChangeHandlers() )
			{
				String ontologyUri = entry.getKey();
				if ( entry.getValue().equals( "updated" ) )
				{
					handler.ontologyUpdated( ontologyUri );
				}
				else
				{
					handler.ontologyAdded( ontologyUri );
				}
			}
		}
	}
	
	private Map<String, String> checkForNewOntologies( File... ontologies )
	{
		Map<String, String> result = new HashMap<String, String>();
		Collection<String> storedOntologies = getStoredOntologies();
		Map<String, String> map = new HashMap<String, String>();
		for ( String ontologyString : getStoredOntologies() )
		{
			map.put( getOntologyUri( ontologyString ), ontologyString );
		}
		
		for ( File ontology : ontologies )
		{
			String ontologyString = readFile( ontology );
			String ontologyUri = getOntologyUri( ontologyString );
			if ( map.containsKey( ontologyUri ) )
			{
				if ( !ontologyString.equals( map.get( ontologyUri ) ) )
				{
					storedOntologies.remove( map.get( ontologyUri ) );
					storedOntologies.add( ontologyString );
					result.put( ontologyUri, "updated" );
				}
			}
			else
			{
				storedOntologies.add( ontologyString );
				result.put( ontologyUri, "added" );
			}
		}
		return result;
	}
	
	String[] getOntologyBaseUris()
	{
		return ontologyBaseUris.toArray(
			new String[ ontologyBaseUris.size() ] );
	}
	
	private void syncOntologies()
	{
		for ( OWLOntology ontology : owl.getOntologies() )
		{
			this.ontologyBaseUris.add( ontology.getURI().toString() );
			
			// Sync classes
			for ( OWLClass owlClass : ontology.getReferencedClasses() )
			{
				this.syncClass( ontology, owlClass );
			}
			
			// Sync properties
			for ( OWLProperty owlProperty :
				ontology.getReferencedDataProperties() )
			{
				this.syncProperty( ontology, owlProperty );
			}
			for ( OWLProperty owlProperty :
				ontology.getReferencedObjectProperties() )
			{
				this.syncProperty( ontology, owlProperty );
			}
		}
	}
	
	private OwlModel getOwlModel()
	{
		return this.owl2Neo.getOwlModel();
	}
	
	private void syncClass( OWLOntology ontology, OWLClass owlClass )
	{
		NodeType nodeType = getNodeType( owlClass.getURI().toString() );
		OwlClass modelClass = getOwlModel().getOwlClass( nodeType );
		TheVisitor visitor = new TheVisitor( modelClass );
		for ( OWLClassAxiom axiom : ontology.getAxioms( owlClass ) )
		{
			axiom.accept( visitor );
		}
	}

	private void syncProperty( OWLOntology ontology, OWLProperty owlProperty )
	{
		TheVisitor visitor = new TheVisitor(
			getModelProperty( owlProperty.getURI().toString() ) );
		if ( owlProperty instanceof OWLDataProperty )
		{
			addPropertyType( owlProperty.getURI().toString(),
				OWLXMLVocabulary.DATA_PROPERTY.getURI().toString() );
			for ( OWLDataPropertyAxiom axiom :
				ontology.getAxioms( ( OWLDataProperty ) owlProperty ) )
			{
				axiom.accept( visitor );
			}
		}
		else
		{
			addPropertyType( owlProperty.getURI().toString(),
				OWLXMLVocabulary.OBJECT_PROPERTY.getURI().toString() );
			for ( OWLObjectPropertyAxiom axiom :
				ontology.getAxioms( ( OWLObjectProperty ) owlProperty ) )
			{
				axiom.accept( visitor );
			}
		}
	}

	private NodeType getNodeType( String uri )
	{
		return this.owl2Neo.getNodeType( uri, true );
	}
	
	private OwlClass getModelClass( String uri )
	{
		return getOwlModel().getOwlClass( getNodeType( uri ) );
	}
	
	private OwlProperty getModelProperty( String uri )
	{
		return getOwlModel().getOwlProperty( uri );
	}

	private NodeType getNodeTypeForClassOrUnion( OWLDescription description )
	{
		if ( !description.isAnonymous() )
		{
			return getNodeType( description.asOWLClass().getURI().toString() );
		}
		return getOrCreateNodeTypeForUnion( getAllClasses( description ) );
	}
	
	private Set<String> getAllClasses( OWLDescription description )
	{
		Set<String> classes = new HashSet<String>();
		if ( description instanceof OWLClass &&
			!description.asOWLClass().isAnonymous() )
		{
			classes.add( description.asOWLClass().getURI().toString() );
		}
		else if ( description instanceof OWLObjectUnionOf )
		{
			OWLObjectUnionOf unionOf = ( OWLObjectUnionOf ) description;
			for ( OWLDescription d : unionOf.getOperands() )
			{
				if ( d.isAnonymous() )
				{
//					System.out.println( "Anonymous class (not supported!) " +
//						d );
					continue;
				}
				classes.add( d.asOWLClass().getURI().toString() );
			}
		}
		else if ( description instanceof OWLObjectIntersectionOf )
		{
			// TODO
			throw new UnsupportedOperationException(
				"'intersectionOf' not implemented" );
		}
		else if ( description instanceof OWLObjectComplementOf )
		{
			// TODO
			throw new UnsupportedOperationException(
				"'complementOf' not implemented" );
		}
		else if ( description instanceof OWLObjectOneOf )
		{
			// TODO
			OWLObjectOneOf oneOf = ( OWLObjectOneOf ) description;
			for ( OWLIndividual individual : oneOf.getIndividuals() )
			{
				classes.add( individual.getURI().toString() );
			}
		}
		else
		{
			throw new RuntimeException( "Unrecognized union type " +
				description.getClass().getName() );
		}
		return classes;
	}

	private NodeType getOrCreateNodeTypeForUnion( Set<String> classes )
	{
		NodeType result = null;
		for ( NodeType nodeType : this.owl2Neo.getMetaManager().getNodeTypes() )
		{
			if ( typeMatches( nodeType, classes ) )
			{
				result = nodeType;
				break;
			}
		}
		
		if ( result == null )
		{
			result = createUnionSuperType( classes );
		}
		owl2Neo.getOwlModel().getOwlClass( result );
		return result;
	}
	
	private boolean typeMatches( NodeType typeToTest, Set<String> classes )
	{
		Collection<NodeType> subTypes = typeToTest.directSubTypes();
		if ( subTypes.size() != classes.size() )
		{
			return false;
		}
		
		for ( NodeType subType : subTypes )
		{
			if ( !classes.contains( subType.getName() ) )
			{
				return false;
			}
		}
		return true;
	}

	private NodeType createUnionSuperType( Set<String> classes )
	{
		String name = "";
		for ( String className : classes )
		{
			if ( name.length() > 0 )
			{
				name += ",";
			}
			name += className;
		}
		name = "Union[" + name + "]";
		
		NodeType type = getNodeType( name );
		for ( String cls : classes )
		{
			type.directSubTypes().add( this.owl2Neo.getNodeType( cls, true ) );
		}
		return type;
	}

	private void addPropertyType( String propertyUri, String propertyType )
	{
		OwlProperty property = getModelProperty( propertyUri );
		Collection<String> types = ( Collection<String> )
			property.get( OwlModel.PROPERTY_TYPE, false );
		if ( types == null )
		{
			types = new HashSet<String>();
			property.set( OwlModel.PROPERTY_TYPE, types );
		}
		types.add( propertyType );
	}

	/**
	 * This visitor gets notifications about all the class/restriction/property
	 * attributes such as range, domain, cardinality a.s.o.
	 * 
	 * It contains many unimplemented <code>visit</code> methods which will
	 * get implemented on demand when the ontologies requires it. They are
	 * there right now, throwing {@link UnsupportedOperationException} for
	 * all methods which are currently unimplemented to give a signal that
	 * the ontologies contains some logic which this visitor currently doesn't
	 * handle (and hence should be implemented).
	 */
	private class TheVisitor extends OWLObjectVisitorAdapter
	{
		private OwlClass modelClass;
		private OwlProperty modelProperty;
		
		TheVisitor( OwlClass modelClass )
		{
			this.modelClass = modelClass;
		}
		
		TheVisitor( OwlProperty modelProperty )
		{
			this.modelProperty = modelProperty;
		}
		
		private void set( String propertyUri, String key, Object value )
		{
			Map<String, Object> map = new HashMap<String, Object>();
			map.put( key, value );
			set( propertyUri, map );
		}
		
		private void set( String propertyUri, Map<String, Object> values )
		{
			OwlProperty modelProperty = getModelProperty( propertyUri );
			AbstractOwlThingie owlThingie = modelProperty;
			if ( this.modelClass != null )
			{
				// This is for a class, hence it's a restriction
				owlThingie = modelClass.getModel().
					addRestriction( modelClass, modelProperty );
			}
			
			for ( String key : values.keySet() )
			{
				owlThingie.set( key, values.get( key ) );
			}
		}
		
		private void addDomain( String propertyUri, String domainClass )
		{
			OwlProperty property = getModelProperty( propertyUri );
			Collection<OwlClass> domain = ( Collection<OwlClass> )
				property.get( OwlModel.DOMAIN, false );
			if ( domain == null )
			{
				domain = new HashSet<OwlClass>();
				property.set( OwlModel.DOMAIN, domain );
			}
			domain.add( getModelClass( domainClass ) );
		}

		private String getPropertyUri( OWLDataPropertyExpression property )
		{
			return ( ( OWLDataProperty ) property ).getURI().toString();
		}
		
		private String getPropertyUri( OWLObjectPropertyExpression property )
		{
			return ( ( OWLObjectProperty ) property ).getURI().toString();
		}
		
		private void visitSubPropertyOf( String superPropertyUri,
			String subPropertyUri )
		{
			OwlProperty superProperty = getModelProperty( superPropertyUri );
			OwlProperty subProperty = getModelProperty( subPropertyUri );
			subProperty.supers().add( superProperty );
		}
		
		private void visitAllRestriction( String propertyUri,
			OWLDescription filler )
		{
			NodeType nodeType = getNodeTypeForClassOrUnion( filler );
			set( propertyUri, OwlModel.RANGE, nodeType );
		}
		
		private void visitPropertyDomain( String propertyUri,
			OWLDescription domain )
		{
			OwlProperty owlProperty =
				getModelProperty( propertyUri );
			for ( String className : getAllClasses( domain ) )
			{
				addDomain( owlProperty.getRdfAbout(), className );
			}
		}
		
		@Override
		public void visit( OWLClass value )
		{
			OwlClass superType = getModelClass( value.getURI().toString() );
			modelClass.supers().add( superType );
		}

		@Override
		public void visit( OWLDataMaxCardinalityRestriction value )
		{
			set( getPropertyUri( value.getProperty() ),
				OwlModel.MAX_CARDINALITY, value.getCardinality() );
		}

		@Override
		public void visit( OWLDataMinCardinalityRestriction value )
		{
			set( getPropertyUri( value.getProperty() ),
				OwlModel.MIN_CARDINALITY, value.getCardinality() );
		}
		
		@Override
		public void visit( OWLDataExactCardinalityRestriction value )
		{
			Map<String, Object> map = new HashMap<String, Object>();
			map.put( OwlModel.MAX_CARDINALITY, value.getCardinality() );
			map.put( OwlModel.MIN_CARDINALITY, value.getCardinality() );
			set( getPropertyUri( value.getProperty() ), map );
		}

		@Override
		public void visit( OWLDataAllRestriction value )
		{
			// TODO
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLDataSomeRestriction value )
		{
			// TODO
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLDataValueRestriction value )
		{
			// TODO
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void visit( OWLDataPropertyRangeAxiom value )
		{
			OWLDataRange range = value.getRange();
			String propertyUri = getPropertyUri( value.getProperty() );
			Object rangeValue = null;
			String datatypeUri = null;
			if ( range instanceof OWLDataType )
			{
				datatypeUri = ( ( OWLDataType ) range ).getURI().toString();
				rangeValue = RdfUtil.getDatatypeClass( datatypeUri );
			}
			else if ( range instanceof OWLDataOneOf )
			{
				OWLDataOneOf oneOf = ( OWLDataOneOf ) range;
				Set<Serializable> values = new HashSet<Serializable>();
				for ( OWLConstant aValue : oneOf.getValues() )
				{
					String stringValue = aValue.getLiteral();
					Serializable realValue = null;
					if ( aValue.isTyped() )
					{
						datatypeUri = aValue.asOWLTypedConstant().
							getDataType().getURI().toString();
						try
						{
							realValue = ( Serializable ) RdfUtil.getRealValue(
								datatypeUri, stringValue );
						}
						catch ( ParseException e )
						{
							throw new RuntimeException( e );
						}
					}
					else
					{
						realValue = stringValue;
					}
					values.add( realValue );
				}
				rangeValue = values;
			}
			// TODO complementOf, intersectionOf?
			else
			{
				throw new RuntimeException( "Unknown range type " + range +
					":" + range.getClass().getName() );
			}
			if ( datatypeUri != null )
			{
				set( propertyUri, OwlModel.DATA_TYPE, datatypeUri );
			}
			set( propertyUri, OwlModel.RANGE, rangeValue );
		}

		@Override
		public void visit( OWLFunctionalDataPropertyAxiom value )
		{
			addPropertyType( getPropertyUri( value.getProperty() ),
				OWLXMLVocabulary.FUNCTIONAL_DATA_PROPERTY.getURI().toString() );
		}

		@Override
		public void visit( OWLDataPropertyDomainAxiom value )
		{
			visitPropertyDomain( getPropertyUri( value.getProperty() ),
				value.getDomain() );
		}

		@Override
		public void visit( OWLDataSubPropertyAxiom value )
		{
			visitSubPropertyOf( getPropertyUri( value.getSuperProperty() ),
				getPropertyUri( value.getSubProperty() ) );
		}

		@Override
		public void visit( OWLObjectAllRestriction value )
		{
			visitAllRestriction( getPropertyUri( value.getProperty() ),
				value.getFiller() );
		}

		@Override
		public void visit( OWLObjectComplementOf value )
		{
			// TODO
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLObjectIntersectionOf value )
		{
			// TODO
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLObjectOneOf value )
		{
			// TODO
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLObjectSomeRestriction value )
		{
			visitAllRestriction( getPropertyUri( value.getProperty() ),
				value.getFiller() );
		}

		@Override
		public void visit( OWLObjectUnionOf value )
		{
			// TODO
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLObjectValueRestriction value )
		{
			// This represents an object
			try
			{
				URI uri = new URI( value.getValue().getURI().toString() );
				set( getPropertyUri( value.getProperty() ), OwlModel.RANGE,
					uri );
			}
			catch ( URISyntaxException e )
			{
				throw new RuntimeException( e );
			}
		}

		@Override
		public void visit( OWLObjectMaxCardinalityRestriction value )
		{
			set( getPropertyUri( value.getProperty() ),
				OwlModel.MAX_CARDINALITY, value.getCardinality() );
		}

		@Override
		public void visit( OWLObjectMinCardinalityRestriction value )
		{
			set( getPropertyUri( value.getProperty() ),
				OwlModel.MIN_CARDINALITY, value.getCardinality() );
		}

		@Override
		public void visit( OWLObjectExactCardinalityRestriction value )
		{
			Map<String, Object> map = new HashMap<String, Object>();
			map.put( OwlModel.MAX_CARDINALITY, value.getCardinality() );
			map.put( OwlModel.MIN_CARDINALITY, value.getCardinality() );
			set( getPropertyUri( value.getProperty() ), map );
		}

		@Override
		public void visit( OWLFunctionalObjectPropertyAxiom value )
		{
			addPropertyType( getPropertyUri( value.getProperty() ),
				OWLXMLVocabulary.FUNCTIONAL_OBJECT_PROPERTY.getURI().
				toString() );
		}

		@Override
		public void visit( OWLObjectPropertyDomainAxiom value )
		{
			visitPropertyDomain( getPropertyUri( value.getProperty() ),
				value.getDomain() );
		}

		@Override
		public void visit( OWLObjectPropertyRangeAxiom value )
		{
			OWLDescription range = value.getRange();
			NodeType rangeValue = getNodeTypeForClassOrUnion( range );
			String propertyUri = getPropertyUri( value.getProperty() );
			set( propertyUri, OwlModel.RANGE, rangeValue );
		}

		@Override
		public void visit( OWLObjectSelfRestriction value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLAntiSymmetricObjectPropertyAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLAxiomAnnotationAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLClassAssertionAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLConstantAnnotation value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLDataComplementOf value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLDataOneOf value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLDataProperty value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLDataPropertyAssertionAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLDataRangeFacetRestriction value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLDataRangeRestriction value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLDeclarationAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLDifferentIndividualsAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLDisjointClassesAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLDisjointDataPropertiesAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLDisjointObjectPropertiesAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLDisjointUnionAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLEntityAnnotationAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLEquivalentClassesAxiom value )
		{
			// TODO
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLEquivalentDataPropertiesAxiom value )
		{
//			for ( OWLDataPropertyExpression p : value.getProperties() )
//			{
//				OWLDataProperty property = ( OWLDataProperty ) p;
//				String uri = property.getURI().toString();
//				if ( !uri.equals( modelProperty.getRdfAbout() ) )
//				{
//					modelProperty.equivalentThing().set(
//						getModelProperty( uri ) );
//				}
//			}
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLEquivalentObjectPropertiesAxiom value )
		{
			// TODO
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLImportsDeclaration value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLInverseFunctionalObjectPropertyAxiom value )
		{
			// TODO
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLInverseObjectPropertiesAxiom value )
		{
			set( getPropertyUri( value.getFirstProperty() ),
				OwlModel.INVERSE_OF,
					getPropertyUri( value.getSecondProperty() ) );
		}

		@Override
		public void visit( OWLIrreflexiveObjectPropertyAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLNegativeDataPropertyAssertionAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLNegativeObjectPropertyAssertionAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLObjectAnnotation value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLObjectProperty value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLObjectPropertyAssertionAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLObjectPropertyChainSubPropertyAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLObjectPropertyInverse value )
		{
			// TODO
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLObjectSubPropertyAxiom value )
		{
			visitSubPropertyOf( getPropertyUri( value.getSuperProperty() ),
				getPropertyUri( value.getSubProperty() ) );
		}

		@Override
		public void visit( OWLOntologyAnnotationAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLReflexiveObjectPropertyAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLSameIndividualsAxiom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLSubClassAxiom value )
		{
			value.getSuperClass().accept( this );
		}

		@Override
		public void visit( OWLSymmetricObjectPropertyAxiom value )
		{
			addPropertyType( getPropertyUri( value.getProperty() ),
				OWLXMLVocabulary.SYMMETRIC_OBJECT_PROPERTY.getURI().
				toString() );
		}

		@Override
		public void visit( OWLTransitiveObjectPropertyAxiom value )
		{
			addPropertyType( getPropertyUri( value.getProperty() ),
				OWLXMLVocabulary.TRANSITIVE_OBJECT_PROPERTY.getURI().
				toString() );
		}

		@Override
		public void visit( OWLDataType value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLIndividual value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLOntology value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLTypedConstant value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( OWLUntypedConstant value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( SWRLAtomConstantObject value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( SWRLAtomDVariable value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( SWRLAtomIndividualObject value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( SWRLAtomIVariable value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( SWRLBuiltInAtom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( SWRLClassAtom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( SWRLDataRangeAtom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( SWRLDataValuedPropertyAtom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( SWRLDifferentFromAtom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( SWRLObjectPropertyAtom value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( SWRLRule value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void visit( SWRLSameAsAtom value )
		{
			throw new UnsupportedOperationException();
		}
	}
}
