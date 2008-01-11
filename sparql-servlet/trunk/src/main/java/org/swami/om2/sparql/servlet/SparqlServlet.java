package org.swami.om2.sparql.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import name.levering.ryan.sparql.common.RdfBindingSet;
import name.levering.ryan.sparql.common.Variable;
import name.levering.ryan.sparql.model.Query;
import name.levering.ryan.sparql.model.SelectQuery;
import name.levering.ryan.sparql.parser.ParseException;
import name.levering.ryan.sparql.parser.SPARQLParser;
import org.neo4j.api.core.Transaction;
import org.swami.om2.neorepo.sparql.NeoBindingRow;
import org.swami.om2.neorepo.sparql.NeoRdfSource;
import org.swami.om2.neorepo.sparql.NeoVariable;
import org.swami.om2.neorepo.sparql.NeoVariable.VariableType;

public class SparqlServlet extends HttpServlet
{
	private boolean debugEnabled;
	
	public SparqlServlet()
	{
		this( false );
	}
	
	SparqlServlet( boolean debugEnabled )
	{
		this.debugEnabled = debugEnabled;
	}
	
	private boolean isDebugEnabled()
	{
		return this.debugEnabled;
	}
	
	@Override
	protected void doGet( HttpServletRequest request,
	    HttpServletResponse response ) throws IOException
	{		
		if ( this.isDebugEnabled() )
		{
			response.setContentType( "text/plain" );	
		}
		else
		{
			response.setContentType( "application/sparql-results+xml" );
		}
		
		PrintWriter out = response.getWriter();
		String encodedQuery = "";
		Transaction tx = null;
		try
        {
			encodedQuery = request.getParameter( "query" );
			tx = Transaction.begin();
			Query query = SPARQLParser.parse( new StringReader( 
			 	encodedQuery ) );
			if ( query instanceof SelectQuery )
			{
				long time = System.currentTimeMillis();
				RdfBindingSet result = ( ( SelectQuery ) query ).execute(
					new NeoRdfSource() );
				time = System.currentTimeMillis() - time;
				queryExecuted( encodedQuery, time );
				out.println( "<?xml version=\"1.0\"?>" );
				out.println( 
					"<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">"
					);
				out.println( "\t<head>" );
				List<NeoVariable> vars = result.getVariables();
				for ( Variable var : vars )
				{
					out.println( "\t\t<variable name=\"" + var.getName() + 
						"\"/>" );
				}
				out.println( "\t</head>" );
				out.println( 
					"\t<results distinct=\"false\" ordered=\"false\">" );
				Iterator iterator = result.iterator();
				while ( iterator.hasNext() )
				{
					out.println( "\t\t<result>" );
					NeoBindingRow bindingRow =
						( NeoBindingRow ) iterator.next();
					for ( NeoVariable var : vars ) 
					{
						out.println( "\t\t\t<binding name=\"" + 
							var.getName() + "\">" +
							getStartHeaderVarType( var.getVariableType() ) +
							bindingRow.getValue( var ) + 
							getEndHeaderVarType( var.getVariableType() ) +
							"</binding>" );
					}
					out.println( "\t\t</result>" );
				}
				out.println( "\t</results>" );
				out.println( "</sparql>" );
			}
			else
			{	// bad request, query parsing didn't resulted in  
				// select query type
				response.sendError( HttpServletResponse.SC_BAD_REQUEST, 
					"Query[" + encodedQuery + "] not a select query." );
			}
			tx.success();
        }
        catch ( ParseException pe )
        {
        	response.sendError( HttpServletResponse.SC_BAD_REQUEST,  
        		"Query[" + encodedQuery + "] unable to parse, " + pe );
	        // maybe internal logging should log pe stacktrace here 
        }
        // any other exception (runtime exception) will result in a 
        // internal server error (500) including exception stacktrace 
        // and log message
        catch ( Throwable t )
        {
        	throw new RuntimeException( "Internal error parsing query[" + 
        		encodedQuery + "]", t );
        }
        finally
        {
        	if ( tx != null )
        	{
        		tx.finish();
        	}
        }
		out.close();
	}
	
	protected void queryExecuted( String query, long timeSpent )
	{
	}

	private String getStartHeaderVarType( VariableType variableType )
    {
	    if ( variableType == VariableType.URI )
	    {
	    	return "<uri>";
	    }
	    if ( variableType == VariableType.LITERAL )
	    {
	    	return "<literal>";
	    }
	    return "";
    }

	private String getEndHeaderVarType( VariableType variableType )
    {
	    if ( variableType == VariableType.URI )
	    {
	    	return "</uri>";
	    }
	    if ( variableType == VariableType.LITERAL )
	    {
	    	return "</literal>";
	    }
	    return "";
    }
}
