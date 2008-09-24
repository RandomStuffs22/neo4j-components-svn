package org.neo4j.api.template.util;

public interface Mapper<S, D>
{
    D map(S source);
}
