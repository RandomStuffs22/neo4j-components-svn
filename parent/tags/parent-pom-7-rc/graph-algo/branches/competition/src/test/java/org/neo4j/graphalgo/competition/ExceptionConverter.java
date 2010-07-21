package org.neo4j.graphalgo.competition;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * A converter that converts the result of a method invocation when a particular
 * exception is intercepted.
 * 
 * @param <EX> the type to convert from.
 */
public abstract class ExceptionConverter<EX extends Exception>
{
    private final Class<EX> exceptionType;

    /**
     * Create a converter from one exception type to another.
     * 
     * @param exception the exception type to catch and convert.
     */
    protected ExceptionConverter( Class<EX> exception )
    {
        if ( exception == null ) throw new NullPointerException();
        this.exceptionType = exception;
    }

    /**
     * Convert the exception.
     * 
     * @param cause the method that caused the exception.
     * @param exception the exception.
     * @return the converted result.
     * @throws Throwable if the result of the conversion is to throw an
     *             exception.
     */
    protected abstract Object convert( Method cause, EX exception )
            throws Throwable;

    /**
     * Crate a proxy instance of a class that delegates to the specified object
     * and converts the specified exception.
     * 
     * @param <T> the type to proxy.
     * @param type the type to proxy.
     * @param object the object to delegate to.
     * @return the proxy instance.
     */
    public final <T> T proxy( Class<T> type, final T object )
    {
        if ( object == null ) throw new NullPointerException();
        return type.cast( Proxy.newProxyInstance( type.getClassLoader(),
                new Class[] { type }, new InvocationHandler()
                {
                    public Object invoke( Object proxy, Method method,
                            Object[] args ) throws Throwable
                    {
                        try
                        {
                            return method.invoke( object, args );
                        }
                        catch ( IllegalArgumentException e )
                        {
                            throw new InvocationError( e );
                        }
                        catch ( IllegalAccessException e )
                        {
                            throw new InvocationError( e );
                        }
                        catch ( InvocationTargetException e )
                        {
                            Throwable ex = e.getTargetException();
                            if ( exceptionType.isInstance( ex ) )
                            {
                                return convert( method, exceptionType.cast( ex ) );
                            }
                            else
                            {
                                throw ex;
                            }
                        }
                    }
                } ) );
    }

    @SuppressWarnings( "serial" )
    private static class InvocationError extends Error
    {
        InvocationError( Exception cause )
        {
            super( cause );
        }
    }
}
