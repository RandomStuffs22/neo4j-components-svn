package org.neo4j.meta.input.owl;

import org.semanticweb.owl.model.OWLObject;

public interface UnsupportedConstructHandler
{
    void handle( OWLObject owlConstruct );
}
