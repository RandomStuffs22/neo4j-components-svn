package org.swami.om2.sparql.servlet;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public class Main
{
	public static void main( String[] args ) throws Exception
	{
		start();
	}

	public static void start() throws Exception
	{
		Server server = new Server( 8080 );
		Context root = new Context( server, "/", Context.SESSIONS );
		root.addServlet( new ServletHolder( new SparqlServlet( true ) ), "/*" );
		server.start();
	}
}
