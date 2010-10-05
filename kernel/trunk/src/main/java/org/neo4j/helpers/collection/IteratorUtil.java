/**
 * Copyright (c) 2002-2010 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.helpers.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Contains common functionality regarding {@link Iterator}s and
 * {@link Iterable}s.
 */
public abstract class IteratorUtil
{
    /**
     * Returns the given iterator's first element or {@code null} if no
     * element found.
     * 
     * @param <T> the type of elements in {@code iterator}.
     * @param iterator the {@link Iterator} to get elements from.
     * @return the first element in the {@code iterator}, or {@code null} if no
     * element found.
     */
    public static <T> T firstOrNull( Iterator<T> iterator )
    {
        return iterator.hasNext() ? iterator.next() : null;
    }
    
    /**
     * Returns the given iterator's first element. If no element is found a
     * {@link NoSuchElementException} is thrown.
     * 
     * @param <T> the type of elements in {@code iterator}.
     * @param iterator the {@link Iterator} to get elements from.
     * @return the first element in the {@code iterator}.
     * @throws {@link NoSuchElementException} if no element found.
     */
    public static <T> T first( Iterator<T> iterator )
    {
        return assertNotNull( iterator, firstOrNull( iterator ) );
    }
    
    /**
     * Returns the given iterator's last element or {@code null} if no
     * element found.
     * 
     * @param <T> the type of elements in {@code iterator}.
     * @param iterator the {@link Iterator} to get elements from.
     * @return the last element in the {@code iterator}, or {@code null} if no
     * element found.
     */
    public static <T> T lastOrNull( Iterator<T> iterator )
    {
        T result = null;
        while ( iterator.hasNext() )
        {
            result = iterator.next();
        }
        return result;
    }
    
    /**
     * Returns the given iterator's last element. If no element is found a
     * {@link NoSuchElementException} is thrown.
     * 
     * @param <T> the type of elements in {@code iterator}.
     * @param iterator the {@link Iterator} to get elements from.
     * @return the last element in the {@code iterator}.
     * @throws {@link NoSuchElementException} if no element found.
     */
    public static <T> T last( Iterator<T> iterator )
    {
        return assertNotNull( iterator, lastOrNull( iterator ) );
    }
    
    /**
     * Returns the given iterator's single element or {@code null} if no
     * element found. If there is more than one element in the iterator a
     * {@link NoSuchElementException} will be thrown.
     * 
     * @param <T> the type of elements in {@code iterator}.
     * @param iterator the {@link Iterator} to get elements from.
     * @return the single element in {@code iterator}, or {@code null} if no
     * element found.
     * @throws {@link NoSuchElementException} if more than one element was
     * found.
     */
    public static <T> T singleOrNull( Iterator<T> iterator )
    {
        T result = iterator.hasNext() ? iterator.next() : null;
        if ( iterator.hasNext() )
        {
            throw new NoSuchElementException( "More than one element in " +
                iterator + ". First element is '" + result +
                "' and the element value is '" + iterator.next() + "'" );
        }
        return result;
    }
    
    /**
     * Returns the given iterator's single element. If there are no elements
     * or more than one element in the iterator a {@link NoSuchElementException}
     * will be thrown.
     * 
     * @param <T> the type of elements in {@code iterator}.
     * @param iterator the {@link Iterator} to get elements from.
     * @return the single element in the {@code iterator}.
     * @throws {@link NoSuchElementException} if there isn't exactly one
     * element.
     */
    public static <T> T single( Iterator<T> iterator )
    {
        return assertNotNull( iterator, singleOrNull( iterator ) );
    }

    private static <T> T assertNotNull( Iterator<T> iterator, T result )
    {
        if ( result == null )
        {
            throw new NoSuchElementException( "No element found in " + iterator );
        }
        return result;
    }
    
    /**
     * Returns the given iterable's first element or {@code null} if no
     * element found.
     * 
     * @param <T> the type of elements in {@code iterable}.
     * @param iterable the {@link Iterable} to get elements from.
     * @return the first element in the {@code iterable}, or {@code null} if no
     * element found.
     */
    public static <T> T firstOrNull( Iterable<T> iterable )
    {
        return firstOrNull( iterable.iterator() );
    }
    
    /**
     * Returns the given iterable's first element. If no element is found a
     * {@link NoSuchElementException} is thrown.
     * 
     * @param <T> the type of elements in {@code iterable}.
     * @param iterable the {@link Iterable} to get elements from.
     * @return the first element in the {@code iterable}.
     * @throws {@link NoSuchElementException} if no element found.
     */
    public static <T> T first( Iterable<T> iterable )
    {
        return first( iterable.iterator() );
    }
    
    /**
     * Returns the given iterable's last element or {@code null} if no
     * element found.
     * 
     * @param <T> the type of elements in {@code iterable}.
     * @param iterable the {@link Iterable} to get elements from.
     * @return the last element in the {@code iterable}, or {@code null} if no
     * element found.
     */
    public static <T> T lastOrNull( Iterable<T> iterable )
    {
        return lastOrNull( iterable.iterator() );
    }
    
    /**
     * Returns the given iterable's last element. If no element is found a
     * {@link NoSuchElementException} is thrown.
     * 
     * @param <T> the type of elements in {@code iterable}.
     * @param iterable the {@link Iterable} to get elements from.
     * @return the last element in the {@code iterable}.
     * @throws {@link NoSuchElementException} if no element found.
     */
    public static <T> T last( Iterable<T> iterable )
    {
        return last( iterable.iterator() );
    }
    
    /**
     * Returns the given iterable's single element or {@code null} if no
     * element found. If there is more than one element in the iterable a
     * {@link NoSuchElementException} will be thrown.
     * 
     * @param <T> the type of elements in {@code iterable}.
     * @param iterable the {@link Iterable} to get elements from.
     * @return the single element in {@code iterable}, or {@code null} if no
     * element found.
     * @throws {@link NoSuchElementException} if more than one element was
     * found.
     */
    public static <T> T singleOrNull( Iterable<T> iterable )
    {
        return singleOrNull( iterable.iterator() );
    }
    
    /**
     * Returns the given iterable's single element. If there are no elements
     * or more than one element in the iterable a {@link NoSuchElementException}
     * will be thrown.
     * 
     * @param <T> the type of elements in {@code iterable}.
     * @param iterable the {@link Iterable} to get elements from.
     * @return the single element in the {@code iterable}.
     * @throws {@link NoSuchElementException} if there isn't exactly one
     * element.
     */
    public static <T> T single( Iterable<T> iterable )
    {
        return single( iterable.iterator() );
    }
    
    /**
     * Adds all the items in {@code iterator} to {@code collection}.
     * @param <C> the type of {@link Collection} to add to items to.
     * @param <T> the type of items in the collection and iterator.
     * @param iterator the {@link Iterator} to grab the items from.
     * @param collection the {@link Collection} to add the items to.
     * @return the {@code collection} which was passed in, now filled
     * with the items from {@code iterator}.
     */
    public static <C extends Collection<T>,T> C addToCollection( Iterator<T> iterator,
            C collection )
    {
        while ( iterator.hasNext() )
        {
            collection.add( iterator.next() );
        }
        return collection;
    }

    /**
     * Adds all the items in {@code iterator} to {@code collection}.
     * @param <C> the type of {@link Collection} to add to items to.
     * @param <T> the type of items in the collection and iterator.
     * @param iterable the {@link Iterator} to grab the items from.
     * @param collection the {@link Collection} to add the items to.
     * @return the {@code collection} which was passed in, now filled
     * with the items from {@code iterator}.
     */
    public static <C extends Collection<T>,T> C addToCollection( Iterable<T> iterable,
            C collection )
    {
        return addToCollection(iterable.iterator(), collection);
    }

    /**
     * Exposes {@code iterator} as an {@link Iterable}. It breaks the contract
     * of {@link Iterable} in that it returns the supplied iterator instance for
     * each call to {@code iterator()} on the returned {@link Iterable}
     * instance. This method mostly exists to make it easy to use an
     * {@link Iterator} in a for-loop.
     * 
     * @param <T> the type of items in the iterator.
     * @param iterator the iterator to expose as an {@link Iterable}.
     * @return the supplied iterator posing as an {@link Iterable}.
     */
    public static <T> Iterable<T> asIterable( final Iterator<T> iterator )
    {
        return new Iterable<T>()
        {
            public Iterator<T> iterator()
            {
                return iterator;
            }
        };
    }
    
    /**
     * Counts the number of items in the {@code iterator} by looping
     * through it.
     * @param <T> the type of items in the iterator.
     * @param iterator the {@link Iterator} to count items in.
     * @return the number of found in {@code iterator}.
     */
    public static <T> int count( Iterator<T> iterator )
    {
        int result = 0;
        while ( iterator.hasNext() )
        {
            iterator.next();
            result++;
        }
        return result;
    }

    /**
     * Counts the number of items in the {@code iterable} by looping through it.
     * 
     * @param <T> the type of items in the iterator.
     * @param iterable the {@link Iterable} to count items in.
     * @return the number of found in {@code iterator}.
     */
    public static <T> int count( Iterable<T> iterable )
    {
        return count( iterable.iterator() );
    }
}
