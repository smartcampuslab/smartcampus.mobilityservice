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

package eu.trentorise.smartcampus.mobility.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningRequest;
import eu.trentorise.smartcampus.mobility.controller.extensions.compilable.CompilablePolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.compilable.CompilablePolicyData;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.util.HTTPConnector;
import eu.trentorise.smartcampus.network.JsonUtils;
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
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourney;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourneyParameters;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;
import it.sayservice.platform.smartplanner.data.message.otpbeans.BikeStation;
import it.sayservice.platform.smartplanner.data.message.otpbeans.GeolocalizedStopRequest;
import it.sayservice.platform.smartplanner.data.message.otpbeans.Stop;

/**
 * @author raman
 *
 */
@Component
public class SmartPlannerService implements SmartPlannerHelper {

	private static final String STATION_NOT_PRESENT_IN_REPOSITORY = "station not present in repository";

	@Autowired
	@Value("${smartplanner.router}")
	private String smartplannerRouter;		
	
	private static final String DUMMY = "Nessuna";
	private static final String DEFAULT = "default";
	
	private String smartplannerRest;

	private static final String PLAN = "plan";
	private static final String RECURRENT = "recurrentJourney";


	@Autowired
	@Value("${smartplannerURL}")
	private String smartplannerURL;	
	
	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Resource(name="basicPoliciesMap")
	private Map<String, PlanningPolicy> policiesMap;
	
	@Autowired
	private ExecutorService executorService;
	
	@Autowired
	private DomainStorage storage;
	
	private static final Logger logger = LoggerFactory.getLogger(SmartPlannerService.class);
	
	@PostConstruct
	public void init() {
		smartplannerRest = "/" + smartplannerRouter + "/rest/";
	}
	
	@Override
	public Map<String, PlanningPolicy> getPolicies(Boolean draft) {
		Map<String, PlanningPolicy> result = Maps.newHashMap(); 
		//newHashMap(policiesMap);
		result.putAll(getStoredPolicies(draft));
		return result;
	}
	
	private Map<String, PlanningPolicy> getStoredPolicies(Boolean draft) {
		 Map<String, PlanningPolicy> result = Maps.newTreeMap();
		 Criteria criteria = new Criteria();
		 if (draft != null) {
			 criteria.and("draft").is(draft);
		 }

		List<CompilablePolicyData> compilable = storage.searchDomainObjects(criteria, CompilablePolicyData.class);
		for (CompilablePolicyData policy: compilable) {
			result.put(policy.getPolicyId(), new CompilablePolicy(policy));
		}		
		return result;
	}
	
	
	private PlanningPolicy getPlanningPolicy(String policyId, Boolean draft) {
		PlanningPolicy policy = policiesMap.get(policyId);
		
		if (policy == null) {
			Map<String, PlanningPolicy> stored = getStoredPolicies(draft);
			
			policy = stored.get(policyId);
			if (policy == null) {
				return policiesMap.get(DUMMY);
			}
		}
		
		return policy; 

	}

	private String performGET(String request, String query) throws Exception {
		return HTTPConnector.doGet(smartplannerURL+request, query, MediaType.APPLICATION_JSON_VALUE, null, "UTF-8");
	}

	private String performPOST(String request, String body) throws Exception {
		return HTTPConnector.doPost(smartplannerURL+request, body, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE);
	}

	@Override
	public String stopTimetable(String agencyId, String routeId, String stopId) throws Exception {
		return performGET(smartplannerRest + "gettimetable/" + agencyId + "/" + routeId + "/" + stopId, null);
	}

	
	@Override
	public String stopTimetable(String agencyId, String stopId, Integer maxResults) throws Exception {
		return performGET(smartplannerRest + "getlimitedtimetable/" + agencyId + "/" + stopId + "/" + maxResults, null);
	}

	@Override
	public String transitTimes(String agencyId, String routeId, Long from, Long to) throws Exception {
		return performGET(smartplannerRest + "getTransitTimes/" + agencyId + "/" + routeId + "/" + from + "/" + to, null);
	}
	
	@Override
	public String extendedTransitTimes(String agencyId, String routeId, Long from, Long to) throws Exception {
		return performGET(smartplannerRest + "getTransitTimes/" + agencyId + "/" + routeId + "/" + from + "/" + to + "/extended", null);
	}
	

	
	@Override
	public String delays(String agencyId, String routeId, Long from, Long to) throws Exception {
		return performGET(smartplannerRest + "getTransitDelays/" + agencyId + "/" + routeId + "/" + from + "/" + to, null);
	}
	
	@Override
	public RecurrentJourney planRecurrent(RecurrentJourneyParameters parameters) throws Exception {
		List<String> reqs = buildRecurrentJourneyPlannerRequest(parameters);
		List<SimpleLeg> legs = new ArrayList<SimpleLeg>();
		for (String req : reqs) {
			String plan = performGET(smartplannerRest + RECURRENT, req);
			List<?> sl = mapper.readValue(plan, List.class);
			for (Object o : sl) {
				legs.add(mapper.convertValue(o, SimpleLeg.class));
			}
		}

		RecurrentJourney journey = new RecurrentJourney();
		journey.setParameters(parameters);
		journey.setLegs(legs);
		journey.setMonitorLegs(buildMonitorMap(legs));
		return journey;
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

	@Override
	public RecurrentJourney replanRecurrent(RecurrentJourneyParameters parameters, RecurrentJourney oldJourney) throws Exception {
		List<String> reqs = buildRecurrentJourneyPlannerRequest(parameters);
		List<SimpleLeg> legs = new ArrayList<SimpleLeg>();
		for (String req : reqs) {
			String plan = performGET(smartplannerRest + RECURRENT, req); 
			List<?> sl = mapper.readValue(plan, List.class);
			for (Object o : sl) {
				legs.add((SimpleLeg) mapper.convertValue(o, SimpleLeg.class));
			}
		}
		RecurrentJourney journey = new RecurrentJourney();
		journey.setParameters(parameters);
		journey.setLegs(legs);
		journey.setMonitorLegs(buildMonitorMap(legs, oldJourney.getMonitorLegs()));
		return journey;

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

	@Override
	public String parkingsByAgency(String agencyId) throws Exception {
		return performGET(smartplannerRest + "getParkingsByAgency?agencyId=" + agencyId, null);
	}
	
	@Override
	public String bikeStations() throws Exception {
		return performGET(smartplannerRest + "getBikeStations", null);
	}	

	@Override
	public String bikeSharingByAgency(String agencyId) throws Exception {
		return performGET(smartplannerRest + "getBikeSharingByAgency?agencyId=" + agencyId, null);
	}

	@Override
	public void addBikeSharingStations(List<BikeStation> stations) throws Exception {
		String body = mapper.writeValueAsString(stations);
		performPOST(smartplannerRest + "data/bikesharing", body);
	}
	
	@Override
	public String roadInfoByAgency(String agencyId, Long from, Long to) throws Exception {
		return performGET(smartplannerRest + "getAR?agencyId=" + agencyId + "&from=" + from + "&to=" + to, null);
	}
	@Override
	public String routes(String agencyId) throws Exception {
		return performGET(smartplannerRest + "getroutes/" + agencyId, null);
	}

	@Override
	public String stops(String agencyId, String routeId) throws Exception {
		return performGET(smartplannerRest + "getstops/" + agencyId + "/" + routeId, null);
	}

	@Override
	public String stops(String agencyId, String routeId, double latitude, double longitude, double radius) throws Exception {
		return performGET(smartplannerRest + "getstops/" + agencyId + "/" + routeId + "/" + latitude + "/" + longitude + "/" + radius, null);
	}
	
	@Override
	public List<Stop> stops(String agencyId, double lat, double lng, double radius, Integer page, Integer count) throws Exception {
		GeolocalizedStopRequest gsr = new GeolocalizedStopRequest();
		gsr.setAgencyId(agencyId);
		gsr.setCoordinates(new double[]{lat,lng});
		gsr.setRadius(radius);
		gsr.setPageSize(count == null ? 100 : count);
		gsr.setPageNumber(page == null ? 0 : page);
		String content = mapper.writeValueAsString(gsr);

		String res = performPOST(smartplannerRest + "getGeolocalizedStops", content);
		return JsonUtils.toObjectList(res, Stop.class);

	}

	@Override
	public synchronized List<Itinerary> planSingleJourney(SingleJourney journeyRequest, String policyId) throws Exception {
		// TODO: final only? draft?
		PlanningPolicy planningPolicy = getPlanningPolicy(policyId, null);

		List<PlanningRequest> planRequests = planningPolicy.generatePlanRequests(journeyRequest);
		List<PlanningRequest> successfulPlanRequests = Lists.newArrayList();
		
		int iteration = 0;
		do {

			List<Future<PlanningRequest>> results = Lists.newArrayList();

			for (PlanningRequest pr : planRequests) {
				CallablePlanningRequest callableReq = new CallablePlanningRequest();
				callableReq.setRequest(pr);
				Future<PlanningRequest> future = executorService.submit(callableReq);
				results.add(future);
			}

			for (Future<PlanningRequest> plan : results) {
				PlanningRequest pr = plan.get();
				List<?> its = mapper.readValue(pr.getPlan(), List.class);
				for (Object it : its) {
					Itinerary itinerary = mapper.convertValue(it, Itinerary.class);
					pr.getItinerary().add(itinerary);
					itinerary.setPromoted(pr.isPromoted());
				}
			}

			List<PlanningRequest> evaluated = planningPolicy.evaluatePlanResults(planRequests);
			successfulPlanRequests.addAll(evaluated);
			
			iteration++;
			for (PlanningRequest pr: planRequests) {
				pr.setIteration(iteration);
			}
		} while (!planRequests.isEmpty() && iteration < 2);

		List<Itinerary> itineraries = planningPolicy.extractItinerariesFromPlanResults(journeyRequest, successfulPlanRequests);
		
		List<Itinerary> sortedItineraries = planningPolicy.filterAndSortItineraries(journeyRequest, itineraries);
		
		return sortedItineraries; 
	}
	
	private class CallablePlanningRequest implements Callable<PlanningRequest> {
		
		private PlanningRequest request;
		
		public void setRequest(PlanningRequest req) {
			this.request = req;
		}
		
		@Override
		public PlanningRequest call() throws Exception {
			try {
				String plan = performGET(smartplannerRest + PLAN, request.getRequest());
				request.setPlan(plan);
			} catch (Exception e) {
				e.printStackTrace();
				request.setPlan(null);
			}
			return request;
		}
	}	

	@Override
	public void sendAlert(Alert alert) throws Exception {
		String req = mapper.writeValueAsString(alert);
		String param = null;
		if (alert instanceof AlertDelay) {
			param = "updateAD";
		} else if (alert instanceof AlertAccident) {
			param = "updateAE";
		} else if (alert instanceof AlertStrike) {
			param = "updateAS";
		} else if (alert instanceof AlertParking) {
			param = "updateAP";
		} else if (alert instanceof AlertRoad) {
			param = "updateAR";
		} else {
			throw new IllegalArgumentException("Unknown alert type "+alert.getClass().getName());
		}
		
		String url = smartplannerURL + smartplannerRest + param;
		logger.info("Sending alert to " + url);
		String result = HTTPConnector.doPost(url, req, MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE);
//		logger.info(result);				
		processAlerResult(alert, result);
	}	

	private void processAlerResult(Alert alert, String result) {
		if (result == null) {
			
		} else if (result.contains(STATION_NOT_PRESENT_IN_REPOSITORY)) {
			AlertParking ap = (AlertParking)alert;
			
			if (ap.getPlace() != null && ap.getPlace().getAgencyId() != null) {
				if (ap.getPlace().getAgencyId().contains("BIKE_SHARING")) {
					BikeStation bs = new BikeStation(ap.getPlace(), "BIKE-RENTAL", 0, 0, ap.getNoOfvehicles(), ap.getPlacesAvailable());
				}
			}
			
			System.err.println(ap.getId() + " / " + ap.getDescription());
		}
	}
	
	@Override
	public InputStream routesDB(String appId) throws Exception {
		return HTTPConnector.doStreamGet(smartplannerURL + smartplannerRest + "routesDB/" + appId, null, "application/zip", null);
	}

	@Override
	public InputStream extendedRoutesDB(String appId) throws Exception {
		return HTTPConnector.doStreamGet(smartplannerURL + smartplannerRest + "routesDB/" + appId + "/extended", null, "application/zip", null);
	}	
	
	@Override
	public String getVersions() throws Exception {
		return performGET(smartplannerRest + "versions", null);
	}

	@Override
	public String getTaxiStations(double latitude, double longitude, double radius) throws Exception {

		String response = null;
		double defaultRadius = 500;
		if (latitude > -1 && longitude > -1) {

			if (radius > -1) {
				defaultRadius = radius;
			}

			response = performGET(
					smartplannerRest + "taxisNearPoint?lat=" + latitude + "&lon=" + longitude + "&radius=" + defaultRadius, null);
		}
		return response;

	}

	@Override
	public String getAllTaxiStations() throws Exception {

		String response = null;

		response = performGET(smartplannerRest + "taxis",	null);

		return response;

	}
	
	
	@Override
	public String getTaxiAgencyContacts() throws Exception {

		String response = null;

		response = performGET(smartplannerRest + "taxi/contacts/",	null);

		return response;

	}	
	
	
	@Override
	public String getAgencyTaxiStations(String agencyId) throws Exception {
		String response = null;

		response = performGET(smartplannerRest + "taxis/"+agencyId,	null);

		return response;
	}

	@Override
	public String getTaxiAgencyContacts(String agencyId) throws Exception {
		String response = null;

		response = performGET(smartplannerRest + "taxi/contacts/"+agencyId,	null);

		return response;
	}

	@Override
	public InputStream gtfs(String agencyId) throws Exception {
		return HTTPConnector.doStreamGet(smartplannerURL + smartplannerRest + "gtfs/" + agencyId, null, "application/zip", null);
	}	
	

	
}
