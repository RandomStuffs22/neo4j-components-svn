package org.neo4j.api.template.util;

import org.neo4j.api.core.RelationshipType;

public class NamedRelationshipType implements RelationshipType
{
    private final String name;

    public NamedRelationshipType(final String name)
    {
        if (name == null)
            throw new IllegalArgumentException("Name must not be null");
        this.name = name;
    }

    public String name()
    {
        return name;
    }
}
