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
package org.neo4j.shell.neo.apps;

import java.lang.reflect.Array;
import java.rmi.RemoteException;
import java.util.regex.Pattern;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.NotFoundException;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;
import org.neo4j.shell.AbstractApp;
import org.neo4j.shell.AbstractClient;
import org.neo4j.shell.App;
import org.neo4j.shell.AppCommandParser;
import org.neo4j.shell.Output;
import org.neo4j.shell.Session;
import org.neo4j.shell.ShellException;
import org.neo4j.shell.neo.NeoShellServer;

/**
 * An implementation of {@link App} which has common methods and functionality
 * to use with neo.
 */
public abstract class NeoApp extends AbstractApp
{
    private static final String CURRENT_KEY = "CURRENT_DIR";
    
    /**
     * @param server the {@link NeoShellServer} to get the current
     * {@link NodeOrRelationship} from.
     * @param session the {@link Session} used by the client.
     * @return the current {@link NodeOrRelationship} the client stands on
     * at the moment.
     * @throws ShellException if some error occured.
     */
    public static NodeOrRelationship getCurrent( NeoShellServer server,
        Session session ) throws ShellException
    {
        String currentThing = ( String ) safeGet( session, CURRENT_KEY );
        NodeOrRelationship result = null;
        if ( currentThing == null )
        {
            result = NodeOrRelationship.wrap(
                server.getNeo().getReferenceNode() );
            setCurrent( session, result );
        }
        else
        {
            TypedId typedId = new TypedId( currentThing );
            result = getThingById( server, typedId );
        }
        return result;
    }
    
    protected NodeOrRelationship getCurrent( Session session )
        throws ShellException
    {
        return getCurrent( getNeoServer(), session );
    }
    
    protected static void setCurrent( Session session,
        NodeOrRelationship current )
    {
        safeSet( session, CURRENT_KEY, current.getTypedId().toString() );
    }
    
    protected void assertCurrentIsNode( Session session )
        throws ShellException
    {
        NodeOrRelationship current = getCurrent( session );
        if ( !current.isNode() )
        {
            throw new ShellException(
                "You must stand on a node to be able to do this" );
        }
    }
    
    protected NeoShellServer getNeoServer()
    {
        return ( NeoShellServer ) this.getServer();
    }

    protected RelationshipType getRelationshipType( String name )
    {
        return new NeoAppRelationshipType( name );
    }
    
    protected Direction getDirection( String direction ) throws ShellException
    {
        return getDirection( direction, Direction.OUTGOING );
    }

    protected Direction getDirection( String direction,
        Direction defaultDirection ) throws ShellException
    {
        return ( Direction ) parseEnum(
            Direction.class, direction, defaultDirection ); 
    }
    
    protected static NodeOrRelationship getThingById( NeoShellServer server,
        TypedId typedId ) throws ShellException
    {
        NodeOrRelationship result = null;
        if ( typedId.isNode() )
        {
            try
            {
                result = NodeOrRelationship.wrap(
                    server.getNeo().getNodeById( typedId.getId() ) );
            }
            catch ( NotFoundException e )
            {
                throw new ShellException( "Node " + typedId.getId() +
                    " not found" );
            }
        }
        else
        {
            try
            {
                result = NodeOrRelationship.wrap(
                    server.getNeo().getRelationshipById( typedId.getId() ) );
            }
            catch ( NotFoundException e )
            {
                throw new ShellException( "Relationship " + typedId.getId() +
                    " not found" );
            }
        }
        return result;
    }
    
    protected NodeOrRelationship getThingById( TypedId typedId )
        throws ShellException
    {
        return getThingById( getNeoServer(), typedId );
    }

    protected Node getNodeById( long id )
    {
        return this.getNeoServer().getNeo().getNodeById( id );
    }

    public final String execute( AppCommandParser parser, Session session,
        Output out ) throws ShellException
    {
        Transaction tx = getNeoServer().getNeo().beginTx();
        try
        {
            String result = this.exec( parser, session, out );
            tx.success();
            return result;
        }
        catch ( RemoteException e )
        {
            throw new ShellException( e );
        }
        finally
        {
            tx.finish();
        }
    }

    protected String directionAlternatives()
    {
        return "OUTGOING, INCOMING, o, i";
    }

    protected abstract String exec( AppCommandParser parser, Session session,
        Output out ) throws ShellException, RemoteException;

    protected String getDisplayNameForCurrent( Session session )
        throws ShellException
    {
        NodeOrRelationship current = getCurrent( session );
        return current.isNode() ? "(me)" : "<me>";
    }

    /**
     * @param server the {@link NeoShellServer} to run at.
     * @param session the {@link Session} used by the client.
     * @param thing the thing to get the name-representation for.
     * @return the display name for a {@link Node}.
     */
    public static String getDisplayName( NeoShellServer server,
        Session session, NodeOrRelationship thing )
    {
        if ( thing.isNode() )
        {
            return getDisplayName( server, session, thing.asNode() );
        }
        else
        {
            return getDisplayName( server, session, thing.asRelationship(),
                true );
        }
    }
    
    /**
     * @param server the {@link NeoShellServer} to run at.
     * @param session the {@link Session} used by the client.
     * @param typedId the id for the item to display.
     * @return a display string for the {@code typedId}.
     * @throws ShellException if an error occurs.
     */
    public static String getDisplayName( NeoShellServer server,
        Session session, TypedId typedId ) throws ShellException
    {
        return getDisplayName( server, session,
            getThingById( server, typedId ) );
    }

    /**
     * @param server the {@link NeoShellServer} to run at.
     * @param session the {@link Session} used by the client.
     * @param node the {@link Node} to get a display string for.
     * @return a display string for {@code node}.
     */
    public static String getDisplayName( NeoShellServer server,
        Session session, Node node )
    {
        StringBuffer result = new StringBuffer(
//            "(" + node.getId() + ")" );
            "(" + node.getId() );
        String title = findTitle( server, session, node );
        if ( title != null )
        {
//            result.append( " " + title );
            result.append( ", " + title );
        }
        result.append( ")" );
        return result.toString();
    }
    
    private static String findTitle( NeoShellServer server, Session session,
        Node node )
    {
        String keys = ( String ) safeGet( session,
            AbstractClient.TITLE_KEYS_KEY );
        if ( keys == null )
        {
            return null;
        }
        
        String[] titleKeys = keys.split( Pattern.quote( "," ) );
        Pattern[] patterns = new Pattern[ titleKeys.length ];
        for ( int i = 0; i < titleKeys.length; i++ )
        {
            patterns[ i ] = Pattern.compile( titleKeys[ i ] );
        }
        for ( Pattern pattern : patterns )
        {
            for ( String nodeKey : node.getPropertyKeys() )
            {
                if ( matches( pattern, nodeKey, false, false ) )
                {
                    return trimLength( session,
                        format( node.getProperty( nodeKey ), false ) );
                }
            }
        }
        return null;
    }

    private static String trimLength( Session session, String string )
    {
        String maxLengthString = ( String )
            safeGet( session, AbstractClient.TITLE_MAX_LENGTH );
        int maxLength = maxLengthString != null ?
            Integer.parseInt( maxLengthString ) : Integer.MAX_VALUE;
        if ( string.length() > maxLength )
        {
            string = string.substring( 0, maxLength ) + "...";
        }
        return string;
    }

    /**
     * @param server the {@link NeoShellServer} to run at.
     * @param session the {@link Session} used by the client.
     * @param relationship the {@link Relationship} to get a display name for.
     * @param verbose whether or not to include the relationship id as well.
     * @return a display string for the {@code relationship}.
     */
    public static String getDisplayName( NeoShellServer server,
        Session session, Relationship relationship, boolean verbose )
    {
        StringBuffer result = new StringBuffer( "<" );
        if ( verbose )
        {
            result.append( relationship.getId() + ", " );
        }
        result.append( relationship.getType().name() + ">" );
        return result.toString();
    }
    
    protected static String fixCaseSensitivity( String string,
        boolean caseInsensitive )
    {
        return caseInsensitive ? string.toLowerCase() : string;
    }
    
    protected static Pattern newPattern( String pattern,
        boolean caseInsensitive )
    {
        return pattern == null ? null : Pattern.compile(
            fixCaseSensitivity( pattern, caseInsensitive ) );
    }
    
    protected static boolean matches( Pattern patternOrNull, String value,
        boolean caseInsensitive, boolean loose )
    {
        if ( patternOrNull == null )
        {
            return true;
        }

        value = fixCaseSensitivity( value, caseInsensitive );
        return loose ?
            patternOrNull.matcher( value ).find() :
            patternOrNull.matcher( value ).matches();
    }
    
    protected static <T extends Enum<T>> Enum<T> parseEnum(
        Class<T> enumClass, String name, Enum<T> defaultValue  )
    {
        if ( name == null )
        {
            return defaultValue;
        }
        
        name = name.toLowerCase();
        for ( T enumConstant : enumClass.getEnumConstants() )
        {
            if ( enumConstant.name().equalsIgnoreCase( name ) )
            {
                return enumConstant;
            }
        }
        for ( T enumConstant : enumClass.getEnumConstants() )
        {
            if ( enumConstant.name().toLowerCase().startsWith( name ) )
            {
                return enumConstant;
            }
        }
        throw new IllegalArgumentException( "No '" + name + "' or '" +
            name + ".*' in " + enumClass );
    }
    
    protected static String frame( String string, boolean frame )
    {
        return frame ? "[" + string + "]" : string;
    }
    
    protected static String format( Object value, boolean includeFraming )
    {
        String result = null;
        if ( value.getClass().isArray() )
        {
            StringBuffer buffer = new StringBuffer();
            int length = Array.getLength( value );
            for ( int i = 0; i < length; i++ )
            {
                Object singleValue = Array.get( value, i );
                if ( i > 0 )
                {
                    buffer.append( "," );
                }
                buffer.append( frame( singleValue.toString(),
                    includeFraming ) );
            }
            result = buffer.toString();
        }
        else
        {
            result = frame( value.toString(), includeFraming );
        }
        return result;
    }
    
    private static class NeoAppRelationshipType implements RelationshipType
    {
        private String name;

        private NeoAppRelationshipType( String name )
        {
            this.name = name;
        }

        public String name()
        {
            return this.name;
        }

        @Override
        public String toString()
        {
            return name();
        }
    }
}