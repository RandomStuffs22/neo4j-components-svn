package org.neo4j.owl2neo;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility for converting to and from standard XML schema formats,
 * i.e. xml-schema-datatype-formatted-string --> java object and vice versa.
 */
public abstract class RdfUtil
{
	/**
	 * The standard XML schema base uri which is the base uri for all these
	 * data types.
	 */
	public static final String NS_XML_SCHEMA =
		"http://www.w3.org/2001/XMLSchema#";
	
	/**
	 * See http://www.w3.org/TR/owl-guide/ section
	 * "3.2.2 Properties and Datatypes" for more info.
	 */
	private static Map<String, ValueConverter> datatypePropertyTypes;
	static
	{
		datatypePropertyTypes = new HashMap<String, ValueConverter>();
		datatypePropertyTypes.put( NS_XML_SCHEMA + "string",
			new NormalConverter( String.class ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "normalizedString",
			new NormalConverter( String.class ) );
		
		datatypePropertyTypes.put( NS_XML_SCHEMA + "boolean",
			new NormalConverter( Boolean.class ) );
		
		datatypePropertyTypes.put( NS_XML_SCHEMA + "byte",
			new NormalConverter( Byte.class ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "unsignedByte",
			new NormalConverter( Byte.class ) );

		datatypePropertyTypes.put( NS_XML_SCHEMA + "short",
			new NormalConverter( Short.class ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "unsignedShort",
			new NormalConverter( Short.class ) );
		
		datatypePropertyTypes.put( NS_XML_SCHEMA + "int",
			new NormalConverter( Integer.class ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "unsignedInt",
			new NormalConverter( Integer.class ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "integer",
			new NormalConverter( Integer.class ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "nonNegativeInteger",
			new NormalConverter( Integer.class ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "positiveInteger",
			new NormalConverter( Integer.class ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "nonPositiveInteger",
			new NormalConverter( Integer.class ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "negativeInteger",
			new NormalConverter( Integer.class ) );
		
		datatypePropertyTypes.put( NS_XML_SCHEMA + "long",
			new NormalConverter( Long.class ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "unsignedLong",
			new NormalConverter( Long.class ) );
		
		datatypePropertyTypes.put( NS_XML_SCHEMA + "float",
			new NormalConverter( Float.class ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "double",
			new NormalConverter( Double.class ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "decimal",
			new NormalConverter( Double.class ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "gYear",
			new DateConverter( new SimpleDateFormat( "yyyy" ) ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "gMonth",
			new DateConverter( new SimpleDateFormat( "yyyy-MM" ) ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "gYearMonth",
			new DateConverter( new SimpleDateFormat( "yyyy-MM" ) ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "dateTime",
			new DateConverter( new XmlSchemaDateFormat() ) );
		datatypePropertyTypes.put( NS_XML_SCHEMA + "date",
			new DateConverter( new SimpleDateFormat( "yyyy-MM-dd" ) ) );
		
		// TODO Also support these: hexBinary, base64Binary,
		// gMonthDay, gDay, anyURI, token, language, NMTOKEN, Name,
		// NCName, time.
		
		datatypePropertyTypes =
			Collections.unmodifiableMap( datatypePropertyTypes );
	}

	/**
	 * Returns a {@link Class} corresponding to the <code>rdfDatatypeUri</code>
	 * URI.
	 * @param rdfDatatypeUri the RDF data type URI.
	 * @return the {@link Class} for <code>rdfDatatypeUri</code>.
	 * @throws RuntimeException if the type wasn't recognized.
	 */
	public static ValueConverter getDatatypeConverter( String rdfDatatypeUri )
	{
		ValueConverter converter = datatypePropertyTypes.get( rdfDatatypeUri );
		if ( converter == null )
		{
			throw new RuntimeException( "Unrecognized data type uri " +
				rdfDatatypeUri );
		}
		return converter;
	}
	
	/**
	 * Converts a string value into a java object depending on the data type.
	 * @param rdfDatatypeUri the data type which the value is supposed
	 * to be converted into.
	 * @param value the string value representing the value.
	 * @return the converted string as a java fundamental object,
	 * or {@link Date}.
	 * @throws ParseException if the string value has an invalid format.
	 */
	public static Object getRealValue( String rdfDatatypeUri, String value )
		throws ParseException
	{
		return getDatatypeConverter( rdfDatatypeUri ).convert( value );
	}
	
	/**
	 * Converts a java object value into a string, so that
	 * {@link #getStringValue(String, Object)} and
	 * {@link #getStringValue(String, Object)} are each others counter parts.
	 * @param rdfDatatypeUri the data type to format the value into.
	 * @param value the java object which is to be converted into a string.
	 * @return the converted value as a string.
	 */
	public static String getStringValue( String rdfDatatypeUri, Object value )
	{
		return getDatatypeConverter( rdfDatatypeUri ).convertToString( value );
	}
	
	/**
	 * @param rdfDatatypeUri the data type to get the java value class for.
	 * @return the java class representing a certain data type.
	 */
	public static Class<?> getDatatypeClass( String rdfDatatypeUri )
	{
		return getDatatypeConverter( rdfDatatypeUri ).getDatatype();
	}
	
	/**
	 * A converter of values to and from a String, depending on data type.
	 */
	public abstract static class ValueConverter
	{
		/**
		 * Converts a String value into its java object representation.
		 * @param value the String representation of the value.
		 * @return the String value represented as a java object.
		 * @throws ParseException if the String value has an invalid format.
		 */
		public abstract Object convert( String value ) throws ParseException;
		
		/**
		 * Converts a java object into its String representation.
		 * @param value the value to convert into a String.
		 * @return the value as a String.
		 */
		public abstract String convertToString( Object value );
		
		/**
		 * @return the java class to expect of objects from
		 * {@link #convert(String)}.
		 */
		public abstract Class<?> getDatatype();
	}
	
	private static class NormalConverter extends ValueConverter
	{
		private Class<?> cls;
		
		NormalConverter( Class<?> cls )
		{
			this.cls = cls;
		}
		
		@Override
		public Object convert( String value ) throws ParseException
		{
			try
			{
				return cls.getConstructor( String.class ).newInstance( value );
			}
			catch ( Exception e )
			{
				throw new RuntimeException( e );
			}
		}
		
		@Override
		public String convertToString( Object value )
		{
			return value.toString();
		}
		
		@Override
		public Class<?> getDatatype()
		{
			return cls;
		}
	}
	
	private static class DateConverter extends ValueConverter
	{
		private DateFormat format;
		
		DateConverter( DateFormat format )
		{
			this.format = format;
		}
		
		@Override
		public Object convert( String value ) throws ParseException
		{
			return format.parse( value );
		}
		
		@Override
		public String convertToString( Object value )
		{
			Date date = ( Date ) value;
			return format.format( date );
		}
		
		@Override
		public Class<?> getDatatype()
		{
			return Date.class;
		}
	}
}
