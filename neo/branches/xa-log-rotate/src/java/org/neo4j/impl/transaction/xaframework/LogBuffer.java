package org.neo4j.impl.transaction.xaframework;

import java.io.IOException;
import java.nio.channels.FileChannel;

public interface LogBuffer
{

    public LogBuffer put( byte b ) throws IOException;

    public LogBuffer putInt( int i ) throws IOException;

    public LogBuffer putLong( long l ) throws IOException;

    public LogBuffer put( byte[] bytes ) throws IOException;

    public LogBuffer put( char[] chars ) throws IOException;

    public void force() throws IOException;

    public long getFileChannelPosition() throws IOException;

    public FileChannel getFileChannel();

}