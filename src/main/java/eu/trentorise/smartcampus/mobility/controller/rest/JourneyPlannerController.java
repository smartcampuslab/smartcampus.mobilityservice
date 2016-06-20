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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.trentorise.smartcampus.mobility.gamification.model.SavedTrip;
import eu.trentorise.smartcampus.mobility.logging.StatLogger;
import eu.trentorise.smartcampus.mobility.model.BasicItinerary;
import eu.trentorise.smartcampus.mobility.model.BasicRecurrentJourney;
import eu.trentorise.smartcampus.mobility.service.AlertSender;
import eu.trentorise.smartcampus.mobility.service.NotificationHelper;
import eu.trentorise.smartcampus.mobility.service.SmartPlannerHelper;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.storage.RecurrentJourneyObject;
import eu.trentorise.smartcampus.mobility.util.ConnectorException;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;
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
	private GamificationHelper gamificationHelper;

	@Override
	protected AuthServices getAuthServices() {
		return services;
	}

	@Autowired
	private DomainStorage domainStorage;	

	@Autowired
	private SmartPlannerHelper smartPlannerHelper;
	
	@Autowired
	private AlertSender alertSender;

	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	// no crud
	@RequestMapping(method = RequestMethod.POST, value = "/plansinglejourney")
	public @ResponseBody
	List<Itinerary> planSingleJourney(HttpServletResponse response, @RequestBody SingleJourney journeyRequest, @RequestParam(required = false) String policyId, @RequestHeader(required=false,value="UserID") String userId, @RequestHeader(required=false,value="AppName") String appName) throws InvocationException {
		try {
			domainStorage.savePlanRequest(journeyRequest, userId, appName);
			
			String userFromToken = getUserId();
			statLogger.log(journeyRequest, userFromToken);
			logger.info("-"+userId  + "~AppConsume~plan");

			List<Itinerary> results = smartPlannerHelper.planSingleJourney(journeyRequest, policyId);
			for (Itinerary itinerary: results) {
				gamificationHelper.computeEstimatedGameScore(itinerary, false);
			}
			return results;
		} catch (Exception e) {
			e.printStackTrace();
			response.addHeader("error_msg", e.getMessage());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
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
			if (itinerary.getAppId() == null || itinerary.getAppId().isEmpty()) {
				io.setAppId(NotificationHelper.MS_APP);
			} else {
				io.setAppId(itinerary.getAppId());
			}
			io.setRecurrency(itinerary.getRecurrency());

			domainStorage.saveItinerary(io);
			
			SavedTrip st = new SavedTrip(new Date(), io, RequestMethod.POST.toString());
			domainStorage.saveSavedTrips(st);
			
			itinerary.setClientId(clientId);
			return itinerary;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/itinerary/{itineraryId}")
	public @ResponseBody
	Boolean updateItinerary(HttpServletResponse response, @RequestBody BasicItinerary itinerary, @PathVariable String itineraryId) throws InvocationException {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			String objectClientId = itinerary.getClientId();
			if (!itineraryId.equals(objectClientId)) {
				response.setStatus(HttpServletResponse.SC_CONFLICT);
				return null;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("clientId", itineraryId);
			
			ItineraryObject res = domainStorage.searchDomainObject(pars, ItineraryObject.class);				
			
			if (res != null) {
				if (!userId.equals(res.getUserId())) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					return null;
				}
				
				res.setClientId(itinerary.getClientId());
				res.setUserId(userId);
				res.setOriginalFrom(itinerary.getOriginalFrom());
				res.setOriginalTo(itinerary.getOriginalTo());
				res.setName(itinerary.getName());
				res.setData(itinerary.getData());
				if (itinerary.getAppId() == null || itinerary.getAppId().isEmpty()) {
					res.setAppId(NotificationHelper.MS_APP);
				} else {
					res.setAppId(itinerary.getAppId());
				}
				res.setRecurrency(itinerary.getRecurrency());
				
				domainStorage.saveItinerary(res);
				
				SavedTrip st = new SavedTrip(new Date(), res, RequestMethod.PUT.toString());
				domainStorage.saveSavedTrips(st);				

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
			if (res != null && !userId.equals(res.getUserId())) {
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

			if (res == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return false;
			}
			
			if (!userId.equals(res.getUserId())) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			domainStorage.deleteItinerary(itineraryId);
			
			SavedTrip st = new SavedTrip(new Date(), res, RequestMethod.DELETE.toString());
			domainStorage.saveSavedTrips(st);							
			
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

			if (res == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return false;
			}			
			
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
			return smartPlannerHelper.planRecurrent(parameters);
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
			if (recurrent.getAppId() == null || recurrent.getAppId().isEmpty()) {
				rec.setAppId(NotificationHelper.MS_APP);
			} else {
				rec.setAppId(recurrent.getAppId());
			}
			
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

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("clientId", itineraryId);
			RecurrentJourneyObject res = domainStorage.searchDomainObject(pars, RecurrentJourneyObject.class);	

			if (res != null) {
				if (!userId.equals(res.getUserId())) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					return null;
				}	 else {
					RecurrentJourney oldJourney = res.getData();
					return smartPlannerHelper.replanRecurrent(parameters,oldJourney);
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
//			RecurrentJourneyObject res = domainStorage.searchDomainObjectFixForSpring(pars, RecurrentJourneyObject.class);				
			RecurrentJourneyObject res = domainStorage.searchDomainObject(pars, RecurrentJourneyObject.class);				
			
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
//			RecurrentJourneyObject res = domainStorage.searchDomainObjectFixForSpring(pars, RecurrentJourneyObject.class);
			RecurrentJourneyObject res = domainStorage.searchDomainObject(pars, RecurrentJourneyObject.class);

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

			if (res == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return false;
			}			
			
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

		if (res == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return false;
		}		
		
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
		Map<String, Object> contentMap = map;
		switch (type) {
		case ACCIDENT:
			alert = mapper.convertValue(contentMap, AlertAccident.class);
			break;
		case DELAY:
			alert = mapper.convertValue(contentMap, AlertDelay.class);
			if (userId != null) logger.info("-"+userId  + "~AppProsume~delay=" + ((AlertDelay)alert).getTransport().getAgencyId());
			break;
		case PARKING:
			alert = mapper.convertValue(contentMap, AlertParking.class);
			break;
		case STRIKE:
			alert = mapper.convertValue(contentMap, AlertStrike.class);
			break;
		case ROAD:
			alert = mapper.convertValue(contentMap, AlertRoad.class);
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
		alertSender.publishAlert(alert);
	}

	// /////////////////////////////////////////////////////////////////////////////

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
		
}
