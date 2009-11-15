package org.neo4j.owl2neo;

import java.util.Collection;
import java.util.HashSet;
import org.neo4j.api.core.Transaction;
import org.neo4j.meta.model.MetaModelProperty;

/**
 * Represents one <owl:DatatypeProperty>, <owl:ObjectProperty> or
 * <owl:FunctionalProperty> in an ontology.
 */
public class OwlProperty extends AbstractOwlThingie
{
    private Owl2Neo owl2neo;
    private MetaModelProperty modelProperty;
	private Collection<AbstractOwlThingie> supers =
		new HashSet<AbstractOwlThingie>();
	
	OwlProperty( Owl2Neo owl2neo, OwlModel model, MetaModelProperty property )
	{
		super( model, property.getName() );
		this.owl2neo = owl2neo;
		this.modelProperty = property;
	}

	/**
	 * Returns all direct super properties to this property.
	 */
	@Override
	public Collection<AbstractOwlThingie> supers()
	{
		return supers;
	}
	
	/**
	 * Determines if <code>owlClass</code> is in this properties domain,
	 * the checks are recursive (super classes are checked also).
	 * @param owlClass the {@link OwlClass} to match.
	 * @return <code>true</code> if <code>owlClass</code> is in this property's
	 * domain (i.e. if this property is applicable to <code>owlClass</code>),
	 * otherwise <code>false</code>.
	 */
	public boolean isInDomain( OwlClass owlClass )
	{
		Transaction tx = getModel().getOwl2Neo().getNeo().beginTx();
		try
		{
			boolean result = false;
			Collection<OwlClass> domain = ( Collection<OwlClass> )
				get( OwlModel.DOMAIN );
			if ( domain != null )
			{
				for ( OwlClass domainClass : domain )
				{
					if ( domainClass.isAssignableFrom( owlClass ) )
					{
						result = true;
						break;
					}
				}
			}
			else
			{
				// No domain = applies to all?
				result = true;
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
		return "*" + getRdfAbout();
	}
}
