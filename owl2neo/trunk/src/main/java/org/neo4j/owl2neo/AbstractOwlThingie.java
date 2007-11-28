package org.neo4j.owl2neo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The abstract class for the {@link OwlModel} objects: {@link OwlClass},
 * {@link OwlProperty} and {@link OwlRestriction}. It contains hierarchial
 * methods as well as property querying/modifying methods.
 */
public abstract class AbstractOwlThingie
{
	private OwlModel model;
	private String rdfAbout;
	private Map<String, Object> properties = new HashMap<String, Object>();
	
	/**
	 * @param model the {@link OwlModel} to use.
	 * @param rdfAbout the uri of this object.
	 */
	public AbstractOwlThingie( OwlModel model, String rdfAbout )
	{
		this.model = model;
		this.rdfAbout = rdfAbout;
	}
	
	/**
	 * @return the associated {@link OwlModel} instance.
	 */
	public OwlModel getModel()
	{
		return model;
	}
	
	/**
	 * @return the URI representing this object.
	 */
	public String getRdfAbout()
	{
		return rdfAbout;
	}
	
	/**
	 * @return a modifiable {@link Collection} with this objects super objects.
	 */
	public abstract Collection<AbstractOwlThingie> supers();
	
	/**
	 * Sets a property for this object, always non-recursive.
	 * @param key the property key, f.ex. {@link OwlModel#MIN_CARDINALITY}
	 * @param value the property value, f.ex. 1 for
	 * {@link OwlModel#MIN_CARDINALITY}.
	 */
	public void set( String key, Object value )
	{
		this.properties.put( key, value );
	}
	
	/**
	 * Removes a property set with {@link #set(String, Object)}.
	 * @param key the property key to remove.
	 */
	public void remove( String key )
	{
		this.properties.remove( key );
	}
	
	protected Object getFlat( String key )
	{
		return properties.get( key );
	}
	
	/**
	 * Returns a property value, recursively.
	 * @param key the property key, f.ex. {@link OwlModel#DOMAIN}.
	 * @return the property value for <code>key</code> (recursively) or
	 * <code>null</code> if no value was found.
	 */
	public Object get( String key )
	{
		return get( key, true );
	}
	
	/**
	 * Returns a property value (optionally recursive).
	 * @param key the property key, f.ex. {@link OwlModel#PROPERTY_TYPE}.
	 * @param recursive wether to look recursively or not.
	 * @return the property value for <code>key</code> or
	 * <code>null</code> if no value was found.
	 */
	public Object get( String key, boolean recursive )
	{
		Object value = getFlat( key );
		if ( value != null )
		{
			return value;
		}
		if ( recursive )
		{
			for ( AbstractOwlThingie superThing : supers() )
			{
				value = superThing.get( key );
				if ( value != null )
				{
					return value;
				}
			}
		}
		return null;
	}
	
	/**
	 * @return all the property keys for all existing property values.
	 * It even looks recursively in the super objects for keys.
	 */
	public Iterable<String> keys()
	{
		Set<String> keys = new HashSet<String>();
		gatherKeys( keys );
		return keys;
	}
	
	private void gatherKeys( Set<String> keys )
	{
		keys.addAll( properties.keySet() );
		for ( AbstractOwlThingie superThing : supers() )
		{
			superThing.gatherKeys( keys );
		}
	}
	
	@Override
	public int hashCode()
	{
		if ( rdfAbout == null )
		{
			return super.hashCode();
		}
		else
		{
			return rdfAbout.hashCode();
		}
	}
	
	@Override
	public boolean equals( Object o )
	{
		if ( rdfAbout == null )
		{
			return super.equals( o );
		}
		else
		{
			if ( !o.getClass().equals( getClass() ) )
			{
				return false;
			}
			return ( ( AbstractOwlThingie ) o ).rdfAbout.equals( rdfAbout );
		}
	}
}
