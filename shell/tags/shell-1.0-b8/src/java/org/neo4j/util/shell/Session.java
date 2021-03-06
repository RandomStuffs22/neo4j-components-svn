/*
 * Copyright (c) 2002-2008 "Neo Technology,"
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
package org.neo4j.util.shell;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * A session (or environment) for a shell client.
 */
public interface Session extends Remote
{
	/**
	 * Sets a session value.
	 * @param key the session key.
	 * @param value the value.
	 * @throws RemoteException RMI error.
	 */
	void set( String key, Serializable value ) throws RemoteException;
	
	/**
	 * @param key the key to get the session value for.
	 * @return the value for the {@code key}.
	 * @throws RemoteException RMI error.
	 */
	Serializable get( String key ) throws RemoteException;
	
	/**
	 * Removes a value from the session.
	 * @param key the session key to remove.
	 * @return the removed value, or null if none.
	 * @throws RemoteException RMI error.
	 */
	Serializable remove( String key ) throws RemoteException;
	
	/**
	 * @return all the available session keys.
	 * @throws RemoteException RMI error.
	 */
	String[] keys() throws RemoteException;
}
