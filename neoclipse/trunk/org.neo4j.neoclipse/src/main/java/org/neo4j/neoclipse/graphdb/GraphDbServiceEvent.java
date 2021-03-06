/*
 * Licensed to "Neo Technology," Network Engine for Objects in Lund AB
 * (http://neotechnology.com) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Neo Technology licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at (http://www.apache.org/licenses/LICENSE-2.0). Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.neo4j.neoclipse.graphdb;

import java.util.EventObject;

/**
 * This class represents a change in the neo service.
 * 
 * @author Peter H&auml;nsgen
 */
public class GraphDbServiceEvent extends EventObject
{
    private static final long serialVersionUID = 1L;
    /**
     * The status.
     */
    protected final GraphDbServiceStatus status;

    /**
     * The constructor.
     */
    public GraphDbServiceEvent( final GraphDbServiceManager source,
            final GraphDbServiceStatus status )
    {
        super( source );
        this.status = status;
    }
    
    /**
     * Returns the service status.
     */
    public GraphDbServiceStatus getStatus()
    {
        return status;
    }

    /**
     * Get info on the service mode.
     */
    public boolean isReadOnlyMode()
    {
        GraphDbServiceManager serviceMgr = (GraphDbServiceManager) source;
        return serviceMgr.isReadOnlyMode();
    }
}
