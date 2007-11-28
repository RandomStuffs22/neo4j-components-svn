package org.neo4j.util.shell.apps.extra;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
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

public class GshExecutor
{
	public static final String PATH_STRING = "GSH_PATH";
	public static final String BINDING_CLASS = "groovy.lang.Binding";
	public static final String ENGINE_CLASS = "groovy.util.GroovyScriptEngine";
	
	public void execute( String line, Session session, Output out )
		throws ShellException
	{
		this.ensureGroovyIsInClasspath();
		if ( line == null || line.trim().length() == 0 )
		{
			throw new ShellException( "Need to supply groovy scripts" );
		}
		
		List<String> pathList = this.getEnvPaths( session );
		Object groovyScriptEngine = this.newGroovyScriptEngine(
			pathList.toArray( new String[ pathList.size() ] ) );
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "out", new GshOutput( out ) );
		properties.put( "session", session );
		this.runGroovyScripts( groovyScriptEngine, properties, line );
	}

	private void runGroovyScripts( Object groovyScriptEngine,
		Map<String, Object> properties, String line ) throws ShellException
	{
		ArgReader reader = new ArgReader(
			AppCommandParser.tokenizeStringWithQuotes( line ) );
		HashMap<String, Object> hashMap =
			( HashMap<String, Object> ) properties;
		while ( reader.hasNext() )
		{
			String arg = reader.next();
			if ( arg.startsWith( "--" ) )
			{
				String[] scriptArgs = getScriptArgs( reader );
				String scriptName = arg.substring( 2 );
				Map<String, Object> props =
					( Map<String, Object> ) hashMap.clone();
				props.put( "args", scriptArgs );
				this.runGroovyScript( groovyScriptEngine, scriptName,
					this.newGroovyBinding( props ) );
			}
		}
	}
	
	private void runGroovyScript( Object groovyScriptEngine,
		String scriptName, Object groovyBinding ) throws ShellException
	{
		try
		{
			Method runMethod = groovyScriptEngine.getClass().getMethod(
				"run", String.class, groovyBinding.getClass() );
			runMethod.invoke( groovyScriptEngine, scriptName + ".groovy",
				groovyBinding );
		}
		catch ( Exception e )
		{
			// Don't pass the exception on because the client most certainly
			// doesn't have groovy in the classpath.
			throw new ShellException( "Groovy exception: " +
				this.findProperMessage( e ) );
		}
	}
	
	private String findProperMessage( Throwable e )
	{
		String message = e.getMessage();
		if ( e.getCause() != null )
		{
			message = this.findProperMessage( e.getCause() );
		}
		return message;
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
			collectPaths( list, ( String ) session.get( PATH_STRING ) );
			collectPaths( list, Gsh.DEFAULT_PATHS );
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
	
	private Object newGroovyBinding( Map<String, Object> properties )
		throws ShellException
	{
		try
		{
			Class cls = Class.forName( BINDING_CLASS );
			Object binding = cls.newInstance();
			Method setPropertyMethod =
				cls.getMethod( "setProperty", String.class, Object.class );
			for ( String key : properties.keySet() )
			{
				setPropertyMethod.invoke( binding, key, properties.get( key ) );
			}
			return binding;
		}
		catch ( Exception e )
		{
			throw new ShellException( "Invalid groovy classes", e );
		}
	}

	private Object newGroovyScriptEngine( String[] paths )
		throws ShellException
	{
		try
		{
			Class cls = Class.forName( ENGINE_CLASS );
			return cls.getConstructor( String[].class ).newInstance(
				new Object[] { paths } );
		}
		catch ( Exception e )
		{
			throw new ShellException( "Invalid groovy classes", e );
		}
	}
	
	private void ensureGroovyIsInClasspath() throws ShellException
	{
		try
		{
			Class.forName( BINDING_CLASS );
		}
		catch ( ClassNotFoundException e )
		{
			throw new ShellException( "Groovy couldn't be found", e );
		}
	}

	private static class ArgReader implements Iterator<String>
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
		
		public void mark()
		{
			this.mark = this.index;
		}
		
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
	
	public static class GshOutput implements Output
	{
		private Output source;
		
		GshOutput( Output output )
		{
			this.source = output;
		}

		public void print( Serializable object ) throws RemoteException
		{
			source.print( object );
		}
		
		public void println( Serializable object ) throws RemoteException
		{
			source.println( object );
		}
		
		public Appendable append( char c ) throws IOException
		{
			return source.append( c );
		}
		
		public Appendable append( CharSequence csq, int start, int end )
		    throws IOException
		{
			return source.append( csq, start, end );
		}
		
		public Appendable append( CharSequence csq ) throws IOException
		{
			return source.append( csq );
		}
		
		public void print( Object object ) throws RemoteException
		{
			source.print( object.toString() );
		}
		
		public void println() throws RemoteException
		{
			source.println();
		}
		
		public void println( Object object ) throws RemoteException
		{
			source.println( object.toString() );
		}
	}
}
