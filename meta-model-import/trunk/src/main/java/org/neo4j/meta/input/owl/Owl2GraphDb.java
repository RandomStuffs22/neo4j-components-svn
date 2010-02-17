package org.neo4j.meta.input.owl;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.meta.model.MetaModel;

/**
 * Reads one or more ontologies and stores that data in Neo4j as nodes
 * and relationships so that other components can get information about
 * the data structure.
 * 
 * This is the entry class for this component, instantiate it
 * (with dependency injection) to use this component.
 */
public class Owl2GraphDb
{
//    /**
//     * Used to store that a property is a FUNCTIONAL, DATA, OBJECT or
//     * SYMMETRIC f.ex. Can be more than one, see
//     * {@link MetaModelThing#getAdditionalProperties(String)}.
//     */
//    public static final String PROPERTY_TYPE = "property_type";
//    
	private GraphDatabaseService graphDb;
	private MetaModel metaModel;
	private Owl2GraphDbUtil util;
	private UnsupportedConstructHandler unsupportedConstructHandler;
	private Collection<OntologyChangeHandler> changeHandlers =
		new HashSet<OntologyChangeHandler>();

	/**
	 * @param metaModel the {@link MetaModel} to use for storing
	 * information about the ontologies.
	 */
	public Owl2GraphDb( GraphDatabaseService graphDb, MetaModel metaModel,
	    UnsupportedConstructHandler unsupportedConstructHandler )
	{
		this.graphDb = graphDb;
		this.metaModel = metaModel;
		this.unsupportedConstructHandler = unsupportedConstructHandler;
		this.util = new Owl2GraphDbUtil( this );
	}
	
    public Owl2GraphDb( GraphDatabaseService graphDb, MetaModel metaModel )
    {
        this( graphDb, metaModel, new StrictUnsupportedConstructHandler() );
    }
    
	/**
	 * @return the {@link GraphDatabaseService} instance.
	 */
	public GraphDatabaseService getGraphDb()
	{
		return this.graphDb;
	}
	
	/**
	 * @return the {@link MetaModel} received in the constructor.
	 */
	public MetaModel getMetaModel()
	{
		return this.metaModel;
	}
	
	UnsupportedConstructHandler getUnsupportedConstructHandler()
	{
	    return this.unsupportedConstructHandler;
	}
	
	public void addOntologyChangeHandler( OntologyChangeHandler handler )
	{
		this.changeHandlers.add( handler );
	}
	
	public Iterable<OntologyChangeHandler> getOntologyChangeHandlers()
	{
		return this.changeHandlers;
	}

	public void syncOntologiesWithGraphDbRepresentation( File... ontologies )
	{
		syncOntologiesWithGraphDbRepresentation( false, ontologies );
	}
	
	/**
	 * Performs the synchronization of ontologies into neo4j representation.
	 * @param ontologies an array of files containing ontologies.
	 */
	public void syncOntologiesWithGraphDbRepresentation(
		boolean clearPreviousOntologies, File... ontologies )
	{
		util.syncOntologiesWithGraphDbRepresentation(
			clearPreviousOntologies, ontologies );
	}
	
	/**
	 * @return all the read ontologies base URIs. 
	 */
	public String[] getOntologyBaseUris()
	{
		return util.getOntologyBaseUris();
	}
}
