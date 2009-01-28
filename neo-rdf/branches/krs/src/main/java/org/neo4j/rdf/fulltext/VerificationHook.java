package org.neo4j.rdf.fulltext;

public interface VerificationHook
{
    public static enum Status
    {
        OK,
        NOT_LITERAL,
        WRONG_LITERAL,
        MISSING,
    }
    
    Status verify( long id, String predicate, Object literal );
}
