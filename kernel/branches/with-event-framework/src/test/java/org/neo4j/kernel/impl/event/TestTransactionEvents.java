package org.neo4j.kernel.impl.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.kernel.impl.AbstractNeo4jTestCase;

public class TestTransactionEvents extends AbstractNeo4jTestCase
{
    @Test
    public void testRegisterUnregisterHandlers()
    {
        commit();
        Object value1 = 10;
        Object value2 = 3.5D;
        DummyTransactionEventHandler<Integer> handler1 = new DummyTransactionEventHandler<Integer>(
                (Integer) value1 );
        DummyTransactionEventHandler<Double> handler2 = new DummyTransactionEventHandler<Double>(
                (Double) value2 );

        try
        {
            getGraphDb().unregisterTransactionEventHandler( handler1 );
            fail( "Shouldn't be able to do unregister on a "
                  + "unregistered handler" );
        }
        catch ( IllegalStateException e )
        { /* Good */
        }

        assertTrue( handler1 == getGraphDb().registerTransactionEventHandler(
                handler1 ) );
        assertTrue( handler1 == getGraphDb().registerTransactionEventHandler(
                handler1 ) );
        assertTrue( handler1 == getGraphDb().unregisterTransactionEventHandler(
                handler1 ) );

        try
        {
            getGraphDb().unregisterTransactionEventHandler( handler1 );
            fail( "Shouldn't be able to do unregister on a "
                  + "unregistered handler" );
        }
        catch ( IllegalStateException e )
        { /* Good */
        }

        assertTrue( handler1 == getGraphDb().registerTransactionEventHandler(
                handler1 ) );
        assertTrue( handler2 == getGraphDb().registerTransactionEventHandler(
                handler2 ) );
        assertTrue( handler1 == getGraphDb().unregisterTransactionEventHandler(
                handler1 ) );
        assertTrue( handler2 == getGraphDb().unregisterTransactionEventHandler(
                handler2 ) );

        getGraphDb().registerTransactionEventHandler( handler1 );
        newTransaction();
        commit();
        assertTrue( handler1.committed );
        assertEquals( value1, handler1.receivedState );
        assertNotNull( handler1.receivedTransactionData );
        getGraphDb().unregisterTransactionEventHandler( handler1 );
    }

    @Test
    public void shouldGetCorrectTransactionDataUponCommit()
    {
        // Create new data, nothing modified, just added/created
        TransactionDataSortOf expectedData = new TransactionDataSortOf();
        VerifyingTransactionEventHandler handler = new VerifyingTransactionEventHandler(
                expectedData );
        getGraphDb().registerTransactionEventHandler( handler );
        newTransaction();
        Node node1 = null, node2 = null, node3 = null;
        Relationship rel1 = null, rel2 = null;
        try
        {
            node1 = getGraphDb().createNode();
            expectedData.expectedCreatedNodes.add( node1 );

            node2 = getGraphDb().createNode();
            expectedData.expectedCreatedNodes.add( node2 );

            rel1 = node1.createRelationshipTo( node2, RelTypes.TXEVENT );
            expectedData.expectedCreatedRelationships.add( rel1 );

            node1.setProperty( "name", "Mattias" );
            expectedData.assignedProperty( node1, "name", "Mattias", null );

            node1.setProperty( "last name", "Persson" );
            expectedData.assignedProperty( node1, "last name", "Persson", null );

            node1.setProperty( "counter", 10 );
            expectedData.assignedProperty( node1, "counter", 10, null );

            rel1.setProperty( "description", "A description" );
            expectedData.assignedProperty( rel1, "description",
                    "A description", null );

            rel1.setProperty( "number", 4.5D );
            expectedData.assignedProperty( rel1, "number", 4.5D, null );

            node3 = getGraphDb().createNode();
            expectedData.expectedCreatedNodes.add( node3 );
            rel2 = node3.createRelationshipTo( node2, RelTypes.TXEVENT );
            expectedData.expectedCreatedRelationships.add( rel2 );

            node3.setProperty( "name", "Node 3" );

            newTransaction();
            assertTrue( handler.hasBeenCalled() );
        }
        finally
        {
            getGraphDb().unregisterTransactionEventHandler( handler );
        }

        // Use the above data and modify it, change properties, delete stuff
        expectedData = new TransactionDataSortOf();
        handler = new VerifyingTransactionEventHandler( expectedData );
        getGraphDb().registerTransactionEventHandler( handler );
        newTransaction();
        try
        {
            Node tempNode = getGraphDb().createNode();
            Relationship tempRel = tempNode.createRelationshipTo( node1,
                    RelTypes.TXEVENT );
            tempNode.setProperty( "something", "Some value" );
            tempRel.setProperty( "someproperty", 101010 );
            tempNode.removeProperty( "nothing" );

            node3.setProperty( "test", "hello" );
            node3.setProperty( "name", "No name" );
            node3.delete();
            expectedData.expectedDeletedNodes.add( node3 );

            node1.setProperty( "new name", "A name" );
            node1.setProperty( "new name", "A better name" );
            expectedData.assignedProperty( node1, "new name", "A better name",
                    null );
            node1.setProperty( "name", "Nothing" );
            node1.setProperty( "name", "Mattias Persson" );
            expectedData.assignedProperty( node1, "name", "Mattias Persson",
                    "Mattias" );
            node1.removeProperty( "counter" );
            expectedData.removedProperty( node1, "counter", null, 10 );
            node1.removeProperty( "last name" );
            node1.setProperty( "last name", "Hi" );
            expectedData.assignedProperty( node1, "last name", "Hi", "Persson" );

            rel2.delete();
            expectedData.expectedDeletedRelationships.add( rel2 );

            rel1.removeProperty( "number" );
            expectedData.removedProperty( rel1, "number", null, 4.5D );
            rel1.setProperty( "description", "Ignored" );
            rel1.setProperty( "description", "New" );
            expectedData.assignedProperty( rel1, "description", "New",
                    "A description" );

            tempRel.delete();
            tempNode.delete();
        }
        finally
        {
            getGraphDb().unregisterTransactionEventHandler( handler );
        }
    }

    private static enum RelTypes implements RelationshipType
    {
        TXEVENT
    }

    private static class DummyTransactionEventHandler<T> implements
            TransactionEventHandler<T>
    {
        private final T object;
        private TransactionData receivedTransactionData;
        private T receivedState;
        private Boolean committed;

        public DummyTransactionEventHandler( T object )
        {
            this.object = object;
        }

        public void afterCommit( TransactionData data, T state )
        {
            this.receivedState = state;
            this.committed = true;
        }

        public void afterRollback( TransactionData data, T state )
        {
            this.receivedState = state;
            this.committed = false;
        }

        public T beforeCommit( TransactionData data ) throws Exception
        {
            this.receivedTransactionData = data;
            return object;
        }
    }
}