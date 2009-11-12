package org.neo4j.owl2neo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.neo4j.meta.model.MetaModelClass;
import org.neo4j.util.AbstractNeoSet;

/**
 * A utility {@link Collection} which redirects all calls to the neo meta model
 * {@link MetaManager} so that the actual hierarchy gets persistent, using the
 * {@link NodeType}s.
 */
public class SuperClassesCollection extends AbstractNeoSet<AbstractOwlThingie>
{
	private Owl2Neo owl2Neo;
	private OwlClass owlClass;
	
	/**
	 * @param owl2Neo the {@link Owl2Neo} to use for getting class
	 * hierarchy information.
	 * @param owlClass the {@link OwlClass} to get the super classes for.
	 */
	public SuperClassesCollection( Owl2Neo owl2Neo, OwlClass owlClass )
	{
		super( owl2Neo.getNeo() );
		this.owl2Neo = owl2Neo;
		this.owlClass = owlClass;
	}
	
	private Collection<MetaModelClass> supers()
	{
		return owlClass.getNodeType().getDirectSupers();
	}
	
	public boolean add( AbstractOwlThingie o )
	{
		return supers().add( ( ( OwlClass ) o ).getNodeType() );
	}

	public void clear()
	{
		supers().clear();
	}

	public boolean contains( Object o )
	{
		return supers().contains( ( ( OwlClass ) o ).getNodeType() );
	}

	public boolean isEmpty()
	{
		return supers().isEmpty();
	}

	public Iterator<AbstractOwlThingie> iterator()
	{
		return new SuperIterator();
	}

	public boolean remove( Object o )
	{
		return supers().remove( o );
	}

	public int size()
	{
		return supers().size();
	}
	
	public boolean retainAll( Collection<?> o )
	{
		Collection<MetaModelClass> nodeTypes =
		    new HashSet<MetaModelClass>();
		for ( Object item : o )
		{
			nodeTypes.add( owl2Neo.getNodeType(
				( ( OwlClass ) item ).getRdfAbout(), false ) );
		}
		return supers().retainAll( nodeTypes );
	}

	public Object[] toArray()
	{
		Collection<MetaModelClass> supers = supers();
		Object[] result = new Object[ supers.size() ];
		int i = 0;
		for ( MetaModelClass nodeType : supers )
		{
			result[ i++ ] = owlClass.getModel().getOwlClass( nodeType );
		}
		return result;
	}

	public <R> R[] toArray( R[] array )
	{
		Collection<MetaModelClass> supers = supers();
		int i = 0;
		for ( MetaModelClass nodeType : supers )
		{
			array[ i++ ] = ( R ) owlClass.getModel().getOwlClass( nodeType );
		}
		return array;
	}
	
	private class SuperIterator implements Iterator<AbstractOwlThingie>
	{
		private Iterator<MetaModelClass> sourceIterator;
		
		private SuperIterator()
		{
			sourceIterator = supers().iterator();
		}
		
		public boolean hasNext()
		{
			return sourceIterator.hasNext();
		}

		public AbstractOwlThingie next()
		{
			return owlClass.getModel().getOwlClass( sourceIterator.next() );
		}

		public void remove()
		{
			sourceIterator.remove();
		}
	}
}
