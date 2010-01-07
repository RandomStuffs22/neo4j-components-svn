/**
 * Copyright [2009] [AT&T] 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *      http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 *
 */
package com.neo.search.graph.domain;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.ReturnableEvaluator;
import org.neo4j.api.core.StopEvaluator;
import org.neo4j.api.core.Transaction;
import org.neo4j.api.core.Traverser;

public class UserFactoryImpl implements IUserFactory {

	private Map<Integer, IUser> idToNodes = new HashMap<Integer, IUser>();

	private NeoService neo;
	private Node orderFactoryNode;
	private static IUserFactory factory;

	private UserFactoryImpl(NeoService neo) {
		this.neo = neo;

		Transaction tx = neo.beginTx();
		Relationship rel = null;
		try {
			// iterate through all nodes and construct a look up
			// might this be something we want to look at with
			// a heat look up for scale in the future ?
			// e.g. a custom ReturnableEvaluator which takes a time stamp
			// evaluation
			// to load into a head cache?, cache miss forces a look up?
			int i = 0;
			for (Node n : neo.getAllNodes()) {
				try {
					if (i > 1) {
						IUser user = new UserImpl(n);
						
						System.out.println("Adding " + user.getId());
						idToNodes.put(user.getId(), user);
					}
				} catch (Exception e){
					
					e.printStackTrace();
				}
				i++;
			}
			rel = neo.getReferenceNode().getSingleRelationship(
					RecRelations.RECOMMENDS, Direction.OUTGOING);

			if (rel == null) {
				orderFactoryNode = neo.createNode();
				neo.getReferenceNode().createRelationshipTo(orderFactoryNode,
						RecRelations.RECOMMENDS);
			} else {
				orderFactoryNode = rel.getEndNode();

			}

			tx.success();
		} catch (Exception e) {
			tx.failure();
			e.printStackTrace();
		} finally {
			tx.finish();
		}

	}

	public IUser createUser(int id) {
		Transaction tx = neo.beginTx();

		Node node = neo.createNode();
		orderFactoryNode.createRelationshipTo(node, RecRelations.RECOMMENDS);

		IUser u = new UserImpl(node);
		u.setId(id);
		idToNodes.put(id, u);
		tx.success();
		tx.finish();

		return u;
	}

	public IUser getUserById(int id) {
		return idToNodes.get(id);
	}

	public void addRelationship(IUser from, IUser to) {
		Transaction tx = neo.beginTx();
		from.addRelation(to);
		tx.success();
		tx.finish();

	}

	public static IUserFactory getInstance() {
		if (factory == null) {
			factory = new UserFactoryImpl(new EmbeddedNeo("/tmp/neo"));
		}

		return factory;
	}

	public static IUserFactory getInstance(String neodb){
		factory = new UserFactoryImpl(new EmbeddedNeo(neodb));
		return factory;
	}
	
	public int getUserId(IUser user){
		Transaction tx = neo.beginTx();
		int result = user.getId();
		tx.success();
		tx.finish();
		return result;
	}
	
	
	public Iterator<IUser> getRelatedUsers(final int id) {

		return new Iterator<IUser>() {

			private final Node underlyingNode;
			private final Iterator<Node> iterator;
			{
				Transaction tx = neo.beginTx();
				underlyingNode = ((UserImpl) getUserById(id))
								.getUnderlyingNode();
				iterator = underlyingNode.traverse(
								Traverser.Order.BREADTH_FIRST, 
								StopEvaluator.DEPTH_ONE,
								ReturnableEvaluator.ALL_BUT_START_NODE, 
								RecRelations.KNOWS,
								Direction.OUTGOING).iterator();

				tx.success();
				tx.finish();
			}
			
			public boolean hasNext() {
				Transaction tx = neo.beginTx();
				boolean result = iterator.hasNext();
				tx.success();
				tx.finish();
				return result;
			}

			public IUser next() {
				Transaction tx = neo.beginTx();
				Node n = iterator.next();
				tx.success();
				tx.finish();
				return new UserImpl(n);
			}

			public void remove() {
				iterator.remove();
			}

		};
	}

	
}
