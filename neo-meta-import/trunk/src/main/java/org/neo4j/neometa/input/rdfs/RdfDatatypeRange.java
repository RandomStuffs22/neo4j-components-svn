package org.neo4j.neometa.input.rdfs;

import java.text.ParseException;

import org.neo4j.neometa.input.rdfs.RdfUtil.ValueConverter;
import org.neo4j.neometa.structure.MetaStructureProperty;
import org.neo4j.neometa.structure.SimpleStringPropertyRange;

public class RdfDatatypeRange extends SimpleStringPropertyRange
{
	private String datatype;
	
	public RdfDatatypeRange( String datatype )
	{
		this.datatype = datatype;
	}
	
	public RdfDatatypeRange()
	{
	}
	
	public String getRdfDatatype()
	{
		return this.datatype;
	}
	
	@Override
	protected String toStringRepresentation( MetaStructureProperty property )
	{
		return this.datatype;
	}
	
	@Override
	protected void fromStringRepresentation( MetaStructureProperty property,
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
}
