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
package eu.trentorise.smartcampus.mobility.controller.rest;

import it.sayservice.platform.client.DomainEngineClient;
import it.sayservice.platform.client.InvocationException;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.SimpleLeg;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.Transport;
import it.sayservice.platform.smartplanner.data.message.alerts.Alert;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertAccident;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoad;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertStrike;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertType;
import it.sayservice.platform.smartplanner.data.message.alerts.CreatorType;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourney;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourneyParameters;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.trentorise.smartcampus.mobility.util.ConnectorException;
import eu.trentorise.smartcampus.mobility.util.HTTPConnector;

@Controller
public class JourneyPlannerController {

	@Autowired
	private DomainEngineClient domainClient;

	@Autowired
	@Value("${otp.url}")
	private String otpURL;

	private static ObjectMapper mapper = new ObjectMapper(); 
	static {
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	
	// private static final String OTP_LOCATION = "http://213.21.154.84:7070";
	// private static final String OTP_LOCATION = "http://localhost:7070";

	public static final String SMARTPLANNER = "/smart-planner/api-webapp/planner/";
	// http://localhost:7070

	private static final String PLAN = "plan";
	private static final String RECURRENT = "recurrentJourney";

	// no crud
	@RequestMapping(method = RequestMethod.POST, value = "/plansinglejourney")
	public @ResponseBody
	List<Itinerary> planSingleJourney(HttpServletResponse response, @RequestBody SingleJourney journeyRequest) throws InvocationException {
		try {

			List<String> reqs = buildItineraryPlannerRequest(journeyRequest);

			List<Itinerary> itineraries = new ArrayList<Itinerary>();

			for (String req : reqs) {
				String plan = HTTPConnector.doGet(otpURL + SMARTPLANNER + PLAN, req, MediaType.APPLICATION_JSON, null, "UTF-8");
				List<?> its = mapper.readValue(plan, List.class);
				for (Object it : its) {
					Itinerary itinerary = mapper.convertValue(it, Itinerary.class);
					itineraries.add(itinerary);
				}
			}

			ItinerarySorter.sort(itineraries, journeyRequest.getRouteType());

			return itineraries;
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	private List<String> buildItineraryPlannerRequest(SingleJourney request) {
		List<String> reqs = new ArrayList<String>();
		for (TType type : request.getTransportTypes()) {
			int its = request.getResultsNumber();
			if (its == 0) its = 1;
			String req = String.format("from=%s,%s&to=%s,%s&date=%s&departureTime=%s&transportType=%s&numOfItn=%s", request.getFrom().getLat(), request.getFrom().getLon(), request.getTo().getLat(), request.getTo().getLon(), request.getDate(), request.getDepartureTime(), type, its);
			reqs.add(req);
		}

		return reqs;
		// String[] resp = new String[request.getTransportTypes().length];
		// return reqs.toArray(resp);
	}

	


	// RECURRENT

	@RequestMapping(method = RequestMethod.POST, value = "/planrecurrent")
	public @ResponseBody
	RecurrentJourney planRecurrentJourney(HttpServletResponse response, @RequestBody RecurrentJourneyParameters parameters) throws InvocationException {
		try {
			List<String> reqs = buildRecurrentJourneyPlannerRequest(parameters);
			List<SimpleLeg> legs = new ArrayList<SimpleLeg>();
			for (String req : reqs) {
				String plan = HTTPConnector.doGet(otpURL + SMARTPLANNER + RECURRENT, req, MediaType.APPLICATION_JSON, null, "UTF-8");
				List<?> sl = mapper.readValue(plan, List.class);
				for (Object o : sl) {
					legs.add((SimpleLeg) mapper.convertValue(o, SimpleLeg.class));
				}
			}

			RecurrentJourney journey = new RecurrentJourney();
			journey.setParameters(parameters);
			journey.setLegs(legs);
			journey.setMonitorLegs(buildMonitorMap(legs));
			return journey;
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		return null;
	}

	private Map<String, Boolean> buildMonitorMap(List<SimpleLeg> legs) {
		Map<String, Boolean> result = new TreeMap<String, Boolean>();

		for (SimpleLeg leg : legs) {
			Transport transport = leg.getTransport();
			if (transport.getType() != TType.BUS && transport.getType() != TType.TRAIN) {
				continue;
			}
			String id = transport.getAgencyId() + "_" + transport.getRouteId();
			if (!result.containsKey(id)) {
				result.put(id, true);
			}
		}

		return result;
	}

	private List<String> buildRecurrentJourneyPlannerRequest(RecurrentJourneyParameters request) {
		List<String> reqs = new ArrayList<String>();
		for (TType type : request.getTransportTypes()) {
			String rec = request.getRecurrence().toString().replaceAll("[\\[\\] ]", "");
			String req = String.format("recurrence=%s&from=%s&to=%s&time=%s&interval=%s&transportType=%s&routeType=%s&fromDate=%s&toDate=%s&numOfItn=%s", rec, request.getFrom().toLatLon(), request.getTo().toLatLon(), request.getTime(), request.getInterval(), type, request.getRouteType(), request.getFromDate(), request.getToDate(), request.getResultsNumber());
			reqs.add(req);
		}
		return reqs;
	}

	

	// ALERTS

	@RequestMapping(method = RequestMethod.POST, value = "/alert/service")
	public @ResponseBody
	void submitServiceAlert(HttpServletResponse response, @RequestBody Map<String, Object> map) throws InvocationException {
		try {
			submitAlert(map, null, getClientId());
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private void submitAlert(Map<String, Object> map, String userId, String clientId)
			throws InvocationException {
		AlertType type = AlertType.getAlertType((String) map.get("type"));

		Alert alert = null;
		String method = "";
		Map<String, Object> contentMap = map;
		switch (type) {
		case ACCIDENT:
			alert = mapper.convertValue(contentMap, AlertAccident.class);
			method = "submitAlertAccident";
			break;
		case DELAY:
			alert = mapper.convertValue(contentMap, AlertDelay.class);
			method = "submitAlertDelay";
			break;
		case PARKING:
			alert = mapper.convertValue(contentMap, AlertParking.class);
			method = "submitAlertParking";
			break;
		case STRIKE:
			alert = mapper.convertValue(contentMap, AlertStrike.class);
			method = "submitAlertStrike";
			break;
		case ROAD:
			alert = mapper.convertValue(contentMap, AlertRoad.class);
			method = "submitAlertRoad";
			break;
		default:
			break;
		}

		alert.setType(type);
		if (userId != null) {
			alert.setCreatorId(userId);
			alert.setCreatorType(CreatorType.USER);
		} else if (clientId != null) {
			alert.setCreatorId(clientId);
			alert.setCreatorType(CreatorType.SERVICE);
		} else {
			throw new IllegalArgumentException("unknown sender");
		}

		Map<String, Object> pars = new HashMap<String, Object>();
		pars.put("newAlert", alert);
		// pars.put("userId", userId);
		domainClient.invokeDomainOperation(method, "smartcampus.services.journeyplanner.AlertFactory", "smartcampus.services.journeyplanner.AlertFactory.0", pars, userId, "vas_journeyplanner_subscriber");
	}

	// /////////////////////////////////////////////////////////////////////////////	

	@RequestMapping(method = RequestMethod.GET, value = "/getparkingsbyagency/{agencyId}")
	public @ResponseBody
	void getParkingsByAgency(HttpServletResponse response, @PathVariable String agencyId) throws InvocationException {
		try {
			String address =  otpURL + SMARTPLANNER + "getParkingsByAgency?agencyId=" + agencyId;
			
			String routes = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
			
			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(routes);

		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}			

	@RequestMapping(method = RequestMethod.GET, value = "/getroadinfobyagency/{agencyId}/{from}/{to}")
	public @ResponseBody
	void getRoadInfoByAgency(HttpServletResponse response, @PathVariable String agencyId, @PathVariable Long from, @PathVariable Long to) throws InvocationException {
		try {
			String address =  otpURL + SMARTPLANNER + "getAR?agencyId=" + agencyId + "&from=" + from + "&to=" + to;
			
			String roadInfo = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
			
			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(roadInfo);

		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * @return UserDetails instance from security context
	 */
	protected String getClientId() {
		return "service";
	}

}
