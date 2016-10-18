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
import static org.junit.Assert.assertNotNull;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourney;

import java.util.Collections;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import eu.trentorise.smartcampus.mobility.service.AlertSender;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.storage.RecurrentJourneyObject;

/**
 * @author raman
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class TestAlertSender {

	@Autowired
	private DomainStorage storage;

	@Autowired
	private AlertSender alertSender;
	
	@Before
	public void init() {
		storage.reset();
	}

	@Test
	public void testTrainSingleMatchingAlert() throws Exception{
		Itinerary withCar = ObjectCreator.createTransit();
		String id = new ObjectId().toString();
		ItineraryObject io = new ItineraryObject("1", id, withCar, withCar.getFrom(), withCar.getTo(), "test");
		io.setMonitor(true);
		storage.saveItinerary(io);

		AlertDelay gt = ObjectCreator.createTrainDelayForSingle(10);
		alertSender.publishTrains(Collections.singletonList(gt));
		
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		long found = hasDelayAlert(io);
		assertEquals(10*60*1000L,found);
	}

	@Test
	public void testTrainSingleNonMatchingAlert() throws Exception {
		Itinerary withCar = ObjectCreator.createTransit();
		String id = new ObjectId().toString();
		ItineraryObject io = new ItineraryObject("1", id, withCar, withCar.getFrom(), withCar.getTo(), "test");
		io.setMonitor(true);
		storage.saveItinerary(io);

		AlertDelay gt = ObjectCreator.createTrainDelayForSingle(10);
		gt.getTransport().setTripId("666");
		alertSender.publishTrains(Collections.singletonList(gt));
		
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		long found = hasDelayAlert(io);
		assertEquals(-1L,found);
	}
	
	@Test
	public void testTrainSingleMatchingAlertOnTime() throws Exception {
		Itinerary withCar = ObjectCreator.createTransit();
		String id = new ObjectId().toString();
		ItineraryObject io = new ItineraryObject("1", id, withCar, withCar.getFrom(), withCar.getTo(), "test");
		io.setMonitor(true);
		storage.saveItinerary(io);

		AlertDelay gt = ObjectCreator.createTrainDelayForSingle(0);
		alertSender.publishTrains(Collections.singletonList(gt));
		
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		long found = hasDelayAlert(io);
		assertEquals(-1L, found);
	}

	@Test
	public void testTrainMatchingAlerts() throws Exception {
		Itinerary itinerary = ObjectCreator.createTransit();
		String id = new ObjectId().toString();
		ItineraryObject io = new ItineraryObject("1", id, itinerary, itinerary.getFrom(), itinerary.getTo(), "test");
		io.setMonitor(true);
		storage.saveItinerary(io);

		// delay 6 min, should be registered
		AlertDelay gt = ObjectCreator.createTrainDelayForSingle(6);
		alertSender.publishTrains(Collections.singletonList(gt));
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		long found = hasDelayAlert(io);
		assertEquals(6*60*1000, found);
		
		// delay 6 min again, should not be registered
		gt = ObjectCreator.createTrainDelayForSingle(6);
		alertSender.publishTrains(Collections.singletonList(gt));
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		found = hasDelayAlert(io);
		assertEquals(6*60*1000, found);
		
		// delay 8 min, should not be registered
		gt = ObjectCreator.createTrainDelayForSingle(8);
		alertSender.publishTrains(Collections.singletonList(gt));
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		found = hasDelayAlert(io);
		assertEquals(6*60*1000, found);
		
		// delay 12, should be registered
		gt = ObjectCreator.createTrainDelayForSingle(12);
		alertSender.publishTrains(Collections.singletonList(gt));
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		found = hasDelayAlert(io);
		assertEquals(12*60*1000, found);

		// delay 0 min, should be registered
		gt = ObjectCreator.createTrainDelayForSingle(0);
		alertSender.publishTrains(Collections.singletonList(gt));
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		found = hasDelayAlert(io);
		assertEquals(0, found);

	}

	private long hasDelayAlert(ItineraryObject io) {
		for (Leg leg : io.getData().getLeg()) {
			if (leg.getAlertDelayList().size() > 0) return leg.getAlertDelayList().get(0).getDelay(); 
		}
		return -1L;
	}
	
	@Test
	public void testRecurrentTrainSingleMatchingAlert() throws Exception{
		RecurrentJourney rj = ObjectCreator.createRecurrent();
		
		String id = new ObjectId().toString();
		RecurrentJourneyObject ro = new RecurrentJourneyObject("1", id, rj, "test");
		ro.setMonitor(true);
		storage.saveRecurrent(ro);

		AlertDelay gt = ObjectCreator.createTrainDelayForRecurrent(10);
		alertSender.publishTrains(Collections.singletonList(gt));

		ro = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), RecurrentJourneyObject.class);
		assertNotNull(ro);
	
		// alert 10 min
		String alertId = ObjectCreator.idTrainDelayForRecurrent();
		long found = hasDelayAlert(ro, alertId);
		assertEquals(10*60*1000,found);
	}

	@Test
	public void testRecurrentTrainSingleNonMatchingAlert() throws Exception{
		RecurrentJourney rj = ObjectCreator.createRecurrent();
		
		String id = new ObjectId().toString();
		RecurrentJourneyObject ro = new RecurrentJourneyObject("1", id, rj, "test");
		ro.setMonitor(true);
		storage.saveRecurrent(ro);

		AlertDelay gt = ObjectCreator.createTrainDelayForRecurrent(10);
		gt.getTransport().setTripId("666");
		alertSender.publishTrains(Collections.singletonList(gt));

		ro = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), RecurrentJourneyObject.class);
		assertNotNull(ro);
	
		// alert 10 min
		String alertId = ObjectCreator.idTrainDelayForRecurrent();
		long found = hasDelayAlert(ro, alertId);
		assertEquals(-1,found);
	}

	@Test
	public void testRecurrentTrainSingleOnTimeAlert() throws Exception{
		RecurrentJourney rj = ObjectCreator.createRecurrent();
		
		String id = new ObjectId().toString();
		RecurrentJourneyObject ro = new RecurrentJourneyObject("1", id, rj, "test");
		ro.setMonitor(true);
		storage.saveRecurrent(ro);

		AlertDelay gt = ObjectCreator.createTrainDelayForRecurrent(0);
		alertSender.publishTrains(Collections.singletonList(gt));

		ro = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), RecurrentJourneyObject.class);
		assertNotNull(ro);
	
		// alert -0 min
		String alertId = ObjectCreator.idTrainDelayForRecurrent();
		long found = hasDelayAlert(ro, alertId);
		assertEquals(-1,found);
	}

	@Test
	public void testRecurrentTrainAlerts() throws Exception{
		RecurrentJourney rj = ObjectCreator.createRecurrent();
		
		String id = new ObjectId().toString();
		RecurrentJourneyObject ro = new RecurrentJourneyObject("1", id, rj, "test");
		ro.setMonitor(true);
		storage.saveRecurrent(ro);

		AlertDelay gt = ObjectCreator.createTrainDelayForRecurrent(6);
		alertSender.publishTrains(Collections.singletonList(gt));

		ro = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), RecurrentJourneyObject.class);
	
		// alert 6 min
		String alertId = ObjectCreator.idTrainDelayForRecurrent();
		long found = hasDelayAlert(ro, alertId);
		assertEquals(60*6*1000,found);
		
		// alert 8 min, not registered
//		gt = ObjectCreator.createTrainDelayForRecurrent(8);
//		alertSender.publishTrains(Collections.singletonList(gt));
//		ro = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), RecurrentJourneyObject.class);
//		found = hasDelayAlert(ro, alertId);
//		assertEquals(60*6*1000,found);

		// alert 12 min, registered
		gt = ObjectCreator.createTrainDelayForRecurrent(12);
		alertSender.publishTrains(Collections.singletonList(gt));
		ro = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), RecurrentJourneyObject.class);
		found = hasDelayAlert(ro, alertId);
		assertEquals(60*12*1000,found);

		// alert 12 min, registered
		gt = ObjectCreator.createTrainDelayForRecurrent(0);
		alertSender.publishTrains(Collections.singletonList(gt));
		ro = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), RecurrentJourneyObject.class);
		found = hasDelayAlert(ro, alertId);
		assertEquals(0,found);

	}



	private long hasDelayAlert(RecurrentJourneyObject ro, String id) {
		if (ro.getAlertsSent() == null || ro.getAlertsSent().getAlertsValues() == null || ro.getAlertsSent().getAlertsValues().isEmpty()) return -1;
		return ro.getAlertsSent().getAlertsValues().get(id);
	}
/*
	@Test
	public void testParking() throws Exception {
		Itinerary withCar = ObjectCreator.createCarWithParking();
		String id = new ObjectId().toString();
		ItineraryObject io = new ItineraryObject("1", id, withCar, withCar.getFrom(), withCar.getTo(), "test");
		io.setMonitor(true);
		storage.saveItinerary(io);

		// parking with 20 slots, should not be registered
		Parking 
		parking = ObjectCreator.createParking(20);
		alertSender.publishParkings(Collections.singletonList(parking));
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		int found = hasParkingAlert(io);
		assertEquals(-1, found);
		
		// parking with 2 slots, should be registered
		parking = ObjectCreator.createParking(2);
		alertSender.publishParkings(Collections.singletonList(parking));
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		found = hasParkingAlert(io);
		assertEquals(2, found);

		// parking with 5 slots, should not be registered
		parking = ObjectCreator.createParking(5);
		alertSender.publishParkings(Collections.singletonList(parking));
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		found = hasParkingAlert(io);
		assertEquals(2, found);
	}

	@Test
	public void testBikeSharing() throws Exception {
		Itinerary withCar = ObjectCreator.createWithBikeSharing();
		String id = new ObjectId().toString();
		ItineraryObject io = new ItineraryObject("1", id, withCar, withCar.getFrom(), withCar.getTo(), "test");
		io.setMonitor(true);
		storage.saveItinerary(io);

		// parking with 10 slots, should not be registered
		Parking 
		parking = ObjectCreator.createBikeSharingFrom(10);
		alertSender.publishParkings(Collections.singletonList(parking));
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		int found = hasParkingAlertVehicles(io, "Parco Venezia");
		assertEquals(-1, found);
		
		// irrelevant place, should not be registered
		parking = ObjectCreator.createBikeSharingFrom(2);
		parking.setId("666");
		alertSender.publishParkings(Collections.singletonList(parking));
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		found = hasParkingAlertVehicles(io,"Parco Venezia");
		assertEquals(-1, found);
		
		// parking with 2 vehicles, should be registered
		parking = ObjectCreator.createBikeSharingFrom(2);
		alertSender.publishParkings(Collections.singletonList(parking));
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		found = hasParkingAlertVehicles(io, "Parco Venezia");
		assertEquals(2, found);

		// parking with 10 places, should not be registered
		parking = ObjectCreator.createBikeSharingTo(10);
		alertSender.publishParkings(Collections.singletonList(parking));
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		found = hasParkingAlertPlaces(io, "Biki Piazzale Follone - Rovereto");
		assertEquals(-1, found);

		// parking with 2 places, should be registered
		parking = ObjectCreator.createBikeSharingTo(2);
		alertSender.publishParkings(Collections.singletonList(parking));
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		found = hasParkingAlertPlaces(io, "Biki Piazzale Follone - Rovereto");
		assertEquals(2, found);

	}
*/
	private int hasParkingAlert(ItineraryObject io) {
		for (Leg leg : io.getData().getLeg()) {
			if (leg.getAlertParkingList().size() > 0) return leg.getAlertParkingList().get(0).getPlacesAvailable(); 
		}
		return -1;
	}

	private int hasParkingAlertVehicles(ItineraryObject io, String id) {
		for (Leg leg : io.getData().getLeg()) {
			if (leg.getAlertParkingList().size() > 0) {
				for (AlertParking ap : leg.getAlertParkingList()) {
					if (id.equals(ap.getPlace().getId())) return ap.getNoOfvehicles(); 
				}
			}
		}
		return -1;
	}
	private int hasParkingAlertPlaces(ItineraryObject io, String id) {
		for (Leg leg : io.getData().getLeg()) {
			if (leg.getAlertParkingList().size() > 0) {
				for (AlertParking ap : leg.getAlertParkingList()) {
					if (id.equals(ap.getPlace().getId())) return ap.getPlacesAvailable(); 
				}
			}
		}
		return -1;
	}

	@Test
	public void testDelayAlert() throws Exception{
		Itinerary withCar = ObjectCreator.createTransit();
		String id = new ObjectId().toString();
		ItineraryObject io = new ItineraryObject("1", id, withCar, withCar.getFrom(), withCar.getTo(), "test");
		io.setMonitor(true);
		storage.saveItinerary(io);

		AlertDelay delay = ObjectCreator.createAlertDelay(10);
		alertSender.publishAlert(delay);
		
		io = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", id), ItineraryObject.class);
		long found = hasDelayAlert(io);
		assertEquals(10*60*1000,found);
	}

}
