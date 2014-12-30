/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package eu.trentorise.smartcampus.mobility.test.script;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.Mongo;
import com.mongodb.MongoException;

import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.storage.RecurrentJourneyObject;
import eu.trentorise.smartcampus.network.JsonUtils;

/**
 * @author raman
 *
 */
public class ConvertData {

	private MongoTemplate template;
	private MongoTemplate targetTemplate;
	
	public ConvertData() throws UnknownHostException, MongoException {
		super();
		template = new MongoTemplate(new Mongo(), "smartsayback");
		targetTemplate = new MongoTemplate(new Mongo(), "mobility-domain");
	}


	public void convert() {
		targetTemplate.dropCollection("itinerary");
		targetTemplate.dropCollection("recurrent");
		List<Map> itineraries = template.find(Query.query(Criteria.where("type").is("smartcampus.services.journeyplanner.ItineraryObject")), Map.class, "domainObject");
		for (Map m : itineraries) {
			targetTemplate.save(JsonUtils.convert(m.get("content"), ItineraryObject.class), "itinerary");	
		}
		
		itineraries = template.find(Query.query(Criteria.where("type").is("smartcampus.services.journeyplanner.RecurrentJourneyObject")), Map.class, "domainObject");
		for (Map m : itineraries) {
//			list.add();
			targetTemplate.save(JsonUtils.convert(m.get("content"), RecurrentJourneyObject.class), "recurrent");	
		}
	}


	public static void main(String[] args) throws UnknownHostException, MongoException {
		new ConvertData().convert();
	}
}
