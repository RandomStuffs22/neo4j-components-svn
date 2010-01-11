/*
 * Copyright (c) 2002-2009 "Neo Technology,"
 *     Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 * 
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.api.core;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * Can start and stop a shell server via reflection if the shell should happen
 * to be on the classpath, else it will gracefully say that it isn't there.
 */
class ShellService
{
    private final NeoService neo;
    private final Object shellServer;
    
    ShellService( NeoService neo, Map<String, Serializable> config )
        throws ShellNotAvailableException, RemoteException
    {
        this.neo = neo;
        if ( !shellDependencyAvailable() )
        {
            throw new ShellNotAvailableException();
        }
        this.shellServer = startShellServer( config );
    }
    
    private boolean shellDependencyAvailable()
    {
        try
        {
            Class.forName( "org.neo4j.shell.ShellServer" );
            return true;
        }
        catch ( Throwable t )
        {
            return false;
        }
    }
    
    private Object startShellServer( Map<String, Serializable> config )
        throws RemoteException
    {
        Integer port = ( Integer )
            getConfig( config, "port", "DEFAULT_PORT" );
        String name = ( String )
            getConfig( config, "name", "DEFAULT_NAME" );
        try
        {
            Class<?> shellServerClass =
                Class.forName( "org.neo4j.shell.neo.NeoShellServer" );
            Object shellServer = shellServerClass.getConstructor(
                NeoService.class ).newInstance( neo );
            shellServer.getClass().getMethod( "makeRemotelyAvailable",
                Integer.TYPE, String.class ).invoke( shellServer, port, name );
            return shellServer;
        }
        catch ( Exception e )
        {
            throw new RemoteException( "Couldn't start shell '" + name +
                "' at port " + port, e );
        }
    }
    
    private Serializable getConfig( Map<String, Serializable> config,
        String key, String defaultVariableName ) throws RemoteException
    {
        Serializable result = config.get( key );
        if ( result == null )
        {
            try
            {
                result = ( Serializable ) Class.forName(
                    "org.neo4j.shell.AbstractServer" ).
                        getDeclaredField( defaultVariableName ).get( null );
            }
            catch ( Exception e )
            {
                throw new RemoteException( "Default variable not found", e );
            }
        }
        return result;
    }

    public boolean shutdown() throws ShellNotAvailableException
    {
        try
        {
            shellServer.getClass().getMethod( "shutdown" ).invoke( shellServer );
            return true;
        }
        catch ( Exception e )
        {
            // TODO Really swallow this? Why not, who cares?
            return false;
        }
    }
    
    static class ShellNotAvailableException extends Exception
    {
        public ShellNotAvailableException()
        {
            super();
        }
    }
}