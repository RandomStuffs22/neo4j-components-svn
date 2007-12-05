package test.owl2neo;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.neo4j.owl2neo.RdfUtil;

/**
 * Tests date parsing.
 */
public class TestDateFormats extends TestCase
{
	/**
	 * Tests standard xml date formats.
	 */
	public void testStandardXmlDateTimeFormats()
	{
		Map<String, List<String>> dates = new HashMap<String, List<String>>();
		dates.put( RdfUtil.NS_XML_SCHEMA + "dateTime", Arrays.asList(
			"2007-09-14T20:43:10",
			"2007-09-14T20:43:10Z",
			"2007-09-14T20:43:10+02:00",
			"2007-09-14T20:43:10.3",
			"2007-09-14T20:43:10.3Z",
			"2007-09-14T20:43:10.3-01:00",
			"2007-09-14T20:43:10.42",
			"2007-09-14T20:43:10.42Z",
			"2007-09-14T20:43:10.42-01:00",
			"2007-09-14T20:43:10.102",
			"2007-09-14T20:43:10.102Z",
			"2007-09-14T20:43:10.102-01:00" ) );
		dates.put( RdfUtil.NS_XML_SCHEMA + "date", Arrays.asList(
			"2007-09-14" ) );
		
		try
		{
			for ( String format : dates.keySet() )
			{
				for ( String dateString : dates.get( format ) )
				{
					Object result = RdfUtil.getRealValue( format, dateString );
					assertNotNull( result );
					assertEquals( Date.class, result.getClass() );
					Date date = ( Date ) result;
					Calendar calendar = Calendar.getInstance();
					calendar.setTime( date );
					assertEquals( dateString, 2007,
						calendar.get( Calendar.YEAR ) );
					assertEquals( dateString, Calendar.SEPTEMBER,
						calendar.get( Calendar.MONTH ) );
					assertEquals( dateString, 14,
						calendar.get( Calendar.DAY_OF_MONTH ) );
				}
			}
		}
		catch ( ParseException e )
		{
			fail( "Unable to parse: " + e.toString() );
		}
	}
	
	public void testTimeFormat()
	{
		try
		{
			Date date = ( Date ) RdfUtil.getRealValue(
				RdfUtil.NS_XML_SCHEMA + "time", "12:00:03" );
			Calendar cal = Calendar.getInstance();
			cal.setTime( date );
			assertEquals( 12, cal.get( Calendar.HOUR_OF_DAY ) );
			assertEquals( 0, cal.get( Calendar.MINUTE ) );
			assertEquals( 3, cal.get( Calendar.SECOND ) );
			
			date = ( Date ) RdfUtil.getRealValue(
				RdfUtil.NS_XML_SCHEMA + "time", "20:32:12.146" );
			cal.setTime( date );
			assertEquals( 20, cal.get( Calendar.HOUR_OF_DAY ) );
			assertEquals( 32, cal.get( Calendar.MINUTE ) );
			assertEquals( 12, cal.get( Calendar.SECOND ) );
			assertEquals( 146, cal.get( Calendar.MILLISECOND ) );
		}
		catch ( ParseException e )
		{
			fail( "Unable to parse " + e.toString() );
		}
	}
}
