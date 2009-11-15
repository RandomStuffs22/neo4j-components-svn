package org.neo4j.owl2neo;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.meta.model.MetaModelClass;
import org.neo4j.meta.model.MetaModelProperty;
import org.semanticweb.owl.vocab.OWLXMLVocabulary;

/**
 * A simple, more light-weight representation of the ontologies. The model
 * consists of {@link OwlClass} which represents an <owl:Class> entity,
 * {@link OwlProperty} which represents an <owl:DatatypeProperty> or an
 * <owl:ObjectProperty> and {@link OwlRestriction} which represents restrictions
 * on properties for classes with <owl:Restriction>.
 * 
 * The model is not persistent and will be re-read at startup.
 */
public class OwlModel
{
	/**
	 * Represents <owl:maxCardinality> / <owl:cardinality>.
	 * Value is an {@link Integer}.
	 */
	public static final String MIN_CARDINALITY = "min_cardinality";

	/**
	 * Represents <owl:minCardinality> / <owl:cardinality>
	 * Value is an {@link Integer}.
	 */
	public static final String MAX_CARDINALITY = "max_cardinality";
	
	/**
	 * Represents a range for a property. Value be one of:
	 * {@link Collection} when the range is an <owl:DataRange>.
	 * {@link MetaModelClass} when the range is a node type, (non-literal).
	 * {@link Class} when the range is a data type (literal).
	 * {@link URI} when the range is an instance of an object.
	 */
	public static final String RANGE = "range";
	
	/**
	 * Represents all the types of a property.
	 * Value is a {@link Collection}.
	 * Possible values in the collection are (the String version of the URIs):
	 * {@link OWLXMLVocabulary#DATA_PROPERTY}
	 * {@link OWLXMLVocabulary#OBJECT_PROPERTY},
	 * {@link OWLXMLVocabulary#FUNCTIONAL_DATA_PROPERTY},
	 * {@link OWLXMLVocabulary#FUNCTIONAL_OBJECT_PROPERTY},
	 * {@link OWLXMLVocabulary#SYMMETRIC_OBJECT_PROPERTY},
	 * {@link OWLXMLVocabulary#TRANSITIVE_OBJECT_PROPERTY}.
	 */
	public static final String PROPERTY_TYPE = "property_type";
	
	/**
	 * The <owl:domain> for a property. Value is a {@link Collection}.
	 */
	public static final String DOMAIN = "domain";
	
	/**
	 * Inverse of another model object of the same type.
	 * F.ex. an OwlProperty instance is the inverse of another OwlProperty
	 * instance, or an OwlClass instance is the inverse if another
	 * OwlClass instance.
	 */
	public static final String INVERSE_OF = "inverse_of";
	
	/**
	 * The data type if this is a datatypeLiteral, f.ex.
	 * http://www.w3.org/2001/XMLSchema#dateTime
	 */
	public static final String DATA_TYPE = "data_type";

	private Map<MetaModelClass, OwlClass> classes =
	    new HashMap<MetaModelClass, OwlClass>();
	private Map<MetaModelProperty, OwlProperty> properties =
		new HashMap<MetaModelProperty, OwlProperty>();
	private Owl2Neo owl2Neo;
	
	private boolean initialized;
	private Map<OwlClass, Set<OwlProperty>> propertiesPerClass =
		new HashMap<OwlClass, Set<OwlProperty>>();
	
	/**
	 * @param owl2Neo the underlying {@link Owl2Neo} to use as information
	 * source.
	 */
	public OwlModel( Owl2Neo owl2Neo )
	{
		this.owl2Neo = owl2Neo;
	}
	
	Owl2Neo getOwl2Neo()
	{
		return this.owl2Neo;
	}

	/**
	 * @param nodeType the {@link MetaModelClass} the returned {@link OwlClass}
	 * instance represents.
	 * @return the {@link OwlClass} instance representing <code>nodeType</code>.
	 */
	public OwlClass getOwlClass( MetaModelClass nodeType )
	{
		OwlClass owlClass = classes.get( nodeType );
		if ( owlClass == null )
		{
			owlClass = new OwlClass( owl2Neo, this, nodeType );
			classes.put( nodeType, owlClass );
		}
		return owlClass;
	}
	
	/**
	 * @param propertyUri the URI of the {@link OwlProperty} to look for.
	 * @return <code>true</code> if there exists an {@link OwlProperty}
	 * instance corresponding to the URI <code>propertyUri</code>.
	 */
	public boolean hasOwlProperty( String propertyUri )
	{
//		return properties.containsKey( propertyUri );
	    return owl2Neo.getMetaManager().getGlobalNamespace().getMetaProperty(
	        propertyUri, false ) != null;
	}
	
	public OwlProperty getOwlProperty( MetaModelProperty property )
	{
		OwlProperty owlProperty = properties.get( property );
		if ( owlProperty == null )
		{
			owlProperty = new OwlProperty( owl2Neo, this, property );
			properties.put( property, owlProperty );
		}
		return owlProperty;
	}
	
	/**
	 * @return all the property URIs which exists in this model.
	 */
	public OwlProperty[] getOwlPropertyUris()
	{
		return this.properties.keySet().toArray(
			new OwlProperty[ this.properties.size() ] );
	}
	
	/**
	 * The main way to look things up in the {@link OwlModel}. You can f.ex.
	 * ask for {@link #MAX_CARDINALITY} for {@link OwlProperty}
	 * <code>propertyUri</code> and <code>nodeTypes</code>.
	 * The method takes all class restrictions into consideration also,
	 * as well as property/class inheritage. This is how it looks:
	 * 
	 * 1. For each class (node type) in <code>nodeTypes</code> is an instance of
	 *    (recursively), check if there are any restriction and return that
	 *    restriction value.
	 * 2. If no restriction was found then see if the requested property exists
	 *    on the property itself (or its super properties).
	 * 3. If it wasn't found on the property either then return
	 *    <code>null</code>.
	 * 
	 * @param key the value key to look for, f.ex. {@link #MAX_CARDINALITY}.
	 * @param nodeTypes all the node types to get the {@link OwlClass}
	 * instances from.
	 * @return the property value for a given {@link OwlProperty} and
	 * its node types.
	 */
	public Object findPropertyValue( String propertyUri, String key,
		Iterable<MetaModelClass> nodeTypes )
	{
		OwlProperty owlProperty = getOwlProperty(
		    owl2Neo.getMetaManager().getGlobalNamespace().getMetaProperty(
		        propertyUri, false ) );
		Object result = null;
		for ( MetaModelClass nodeType : nodeTypes )
		{
			OwlClass owlClass = getOwlClass( nodeType );
			Object value = tryGetFromRestrictions( owlClass, owlProperty, key );
			if ( value != null )
			{
				result = value;
				break;
			}
		}
		if ( result == null )
		{
			result = owlProperty.get( key );
		}
		return result;
	}
	
	/**
	 * Returns all the properties which are associated with (and appliable to)
	 * {@code owlClass}.
	 * @param owlClass the OWL class to get properties for.
	 * @return a collection of properties associated with an {@link OwlClass}.
	 */
	public Collection<OwlProperty> getPropertiesRegardingClass(
		OwlClass owlClass )
	{
		ensureInitialized();
		return propertiesPerClass.get( owlClass );
	}
	
	/**
	 * Returns <code>true</code> if any class in <code>nodeTypes</code>
	 * is in <code>propertyUri</code>'s domain (recursively).  
	 * 
	 * @param nodeTypes all the {@link MetaModelClass}s.
	 * @return <code>true</code> if <code>propertyUri</code> is allowed in any
	 * of the classes in <code>nodeTypes</code>.
	 */
	public boolean propertyIsAllowedOnInstance( MetaModelProperty property,
		Iterable<MetaModelClass> nodeTypes )
	{
		ensureInitialized();
		if ( !hasOwlProperty( property.getName() ) )
		{
			return false;
		}
		
		OwlProperty owlProperty = getOwlProperty( property );
		for ( MetaModelClass nodeType : nodeTypes )
		{
			OwlClass owlClass = getOwlClass( nodeType );
			if ( getPropertiesRegardingClass(
				owlClass ).contains( owlProperty ) )
			{
				return true;
			}
		}
		return false;
	}

	private Object tryGetFromRestrictions( OwlClass owlClass,
		OwlProperty property, String key )
	{
		for ( OwlRestriction restriction : owlClass.restrictions( property ) )
		{
			Object value = restriction.get( key );
			if ( value != null )
			{
				return value;
			}
		}
		for ( AbstractOwlThingie superClass : owlClass.supers() )
		{
			Object value = tryGetFromRestrictions( ( OwlClass ) superClass,
				property, key );
			if ( value != null )
			{
				return value;
			}
		}
		return null;
	}
	
	/**
	 * Adds a {@link OwlRestriction} regarding <code>owlClass</code> and
	 * <code>owlProperty</code>. Also adds the returned restriction to
	 * the <code>owlClass</code>'s {@link OwlClass#restrictions(OwlProperty)}
	 * collection.
	 * 
	 * @param owlClass the {@link OwlClass} this restriction regards.
	 * @param owlProperty the {@link OwlProperty} this restriction regards.
	 * @return the created {@link OwlRestriction} instance.
	 */
	public OwlRestriction addRestriction( OwlClass owlClass,
		OwlProperty owlProperty )
	{
		OwlRestriction restriction = new OwlRestriction( this, owlClass,
			owlProperty );
		owlClass.restrictions( owlProperty ).add( restriction );
		return restriction;
	}
	
	private void ensureInitialized()
	{
		if ( initialized )
		{
			return;
		}
		
		for ( OwlClass owlClass :
			classes.values().toArray( new OwlClass[ 0 ] ) )
		{
			Set<OwlProperty> set = new HashSet<OwlProperty>();
			gatherProperties( set, owlClass );
			propertiesPerClass.put( owlClass,
				Collections.unmodifiableSet( set ) );
		}
		
		initialized = true;
	}

	private void gatherProperties( Set<OwlProperty> set, OwlClass owlClass )
	{
		for ( OwlProperty property : properties.values() )
		{
			if ( property.isInDomain( owlClass ) )
			{
				set.add( property );
			}
		}
		set.addAll( Arrays.asList( owlClass.getRestrictionProperties() ) );
	}
}
