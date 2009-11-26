package org.neo4j.meta.input.owl;

import org.semanticweb.owl.model.OWLObject;

public class StrictUnsupportedConstructHandler implements
    UnsupportedConstructHandler
{
    public void handle( OWLObject owlConstruct )
    {
        throw new UnsupportedOperationException( "OWL construct " +
            owlConstruct + " (" + owlConstruct.getClass().getName() +
            ") not supported" );
    }
}
