/* Copyright 2007 Viktor Klang
 * viktor.klang@gmail.com
 * */
package org.neo4j.ext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.ReturnableEvaluator;
import org.neo4j.api.core.StopEvaluator;
import org.neo4j.api.core.Traverser;
import org.neo4j.api.core.Traverser.Order;
import org.neo4j.impl.traversal.TraverserFactory;

/**
 * NeoQuery is a way to fluently define your traversal strategies<br/>
 * 
 * Example:<CODE><PRE> Traverser t = NeoQuery.orderByDepth()
 *                				  .include({@link RelationshipType},{@link Direction})
 *                				  .untilEndOfNetwork()
 *                				  .traverse(node);</CODE></PRE>
 *  Tip: use a static import of this class to be less verbose.<br/>              
 * 
 * @author Viktor Klang
 *
 */
public class NeoQuery 
{
	//These two are cached array type holders, used for the List.toArray(T[])
	protected final static RelationshipType[] relationshipArrayType = new RelationshipType[0];
	protected final static Direction[] directionArrayType = new Direction[0];
	
	//Our members
	private Order traversalOrder;
	private StopEvaluator stopEvaluator;
	private ReturnableEvaluator returnableEvaluator;
	private List<RelationshipType> types;
	private List<Direction> directions;
	
	/**
	 * Creates a new NeoQuery with the specified order
	 * @param the desired Order
	 * @return a newly created NeoQuery
	 */
	public static NeoQuery orderBy(Order order)
	{
		return new NeoQuery(order);
	}
	
	/**
	 * This is a shorthand for <CODE>orderBy(Order.BREADTH_FIRST)</CODE><br/>
	 * @return a newly created NeoQuery
	 */
	public static NeoQuery orderByBreadth()
	{
		return orderBy(Order.BREADTH_FIRST);
	}
	
	/**
	 * This is a shorthand for <CODE>orderBy(Order.DEPTH_FIRST)</CODE><br/>
	 * @return a newly created NeoQuery
	 */
	public static NeoQuery byDepth()
	{
		return orderBy(Order.DEPTH_FIRST);
	}
	
	/**
	 * Creates a new NeoQuery instance
	 * @throws {@link IllegalArgumentException } - If traversalOrder is <CODE>null</CODE>
	 * @param traversalOrder - The desired order of traversal
	 */
	protected NeoQuery(Order traversalOrder){
		if(traversalOrder == null)
			throw new IllegalArgumentException("No Order specified!");
		
		//What we'd really want is a Map<RelationshipType,Direction>, but since it's not supported
		//by the underlying API, let's pretend we love List
		this.types = new ArrayList<RelationshipType>();
		this.directions = new ArrayList<Direction>();
		
		this.traversalOrder = traversalOrder;
	}
	
	/**
	 * Includes the specified type of relationship and in what direction it is to be included<br/>
	 * in the traversal
	 * @throws {@link IllegalArgumentException } - If either parameters are <CODE>null</CODE>
	 * @param type - The type of relationship to be traversed
	 * @param dir - The order of traversal of the specified relationship type
	 * @return this
	 */
	public NeoQuery include(RelationshipType type, Direction dir)
	{
		if(type == null)
			throw new IllegalArgumentException("TraverseSpecification.include does not allow a null RelationshipType");
		
		if(dir == null)
			throw new IllegalArgumentException("TraverseSpecification.include does not allow a null Direction");
		
		types.add(type);
		directions.add(dir);
		return this;
	}

	/**
	 * Retrieves the currently specified order of traversal
	 * @return the currently specified order of traversal
	 */
	protected Order getTraversalOrder() {
		return traversalOrder;
	}

	/**
	 * Sets the specified order of traversal
	 * @param traversalOrder
	 * @return this
	 */
	public NeoQuery setTraversalOrder(Order traversalOrder) {
		this.traversalOrder = traversalOrder;
		return this;
	}

	/**
	 * Retrieves the currently specified StopEvaluator
	 * @return the currently specified StopEvaluator
	 */
	protected StopEvaluator getStopEvaluator() {
		return stopEvaluator;
	}

	/**
	 * Sets the specified StopEvaluator
	 * @param stopEvaluator
	 * @return this
	 */
	public NeoQuery setStopEvaluator(StopEvaluator stopEvaluator) {
		this.stopEvaluator = stopEvaluator;
		return this;
	}

	/**
	 * Retrieves the currently specified ReturnableEvaluator
	 * @return the currently specified ReturnableEvaluator
	 */
	protected ReturnableEvaluator getReturnableEvaluator() {
		return returnableEvaluator;
	}

	/**
	 * Sets the specified ReturnableEvaluator
	 * @param returnableEvaluator
	 * @return this
	 */
	public NeoQuery setReturnableEvaluator(ReturnableEvaluator returnableEvaluator) {
		this.returnableEvaluator = returnableEvaluator;
		return this;
	}
	
	/**
	 * This is a <B>fluent</B> shorthand for <CODE>setReturnableEvaluator(returnableEvaluator)</CODE><br/>
	 * @param returnableEvaluator
	 * @return this
	 */
	public NeoQuery returnAll(ReturnableEvaluator returnableEvaluator)
	{
		return this.setReturnableEvaluator(returnableEvaluator);
	}
	
	/**
	 * This is a <B>fluent</B> shorthand for <CODE>setReturnableEvaluator(ReturnableEvaluator.ALL)</CODE><br/>
	 * @return this
	 */
	public NeoQuery returnAll()
	{
		return this.setReturnableEvaluator(ReturnableEvaluator.ALL);
	}
	
	/**
	 * This is a <B>fluent</B> shorthand for <CODE>setReturnableEvaluator(ReturnableEvaluator.ALL_BUT_START_NODE)</CODE><br/>
	 * @return this
	 */
	public NeoQuery returnAllButFirst()
	{
		return this.setReturnableEvaluator(ReturnableEvaluator.ALL_BUT_START_NODE);
	}
	
	/**
	 * This is a <B>fluent</B> shorthand for <CODE>setStopEvaluator(stopEvaluator)</CODE><br/>
	 * @param stopEvaluator
	 * @return this
	 */
	public NeoQuery until(StopEvaluator stopEvaluator)
	{
		return this.setStopEvaluator(stopEvaluator);
	}
	
	/**
	 * This is a <B>fluent</B> shorthand for <CODE>setStopEvaluator(StopEvaluator.END_OF_NETWORK)</CODE><br/>
	 * @return this
	 */
	public NeoQuery untilEndOfNetwork()
	{
		return this.setStopEvaluator(StopEvaluator.END_OF_NETWORK);
	}
	
	/**
	 * This is a <B>fluent</B> shorthand for <CODE>setStopEvaluator(StopEvaluator.DEPTH_ONE)</CODE><br/>
	 * @return this
	 */
	public NeoQuery untilDepthOne()
	{
		return this.setStopEvaluator(StopEvaluator.DEPTH_ONE);
	}

	/**
	 * 
	 * @return an unmodifiable List of included {@link RelationshipTypes}
	 */
	protected List<RelationshipType> getRelationshipTypes()
	{
		return Collections.unmodifiableList(this.types);
	}
	
	/**
	 * 
	 * @return an array of size 0 .. N containing the specified {@link RelationshipType}s
	 */
	protected RelationshipType[] getRelationshipTypesAsArray()
	{
		if(this.types.isEmpty())
			return relationshipArrayType;
		else
			return this.types.toArray(relationshipArrayType);
	}
	
	/**
	 * 
	 * @return an unmodifiable List of included {@link Direction}s
	 */
	protected List<Direction> getDirections()
	{
		return Collections.unmodifiableList(this.directions);
	}
	
	/**
	 * 
	 * @return an array of size 0 .. N containing the specified {@link Direction}s
	 */
	protected Direction[] getDirectionsAsArray()
	{
		if(this.directions.isEmpty())
			return directionArrayType;
		else
			return this.directions.toArray(directionArrayType);
	}
	
	/**
	 * This method can be used to test wether this NeoQuery has any included {@link RelationshipType}s<br/>
	 * and by that determine if it can be traversed.
	 * @return true if include has added any RelationshipTypes otherwise false
	 */
	public boolean hasRelationsToTraverse()
	{
		return this.types.size() > 0;
	}
	
	/**
	 * Creates a {@link Traverser} object created with the defined {@link TraverserFactory } using the specified parameters.<br/> 
	 * @param node
	 * @throws <CODE>IllegalArgumentException</CODE> - If the specified node is <CODE>null</CODE>, or if <CODE>hasRelationsToTraverse()</CODE> returns <CODE>false</CODE>
	 * @return
	 */
	public Traverser traverse(Node node)
	{
		if(node == null)
			throw new IllegalArgumentException("Node is null!");
		
		if(!this.hasRelationsToTraverse())
			throw new IllegalStateException("No relations to traverse are specified!");
		
		return TraverserFactory.getFactory()
							   .createTraverser(this.getTraversalOrder(), 
									            node, 
									            this.getRelationshipTypesAsArray(),
									            this.getDirectionsAsArray(),
									            this.getStopEvaluator(),
									            this.getReturnableEvaluator());
	}
}
