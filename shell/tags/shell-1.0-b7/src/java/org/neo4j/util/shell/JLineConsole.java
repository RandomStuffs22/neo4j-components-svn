package org.neo4j.util.shell;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Implements the {@link Console} interface with jLine using reflections only.
 */
public class JLineConsole implements Console
{
	private ShellClient client;
	private Object consoleReader;
	private Object completor;
	
	public static JLineConsole newConsoleOrNullIfNotFound( ShellClient client )
	{
		try
		{
			Object consoleReader =
				Class.forName( "jline.ConsoleReader" ).newInstance();
			consoleReader.getClass().getMethod( "setBellEnabled",
				Boolean.TYPE ).invoke( consoleReader, false );
			return new JLineConsole( consoleReader, client );
		}
		catch ( Exception e )
		{
			return null;
		}
	}
	
	private JLineConsole( Object consoleReader, ShellClient client )
	{
		this.consoleReader = consoleReader;
		this.client = client;
	}
	
	public void format( String format, Object... args )
	{
		System.out.print( format );
	}
	
	private void grabAvailableCommands() throws Exception
	{
		Class<?> completorClass = Class.forName( "jline.Completor" );
		if ( completor != null )
		{
			consoleReader.getClass().getMethod( "removeCompletor",
				completorClass ).invoke( consoleReader, completor );
			completor = null;
		}
		
		ArrayList<String> commandList = new ArrayList<String>();
		for ( String command :
			client.getServer().getAllAvailableCommands() )
		{
			commandList.add( command );
		}
		Object commandsArray = Array.newInstance( String.class,
			commandList.size() );
		int counter = 0;
		for ( String command : commandList )
		{
			Array.set( commandsArray, counter++, command );
		}
		
		completor = Class.forName( "jline.SimpleCompletor" ).
		getConstructor( commandsArray.getClass() ).newInstance(
			commandsArray );
		consoleReader.getClass().getMethod( "addCompletor",
			completorClass ).invoke( consoleReader, completor );
	}
	
	public String readLine()
	{
		try
		{
			grabAvailableCommands();
			return ( String ) consoleReader.getClass().getMethod( "readLine" ).
				invoke( consoleReader );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}
}
