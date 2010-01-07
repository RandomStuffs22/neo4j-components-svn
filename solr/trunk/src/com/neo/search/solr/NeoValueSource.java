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
package com.neo.search.solr;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Searcher;
import org.apache.solr.search.function.DocValues;
import org.apache.solr.search.function.ValueSource;

import com.neo.search.graph.domain.IUser;
import com.neo.search.graph.domain.IUserFactory;
import com.neo.search.graph.domain.UserFactoryImpl;

/**
 * Perform a boost between a graph traversal of userId's which
 * have a relationship to an id passed in the social_g(field, id) function query.
 * Boosts are 1 for no match, 2 for a match
 * 
 * @author poleary
 *
 */
public class NeoValueSource extends ValueSource {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1906954031788984871L;
	int userId;
	ValueSource vs;
	// this should already be initialized through NeoSocialUpdateProcessorFactory
	public static IUserFactory usf = UserFactoryImpl.getInstance(); 
	Set<Integer> idSet = new TreeSet<Integer>();

	/**
	 * 
	 * @param vs the valuesource of the field being intersected
	 * @param userId the id of the user whose relationship graph is being traversed
	 */
	public NeoValueSource(ValueSource vs, int userId) {

		this.userId = userId;
		this.vs = vs;
		usf.getUserById(userId);
		Iterator<IUser> iterator = usf.getRelatedUsers(userId);
		// hmm, wonder is there a better way in node tree
		while (iterator.hasNext()) {
			int id = usf.getUserId(iterator.next());
			//System.out.println("My friends are: " + id);
			idSet.add(id);
		}
	}

	@Override
	public DocValues getValues(Map context, IndexReader reader)
			throws IOException {

		return new SocialDocValues(context, reader, userId, vs, idSet);
	}

	@Override
	public void createWeight(Map context, Searcher searcher) throws IOException {

		vs.createWeight(context, searcher);
	}

	@Override
	public String description() {

		return "social_graph";
	}

	@Override
	public boolean equals(Object o) {

		return (hashCode() == ((NeoValueSource) o).hashCode());
	}

	@Override
	public int hashCode() {
		return userId ^ 31 * 100 + idSet.hashCode();
	}

}

class SocialDocValues extends DocValues {

	ValueSource source;
	DocValues vals;
	Set idSet;

	public SocialDocValues(Map context, IndexReader reader, int userId,
			ValueSource source, Set idSet) throws IOException {

		this.source = source;
		this.vals = source.getValues(context, reader);
		this.idSet = idSet;

	}

	@Override
	public int intVal(int doc) {
		int uid = vals.intVal(doc);

		if (idSet.contains(uid)) {
			
			return 2;
		}
		return 0;
	}

	@Override
	public float floatVal(int doc) {
		float result =(float) intVal(doc);
//		if (result >0)
//			System.out.println(doc+": Called floatVal "+result );
		
		return result;
	}

	@Override
	public double doubleVal(int doc) {

		return (double) intVal(doc);
	}

	@Override
	public String strVal(int doc) {

		return Double.toString(doubleVal(doc));
	}

	@Override
	public String toString(int doc) {

		return "social_graph(" + vals.intVal(doc) + " IN_GRAPH ?" + intVal(doc)
				+ ")";
	}

}
