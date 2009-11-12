package org.neo4j.owl2neo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.neo4j.api.core.Transaction;
import org.neo4j.meta.model.MetaModelClass;

/**
 * A class representing an <owl:Class> object in an ontology. An
 * {@link OwlClass} may have one or more {@link OwlRestriction} instances
 * for different properties ({@link #restrictions(OwlProperty)}) as well as
 * its own properties.
 */
public class OwlClass extends AbstractOwlThingie
{
	private Owl2Neo owl2Neo;
	private MetaModelClass nodeType;
	private Map<OwlProperty, Collection<OwlRestriction>> restrictions =
		new HashMap<OwlProperty, Collection<OwlRestriction>>();
	private Collection<AbstractOwlThingie> supers;
	
	OwlClass( Owl2Neo owl2Neo, OwlModel model, MetaModelClass nodeType )
	{
		super( model, nodeType.getName() );
		this.owl2Neo = owl2Neo;
		this.nodeType = nodeType;
		this.supers = new SuperClassesCollection( owl2Neo, this );
	}
	
	/**
	 * @return a {@link SuperClassesCollection} which redirects its calls to
	 * the neometa model, making the changes persistent between
	 * {@link MetaModelClass} instances.
	 */
	@Override
	public Collection<AbstractOwlThingie> supers()
	{
		return supers;
	}
	
	/**
	 * @param property the property to get/add restrictions for.
	 * @return a modifiable {@link Collection} of restrictions for this class
	 * and <code>property</code>. {@link OwlRestriction} instances may be added
	 * to it.
	 */
	public Collection<OwlRestriction> restrictions( OwlProperty property )
	{
		Collection<OwlRestriction> result = restrictions.get( property );
		if ( result == null )
		{
			result = new HashSet<OwlRestriction>();
			restrictions.put( property, result );
		}
		return result;
	}
	
	/**
	 * @return all properties which has a restriction on this OWL class.
	 */
	public OwlProperty[] getRestrictionProperties()
	{
		return restrictions.keySet().toArray(
			new OwlProperty[ restrictions.size() ] );
	}
	
	/**
	 * @return the {@link MetaModelClass} which this class represents.
	 */
	public MetaModelClass getNodeType()
	{
		return nodeType;
	}
	
	/**
	 * Works like {@link Class#isAssignableFrom(Class)}, but for
	 * {@link OwlClass} instances. Similar to
	 * {@link Class#isAssignableFrom(Class)}.
	 * @param owlClass the OWL class to query.
	 * @return {@code true} if this class is assignable from {@code owlClass},
	 * otherwise {@code false}.
	 */
	public boolean isAssignableFrom( OwlClass owlClass )
	{
		if ( this.equals( owlClass ) )
		{
			return true;
		}
		
		Transaction tx = owl2Neo.getNeo().beginTx();
		try
		{
			boolean result = false;
			for ( AbstractOwlThingie superThingie : owlClass.supers() )
			{
				OwlClass superClass = ( OwlClass ) superThingie;
				MetaModelClass superType = superClass.getNodeType();
				if ( this.equals( superClass ) ||
					superType.isSubOf( getNodeType() ) )
				{
					result = true;
					break;
				}
			}
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}
	
	@Override
	public String toString()
	{
		return "[" + getNodeType().getName() + "]";
	}
}
