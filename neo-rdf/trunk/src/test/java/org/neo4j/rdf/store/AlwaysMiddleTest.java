package org.neo4j.rdf.store;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Statement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.model.WildcardStatement;
import org.neo4j.rdf.store.representation.standard.AbstractUriBasedExecutor;

public class AlwaysMiddleTest extends StoreTestCase
{
    public void testWeirdCases() throws Exception
    {
        AlwaysMiddleStore store = new AlwaysMiddleStore( neo(), null );
        Uri resource = new Uri( "something" );
        Context context = new Context( resource.getUriAsString() );
//        store.addStatements( new CompleteStatement(
//            resource, resource, resource, new Context( resource.getUriAsString() ) ) );
//        for ( Statement s : store.getStatements( new WildcardStatement( resource,
//            new Wildcard( "p" ), new Wildcard( "o" ),
//            new Context( resource.getUriAsString() ), null ), false ) )
//        {
//            System.out.println( "" + s );
//        }

        Context[] contexts = new Context[] { context, null };
        store.addStatements( new CompleteStatement(
            resource, resource, resource, contexts ) );
        for ( Statement s : store.getStatements( new WildcardStatement( resource,
            new Wildcard( "p" ), new Wildcard( "o" ) ), false ) )
        {
            System.out.println( "" + s );
        }

        deleteEntireNodeSpace();
    }

    public void testSome() throws Exception
    {
        RdfStore store = new AlwaysMiddleStore( neo(), null );

        Uri emil = new Uri( "http://emil" );
        Uri johan = new Uri( "http://johan" );
        String nickEmil = "Emil";
        String nickEmpa = "Empa";
        Context c1 = new Context( "http://c1" );
        Context c2 = new Context( "http://c2" );

        CompleteStatement emilIsPerson = new CompleteStatement( emil,
            new Uri( AbstractUriBasedExecutor.RDF_TYPE_URI ), PERSON );
        CompleteStatement johanIsPerson = new CompleteStatement( johan,
            new Uri( AbstractUriBasedExecutor.RDF_TYPE_URI ), PERSON );

        CompleteStatement emilKnowsJohanC1 = new CompleteStatement( emil,
            KNOWS, johan, c1 );
        CompleteStatement emilKnowsJohanC2 = new CompleteStatement( emil,
            KNOWS, johan, c2 );
        CompleteStatement emilNickEmilC1 = new CompleteStatement( emil,
            NICKNAME, new Literal( nickEmil ), c1 );
        CompleteStatement emilNickEmpaC2 = new CompleteStatement( emil,
            NICKNAME, new Literal( nickEmpa ), c2 );
        CompleteStatement emilNickEmilC2 = new CompleteStatement( emil,
            NICKNAME, new Literal( nickEmil ), c2 );
        List<Statement> statements = this.addStatements( store,
            emilIsPerson,
            johanIsPerson,
            emilKnowsJohanC1,
            emilKnowsJohanC2,
            emilNickEmilC1,
            emilNickEmpaC2,
            emilNickEmilC2
            );

        debug( "S P O" );
        for ( Statement statement : store.getStatements( new WildcardStatement(
            emil, NICKNAME, new Literal( nickEmil ) ),
                false ) )
        {
            debug( statement.toString() );
        }

        debug( "S P ?O" );
        Iterable<Statement> nicknames = store.getStatements(
            new WildcardStatement( emil, NICKNAME,
                new Wildcard( "nickname" ) ), false );
        Set<String> emilsNicknames = new HashSet<String>(
            Arrays.asList( nickEmil, nickEmpa ) );
        for ( Statement statement : nicknames )
        {
            debug( statement.toString() );
//            assertTrue( emilsNicknames.remove( ( ( Literal )
//                statement.getObject() ).getValue() ) );
        }
//        assertTrue( emilsNicknames.isEmpty() );

        debug( "S ?P ?O" );
        for ( Statement statement : store.getStatements( new WildcardStatement(
            emil, new Wildcard( "predicate" ), new Wildcard( "value" ), c1 ),
                false ) )
        {
            debug( statement.toString() );
        }

        debug( "?S ?P L" );
        for ( Statement statement : store.getStatements( new WildcardStatement(
            new Wildcard( "subject" ), new Wildcard( "predicate" ),
            new Literal( nickEmil ) ), false ) )
        {
            debug( statement.toString() );
        }

        debug( "?S ?P O" );
        for ( Statement statement : store.getStatements( new WildcardStatement(
            new Wildcard( "subject" ), new Wildcard( "predicate" ),
            johan ), false ) )
        {
            debug( statement.toString() );
        }

        debug( "?S P L" );
        for ( Statement statement : store.getStatements( new WildcardStatement(
            new Wildcard( "subject" ), NICKNAME,
            new Literal( nickEmil ) ), false ) )
        {
            debug( statement.toString() );
        }

        debug( "?S P O" );
        for ( Statement statement : store.getStatements( new WildcardStatement(
            new Wildcard( "subject" ), KNOWS, johan, c1 ),
            false ) )
        {
            debug( statement.toString() );
        }

        removeStatements( store, statements, 1 );
        deleteEntireNodeSpace();
    }
}
