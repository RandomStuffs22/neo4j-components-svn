package org.neo4j.onlinebackup.ha;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.impl.nioneo.xa.NeoStoreXaDataSource;
import org.neo4j.onlinebackup.net.AcceptJob;
import org.neo4j.onlinebackup.net.Callback;
import org.neo4j.onlinebackup.net.Connection;
import org.neo4j.onlinebackup.net.HandleIncommingSlaveJob;
import org.neo4j.onlinebackup.net.HandleSlaveConnection;
import org.neo4j.onlinebackup.net.Job;
import org.neo4j.onlinebackup.net.JobEater;
import org.neo4j.onlinebackup.net.SocketException;

public class Master implements Callback
{
    private final EmbeddedGraphDatabase graphDb;
    private final NeoStoreXaDataSource xaDs;

    private final JobEater jobEater;
    private final ServerSocketChannel serverChannel;
    private final int port;
    
    private List<HandleSlaveConnection> slaveList = new 
        CopyOnWriteArrayList<HandleSlaveConnection>();
    
    public Master( String path, Map<String,String> params, int listenPort )
    {
        this.graphDb = new EmbeddedGraphDatabase( path, params );
        this.xaDs = (NeoStoreXaDataSource) graphDb.getConfig().getTxModule()
            .getXaDataSourceManager().getXaDataSource( "nioneodb" );
        xaDs.keepLogicalLogs( true );
        this.port = listenPort;
        try
        {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking( false );
            serverChannel.socket().bind( new InetSocketAddress( listenPort ) );
            
        }
        catch ( IOException e )
        {
            throw new SocketException( "Unable to bind at port[" + 
                listenPort + "]", e );
        }
        jobEater = new JobEater();
        jobEater.addJob( new AcceptJob( this, serverChannel ) );
        jobEater.start();
    }
    
    public GraphDatabaseService getGraphDbService()
    {
        return graphDb;
    }
    
    public int getPort()
    {
        return this.port;
    }
    
    public void jobExecuted( Job job )
    {
        if ( job instanceof AcceptJob )
        {
            // handle incomming slave
            AcceptJob acceptJob = (AcceptJob) job;
            if ( acceptJob.getAcceptedChannel() != null )
            {
                Connection connection = new Connection( 
                    ((AcceptJob) job).getAcceptedChannel() );
                jobEater.addJob( 
                    new HandleIncommingSlaveJob( connection, this ) );
            }
        }
        else if ( job instanceof HandleIncommingSlaveJob )
        {
            HandleSlaveConnection chainJob = 
                (HandleSlaveConnection) job.getChainJob();
            if ( chainJob != null )
            {
                slaveList.add( chainJob );
            }
            else
            {
                System.out.println( "null chain job" );
            }
        }
    }
    
    public void shutdown()
    {
        jobEater.stopEating();
        try
        {
            serverChannel.close();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
        graphDb.shutdown();
    }
    
    public long getIdentifier()
    {
        return xaDs.getRandomIdentifier();
    }
    
    public long getCreationTime()
    {
        return xaDs.getCreationTime();
    }
    
    public long getVersion()
    {
        return xaDs.getCurrentLogVersion();
    }
    
    public ReadableByteChannel getLog( long version ) throws IOException
    {
        return xaDs.getLogicalLog( version );
    }
    
    public long getLogLength( long version )
    {
        return xaDs.getLogicalLogLength( version );
    }
    
    public boolean hasLog( long version )
    {
        return xaDs.hasLogicalLog( version );
    }

    public void rotateLogAndPushToSlaves() throws IOException
    {
        long version = getVersion();
        xaDs.rotateLogicalLog();
        for ( HandleSlaveConnection slave : slaveList )
        {
            if ( !slave.offerLogToSlave( version ) )
            {
                System.out.println( "Failed to offer log to slave: " + slave );
            }
        }
    }
}