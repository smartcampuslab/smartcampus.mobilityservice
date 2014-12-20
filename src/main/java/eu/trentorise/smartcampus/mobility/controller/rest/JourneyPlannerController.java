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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import eu.trentorise.smartcampus.mobility.controller.extensions.ItineraryRequestEnricher;
import eu.trentorise.smartcampus.mobility.controller.extensions.PlanRequest;
import eu.trentorise.smartcampus.mobility.controller.extensions.PromotedJourneyRequestConverter;
import eu.trentorise.smartcampus.mobility.logging.StatLogger;
import eu.trentorise.smartcampus.mobility.processor.handlers.BikeSharingHandler;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.storage.RecurrentJourneyObject;
import eu.trentorise.smartcampus.mobility.sync.BasicItinerary;
import eu.trentorise.smartcampus.mobility.sync.BasicRecurrentJourney;
import eu.trentorise.smartcampus.mobility.util.ConnectorException;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;
import eu.trentorise.smartcampus.mobility.util.HTTPConnector;
import eu.trentorise.smartcampus.network.JsonUtils;
import eu.trentorise.smartcampus.resourceprovider.controller.SCController;
import eu.trentorise.smartcampus.resourceprovider.model.AuthServices;

@Controller
public class JourneyPlannerController extends SCController {

	@Autowired
	private StatLogger statLogger;
	private Logger logger = Logger.getLogger(this.getClass());

	
	@Autowired
	private AuthServices services;

	@Autowired
	private ItineraryRequestEnricher itineraryRequestEnricher;

	@Autowired
	private GamificationHelper gamificationHelper;

	@Autowired
	private PromotedJourneyRequestConverter promotedJourneyRequestConverter;

	@Override
	protected AuthServices getAuthServices() {
		return services;
	}

//	@Autowired
//	private DomainEngineClient domainClient;
	
	@Autowired
	private ExecutorService executorService;
	
	@Autowired
	private DomainStorage domainStorage;	

	@Autowired
	@Value("${otp.url}")
	private String otpURL;
	
	@Autowired
	private BikeSharingHandler bikeSharingCache;

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
			String userId = getUserId();
			statLogger.log(journeyRequest, userId);
			logger.info("-"+userId  + "~AppConsume~plan");

			Map<String, Itinerary> itineraryCache = new TreeMap<String, Itinerary>();

			promotedJourneyRequestConverter.modifyRequest(journeyRequest);
			
			List<PlanRequest> reqs = buildItineraryPlannerRequest(journeyRequest, true);
			Multimap<Integer, Itinerary> evalIts = ArrayListMultimap.create();

			List<Itinerary> itineraries = new ArrayList<Itinerary>();

			List<Future<PlanRequest>> results = Lists.newArrayList();
			Map<String, PlanRequest> reqMap = Maps.newTreeMap();
			for (PlanRequest pr : reqs) {
				reqMap.put(pr.getRequest(), pr);
			}
			
			for (PlanRequest pr : reqMap.values()) {
				CallableItineraryRequest callableReq = new CallableItineraryRequest();
				callableReq.setRequest(pr);
				Future<PlanRequest> future = executorService.submit(callableReq);
				results.add(future);
			}
			for (Future<PlanRequest> plan: results) {
				PlanRequest pr = plan.get();
				List<?> its = mapper.readValue(pr.getPlan(), List.class);
				for (Object it : its) {
					Itinerary itinerary = mapper.convertValue(it, Itinerary.class);
					pr.getItinerary().add(itinerary);
					if (pr.getValue() != 0) {
						itinerary.setPromoted(true);
						evalIts.put(pr.getValue(), itinerary);
					} else {
						itineraries.add(itinerary);
					}
					itineraryCache.put(pr.getRequest(), itinerary);
				}
			}
			
			
			List<Itinerary> evaluated = itineraryRequestEnricher.filterPromotedItineraties(evalIts, journeyRequest.getRouteType());
			itineraries.addAll(evaluated);

			itineraries = itineraryRequestEnricher.removeExtremeItineraties(itineraries, journeyRequest.getRouteType());

			ItinerarySorter.sort(itineraries, journeyRequest.getRouteType());

			itineraryRequestEnricher.completeResponse(journeyRequest, reqs, itineraries);

			promotedJourneyRequestConverter.promoteJourney(reqs);
			
			return itineraries;
//		} catch (ConnectorException e0) {
//			e0.printStackTrace();
//			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	private List<PlanRequest> buildItineraryPlannerRequest(SingleJourney request, boolean expand) {
		List<PlanRequest> reqsList = Lists.newArrayList();
		for (TType type : request.getTransportTypes()) {
			int minitn = 1;
			if (type.equals(TType.TRANSIT)) {
				minitn = 3;
			}
			int itn = Math.max(request.getResultsNumber(), minitn);			
			String req = String.format("from=%s,%s&to=%s,%s&date=%s&departureTime=%s&transportType=%s&numOfItn=%s", request.getFrom().getLat(), request.getFrom().getLon(), request.getTo().getLat(), request.getTo().getLon(), request.getDate(), request.getDepartureTime(), type, itn);
			PlanRequest pr = new PlanRequest();
			pr.setRequest(req);
			pr.setType(type);
			pr.setValue(0);
			reqsList.add(pr);
			if (expand) {
				reqsList.addAll(itineraryRequestEnricher.addPromotedItineraries(request, type));
			}
		}
		
		for (PlanRequest req: reqsList) {
			req.setOriginalRequest(request);
		}
		
		return reqsList;
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

			statLogger.log(itinerary, userId);
			gamificationHelper.saveItinerary(itinerary, userId);
			
			String clientId = itinerary.getClientId();
			
			if (clientId == null) {
				clientId = new ObjectId().toString();
			} else {
				Map<String, Object> pars = new TreeMap<String, Object>();
				pars.put("clientId", clientId);
				ItineraryObject res = domainStorage.searchDomainObject(pars, ItineraryObject.class);

				if (res != null && !userId.equals(res.getUserId())) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					return null;
				}
			}			
			
			ItineraryObject io = new ItineraryObject();
			
			io.setClientId(clientId);
			io.setUserId(userId);
			io.setOriginalFrom(itinerary.getOriginalFrom());
			io.setOriginalTo(itinerary.getOriginalTo());
			io.setName(itinerary.getName());
			io.setData(itinerary.getData());

			domainStorage.saveItinerary(io);
			itinerary.setClientId(clientId);
			return itinerary;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/itinerary")
	public @ResponseBody
	List<ItineraryObject> getItineraries(HttpServletResponse response) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("userId", userId);
			List<ItineraryObject> res = domainStorage.searchDomainObjects(pars, ItineraryObject.class);

			return res;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("clientId", itineraryId);
//			ItineraryObject res = domainStorage.searchDomainObjectFixForSpring(pars, ItineraryObject.class);
			ItineraryObject res = domainStorage.searchDomainObject(pars, ItineraryObject.class);

			if (!userId.equals(res.getUserId())) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			return res;
//			BasicItinerary itinerary = mapper.convertValue(res, BasicItinerary.class);
//			return itinerary;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("clientId", itineraryId);
			ItineraryObject res = domainStorage.searchDomainObject(pars, ItineraryObject.class);

			if (!userId.equals(res.getUserId())) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			domainStorage.deleteItinerary(itineraryId);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return false;
	}	
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/itinerary/{itineraryId}/monitor/{monitor}")
	public @ResponseBody
	Boolean monitorItinerary(HttpServletResponse response, @PathVariable String itineraryId, @PathVariable boolean monitor) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return false;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("clientId", itineraryId);
			ItineraryObject res = domainStorage.searchDomainObject(pars, ItineraryObject.class);

			if (!userId.equals(res.getUserId())) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			res.setMonitor(monitor);
			
			domainStorage.saveItinerary(res);
			
			return monitor;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
			logger.info("-"+userId  + "~AppConsume~monitor");

			String clientId = recurrent.getClientId();
			
			if (clientId == null) {
				clientId = new ObjectId().toString();
			} else {
				Map<String, Object> pars = new TreeMap<String, Object>();
				pars.put("clientId", clientId);
				RecurrentJourneyObject res = domainStorage.searchDomainObject(pars, RecurrentJourneyObject.class);

				if (res != null && !userId.equals(res.getUserId())) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					return null;
				}	
			}
			
			RecurrentJourneyObject rec = new RecurrentJourneyObject();
			rec.setData(recurrent.getData());
			rec.setName(recurrent.getName());
			rec.setUserId(userId);
			rec.setMonitor(recurrent.isMonitor());
			rec.setClientId(clientId);
			
			domainStorage.saveRecurrent(rec);
			
			recurrent.setClientId(clientId);
			return recurrent;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("clientId", itineraryId);
			RecurrentJourneyObject res = domainStorage.searchDomainObject(pars, RecurrentJourneyObject.class);	
			
			if (res != null) {
				if (!userId.equals(res.getUserId())) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					return null;
				}	 else {
					RecurrentJourney oldJourney = res.getData();
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
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("clientId", itineraryId);
			RecurrentJourneyObject res = domainStorage.searchDomainObjectFixForSpring(pars, RecurrentJourneyObject.class);				
			
			if (res != null) {
				if (!userId.equals(res.getUserId())) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					return null;
				}
				
				res.setData(recurrent.getData());
				res.setName(recurrent.getName());
				res.setMonitor(recurrent.isMonitor());
				
				domainStorage.saveRecurrent(res);

				return true;
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
	List<RecurrentJourneyObject> getRecurrentJourneys(HttpServletResponse response) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("userId", userId);
			List<RecurrentJourneyObject> res = domainStorage.searchDomainObjects(pars, RecurrentJourneyObject.class);
			
			return res;

		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}	
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/recurrent/{itineraryId}")
	public @ResponseBody
	RecurrentJourneyObject getRecurrentJourney(HttpServletResponse response, @PathVariable String itineraryId) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("clientId", itineraryId);
			RecurrentJourneyObject res = domainStorage.searchDomainObjectFixForSpring(pars, RecurrentJourneyObject.class);

			if (!userId.equals(res.getUserId())) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}
			return res;
//			BasicRecurrentJourney recurrent = mapper.convertValue(res, BasicRecurrentJourney.class);
//			
//			return recurrent;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("clientId", itineraryId);
			RecurrentJourneyObject res = domainStorage.searchDomainObject(pars, RecurrentJourneyObject.class);

			if (!userId.equals(res.getUserId())) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			domainStorage.deleteRecurrent(itineraryId);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return false;
	}	


	@RequestMapping(method = RequestMethod.GET, value = "/recurrent/{itineraryId}/monitor/{monitor}")
	public @ResponseBody
	Boolean monitorRecurrentJourney(HttpServletResponse response, @PathVariable String itineraryId, @PathVariable boolean monitor) throws InvocationException {
		try {
		String userId = getUserId();
		if (userId == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return null;
		}

		Map<String, Object> pars = new TreeMap<String, Object>();
		pars.put("clientId", itineraryId);
		RecurrentJourneyObject res = domainStorage.searchDomainObject(pars, RecurrentJourneyObject.class);

		if (!userId.equals(res.getUserId())) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return null;
		}
		
			res.setMonitor(monitor);
			domainStorage.saveRecurrent(res);
			return monitor;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
			statLogger.log(map, getUserId());

			submitAlert(map, userId, null);
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(method = RequestMethod.POST, value = "/alert/service")
	public @ResponseBody
	void submitServiceAlert(HttpServletResponse response, @RequestBody Map<String, Object> map) throws InvocationException {
		try {
			submitAlert(map, null, getClientId());
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private void submitAlert(Map<String, Object> map, String userId, String clientId) throws InvocationException {
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
			if (userId != null) logger.info("-"+userId  + "~AppProsume~delay=" + ((AlertDelay)alert).getTransport().getAgencyId());
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
		
		// TODO !!!
//		domainClient.invokeDomainOperation(method, "smartcampus.services.journeyplanner.AlertFactory", "smartcampus.services.journeyplanner.AlertFactory.0", pars, userId, "vas_journeyplanner_subscriber");
	}

	// /////////////////////////////////////////////////////////////////////////////

	@RequestMapping(method = RequestMethod.GET, value = "/getparkingsbyagency/{agencyId}")
	public @ResponseBody
	void getParkingsByAgency(HttpServletResponse response, @PathVariable String agencyId) throws InvocationException {
		try {
			String address = otpURL + SMARTPLANNER + "getParkingsByAgency?agencyId=" + agencyId;

			String routes = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");

			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(routes);

		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(method = RequestMethod.GET, value = "/bikesharing/{comune}")
	public @ResponseBody
	void bikeSharingByComune(HttpServletResponse response, @PathVariable String comune) throws InvocationException {
		response.setContentType("application/json; charset=utf-8");
		try {
			response.getWriter().write(JsonUtils.toJSON(bikeSharingCache.getStations(comune)));
		} catch (IOException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/getbikesharingbyagency/{agencyId}")
	public @ResponseBody
	void getBikeSharingByAgency(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String agencyId) throws InvocationException {
		try {
			String address = otpURL + SMARTPLANNER + "getBikeSharingByAgency?agencyId=" + agencyId;

			String routes = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");

			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(routes);

		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(method = RequestMethod.GET, value = "/getroadinfobyagency/{agencyId}/{from}/{to}")
	public @ResponseBody
	void getRoadInfoByAgency(HttpServletResponse response, @PathVariable String agencyId, @PathVariable Long from, @PathVariable Long to) throws InvocationException {
		try {
			String address = otpURL + SMARTPLANNER + "getAR?agencyId=" + agencyId + "&from=" + from + "&to=" + to;

			String roadInfo = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");

			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(roadInfo);

		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * @return UserDetails instance from security context
	 */
	protected String getClientId() {
		OAuth2Authentication auth = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
		return auth.getAuthorizationRequest().getClientId();
	}

	@Override
	protected String getUserId() {
		try {
			return super.getUserId();
		} catch (Exception e) {
			return null;
		}
	}
	
	private class CallableItineraryRequest implements Callable<PlanRequest> {
		
		private PlanRequest request;
		
		public void setRequest(PlanRequest req) {
			this.request = req;
		}
		
		@Override
		public PlanRequest call() throws Exception {
			String plan =  HTTPConnector.doGet(otpURL + SMARTPLANNER + PLAN, request.getRequest(), MediaType.APPLICATION_JSON, null, "UTF-8");
			request.setPlan(plan);
			return request;
		}};		
		

		
}
