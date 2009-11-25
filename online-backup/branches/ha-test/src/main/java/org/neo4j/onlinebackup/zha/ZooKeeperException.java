package org.neo4j.onlinebackup.zha;

import org.neo4j.onlinebackup.net.SocketException;

public class ZooKeeperException extends SocketException
{
    public ZooKeeperException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
