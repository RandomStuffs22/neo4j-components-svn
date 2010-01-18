package org.neo4j.meta.input.owl;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.meta.model.MetaModel;

/**
 * Owl2Neo reads one or more ontologies and stores that data in neo as nodes
 * and relationships so that other components can get information about
 * the data structure.
 * 
 * This is the entry class for the owl2neometa component, instantiate it
 * (with dependency injection) to use this component.
 */
public class Owl2Neo
{
//    /**
//     * Used to store that a property is a FUNCTIONAL, DATA, OBJECT or
//     * SYMMETRIC f.ex. Can be more than one, see
//     * {@link MetaModelThing#getAdditionalProperties(String)}.
//     */
//    public static final String PROPERTY_TYPE = "property_type";
//    
	private GraphDatabaseService neo;
	private MetaModel metaModel;
	private Owl2NeoUtil util;
	private UnsupportedConstructHandler unsupportedConstructHandler;
	private Collection<OntologyChangeHandler> changeHandlers =
		new HashSet<OntologyChangeHandler>();

	/**
	 * @param metaModel the {@link MetaModel} to use for storing
	 * information about the ontologies.
	 */
	public Owl2Neo( GraphDatabaseService neo, MetaModel metaModel,
	    UnsupportedConstructHandler unsupportedConstructHandler )
	{
		this.neo = neo;
		this.metaModel = metaModel;
		this.unsupportedConstructHandler = unsupportedConstructHandler;
		this.util = new Owl2NeoUtil( this );
	}
	
    public Owl2Neo( GraphDatabaseService neo, MetaModel metaModel )
    {
        this( neo, metaModel, new StrictUnsupportedConstructHandler() );
    }
    
	/**
	 * @return the {@link GraphDatabaseService} instance.
	 */
	public GraphDatabaseService getNeo()
	{
		return this.neo;
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

	public void syncOntologiesWithNeoRepresentation( File... ontologies )
	{
		syncOntologiesWithNeoRepresentation( false, ontologies );
	}
	
	/**
	 * Performs the synchronization of ontologies into neo representation.
	 * @param ontologies an array of files containing ontologies.
	 */
	public void syncOntologiesWithNeoRepresentation(
		boolean clearPreviousOntologies, File... ontologies )
	{
		util.syncOntologiesWithNeoRepresentation(
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