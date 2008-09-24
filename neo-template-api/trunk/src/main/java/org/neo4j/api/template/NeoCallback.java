package org.neo4j.api.template;

public interface NeoCallback
{
    void neo(final Status status, final Graph graph) throws Exception;


    public static interface Status
    {
        void mustRollback();

        void interimCommit();
    }


}
