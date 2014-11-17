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
package eu.trentorise.smartcampus.mobility.processor;

import it.sayservice.platform.client.DomainUpdateListener;
import it.sayservice.platform.core.message.Core.DomainEvent;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertAccident;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoad;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertStrike;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import eu.trentorise.smartcampus.mobility.controller.rest.JourneyPlannerController;
import eu.trentorise.smartcampus.mobility.logging.StatLogger;
import eu.trentorise.smartcampus.mobility.util.HTTPConnector;

public class EventProcessorImpl implements DomainUpdateListener {

	private static final String ALERT_DELAY = "alertDelay";
	private static final String ALERT_STRIKE = "alertStrike";
	private static final String ALERT_PARKING = "alertParking";
	private static final String ALL_ALERT_PARKING = "alertAllParking";
	private static final String ALERT_ACCIDENT = "alertAccident";
	private static final String ALERT_ROAD_BATCH = "sendRoadAlerts";
	private static final String ALERT_ROAD = "alertRoad";

	private static final String CUSTOM = "CUSTOM";

	public static final String ITINERARY_OBJECT = "smartcampus.services.journeyplanner.ItineraryObject";
	public static final String RECURRENT_JOURNEY_OBJECT = "smartcampus.services.journeyplanner.RecurrentJourneyObject";
	public static final String ALERT_FACTORY = "smartcampus.services.journeyplanner.AlertFactory";
	public static final String TRAINS_ALERT_SENDER = "smartcampus.services.journeyplanner.TrainsAlertsSender";
	public static final String PARKING_ALERT_SENDER = "smartcampus.services.journeyplanner.ParkingAlertsSender";
	public static final String ROAD_ALERT_SENDER = "smartcampus.services.journeyplanner.RoadAlertSender";
	public static final String USER_ALERT_SENDER = "smartcampus.services.journeyplanner.UserAlertSender";

	private static ObjectMapper mapper = new ObjectMapper();
	@Autowired
	@Value("${otp.url}")
	private String otpURL;

	@Autowired
	private AlertNotifier notifier;
	
	@Autowired
	private StatLogger statLogger;
	
	private static Log logger = LogFactory.getLog(EventProcessorImpl.class);

	public void onDomainEvents(String subscriptionId, List<DomainEvent> events) {
		for (DomainEvent event : events) {
			 if (event.getDoType().equals(ALERT_FACTORY) && event.getEventType().equals(CUSTOM)) {
				try {
					forwardEvent(event);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (event.getDoType().equals(TRAINS_ALERT_SENDER) && event.getEventType().equals(CUSTOM)) {
				try {
					forwardEvent(event);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (event.getDoType().equals(PARKING_ALERT_SENDER) && event.getEventType().equals(CUSTOM)) {
				try {
					forwardEvent(event);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (event.getAllTypesList().contains(ROAD_ALERT_SENDER) && event.getEventType().equals(CUSTOM)) {
				try {
					forwardEvent(event);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (event.getAllTypesList().contains(USER_ALERT_SENDER) && event.getEventType().equals(CUSTOM)) {
				try {
					notifyUser(event);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * @param event
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	private void notifyUser(DomainEvent e) throws Exception {
		Map<String, Object> map = mapper.readValue(e.getPayload(), Map.class);
		String userId = (String)map.get("userId");
		String clientId = (String)map.get("clientId");
		String name = (String)map.get("title");
		if (e.getEventSubtype().equals(ALERT_STRIKE)) {
			AlertStrike alert = mapper.convertValue(map.get("alert"), AlertStrike.class);
			statLogger.log(alert, userId);
			notifier.notifyStrike(userId, clientId, alert, name);
		} else if (e.getEventSubtype().equals(ALERT_DELAY)) {
			AlertDelay alert = mapper.convertValue(map.get("alert"), AlertDelay.class);
			statLogger.log(alert, userId);
			notifier.notifyDelay(userId, clientId, alert, name);
		} if (e.getEventSubtype().equals(ALERT_PARKING)) {
			AlertParking alert = mapper.convertValue(map.get("alert"), AlertParking.class);
			statLogger.log(alert, userId);
			notifier.notifyParking(userId, clientId, alert, name);
		} if (e.getEventSubtype().equals(ALERT_ACCIDENT)) {
			AlertAccident alert = mapper.convertValue(map.get("alert"), AlertAccident.class);
			statLogger.log(alert, userId);
			notifier.notifyAccident(userId, clientId, alert, name);
		} if (e.getEventSubtype().equals(ALERT_ROAD)) {
			AlertRoad alert = mapper.convertValue(map.get("alert"), AlertRoad.class);
			statLogger.log(alert, userId);
			notifier.notifyRoad(userId, clientId, alert, name);

		}
	}

	@SuppressWarnings("unchecked")
	private void forwardEvent(DomainEvent e) throws Exception {

		Map<String, Object> map = mapper.readValue(e.getPayload(), Map.class);
		
		if (e.getEventSubtype().equals(ALERT_STRIKE)) {
			AlertStrike alert = mapper.convertValue(map.get("alert"), AlertStrike.class);
			// TODO, need stopId?
//			alert.setId();
			String req = mapper.writeValueAsString(alert);
			statLogger.log(alert, null);
			String result = HTTPConnector.doPost(otpURL + JourneyPlannerController.SMARTPLANNER + "updateAS", req, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON);
			logger.info(result);
		} else if (e.getEventSubtype().equals(ALERT_DELAY)) {
			AlertDelay alert = mapper.convertValue(map.get("alert"), AlertDelay.class);
			String req = mapper.writeValueAsString(alert);
			statLogger.log(alert, null);
			String result = HTTPConnector.doPost(otpURL + JourneyPlannerController.SMARTPLANNER + "updateAD", req, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON);
			logger.info(result);			
		} if (e.getDoType().equals(PARKING_ALERT_SENDER) && e.getEventSubtype().equals(ALL_ALERT_PARKING) ||
			  e.getDoType().equals(ALERT_FACTORY) && e.getEventSubtype().equals(ALERT_PARKING)) {
			AlertParking alert = mapper.convertValue(map.get("alert"), AlertParking.class);
			String req = mapper.writeValueAsString(alert);
			statLogger.log(alert, null);
			String result = HTTPConnector.doPost(otpURL + JourneyPlannerController.SMARTPLANNER + "updateAP", req, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON);
			logger.info(result);	
		} if (e.getEventSubtype().equals(ALERT_ACCIDENT)) {
			AlertAccident alert = mapper.convertValue(map.get("alert"), AlertAccident.class);
			String req = mapper.writeValueAsString(alert);
			statLogger.log(alert, null);
			String result = HTTPConnector.doPost(otpURL + JourneyPlannerController.SMARTPLANNER + "updateAE", req, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON);
			logger.info(result);	
		} if (e.getEventSubtype().equals(ALERT_ROAD_BATCH)) {
			AlertRoad[] alerts = mapper.convertValue(map.get("data"), AlertRoad[].class);
			if (alerts != null) {
				for (AlertRoad alertRoad : alerts) {
					String req = mapper.writeValueAsString(alertRoad);
					statLogger.log(alertRoad, null);
					String result = HTTPConnector.doPost(otpURL + JourneyPlannerController.SMARTPLANNER + "updateAR", req, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON);
					logger.info(result);	
				}
			}
		} if (e.getEventSubtype().equals(ALERT_ROAD)) {
			AlertRoad alertRoad = mapper.convertValue(map.get("alert"), AlertRoad.class);
			statLogger.log(alertRoad, null);
			if (alertRoad != null) {
				String req = mapper.writeValueAsString(alertRoad);
				String result = HTTPConnector.doPost(otpURL + JourneyPlannerController.SMARTPLANNER + "updateAR", req, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON);
				logger.info(result);	
			}
		}
	}
}
