package org.neo4j.kernel.impl.core;

import java.util.Collection;

import org.neo4j.graphdb.event.ErrorState;
import org.neo4j.graphdb.event.KernelEventHandler;

public class KernelPanicEventGenerator
{
    private final Collection<KernelEventHandler> handlers;

    public KernelPanicEventGenerator( Collection<KernelEventHandler> handlers )
    {
        this.handlers = handlers;
    }
    
    public void generateEvent( ErrorState error )
    {
        for ( KernelEventHandler handler : handlers )
        {
            handler.kernelPanic( error );
        }
    }
}
