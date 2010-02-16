package org.neo4j.meta.input.owl;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Some common I/O utils.
 */
public abstract class IoUtil
{
	public static void safeClose( Closeable c )
	{
		if ( c != null )
		{
			try
			{
				c.close();
			}
			catch ( IOException e )
			{
				// Ok, what to do?
			}
		}
	}
	
	public static void safeDelete( File file )
	{
		if ( file != null )
		{
			if ( !file.delete() )
			{
				file.deleteOnExit();
			}
		}
	}
}
