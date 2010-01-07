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

import org.apache.lucene.queryParser.ParseException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.ValueSourceParser;
import org.apache.solr.search.function.FunctionQuery;
import org.apache.solr.search.function.ValueSource;


/**
 * this should be called using solr's FunctionQuery
 * {@link FunctionQuery}
 * 
 * Must be configured in solconfig.xml as
 * <pre>
 * &lt;valueSourceParser name="social_g" class="com.neo.search.solr.NeoValueSourceParser" /&gt;
 * 
 * An implementation could be
 * q={boost b=social_g(neoUserId, 122)}popcorn
 * </pre>
 * where social_g is the 
 * @author poleary
 *
 */
public class NeoValueSourceParser extends ValueSourceParser{

	@Override
	public ValueSource parse(FunctionQParser fp) throws ParseException {
		ValueSource vs = fp.parseValueSource();
		int userId = new Integer(fp.parseArg()).intValue();

		return new NeoValueSource(vs,userId);
	}

	
	@Override
	public void init(NamedList args) {
	
		// could include field to intersect here with other configs?
	}
}
