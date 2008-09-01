package org.swami.om2.neorepo.sparql;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import name.levering.ryan.sparql.common.RdfBindingRow;
import name.levering.ryan.sparql.common.RdfBindingSet;
import name.levering.ryan.sparql.common.Variable;

import org.neo4j.util.matching.PatternElement;
import org.neo4j.util.matching.PatternMatch;
import org.openrdf.model.Value;

public class NeoBindingRow implements RdfBindingRow
{
	private NeoRdfBindingSet bindingSet;
	private PatternMatch match;
	
	NeoBindingRow( NeoRdfBindingSet bindingSet, PatternMatch match )
	{
		this.bindingSet = bindingSet;
		this.match = match;
	}
	
	public RdfBindingSet getParentSet()
	{
		return this.bindingSet;
	}
	
	private Object[] neoPropertyAsArray( Object neoPropertyValue )
	{
		if ( neoPropertyValue.getClass().isArray() )
		{
			int length = Array.getLength( neoPropertyValue );
			Object[] result = new Object[ length ];
			for ( int i = 0; i < length; i++ )
			{
				result[ i ] = Array.get( neoPropertyValue, i );
			}
			return result;
		}
		else
		{
			return new Object[] { neoPropertyValue };
		}
	}
	
	public Collection<Value> getValues( Variable variable )
	{
		Object rawValue = getRawValue( variable );
		Collection<Value> result = new ArrayList<Value>();
		if ( rawValue == null )
		{
			result.add( new NeoValue( "" ) );
		}
		else
		{
			for ( Object oneValue : neoPropertyAsArray( rawValue ) )
			{
				result.add( new NeoValue( oneValue ) );
			}
		}
		return result;
	}

	public Value getValue( Variable variable )
	{
		Object rawValue = getRawValue( variable );
		return rawValue == null ? new NeoValue( "" ) : new NeoValue( rawValue );
	}
	
	private Object getRawValue( Variable variable )
	{
		NeoVariable neoVariable = this.getNeoVariable( variable );
		for ( PatternElement element : this.match.getElements() )
		{
			if ( element.getPatternNode().getLabel().equals(
				neoVariable.getNode().getLabel() ) )
			{
				if ( element.getNode().hasProperty(
					neoVariable.getProperty() ) )
				{
					return element.getNode().getProperty(
						neoVariable.getProperty() );
				}
				// Value was optional so just break and return ""
				break;
			}
		}
		return null;
	}

	private NeoVariable getNeoVariable( Variable variable )
	{
		if ( variable instanceof NeoVariable )
		{
			return ( NeoVariable ) variable;
		}
		
		for ( NeoVariable neoVariable : this.bindingSet.getVariables() )
		{
			if ( neoVariable.getName().equals( variable.getName() ) )
			{
				return neoVariable;
			}
		}
		
		throw new RuntimeException( "NeoVariable not found." );
	}

	public List<Value> getValues()
	{
		List<Value> values = new LinkedList<Value>();
		
		for ( NeoVariable variable :
			( List<NeoVariable> ) this.getVariables() )
		{
			for ( PatternElement element : this.match.getElements() )
			{
				if ( variable.getNode().getLabel().equals(
					element.getPatternNode().getLabel() ) )
				{
					values.add( new NeoValue(
						element.getNode().getProperty(
							variable.getProperty() ) ) );
					break;
				}
			}
		}
		
		return values;
	}

	public List<? extends Variable> getVariables()
	{
		return this.bindingSet.getVariables();
	}
	
	static class NeoValue implements Value
	{
		Object value;
		
		NeoValue( Object value )
		{
			this.value = value;
		}

		@Override
		public String toString()
		{
			if ( value == null )
			{
				return "";
			}
			
			return value.toString();
		}
	}
}
