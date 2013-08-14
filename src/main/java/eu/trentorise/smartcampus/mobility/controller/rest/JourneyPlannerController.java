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
import it.sayservice.platform.client.DomainObject;
import it.sayservice.platform.client.InvocationException;
import it.sayservice.platform.core.common.util.ServiceUtil;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.bson.types.ObjectId;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.trentorise.smartcampus.mobility.sync.BasicItinerary;
import eu.trentorise.smartcampus.mobility.sync.BasicRecurrentJourney;
import eu.trentorise.smartcampus.mobility.util.ConnectorException;
import eu.trentorise.smartcampus.mobility.util.HTTPConnector;
import eu.trentorise.smartcampus.resourceprovider.controller.SCController;
import eu.trentorise.smartcampus.resourceprovider.model.AuthServices;

@Controller
public class JourneyPlannerController extends SCController {

	@Autowired
	private AuthServices services;
	@Override
	protected AuthServices getAuthServices() {
		return services;
	}

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
			int its = 1;
			if (type.equals(TType.TRANSIT)) {
				its = 3;
			}
			String req = String.format("from=%s,%s&to=%s,%s&date=%s&departureTime=%s&transportType=%s&numOfItn=%s", request.getFrom().getLat(), request.getFrom().getLon(), request.getTo().getLat(), request.getTo().getLon(), request.getDate(), request.getDepartureTime(), type, its);
			reqs.add(req);
		}

		return reqs;
		// String[] resp = new String[request.getTransportTypes().length];
		// return reqs.toArray(resp);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/itinerary")
	public @ResponseBody
	BasicItinerary saveItinerary(HttpServletResponse response, @RequestBody BasicItinerary itinerary) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			Map<String, Object> pars = new HashMap<String, Object>();
			pars.put("itinerary", itinerary.getData());
			String clientId = itinerary.getClientId();
			if (clientId == null) {
				clientId = new ObjectId().toString();
			}
			pars.put("clientId", clientId);
			pars.put("userId", userId);
			pars.put("originalFrom", itinerary.getOriginalFrom());
			pars.put("originalTo", itinerary.getOriginalTo());
			pars.put("name", itinerary.getName());
			domainClient.invokeDomainOperation("saveItinerary", "smartcampus.services.journeyplanner.ItineraryFactory", "smartcampus.services.journeyplanner.ItineraryFactory.0", pars, userId, "vas_journeyplanner_subscriber");
			itinerary.setClientId(clientId);
			return itinerary;
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/itinerary")
	public @ResponseBody
	List<BasicItinerary> getItineraries(HttpServletResponse response) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("userId", userId);
			List<String> res = domainClient.searchDomainObjects("smartcampus.services.journeyplanner.ItineraryObject", pars, "vas_journeyplanner_subscriber");

			List<BasicItinerary> itineraries = new ArrayList<BasicItinerary>();

			for (String r : res) {
				DomainObject obj = new DomainObject(r);
				BasicItinerary itinerary = mapper.convertValue(obj.getContent(), BasicItinerary.class);
				itineraries.add(itinerary);
			}

			return itineraries;
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/itinerary/{itineraryId}")
	public @ResponseBody
	BasicItinerary getItinerary(HttpServletResponse response, @PathVariable String itineraryId) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			DomainObject res = getObjectByClientId(itineraryId, "smartcampus.services.journeyplanner.ItineraryObject");
			if (res == null) {
				return null;
			}

			if (checkUser(res, userId) == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			Map<String, Object> content = res.getContent();
			BasicItinerary itinerary = mapper.convertValue(content, BasicItinerary.class);

			return itinerary;
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/itinerary/{itineraryId}")
	public @ResponseBody
	Boolean deleteItinerary(HttpServletResponse response, @PathVariable String itineraryId) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			DomainObject res = getObjectByClientId(itineraryId, "smartcampus.services.journeyplanner.ItineraryObject");
			if (res == null) {
				return false;
			}

			if (checkUser(res, userId) == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			Map<String, Object> pars = new HashMap<String, Object>();
			pars.put("userId", userId);
			domainClient.invokeDomainOperation("deleteItinerary", "smartcampus.services.journeyplanner.ItineraryObject", res.getId(), pars, userId, "vas_journeyplanner_subscriber");
			return true;
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return false;
	}

	// no crud
	@RequestMapping(method = RequestMethod.GET, value = "/itinerary/{itineraryId}/monitor/{monitor}")
	public @ResponseBody
	Boolean monitorItinerary(HttpServletResponse response, @PathVariable String itineraryId, @PathVariable boolean monitor) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return false;
			}

			DomainObject res = getObjectByClientId(itineraryId, "smartcampus.services.journeyplanner.ItineraryObject");
			if (res == null) {
				return false;
			}

			if (checkUser(res, userId) == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			Map<String, Object> pars = new HashMap<String, Object>();
			pars.put("flag", monitor);
			pars.put("userId", userId);
			byte[] b = (byte[]) domainClient.invokeDomainOperationSync("setMonitorFlag", "smartcampus.services.journeyplanner.ItineraryObject", res.getId(), pars, "vas_journeyplanner_subscriber");
			String s = (String) ServiceUtil.deserializeObject(b);
			return Boolean.parseBoolean(s);
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return false;
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

	@RequestMapping(method = RequestMethod.POST, value = "/recurrent")
	public @ResponseBody
	BasicRecurrentJourney saveRecurrentJourney(HttpServletResponse response, @RequestBody BasicRecurrentJourney recurrent) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			Map<String, Object> pars = new HashMap<String, Object>();
			pars.put("recurrentJourney", recurrent.getData());
			pars.put("name", recurrent.getName());
			String clientId = recurrent.getClientId();
			if (clientId == null) {
				clientId = new ObjectId().toString();
			}
			pars.put("clientId", clientId);
			pars.put("userId", userId);
			pars.put("monitor", recurrent.isMonitor());
			domainClient.invokeDomainOperation("saveRecurrentJourney", "smartcampus.services.journeyplanner.RecurrentJourneyFactory", "smartcampus.services.journeyplanner.RecurrentJourneyFactory.0", pars, userId, "vas_journeyplanner_subscriber");
			recurrent.setClientId(clientId);
			return recurrent;
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.POST, value = "/recurrent/replan/{itineraryId}")
	public @ResponseBody
	RecurrentJourney planRecurrentJourney(HttpServletResponse response, @RequestBody RecurrentJourneyParameters parameters, @PathVariable String itineraryId) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			List<String> reqs = buildRecurrentJourneyPlannerRequest(parameters);
			List<SimpleLeg> legs = new ArrayList<SimpleLeg>();
			for (String req : reqs) {
				String plan = HTTPConnector.doGet(otpURL + SMARTPLANNER + RECURRENT, req, MediaType.APPLICATION_JSON, null, "UTF-8");
				List<?> sl = mapper.readValue(plan, List.class);
				for (Object o : sl) {
					legs.add((SimpleLeg) mapper.convertValue(o, SimpleLeg.class));
				}
			}

			DomainObject res = getObjectByClientId(itineraryId, "smartcampus.services.journeyplanner.RecurrentJourneyObject");
			if (res != null) {
				String objectId = checkUser(res, userId);
				if (objectId == null) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				} else {
					RecurrentJourney oldJourney = mapper.convertValue(res.getContent().get("data"), RecurrentJourney.class);
					RecurrentJourney journey = new RecurrentJourney();
					journey.setParameters(parameters);
					journey.setLegs(legs);
					journey.setMonitorLegs(buildMonitorMap(legs, oldJourney.getMonitorLegs()));
					return journey;
				}
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}

		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		return null;
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/recurrent/{itineraryId}")
	public @ResponseBody
	Boolean updateRecurrentJourney(HttpServletResponse response, @RequestBody BasicRecurrentJourney recurrent, @PathVariable String itineraryId) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			String objectClientId = recurrent.getClientId();
			if (!itineraryId.equals(objectClientId)) {
				response.setStatus(HttpServletResponse.SC_CONFLICT);
				return null;
			}
			DomainObject res = getObjectByClientId(itineraryId, "smartcampus.services.journeyplanner.RecurrentJourneyObject");
			if (res != null) {
				String objectId = checkUser(res, userId);
				if (objectId == null) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					return null;
				}

				Map<String, Object> pars = new HashMap<String, Object>();
				pars.put("newJourney", recurrent.getData());
				pars.put("newName", recurrent.getName());
				pars.put("newMonitor", recurrent.isMonitor());
				domainClient.invokeDomainOperation("updateRecurrentJourney", "smartcampus.services.journeyplanner.RecurrentJourneyObject", objectId, pars, userId, "vas_journeyplanner_subscriber");
				return true;
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
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

	private Map<String, Boolean> buildMonitorMap(List<SimpleLeg> legs, Map<String, Boolean> old) {
		Map<String, Boolean> result = new TreeMap<String, Boolean>();

		for (SimpleLeg leg : legs) {
			Transport transport = leg.getTransport();
			if (transport.getType() != TType.BUS && transport.getType() != TType.TRAIN) {
				continue;
			}
			String id = transport.getAgencyId() + "_" + transport.getRouteId();
			if (!result.containsKey(id)) {
				if (old.containsKey(id)) {
					result.put(id, old.get(id));
				} else {
					result.put(id, true);
				}
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

	@RequestMapping(method = RequestMethod.GET, value = "/recurrent")
	public @ResponseBody
	List<BasicRecurrentJourney> getRecurrentJourneys(HttpServletResponse response) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("userId", userId);
			List<String> res = domainClient.searchDomainObjects("smartcampus.services.journeyplanner.RecurrentJourneyObject", pars, "vas_journeyplanner_subscriber");

			List<BasicRecurrentJourney> journeys = new ArrayList<BasicRecurrentJourney>();
			for (String r : res) {
				DomainObject obj = new DomainObject(r);
				RecurrentJourney recurrent = mapper.convertValue(obj.getContent().get("data"), RecurrentJourney.class);
				BasicRecurrentJourney recurrentJourney = new BasicRecurrentJourney();
				String clientId = (String) obj.getContent().get("clientId");
				recurrentJourney.setData(recurrent);
				recurrentJourney.setClientId(clientId);
				recurrentJourney.setName((String) obj.getContent().get("name"));
				recurrentJourney.setMonitor((Boolean) obj.getContent().get("monitor"));
				recurrentJourney.setUser((String) obj.getContent().get("userId"));

				journeys.add(recurrentJourney);
			}

			return journeys;

		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/recurrent/{itineraryId}")
	public @ResponseBody
	BasicRecurrentJourney getRecurrentJourney(HttpServletResponse response, @PathVariable String itineraryId) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			DomainObject obj = getObjectByClientId(itineraryId, "smartcampus.services.journeyplanner.RecurrentJourneyObject");
			if (obj == null) {
				return null;
			}
			if (checkUser(obj, userId) == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			Map<String, Object> content = obj.getContent();
			RecurrentJourney recurrent = mapper.convertValue(content.get("data"), RecurrentJourney.class);
			BasicRecurrentJourney recurrentJourney = new BasicRecurrentJourney();
			String objectClientId = (String) content.get("clientId");
			if (!itineraryId.equals(objectClientId)) {
				response.setStatus(HttpServletResponse.SC_CONFLICT);
				return null;
			}
			recurrentJourney.setData(recurrent);
			recurrentJourney.setClientId(itineraryId);
			recurrentJourney.setName((String) obj.getContent().get("name"));
			recurrentJourney.setMonitor((Boolean) obj.getContent().get("monitor"));
			recurrentJourney.setUser((String) obj.getContent().get("userId"));

			return recurrentJourney;
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/recurrent/{itineraryId}")
	public @ResponseBody
	Boolean deleteRecurrentJourney(HttpServletResponse response, @PathVariable String itineraryId) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			DomainObject obj = getObjectByClientId(itineraryId, "smartcampus.services.journeyplanner.RecurrentJourneyObject");
			if (obj == null) {
				return null;
			}
			if (checkUser(obj, userId) == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			Map<String, Object> pars = new HashMap<String, Object>();
			pars.put("userId", userId);
			domainClient.invokeDomainOperation("deleteRecurrentJourney", "smartcampus.services.journeyplanner.RecurrentJourneyObject", obj.getId(), pars, userId, "vas_journeyplanner_subscriber");
			return true;
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	// no crud
	@RequestMapping(method = RequestMethod.GET, value = "/recurrent/{itineraryId}/monitor/{monitor}")
	public @ResponseBody
	Boolean monitorRecurrentJourney(HttpServletResponse response, @PathVariable String itineraryId, @PathVariable boolean monitor) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return false;
			}

			DomainObject obj = getObjectByClientId(itineraryId, "smartcampus.services.journeyplanner.RecurrentJourneyObject");
			if (obj == null) {
				return null;
			}
			if (checkUser(obj, userId) == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}
			Map<String, Object> pars = new HashMap<String, Object>();
			pars.put("flag", monitor);
			pars.put("userId", userId);
			byte[] b = (byte[]) domainClient.invokeDomainOperationSync("setMonitorFlag", "smartcampus.services.journeyplanner.RecurrentJourneyObject", obj.getId(), pars, "vas_journeyplanner_subscriber");
			String s = (String) ServiceUtil.deserializeObject(b);
			return Boolean.parseBoolean(s);

		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return false;
	}

	// ALERTS

	// no crud
	@RequestMapping(method = RequestMethod.POST, value = "/alert/user")
	public @ResponseBody
	void submitUserAlert(HttpServletResponse response, @RequestBody Map<String, Object> map) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			submitAlert(map, userId, null);
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

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

	private DomainObject getObjectByClientId(String id, String type) throws Exception {
		Map<String, Object> pars = new TreeMap<String, Object>();
		pars.put("clientId", id);
		List<String> res = domainClient.searchDomainObjects(type, pars, "vas_journeyplanner_subscriber");
		if (res == null || res.size() == 0) {
			return null;
		}
		return new DomainObject(res.get(0));
	}

	private String checkUser(DomainObject res, String userId) throws IOException, InvocationException {
		if (res == null || userId == null) {
			return null;
		}
		String objectId = res.getId();
		String resUserId = (String) res.getContent().get("userId");
		if (resUserId == null || !resUserId.equals(userId)) {
			return null;
		}
		return objectId;
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
		OAuth2Authentication auth = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
		return auth.getAuthorizationRequest().getClientId();
	}

}
