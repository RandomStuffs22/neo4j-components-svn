package org.neo4j.owl2neo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.neo4j.api.core.Transaction;
import org.neo4j.meta.NodeType;

/**
 * A class representing an <owl:Class> object in an ontology. An
 * {@link OwlClass} may have one or more {@link OwlRestriction} instances
 * for different properties ({@link #restrictions(OwlProperty)}) as well as
 * its own properties.
 */
public class OwlClass extends AbstractOwlThingie
{
	private NodeType nodeType;
	private Map<OwlProperty, Collection<OwlRestriction>> restrictions =
		new HashMap<OwlProperty, Collection<OwlRestriction>>();
	private Collection<AbstractOwlThingie> supers;
	
	OwlClass( Owl2Neo owl2Neo, OwlModel model, NodeType nodeType )
	{
		super( model, nodeType.getName() );
		this.nodeType = nodeType;
		this.supers = new SuperClassesCollection( owl2Neo, this );
	}
	
	/**
	 * @return a {@link SuperClassesCollection} which redirects its calls to
	 * the neometa model, making the changes persisten between {@link NodeType}
	 * instances.
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
	 * @return the {@link NodeType} which this class represents.
	 */
	public NodeType getNodeType()
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
		
		Transaction tx = Transaction.begin();
		try
		{
			boolean result = false;
			for ( AbstractOwlThingie superThingie : owlClass.supers() )
			{
				OwlClass superClass = ( OwlClass ) superThingie;
				NodeType superType = superClass.getNodeType();
				if ( this.equals( superClass ) ||
					superType.isSubTypeOf( getNodeType() ) )
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
