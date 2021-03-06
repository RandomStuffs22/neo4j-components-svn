package org.neo4j.meta.model;

import java.text.ParseException;

import org.neo4j.meta.model.RdfUtil.ValueConverter;

/**
 * Handles datatypes commonly used in RDF/XML Schema.
 */
public class RdfDatatypeRange extends SimpleStringPropertyRange
{
	private String datatype;
	
	/**
	 * @param datatype the datatype uri.
	 */
	public RdfDatatypeRange( String datatype )
	{
		this.datatype = datatype;
	}
	
	/**
	 * Used internally.
	 */
	public RdfDatatypeRange()
	{
	}
	
	/**
	 * @return the RDF datatype for this range.
	 */
	public String getRdfDatatype()
	{
		return this.datatype;
	}
	
	@Override
	protected String toStringRepresentation( MetaModelRestrictable owner )
	{
		return this.datatype;
	}
	
	@Override
	protected void fromStringRepresentation( MetaModelRestrictable owner,
		String stringRepresentation )
	{
		this.datatype = stringRepresentation;
	}
	
	private ValueConverter getConverter()
	{
		return RdfUtil.getDatatypeConverter( getRdfDatatype() );
	}

	@Override
	public Object rdfLiteralToJavaObject( String value ) throws ParseException
	{
		return getConverter().convert( value );
	}
	
	@Override
	public String javaObjectToRdfLiteral( Object value )
	{
		return getConverter().convertToString( value );
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getRdfDatatype() + "]";
	}
}
