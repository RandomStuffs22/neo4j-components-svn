package org.neo4j.meta.input.owl;

public interface OntologyChangeHandler
{
	void ontologyUpdated( String ontologyURI );
	
	void ontologyAdded( String ontologyURI );
}
