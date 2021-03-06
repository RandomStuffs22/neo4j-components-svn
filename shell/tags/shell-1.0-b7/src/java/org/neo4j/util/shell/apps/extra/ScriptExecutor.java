package org.neo4j.util.shell.apps.extra;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.neo4j.util.shell.AppCommandParser;
import org.neo4j.util.shell.Output;
import org.neo4j.util.shell.Session;
import org.neo4j.util.shell.ShellException;

/**
 * Executes groovy scripts purely via reflection
 */
public abstract class ScriptExecutor
{
	protected abstract String getPathKey();
	
	protected String getDefaultPaths()
	{
		return ".:src:src" + File.separator + "script";
	}

	/**
	 * Executes a groovy script (with arguments) defined in {@code line}. 
	 * @param line the line which defines the groovy script with arguments.
	 * @param session the {@link Session} to include as argument in groovy.
	 * @param out the {@link Output} to include as argument in groovy.
	 * @throws ShellException if the execution of a groovy script fails.
	 */
	public void execute( String line, Session session, Output out )
		throws ShellException
	{
		this.ensureDependenciesAreInClasspath();
		if ( line == null || line.trim().length() == 0 )
		{
			return;
		}
		
		List<String> pathList = this.getEnvPaths( session );
		String[] paths = pathList.toArray( new String[ pathList.size() ] );
		Object interpreter = this.newInterpreter( paths );
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "out", out );
		properties.put( "session", session );
		this.runScripts( interpreter, properties, line, paths );
	}

	private void runScripts( Object interpreter,
		Map<String, Object> properties, String line, String[] paths )
		throws ShellException
	{
		ArgReader reader = new ArgReader(
			AppCommandParser.tokenizeStringWithQuotes( line ) );
		while ( reader.hasNext() )
		{
			String arg = reader.next();
			if ( arg.startsWith( "--" ) )
			{
				String[] scriptArgs = getScriptArgs( reader );
				String scriptName = arg.substring( 2 );
				Map<String, Object> props =
					new HashMap<String, Object>( properties );
				props.put( "args", scriptArgs );
				this.runScript( interpreter, scriptName, props, paths );
			}
		}
	}
	
	protected abstract void runScript( Object interpreter,
		String scriptName, Map<String, Object> properties, String[] paths )
		throws ShellException;
	
	protected String findProperMessage( Throwable e )
	{
		String message = e.toString();
		if ( e.getCause() != null )
		{
			message = this.findProperMessage( e.getCause() );
		}
		return message;
	}
	
	protected String stackTraceAsString( Throwable e )
	{
		StringWriter writer = new StringWriter();
		PrintWriter printer = new PrintWriter( writer );
		e.printStackTrace( printer );
		printer.close();
		return writer.getBuffer().toString();
	}
	
	private String[] getScriptArgs( ArgReader reader )
	{
		reader.mark();
		try
		{
			ArrayList<String> list = new ArrayList<String>();
			while ( reader.hasNext() )
			{
				String arg = reader.next();
				if ( arg.startsWith( "--" ) )
				{
					break;
				}
				list.add( arg );
				reader.mark();
			}
			return list.toArray( new String[ list.size() ] );
		}
		finally
		{
			reader.flip();
		}
	}

	private List<String> getEnvPaths( Session session )
		throws ShellException
	{
		try
		{
			List<String> list = new ArrayList<String>();
			collectPaths( list, ( String ) session.get( getPathKey() ) );
			collectPaths( list, getDefaultPaths() );
			return list;
		}
		catch ( RemoteException e )
		{
			throw new ShellException( e );
		}
	}
	
	private void collectPaths( List<String> paths, String pathString )
	{
		if ( pathString != null && pathString.trim().length() > 0 )
		{
			for ( String path : pathString.split( ":" ) )
			{
				paths.add( path );
			}
		}
	}
	
	protected abstract Object newInterpreter( String[] paths )
		throws ShellException;
	
	protected abstract void ensureDependenciesAreInClasspath()
		throws ShellException;

	public static class ArgReader implements Iterator<String>
	{
		private static final int START_INDEX = -1;
		
		private int index = START_INDEX;
		private String[] args;
		private Integer mark;
		
		ArgReader( String[] args )
		{
			this.args = args;
		}
		
		public boolean hasNext()
		{
			return this.index + 1 < this.args.length;
		}
		
		public String next()
		{
			if ( !hasNext() )
			{
				throw new NoSuchElementException();
			}
			this.index++;
			return this.args[ this.index ];
		}
		
		/**
		 * Goes to the previous argument.
		 */
		public void previous()
		{
			this.index--;
			if ( this.index < START_INDEX )
			{
				this.index = START_INDEX;
			}
		}
		
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
		
		/**
		 * Marks the position so that a call to {@link #flip()} returns to that
		 * position.
		 */
		public void mark()
		{
			this.mark = this.index;
		}
		
		/**
		 * Flips back to the position defined in {@link #mark()}.
		 */
		public void flip()
		{
			if ( this.mark == null )
			{
				throw new IllegalStateException();
			}
			this.index = this.mark;
			this.mark = null;
		}
	}
}
