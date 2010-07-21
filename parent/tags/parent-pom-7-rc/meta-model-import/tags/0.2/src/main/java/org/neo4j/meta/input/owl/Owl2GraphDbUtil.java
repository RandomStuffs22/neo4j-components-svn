package org.neo4j.meta.input.owl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.Writer;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.meta.model.ClassRange;
import org.neo4j.meta.model.DataRange;
import org.neo4j.meta.model.MetaModelClass;
import org.neo4j.meta.model.MetaModelObject;
import org.neo4j.meta.model.MetaModelProperty;
import org.neo4j.meta.model.MetaModelRestrictable;
import org.neo4j.meta.model.RdfDatatypeRange;
import org.neo4j.meta.model.ResourceRange;
import org.neo4j.util.GraphDatabaseUtil;
import org.neo4j.util.GraphDbStringSet;
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
import org.semanticweb.owl.model.OWLObject;
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
class Owl2GraphDbUtil
{
	private static enum RelTypes implements RelationshipType
	{
		REF_OWL_2_NEO_ONTOLOGIES,
		OWL_2_NEO_ONTOLOGY,
	}
	
	private OWLOntologyManager owl;
	private Owl2GraphDb owl2GraphDb;
	private Set<String> ontologyBaseUris = new HashSet<String>();
	
	Owl2GraphDbUtil( Owl2GraphDb owl2GraphDb )
	{
		this.owl2GraphDb = owl2GraphDb;
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
		return new GraphDatabaseUtil( owl2GraphDb.getGraphDb() ).getOrCreateSubReferenceNode(
			RelTypes.REF_OWL_2_NEO_ONTOLOGIES );
	}
	
	private Collection<String> getStoredOntologies()
	{
		return new GraphDbStringSet( owl2GraphDb.getGraphDb(), getReferenceNode(),
			RelTypes.OWL_2_NEO_ONTOLOGY ); 
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
		String lookFor = "xmlns";
		int startIndex = -1;
		for ( int tempIndex = 0; true; )
		{
	        tempIndex = entireOntologyAsString.indexOf( lookFor, tempIndex );
	        if ( tempIndex == -1 )
	        {
	            break;
	        }
	        else
	        {
	            char consecutiveChar = entireOntologyAsString.charAt(
	                tempIndex + lookFor.length() );
	            if ( consecutiveChar == ':' )
	            {
	                continue;
	            }
                startIndex = tempIndex + lookFor.length();
	            while ( " =".indexOf(
	                entireOntologyAsString.charAt( startIndex ) ) != -1 )
	            {
	                startIndex++;
	            }
	            break;
	        }
		}
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
	 * Performs the sync from the ontologies to a Neo4j representation,
	 * using {@link Owl2GraphDb#getMetaModel()}.
	 * @param ontologies an array of files, containing ontologies
	 * (in RDF XML format), to sync.
	 * @param clearPreviousOntologies whether or not to remove previously stored
	 * ontologies (ontologies are stored in Neo4j after each sync).
	 */
	public void syncOntologiesWithGraphDbRepresentation(
		boolean clearPreviousOntologies, File... ontologies )
	{
		Transaction tx = owl2GraphDb.getGraphDb().beginTx();
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
				owl2GraphDb.getOntologyChangeHandlers() )
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
	
	private void syncClass( OWLOntology ontology, OWLClass owlClass )
	{
	    System.out.println( "class " + owlClass.getURI().toString() );
		MetaModelClass modelClass =
		    getMetaClass( owlClass.getURI().toString() );
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

	private MetaModelClass getMetaClass( String uri )
	{
		return this.owl2GraphDb.getMetaModel().getGlobalNamespace().getMetaClass(
		    uri, true );
	}
	
	private MetaModelProperty getModelProperty( String uri )
	{
	    return this.owl2GraphDb.getMetaModel().getGlobalNamespace().getMetaProperty(
	        uri, true );
	}

	private MetaModelClass getNodeTypeForClassOrUnion(
	    OWLDescription description )
	{
		if ( !description.isAnonymous() )
		{
			return getMetaClass( description.asOWLClass().getURI().toString() );
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
		    handleUnsupported( description );
		}
		else if ( description instanceof OWLObjectComplementOf )
		{
            handleUnsupported( description );
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
            handleUnsupported( description );
		}
		return classes;
	}

	private void handleUnsupported( OWLObject construct )
    {
	    this.owl2GraphDb.getUnsupportedConstructHandler().handle( construct );
    }
    
    private MetaModelClass getOrCreateNodeTypeForUnion(
	    Set<String> classes )
	{
		MetaModelClass result = null;
		for ( MetaModelClass nodeType : this.owl2GraphDb
		    .getMetaModel().getGlobalNamespace().getMetaClasses() )
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
		return result;
	}
	
	private boolean typeMatches( MetaModelClass typeToTest,
	    Set<String> classes )
	{
		Collection<MetaModelClass> subTypes = typeToTest.getDirectSubs();
		if ( subTypes.size() != classes.size() )
		{
			return false;
		}
		
		for ( MetaModelClass subType : subTypes )
		{
			if ( !classes.contains( subType.getName() ) )
			{
				return false;
			}
		}
		return true;
	}

	private MetaModelClass createUnionSuperType( Set<String> classes )
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
		
		MetaModelClass type = getMetaClass( name );
		for ( String cls : classes )
		{
			type.getDirectSubs().add( getMetaClass( cls ) );
		}
		return type;
	}

	private void addPropertyType( String propertyUri, String propertyType )
	{
		MetaModelProperty property = getModelProperty( propertyUri );
		if ( propertyType.equals(
		    OWLXMLVocabulary.FUNCTIONAL_DATA_PROPERTY.getURI().toString() ) ||
		    propertyType.equals(
		    OWLXMLVocabulary.FUNCTIONAL_OBJECT_PROPERTY.getURI().toString() ) )
		{
		    property.setAdditionalProperty( MetaModelObject.KEY_FUNCTIONALITY,
		        "functional" );
		}
		else if ( propertyType.equals(
		    OWLXMLVocabulary.SYMMETRIC_OBJECT_PROPERTY.getURI().toString() ) )
		{
		    property.setAdditionalProperty( MetaModelObject.KEY_FUNCTIONALITY,
		        "symmetric" );
		}
        else if ( propertyType.equals(
            OWLXMLVocabulary.TRANSITIVE_OBJECT_PROPERTY.getURI().toString() ) )
        {
            property.setAdditionalProperty( MetaModelObject.KEY_FUNCTIONALITY,
                "transitive" );
        }
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
		private MetaModelClass modelClass;
		private MetaModelProperty modelProperty;
		
		TheVisitor( MetaModelClass modelClass )
		{
			this.modelClass = modelClass;
		}
		
		TheVisitor( MetaModelProperty modelProperty )
		{
			this.modelProperty = modelProperty;
		}
		
//		private void set( String propertyUri, String key, Object value )
//		{
//			set( propertyUri, Collections.singletonMap( key, value ) );
//		}
//		
//		private void set( String propertyUri, Map<String, Object> values )
//		{
//			MetaModelProperty modelProperty = getModelProperty( propertyUri );
//			MetaModelRestrictable restriction = modelProperty;
//			if ( this.modelClass != null )
//			{
//				// This is for a class, hence it's a restriction
//				restriction = modelClass.getRestriction( modelProperty, true );
//			}
//			
//			for ( String key : values.keySet() )
//			{
//			    // TODO
////				restriction.set( key, values.get( key ) );
//			}
//		}
		
		private MetaModelRestrictable getRestrictable( String propertyUri )
		{
            MetaModelProperty modelProperty = getModelProperty( propertyUri );
            MetaModelRestrictable restriction = modelProperty;
            if ( this.modelClass != null )
            {
                // This is for a class, hence it's a restriction
                restriction = modelClass.getRestriction( modelProperty, true );
            }
            return restriction;
		}
		
		private void addDomain( String propertyUri, String domainClass )
		{
			MetaModelProperty property = getModelProperty( propertyUri );
			MetaModelClass metaClass = getMetaClass( domainClass );
			metaClass.getDirectProperties().add( property );
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
			MetaModelProperty superProperty =
			    getModelProperty( superPropertyUri );
			MetaModelProperty subProperty = getModelProperty( subPropertyUri );
			subProperty.getDirectSupers().add( superProperty );
		}
		
		private void visitAllRestriction( String propertyUri,
			OWLDescription filler )
		{
			MetaModelClass metaClass = getNodeTypeForClassOrUnion( filler );
			getRestrictable( propertyUri ).setRange(
			    new ClassRange( metaClass ) );
		}
		
		private void visitPropertyDomain( String propertyUri,
			OWLDescription domain )
		{
			MetaModelProperty property = getModelProperty( propertyUri );
			for ( String className : getAllClasses( domain ) )
			{
				addDomain( property.getName(), className );
			}
		}
		
		@Override
		public void visit( OWLClass value )
		{
			MetaModelClass superType =
			    getMetaClass( value.getURI().toString() );
			modelClass.getDirectSupers().add( superType );
		}

		@Override
		public void visit( OWLDataMaxCardinalityRestriction value )
		{
		    getRestrictable( getPropertyUri( value.getProperty() ) )
		        .setMaxCardinality( value.getCardinality() );
		}

		@Override
		public void visit( OWLDataMinCardinalityRestriction value )
		{
            getRestrictable( getPropertyUri( value.getProperty() ) )
                .setMinCardinality( value.getCardinality() );
		}
		
		@Override
		public void visit( OWLDataExactCardinalityRestriction value )
		{
            MetaModelRestrictable restrictable =
                getRestrictable( getPropertyUri( value.getProperty() ) );
            restrictable.setMaxCardinality( value.getCardinality() );
            restrictable.setMinCardinality( value.getCardinality() );
		}

		@Override
		public void visit( OWLDataAllRestriction value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLDataSomeRestriction value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLDataValueRestriction value )
		{
            handleUnsupported( value );
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
			
            if ( rangeValue instanceof Collection<?> )
            {
                getRestrictable( propertyUri ).setRange(
                    new DataRange( datatypeUri, ( ( Collection<?> )
                        rangeValue ).toArray() ) );
            }
            else if ( datatypeUri != null )
			{
			    getRestrictable( propertyUri ).setRange(
			        new RdfDatatypeRange( datatypeUri ) );
			}
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
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLObjectIntersectionOf value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLObjectOneOf value )
		{
            handleUnsupported( value );
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
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLObjectValueRestriction value )
		{
			// This represents an object
		    getRestrictable( getPropertyUri( value.getProperty() ) )
		        .setRange( new ResourceRange(
		            value.getValue().getURI().toString() ) );
		}

		@Override
		public void visit( OWLObjectMaxCardinalityRestriction value )
		{
		    getRestrictable( getPropertyUri( value.getProperty() ) )
		        .setMaxCardinality( value.getCardinality() );
		}

		@Override
		public void visit( OWLObjectMinCardinalityRestriction value )
		{
            getRestrictable( getPropertyUri( value.getProperty() ) )
                .setMinCardinality( value.getCardinality() );
		}

		@Override
		public void visit( OWLObjectExactCardinalityRestriction value )
		{
		    MetaModelRestrictable restrictable =
		        getRestrictable( getPropertyUri( value.getProperty() ) );
		    restrictable.setMaxCardinality( value.getCardinality() );
            restrictable.setMinCardinality( value.getCardinality() );
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
			MetaModelClass rangeValue = getNodeTypeForClassOrUnion( range );
			String propertyUri = getPropertyUri( value.getProperty() );
			getRestrictable( propertyUri ).setRange(
			    new ClassRange( rangeValue ) );
		}

		@Override
		public void visit( OWLObjectSelfRestriction value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLAntiSymmetricObjectPropertyAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLAxiomAnnotationAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLClassAssertionAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLConstantAnnotation value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLDataComplementOf value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLDataOneOf value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLDataProperty value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLDataPropertyAssertionAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLDataRangeFacetRestriction value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLDataRangeRestriction value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLDeclarationAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLDifferentIndividualsAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLDisjointClassesAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLDisjointDataPropertiesAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLDisjointObjectPropertiesAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLDisjointUnionAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLEntityAnnotationAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLEquivalentClassesAxiom value )
		{
            handleUnsupported( value );
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
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLEquivalentObjectPropertiesAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLImportsDeclaration value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLInverseFunctionalObjectPropertyAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLInverseObjectPropertiesAxiom value )
		{
		    MetaModelProperty property1 =
		        getModelProperty( getPropertyUri( value.getFirstProperty() ) );
            MetaModelProperty property2 =
                getModelProperty( getPropertyUri( value.getSecondProperty() ) );
		    property1.setInverseOf( property2 );
		}

		@Override
		public void visit( OWLIrreflexiveObjectPropertyAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLNegativeDataPropertyAssertionAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLNegativeObjectPropertyAssertionAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLObjectAnnotation value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLObjectProperty value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLObjectPropertyAssertionAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLObjectPropertyChainSubPropertyAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLObjectPropertyInverse value )
		{
            handleUnsupported( value );
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
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLReflexiveObjectPropertyAxiom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLSameIndividualsAxiom value )
		{
            handleUnsupported( value );
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
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLIndividual value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLOntology value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLTypedConstant value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( OWLUntypedConstant value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( SWRLAtomConstantObject value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( SWRLAtomDVariable value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( SWRLAtomIndividualObject value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( SWRLAtomIVariable value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( SWRLBuiltInAtom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( SWRLClassAtom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( SWRLDataRangeAtom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( SWRLDataValuedPropertyAtom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( SWRLDifferentFromAtom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( SWRLObjectPropertyAtom value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( SWRLRule value )
		{
            handleUnsupported( value );
		}

		@Override
		public void visit( SWRLSameAsAtom value )
		{
            handleUnsupported( value );
		}
	}
}
