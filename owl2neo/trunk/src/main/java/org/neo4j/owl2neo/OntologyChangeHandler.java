package org.neo4j.owl2neo;

public interface OntologyChangeHandler
{
	void ontologyUpdated( String ontologyURI );
	
	void ontologyAdded( String ontologyURI );
}
