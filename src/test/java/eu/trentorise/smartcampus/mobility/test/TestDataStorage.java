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

package eu.trentorise.smartcampus.mobility.test;

import static org.junit.Assert.*;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourney;

import java.util.Collections;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.storage.RecurrentJourneyObject;

/**
 * @author raman
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class TestDataStorage {

	@Autowired
	private DomainStorage storage;

	@Before
	public void init() {
		storage.reset();
	}
	
	@Test
	public void testItineraryCD() {
		Itinerary withCar = ObjectCreator.createCarWithParking();
		String id = new ObjectId().toString();
		ItineraryObject io = new ItineraryObject("1", id, withCar, withCar.getFrom(), withCar.getTo(), "test");
		storage.saveItinerary(io);
		
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		assertNotNull(io);
		
		List<ItineraryObject> list = storage.searchDomainObjects(Collections.<String,Object>singletonMap("userId", "1"), ItineraryObject.class);
		assertNotNull(list);
		assertTrue(list.size() > 0);
		
		storage.deleteItinerary(id);
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		assertNull(io);
		
	}
	
	@Test
	public void testRecurrentCUD() {
		RecurrentJourney rj = ObjectCreator.createRecurrent();
		
		String id = new ObjectId().toString();
		RecurrentJourneyObject ro = new RecurrentJourneyObject("1", id, rj, "test");
		storage.saveRecurrent(ro);
		
		ro = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), RecurrentJourneyObject.class);
		assertNotNull(ro);

		List<RecurrentJourneyObject> list = storage.searchDomainObjects(Collections.<String,Object>singletonMap("userId", "1"), RecurrentJourneyObject.class);
		assertNotNull(list);
		assertTrue(list.size() > 0);

		storage.deleteRecurrent(id);
		ro = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), RecurrentJourneyObject.class);
		assertNull(ro);
		
	}

}
