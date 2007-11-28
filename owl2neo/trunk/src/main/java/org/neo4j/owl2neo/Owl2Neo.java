package org.neo4j.owl2neo;

import java.io.File;

import org.neo4j.api.core.Transaction;
import org.neo4j.meta.MetaManager;
import org.neo4j.meta.NodeType;

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
	private MetaManager metaManager;
	private OwlModel owlModel;
	private Owl2NeoUtil util;

	/**
	 * @param metaManager the {@link MetaManager} to use for storing
	 * information about the ontologies.
	 */
	public Owl2Neo( MetaManager metaManager )
	{
		this.metaManager = metaManager;
		this.owlModel = new OwlModel( this );
		this.util = new Owl2NeoUtil( this );
	}
	
	/**
	 * @return the {@link MetaManager} received in the constructor.
	 */
	public MetaManager getMetaManager()
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

	/**
	 * A utility for getting/creating a {@link NodeType} with a certain name.
	 * @param name the name of the {@link NodeType} to return/create.
	 * @param createIfNotExists whether or not to create the node type if
	 * it doesn't exist.
	 * @return the {@link NodeType} by the name {@code name}.
	 */
	public NodeType getNodeType( String name, boolean createIfNotExists )
	{
		Transaction tx = Transaction.begin();
		try
		{
			if ( getMetaManager().hasNodeTypeByName( name ) )
			{
				return getMetaManager().getNodeTypeByName( name );
			}
			else if ( !createIfNotExists )
			{
				throw new RuntimeException( "Node type not found '" +
					name + "'" );
			}
			
			NodeType nodeType = metaManager.createNodeType( name );
			tx.success();
			return nodeType;
		}
		finally
		{
			tx.finish();
		}
	}
	
	/**
	 * Performs the synchronization of ontologies into neo representation.
	 * @param ontologies an array of files containing ontologies.
	 */
	public void syncOntologiesWithNeoRepresentation( File... ontologies )
	{
		util.syncOntologiesWithNeoRepresentation( ontologies );
	}
	
	/**
	 * @return all the read ontologies base URIs. 
	 */
	public String[] getOntologyBaseUris()
	{
		return util.getOntologyBaseUris();
	}
}
