package org.neo4j.kernel.impl.event;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.event.PropertyEntry;
import org.neo4j.graphdb.event.TransactionData;

class TransactionDataSortOf
{
    final Set<Node> expectedCreatedNodes = new HashSet<Node>();
    final Set<Relationship> expectedCreatedRelationships = new HashSet<Relationship>();
    final Set<Node> expectedDeletedNodes = new HashSet<Node>();
    final Set<Relationship> expectedDeletedRelationships = new HashSet<Relationship>();
    final Map<Node, Map<String, PropertyEntryImpl<Node>>> expectedAssignedNodeProperties =
            new HashMap<Node, Map<String, PropertyEntryImpl<Node>>>();
    final Map<Relationship, Map<String, PropertyEntryImpl<Relationship>>> expectedAssignedRelationshipProperties =
            new HashMap<Relationship, Map<String, PropertyEntryImpl<Relationship>>>();
    final Map<Node, Map<String, PropertyEntryImpl<Node>>> expectedRemovedNodeProperties =
            new HashMap<Node, Map<String, PropertyEntryImpl<Node>>>();
    final Map<Relationship, Map<String, PropertyEntryImpl<Relationship>>> expectedRemovedRelationshipProperties =
            new HashMap<Relationship, Map<String, PropertyEntryImpl<Relationship>>>();
    
    void assignedProperty( Node node, String key, Object value, Object valueBeforeTx )
    {
        putInMap( this.expectedAssignedNodeProperties, node, key, value, valueBeforeTx );
    }
    
    void assignedProperty( Relationship rel, String key, Object value, Object valueBeforeTx )
    {
        putInMap( this.expectedAssignedRelationshipProperties, rel, key, value, valueBeforeTx );
    }
    
    void removedProperty( Node node, String key, Object value, Object valueBeforeTx )
    {
        putInMap( this.expectedRemovedNodeProperties, node, key, value, valueBeforeTx );
    }
    
    void removedProperty( Relationship rel, String key, Object value, Object valueBeforeTx )
    {
        putInMap( this.expectedRemovedRelationshipProperties, rel, key, value, valueBeforeTx );
    }
    
    <T extends PropertyContainer> void putInMap( Map<T, Map<String, PropertyEntryImpl<T>>> map,
            T entity, String key, Object value, Object valueBeforeTx )
    {
        Map<String, PropertyEntryImpl<T>> innerMap = map.get( entity );
        if ( innerMap == null )
        {
            innerMap = new HashMap<String, PropertyEntryImpl<T>>();
            map.put( entity, innerMap );
        }
        innerMap.put( key, new PropertyEntryImpl<T>( entity, key, value, valueBeforeTx ) );
    }
    
    void compareTo( TransactionData data )
    {
        for ( Node node : data.createdNodes() )
        {
//            System.out.println( "Created node:" + node );
            assertTrue( expectedCreatedNodes.remove( node ) );
        }
        assertTrue( expectedCreatedNodes.isEmpty() );
        
        for ( Relationship rel : data.createdRelationships() )
        {
//            System.out.println( "Created rel:" + rel );
            assertTrue( expectedCreatedRelationships.remove( rel ) );
        }
        assertTrue( expectedCreatedRelationships.isEmpty() );
        
        for ( Node node : data.deletedNodes() )
        {
//            System.out.println( "Deleted node:" + node );
            assertTrue( expectedDeletedNodes.remove( node ) );
        }
        assertTrue( expectedDeletedNodes.isEmpty() );
        
        for ( Relationship rel : data.deletedRelationships() )
        {
//            System.out.println( "Deleted rel:" + rel );
            assertTrue( expectedDeletedRelationships.remove( rel ) );
        }
        assertTrue( expectedDeletedRelationships.isEmpty() );
        
        for ( PropertyEntry<Node> entry : data.assignedNodeProperties() )
        {
//            System.out.println( "Assigned property:" + entry );
            checkAssigned( expectedAssignedNodeProperties, entry );
        }
        assertTrue( expectedAssignedNodeProperties.isEmpty() );

        for ( PropertyEntry<Relationship> entry : data.assignedRelationshipProperties() )
        {
//            System.out.println( "Assigned property:" + entry );
            checkAssigned( expectedAssignedRelationshipProperties, entry );
        }
        assertTrue( expectedAssignedRelationshipProperties.isEmpty() );

        for ( PropertyEntry<Node> entry : data.removedNodeProperties() )
        {
//            System.out.println( "Removed property:" + entry );
            checkRemoved( expectedRemovedNodeProperties, entry );
        }
        assertTrue( expectedRemovedNodeProperties.isEmpty() );

        for ( PropertyEntry<Relationship> entry : data.removedRelationshipProperties() )
        {
//            System.out.println( "Removed property:" + entry );
            checkRemoved( expectedRemovedRelationshipProperties, entry );
        }
        assertTrue( expectedRemovedRelationshipProperties.isEmpty() );
    }
    
    <T extends PropertyContainer> void checkAssigned(
            Map<T, Map<String, PropertyEntryImpl<T>>> map, PropertyEntry<T> entry )
    {
        fetchExpectedPropertyEntry( map, entry ).compareToAssigned( entry );
    }

    <T extends PropertyContainer> void checkRemoved(
            Map<T, Map<String, PropertyEntryImpl<T>>> map, PropertyEntry<T> entry )
    {
        fetchExpectedPropertyEntry( map, entry ).compareToRemoved( entry );
    }
    
    <T extends PropertyContainer> PropertyEntryImpl<T> fetchExpectedPropertyEntry(
            Map<T, Map<String, PropertyEntryImpl<T>>> map, PropertyEntry<T> entry )
    {
        Map<String, PropertyEntryImpl<T>> innerMap = map.get( entry.entity() );
        assertNotNull( innerMap );
        PropertyEntryImpl<T> expectedEntry = innerMap.remove( entry.key() );
        assertNotNull( expectedEntry );
        if ( innerMap.isEmpty() )
        {
            map.remove( entry.entity() );
        }
        return expectedEntry;
    }
}
