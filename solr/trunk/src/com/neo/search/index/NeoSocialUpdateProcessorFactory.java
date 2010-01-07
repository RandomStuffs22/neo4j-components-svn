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
package com.neo.search.index;

import java.io.IOException;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;

import com.neo.search.graph.domain.IUser;
import com.neo.search.graph.domain.IUserFactory;
import com.neo.search.graph.domain.UserFactoryImpl;

/**
 * Required for indexing a graph data with neo4j Configuration in solrconfig.xml
 * is as follows default values shown solrconfig.xml :
 * 
 * <pre>
 * &lt;updateRequestProcessor&gt;
 *    
 *    &lt;processor class=&quot;com.neo.search.index.NeoSocialUpdateProcessorFactory&quot;&gt;
 *      &lt;str name=&quot;neoUserId&quot;&gt;neoUserId&lt;/str&gt;
 *      &lt;str name=&quot;neoRelationshipField&quot;&gt;neoRelationship&lt;/str&gt;
 *      &lt;str name=&quot;neoDB&quot;&gt;/tmp/neo&lt;/str&gt;
 *    &lt;/processor&gt;
 *    &lt;processor class=&quot;solr.RunUpdateProcessorFactory&quot; /&gt;    
 *    &lt;processor class=&quot;solr.LogUpdateProcessorFactory&quot; /&gt; 
 *  &lt;/updateRequestProcessor&gt;
 * </pre>
 */
public class NeoSocialUpdateProcessorFactory extends
		UpdateRequestProcessorFactory {

	public static String userId = "neoUserId";
	public static String relationshipField = "neoRelationship";
	public static String neodb = "/tmp/neo";

	/**
	 * {@link UpdateRequestProcessorFactory} parses args in solrconfig.xml takes
	 * neoUserId and neoRelationshipField
	 */
	@Override
	public void init(NamedList args) {

		Object uid = args.get("neoUserId");
		if (uid != null)
			userId = (String) uid;

		// TODO: can multiple relationships be included here?
		Object relationship = args.get("neoRelationshipField");
		if (relationship != null)
			relationshipField = (String) relationship;

		Object db = args.get("neoDB");
		if (db != null)
			neodb = (String) db;

		// TODO: A map could be used here to include multiple fields
		// in the graph

		UserFactoryImpl.getInstance(neodb);
		super.init(args);
	}

	@Override
	public UpdateRequestProcessor getInstance(SolrQueryRequest req,
			SolrQueryResponse resp, UpdateRequestProcessor next) {
		return new NeoSocialUpdateProcessor(next, userId, relationshipField);
	}

}

class NeoSocialUpdateProcessor extends UpdateRequestProcessor {

	UpdateRequestProcessor next;
	IUserFactory usf = UserFactoryImpl.getInstance();
	String userId = null, relationshipField = null;

	public NeoSocialUpdateProcessor(UpdateRequestProcessor next, String userId,
			String relationshipField) {
		super(next);
		this.next = next;
		this.userId = userId;
		this.relationshipField = relationshipField;
	}

	/**
	 * if a document contains the neoUserId field as defined in
	 * {@link NeoSocialUpdateProcessorFactory} then the neoUserId is added to
	 * the neo social graph, and the rest of the document is indexed if a
	 * document contains a neoRelationshipField (uid:uid2:uid3...) then a
	 * relationship is added to those nodes
	 * 
	 * <pre>
	 * uid -> uid2
	 * uid -> uid3
	 * uid -> uid....
	 * </pre>
	 * 
	 * Relationship documents are not indexed in solr, they should only contain
	 * a relationship field
	 * 
	 */
	@Override
	public void processAdd(AddUpdateCommand cmd) throws IOException {

		SolrInputDocument doc = cmd.getSolrInputDocument();

		String userIds = (String) doc.getFieldValue(relationshipField);
		String baseuserId = (String) doc.getFieldValue(userId);

		if (userIds != null) {
			// this is a relationship request
			// no additional document is added
			addRelationships(userIds);
			// System.out.println("Returning not adding to solr index");
			return;
		}

		if (baseuserId != null) {
			// assume this is an int
			int id = new Integer(baseuserId).intValue();
			if (usf.getUserById(id) == null) {
				usf.createUser(id);
			}
		}

		if (next != null)
			next.processAdd(cmd);

	}

	@Override
	public void processCommit(CommitUpdateCommand cmd) throws IOException {

		if (next != null)
			next.processCommit(cmd);
	}

	@Override
	public void processDelete(DeleteUpdateCommand cmd) throws IOException {
		if (next != null)
			next.processDelete(cmd);
	}

	/**
	 * 
	 * @param userIds
	 *            a string containing uid:uid2:uid3:uid4.... adds relationships
	 *            from uid to uid2, uid3 etc..
	 */
	private void addRelationships(String userIds) {

		String[] users = userIds.split(":");

		// assume first relates to rest
		int baseId = new Integer(users[0]).intValue();
		int sz = users.length;

		IUser user = usf.getUserById(baseId);

		for (int i = 1; i < sz; i++) {
			int relatedTo = new Integer(users[i]).intValue();
			IUser relation = usf.getUserById(relatedTo);
			usf.addRelationship(user, relation);
		}

	}
}