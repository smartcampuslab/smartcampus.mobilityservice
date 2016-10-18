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

import static org.junit.Assert.assertEquals;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;

import java.util.Collections;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

import eu.trentorise.smartcampus.mobility.service.AlertSender;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;

/**
 * @author raman
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class TestParkingHandler {
	@Autowired
	private DomainStorage storage;

	@Autowired
	private AlertSender alertSender;
	
	@Before
	public void init() {
		storage.reset();
	}

	@Test
	public void testPlaces() throws Exception {
		Itinerary bikeSharing = ObjectCreator.createCarWithParking();
		String id = new ObjectId().toString();
		ItineraryObject io = new ItineraryObject("1", id, bikeSharing, bikeSharing.getFrom(), bikeSharing.getTo(), "test");
		io.setMonitor(true);
		storage.saveItinerary(io);

		// many places available
		AlertParking toStation = ObjectCreator.createParking(10);
		alertSender.publishParkings(Lists.asList(toStation, new AlertParking[0]));
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		long found = hasFewPlacesAvailableAlert(io);
		assertEquals(-1, found);
		
		// few places available
		toStation = ObjectCreator.createParking(1);
		alertSender.publishParkings(Lists.asList(toStation, new AlertParking[0]));
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		found = hasFewPlacesAvailableAlert(io);
		assertEquals(1, found);				
	}	
	
	private long hasFewPlacesAvailableAlert(ItineraryObject io) {
		for (Leg leg : io.getData().getLeg()) {
			if (leg.getAlertParkingList().size() > 0) return leg.getAlertParkingList().get(0).getPlacesAvailable(); 
		}
		return -1L;
	}	

}
