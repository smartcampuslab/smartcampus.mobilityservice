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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;

import eu.trentorise.smartcampus.mobility.gamification.GamificationValidator;
import eu.trentorise.smartcampus.mobility.gamification.model.SavedTrip;
import eu.trentorise.smartcampus.mobility.logging.StatLogger;
import eu.trentorise.smartcampus.mobility.model.BasicItinerary;
import eu.trentorise.smartcampus.mobility.model.BasicRecurrentJourney;
import eu.trentorise.smartcampus.mobility.model.RouteMonitoring;
import eu.trentorise.smartcampus.mobility.service.AlertSender;
import eu.trentorise.smartcampus.mobility.service.NotificationHelper;
import eu.trentorise.smartcampus.mobility.service.SmartPlannerHelper;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.storage.RecurrentJourneyObject;
import eu.trentorise.smartcampus.mobility.storage.RouteMonitoringObject;
import eu.trentorise.smartcampus.mobility.util.ConnectorException;
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

@Controller
public class JourneyPlannerController {

	@Autowired
	private StatLogger statLogger;
	private Logger logger = Logger.getLogger(this.getClass());

	@Autowired
	private GamificationValidator gamificationValidator;

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
	public @ResponseBody List<Itinerary> planSingleJourney(HttpServletResponse response, @RequestBody(required=false) SingleJourney journeyRequest, @RequestParam(required = false, defaultValue="default") String policyId,
			@RequestHeader(required = false, value = "UserID") String userId, @RequestHeader(required = false, value = "AppName") String appName) throws Exception {
		try {
			domainStorage.savePlanRequest(journeyRequest, userId, appName);

			String userFromToken = getUserId();
			statLogger.log(journeyRequest, userFromToken);
			logger.info("-" + userId + "~AppConsume~plan");

			List<Itinerary> results = smartPlannerHelper.planSingleJourney(journeyRequest, policyId);
			for (Itinerary itinerary : results) {
				gamificationValidator.computeEstimatedGameScore(appName, itinerary, null, false);
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
	public @ResponseBody BasicItinerary saveItinerary(HttpServletResponse response, @RequestBody(required=false) BasicItinerary itinerary) throws Exception {
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
	public @ResponseBody Boolean updateItinerary(HttpServletResponse response, @RequestBody(required=false) BasicItinerary itinerary, @PathVariable String itineraryId) throws Exception {
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
	public @ResponseBody List<ItineraryObject> getItineraries(HttpServletResponse response) throws Exception {
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
	public @ResponseBody BasicItinerary getItinerary(HttpServletResponse response, @PathVariable String itineraryId) throws Exception {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("clientId", itineraryId);
			// ItineraryObject res =
			// domainStorage.searchDomainObjectFixForSpring(pars,
			// ItineraryObject.class);
			ItineraryObject res = domainStorage.searchDomainObject(pars, ItineraryObject.class);
			if (res != null && !userId.equals(res.getUserId())) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			return res;
			// BasicItinerary itinerary = mapper.convertValue(res,
			// BasicItinerary.class);
			// return itinerary;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/itinerary/{itineraryId}")
	public @ResponseBody Boolean deleteItinerary(HttpServletResponse response, @PathVariable String itineraryId) throws Exception {
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
	public @ResponseBody Boolean monitorItinerary(HttpServletResponse response, @PathVariable String itineraryId, @PathVariable boolean monitor) throws Exception {
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
	public @ResponseBody RecurrentJourney planRecurrentJourney(HttpServletResponse response, @RequestBody(required=false) RecurrentJourneyParameters parameters) throws Exception {
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
	public @ResponseBody BasicRecurrentJourney saveRecurrentJourney(HttpServletResponse response, @RequestBody(required=false) BasicRecurrentJourney recurrent) throws Exception {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}
			logger.info("-" + userId + "~AppConsume~monitor");

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
	public @ResponseBody RecurrentJourney planRecurrentJourney(HttpServletResponse response, @RequestBody(required=false) RecurrentJourneyParameters parameters, @PathVariable String itineraryId)
			throws Exception {
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
				} else {
					RecurrentJourney oldJourney = res.getData();
					return smartPlannerHelper.replanRecurrent(parameters, oldJourney);
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
	public @ResponseBody Boolean updateRecurrentJourney(HttpServletResponse response, @RequestBody(required=false) BasicRecurrentJourney recurrent, @PathVariable String itineraryId) throws Exception {
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
			// RecurrentJourneyObject res =
			// domainStorage.searchDomainObjectFixForSpring(pars,
			// RecurrentJourneyObject.class);
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
	public @ResponseBody List<RecurrentJourneyObject> getRecurrentJourneys(HttpServletResponse response) throws Exception {
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
	public @ResponseBody RecurrentJourneyObject getRecurrentJourney(HttpServletResponse response, @PathVariable String itineraryId) throws Exception {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("clientId", itineraryId);
			// RecurrentJourneyObject res =
			// domainStorage.searchDomainObjectFixForSpring(pars,
			// RecurrentJourneyObject.class);
			RecurrentJourneyObject res = domainStorage.searchDomainObject(pars, RecurrentJourneyObject.class);

			if (!userId.equals(res.getUserId())) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}
			return res;
			// BasicRecurrentJourney recurrent = mapper.convertValue(res,
			// BasicRecurrentJourney.class);
			//
			// return recurrent;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/recurrent/{itineraryId}")
	public @ResponseBody Boolean deleteRecurrentJourney(HttpServletResponse response, @PathVariable String itineraryId) throws Exception {
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
	public @ResponseBody Boolean monitorRecurrentJourney(HttpServletResponse response, @PathVariable String itineraryId, @PathVariable boolean monitor) throws Exception {
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
	public @ResponseBody void submitUserAlert(HttpServletResponse response, @RequestBody(required=false) Map<String, Object> map) throws Exception {
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

//	@RequestMapping(method = RequestMethod.POST, value = "/alert/service")
//	public @ResponseBody void submitServiceAlert(HttpServletResponse response, @RequestBody(required=false) Map<String, Object> map) throws Exception {
//		try {
//			submitAlert(map, null, getClientId());
//		} catch (Exception e) {
//			e.printStackTrace();
//			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//		}
//	}

	@RequestMapping(method = RequestMethod.POST, value = "/monitorroute")
	public @ResponseBody RouteMonitoring saveMonitorRoutes(HttpServletResponse response, @RequestBody(required=false) RouteMonitoring req) throws Exception {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			String clientId = req.getClientId();
			if (clientId == null) {
				clientId = new ObjectId().toString();
				req.setClientId(clientId);
			}	else {
					Map<String, Object> pars = new TreeMap<String, Object>();
					pars.put("clientId", clientId);
					RouteMonitoringObject res = domainStorage.searchDomainObject(pars, RouteMonitoringObject.class);

					if (res != null && !userId.equals(res.getUserId())) {
						response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						return null;
					}
				}

			RouteMonitoringObject obj = new RouteMonitoringObject(req);
			obj.setUserId(userId);

			domainStorage.saveRouteMonitoring(obj);
			
			return req;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return null;
		}
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/monitorroute/{clientId}")
	public @ResponseBody RouteMonitoring updateMonitorRoutes(HttpServletResponse response, @RequestBody(required=false) RouteMonitoring req, @PathVariable String clientId) throws Exception {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}
			
			String objectClientId = req.getClientId();
			if (!clientId.equals(objectClientId)) {
				response.setStatus(HttpServletResponse.SC_CONFLICT);
				return null;
			}			
			
			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("clientId", req.getClientId());			
			RouteMonitoringObject res = domainStorage.searchDomainObject(pars, RouteMonitoringObject.class);

			if (res != null) {
				if (!userId.equals(res.getUserId())) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					return null;
				}
			}
			
			RouteMonitoringObject obj = new RouteMonitoringObject(req);
			obj.setUserId(userId);

			domainStorage.saveRouteMonitoring(obj);
			
			return req;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return null;
		}
	}	
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/monitorroute")
	public @ResponseBody List<RouteMonitoring> getMonitorRoutes(HttpServletResponse response,  @RequestParam(required = false, value = "active") Boolean active) throws Exception {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			
			List<RouteMonitoring> res;
			if (active != null && active.booleanValue()) {
				res = checkTime(userId);
			} else {
				Map<String, Object> pars = new TreeMap<String, Object>();
				pars.put("userId", userId);
				res = domainStorage.searchDomainObjects(pars, RouteMonitoring.class);
			}

			return res;
			
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return null;
		}
	}	
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/monitorroute/{clientId}")
	public @ResponseBody Boolean deletetMonitorRoutes(HttpServletResponse response, @PathVariable String clientId) throws Exception {
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("clientId", clientId);
			RouteMonitoringObject res = domainStorage.searchDomainObject(pars, RouteMonitoringObject.class);

			if (res == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return false;
			}

			if (!userId.equals(res.getUserId())) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}

			domainStorage.deleteRouteMonitoring(clientId);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return false;
	}	
	
	
	
	private List<RouteMonitoring> checkTime(String userId) {
		long now = System.currentTimeMillis();
		Date nowDate = new Date(now);
		Calendar cal = new GregorianCalendar();
		cal.setTime(nowDate);
		
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		String nowHour = sdf.format(nowDate);
		
		
		Criteria criteria = new Criteria("userId").is(userId);
		
		List<RouteMonitoring> res1 = domainStorage.searchDomainObjects(criteria, RouteMonitoring.class);
		List<RouteMonitoring> res2 = Lists.newArrayList();
		
		for (RouteMonitoring rm: res1) {
			if (rm.getRecurrency() == null) {
				continue;
			}
			if (rm.getRecurrency().getFromDate() != null) {
				if (rm.getRecurrency().getFromDate() > now) {
					continue;
				}
			}
			if (rm.getRecurrency().getToDate() != null) {
				if (rm.getRecurrency().getToDate() < now) {
					continue;
				}
			}	
			if (rm.getRecurrency().getFromHour() != null) {
				if (rm.getRecurrency().getFromHour().compareTo(nowHour) > 0) {
					continue;
				}
			}
			if (rm.getRecurrency().getToHour() != null) {
				if (rm.getRecurrency().getToHour().compareTo(nowHour) < 0) {
					continue;
				}
			}			
			if (rm.getRecurrency().getDaysOfWeek() != null && !rm.getRecurrency().getDaysOfWeek().isEmpty()) {
				if (!rm.getRecurrency().getDaysOfWeek().contains(cal.get(Calendar.DAY_OF_WEEK))) {
					continue;
				}
			}
			
			res2.add(rm);
		}
		
		return res2;
	}	
	
	
	
	private void submitAlert(Map<String, Object> map, String userId, String clientId) throws Exception {
		AlertType type = AlertType.getAlertType((String) map.get("type"));

		Alert alert = null;
		Map<String, Object> contentMap = map;
		switch (type) {
		case ACCIDENT:
			alert = mapper.convertValue(contentMap, AlertAccident.class);
			break;
		case DELAY:
			alert = mapper.convertValue(contentMap, AlertDelay.class);
			if (userId != null)
				logger.debug("-" + userId + "~AppProsume~delay=" + ((AlertDelay) alert).getTransport().getAgencyId());
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
//	protected String getClientId() {
//		OAuth2Authentication auth = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
//		return auth.getOAuth2Request().getClientId();
////		return auth.getAuthorizationRequest().getClientId();
//	}

	protected String getUserId() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof String) {
			return (String)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		} else {
			return null;
		}
	}

}
