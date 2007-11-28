package org.neo4j.owl2neo;

import java.util.Collection;
import java.util.Collections;

/**
 * Represents one <owl:Restriction> for a class.
 */
public class OwlRestriction extends AbstractOwlThingie
{
	private static final Collection<AbstractOwlThingie> NO_SUPERS =
		Collections.emptyList();
	
	private OwlClass forClass;
	private OwlProperty onProperty;
	
	OwlRestriction( OwlModel model, OwlClass forClass,
		OwlProperty onProperty )
	{
		super( model, null );
		this.forClass = forClass;
		this.onProperty = onProperty;
	}

	/**
	 * @return an empty {@link Collection} since restrictions has no super
	 * objects.
	 */
	@Override
	public Collection<AbstractOwlThingie> supers()
	{
		return NO_SUPERS;
	}
	
	/**
	 * @return the {@link OwlProperty} this restriction regards. Represents
	 * the <owl:onProperty> statement. 
	 */
	public OwlProperty onProperty()
	{
		return onProperty;
	}
	
	/**
	 * @return the {@link OwlClass} this restriction regards.
	 */
	public OwlClass forClass()
	{
		return forClass;
	}
	
	@Override
	public String toString()
	{
		return ">" + forClass + ":" + onProperty + "<";
	}
}
