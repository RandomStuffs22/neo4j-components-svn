package org.neo4j.owl2neo;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Transaction;
import org.neo4j.meta.model.MetaModel;
import org.neo4j.meta.model.MetaModelClass;

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
	private NeoService neo;
	private MetaModel metaManager;
	private OwlModel owlModel;
	private Owl2NeoUtil util;
	private Collection<OntologyChangeHandler> changeHandlers =
		new HashSet<OntologyChangeHandler>();

	/**
	 * @param metaManager the {@link MetaModel} to use for storing
	 * information about the ontologies.
	 */
	public Owl2Neo( NeoService neo, MetaModel metaManager )
	{
		this.neo = neo;
		this.metaManager = metaManager;
		this.owlModel = new OwlModel( this );
		this.util = new Owl2NeoUtil( this );
	}
	
	/**
	 * @return the {@link NeoService} instance.
	 */
	public NeoService getNeo()
	{
		return this.neo;
	}
	
	/**
	 * @return the {@link MetaModel} received in the constructor.
	 */
	public MetaModel getMetaManager()
	{
		return this.metaManager;
	}
	
	/**
	 * @return this components {@link OwlModel} which has lots of information
	 * about the data structure.
	 */
	public OwlModel getOwlModel()
	{
		return this.owlModel;
	}
	
	public void addOntologyChangeHandler( OntologyChangeHandler handler )
	{
		this.changeHandlers.add( handler );
	}
	
	public Iterable<OntologyChangeHandler> getOntologyChangeHandlers()
	{
		return this.changeHandlers;
	}

	/**
	 * A utility for getting/creating a {@link MetaModelClass} with a
	 * certain name.
	 * @param name the name of the {@link MetaModelClass} to return/create.
	 * @param createIfNotExists whether or not to create the node type if
	 * it doesn't exist.
	 * @return the {@link MetaModelClass} by the name {@code name}.
	 */
	public MetaModelClass getNodeType( String name,
	    boolean createIfNotExists )
	{
		Transaction tx = getNeo().beginTx();
		try
		{
	        return metaManager.getGlobalNamespace().getMetaClass( name,
	            createIfNotExists );
		}
		finally
		{
			tx.finish();
		}
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
