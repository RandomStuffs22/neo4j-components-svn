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

import org.neo4j.api.core.Node;

public class UserImpl implements IUser{

	private final Node underlyingNode;
	
	private static final String KEY_USER_ID = "userid";
	
	public UserImpl(Node underlyingNode){
		this.underlyingNode = underlyingNode;
	}
	
	public int getId() {
		
		return (Integer) underlyingNode.getProperty(KEY_USER_ID);
	}

	public void setId(int id) {
		underlyingNode.setProperty(KEY_USER_ID, id);
	}

	public void addRelation(IUser user) {
		
		underlyingNode.createRelationshipTo(((UserImpl)user).getUnderlyingNode(), 
											RecRelations.KNOWS);
		
	}
	

	
	
	public Node getUnderlyingNode(){
		return underlyingNode;
	}

}
