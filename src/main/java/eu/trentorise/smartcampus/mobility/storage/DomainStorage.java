package eu.trentorise.smartcampus.mobility.storage;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.mongodb.BasicDBObject;

import eu.trentorise.smartcampus.mobility.controller.extensions.compilable.CompilablePolicyData;
import eu.trentorise.smartcampus.mobility.gamification.model.PlanObject;
import eu.trentorise.smartcampus.mobility.gamification.model.SavedTrip;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.model.Announcement;
import eu.trentorise.smartcampus.mobility.model.RouteMonitoring;
import eu.trentorise.smartcampus.mobility.processor.alerts.AlertsSent;
import eu.trentorise.smartcampus.network.JsonUtils;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

@Component
public class DomainStorage {

	private static final String ITINERARY = "itinerary";
	private static final String RECURRENT = "recurrent";
	private static final String DATA = "data";
	private static final String NEWS = "news";
	private static final String GEOLOCATIONS = "geolocations";
	private static final String TRACKED = "trackedInstances";
	private static final String SAVED = "savedtrips";
	private static final String COMPILED_POLICY = "compiledPolicies";
	private static final String MONITORING = "routesMonitoring";
	
	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;
	private static final Logger logger = LoggerFactory.getLogger(DomainStorage.class);

	public DomainStorage() {
	}

	private String getClassCollection(Class<?> cls) {
		if (cls == ItineraryObject.class) {
			return ITINERARY;
		}
		if (cls == RecurrentJourneyObject.class) {
			return RECURRENT;
		}
		if (cls == AlertsSent.class) {
			return DATA;
		}
		if (cls == Announcement.class) {
			return NEWS;
		}
		if (cls == Geolocation.class) {
			return GEOLOCATIONS;
		}
		if (cls == TrackedInstance.class) {
			return TRACKED;
		}	
		if (cls == SavedTrip.class) {
			return SAVED;
		}	
		if (cls == CompilablePolicyData.class) {
			return COMPILED_POLICY;
		}		
		if (cls == RouteMonitoringObject.class || cls == RouteMonitoring.class) {
			return MONITORING;
		}			
		throw new IllegalArgumentException("Unknown class: " + cls.getName());
	}
	
	public void saveItinerary(ItineraryObject io) {
		template.save(io, ITINERARY);
//		ObjectMapper mapper = new ObjectMapper();
//		BasicDBObject obj = mapper.convertValue(io, BasicDBObject.class);
//		template.getCollection(ITINERARY).save(i);
	}
	
	public void deleteItinerary(String clientdId) {
		BasicDBObject query = new BasicDBObject();
		query.put("clientId", clientdId);
		template.getCollection(ITINERARY).remove(query);
	}		
	
	public void saveRecurrent(RecurrentJourneyObject io) {
		template.save(io, RECURRENT);
//		ObjectMapper mapper = new ObjectMapper();
//		BasicDBObject obj = mapper.convertValue(io, BasicDBObject.class);
//		template.getCollection(RECURRENT).save(obj);		
	}	

	public void deleteRecurrent(String clientdId) {
		BasicDBObject query = new BasicDBObject();
		query.put("clientId", clientdId);
		template.getCollection(RECURRENT).remove(query);		
	}		
	
	public AlertsSent getAlertsSent() {
		AlertsSent alerts = searchDomainObject(new TreeMap<String, Object>(), AlertsSent.class);
		if (alerts == null) {
			alerts = new AlertsSent();
		}
		return alerts;
	}
	
	public void updateAlertsSent(AlertsSent alerts) {
		AlertsSent oldAlerts = getAlertsSent();
		oldAlerts.setDelays(alerts.getDelays());
		oldAlerts.setParkings(alerts.getParkings());
		
		try {
			template.save(oldAlerts, DATA);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void saveNews(Announcement announcment) {
		template.save(announcment, NEWS);
	}
	
	public void saveGeolocation(Geolocation geolocation) {
		Query query = new Query(new Criteria("userId").is(geolocation.getUserId()).and("recorded_at").is(geolocation.getRecorded_at()));
		Geolocation geolocationDB = searchDomainObject(query, Geolocation.class);
		if (geolocationDB == null) {
			template.save(geolocation, GEOLOCATIONS);
		} else {

			// ObjectMapper mapper = new ObjectMapper();
			// BasicDBObject dbObject = mapper.convertValue(geolocation,
			// BasicDBObject.class);
			//
			// Update update = Update.fromDBObject(dbObject);

			Update update = new Update();
			update.set("userId", geolocation.getUserId());
			update.set("travelId", geolocation.getTravelId());

			update.set("uuid", geolocation.getUuid());
			update.set("device_id", geolocation.getDevice_id());
			update.set("device_model", geolocation.getDevice_model());

			update.set("latitude", geolocation.getLatitude());
			update.set("longitude", geolocation.getLongitude());
			update.set("geocoding", geolocation.getGeocoding());
			update.set("accuracy", geolocation.getAccuracy());
			update.set("altitude", geolocation.getAltitude());
			update.set("speed", geolocation.getSpeed());
			update.set("heading", geolocation.getHeading());
			update.set("activity_type", geolocation.getActivity_type());
			update.set("activity_confidence", geolocation.getActivity_confidence());
			update.set("battery_level", geolocation.getBattery_level());
			update.set("battery_is_charging", geolocation.getBattery_is_charging());

			update.set("is_moving", geolocation.getIs_moving());
			update.set("geofence", geolocation.getGeofence());
			update.set("recorded_at", geolocation.getRecorded_at());
			update.set("created_at", geolocation.getCreated_at());

			template.updateFirst(query, update, GEOLOCATIONS);
		}
	}
	
	public void saveTrackedInstance(TrackedInstance tracked) {
		Query query = new Query(
				new Criteria("clientId").is(tracked.getClientId())
				.and("day").is(tracked.getDay())
				.and("userId").is(tracked.getUserId()));
		TrackedInstance trackedDB = searchDomainObject(query, TrackedInstance.class);
		if (trackedDB == null) {
			template.save(tracked, TRACKED);
		} else {
			Update update = new Update();
			if (tracked.getItinerary() != null) {
				update.set("itinerary", tracked.getItinerary());
			}
			if (tracked.getGeolocationEvents() != null && !tracked.getGeolocationEvents().isEmpty()) {
				update.set("geolocationEvents", tracked.getGeolocationEvents());
			}

			if (tracked.getStarted() != null) {
				update.set("started", tracked.getStarted());
			}
			if (tracked.getComplete() != null) {
				update.set("complete", tracked.getComplete());
			}
			if (tracked.getValidationResult() != null) {
				update.set("validationResult", tracked.getValidationResult());
			}	
			if (tracked.getScore() != null) {
				update.set("score", tracked.getScore());
			}
			if (tracked.getDeviceInfo() != null && !tracked.getDeviceInfo().isEmpty()) {
				update.set("deviceInfo", tracked.getDeviceInfo());
			}
			update.set("changedValidity", tracked.getChangedValidity());
			update.set("scoreStatus", tracked.getScoreStatus());
			update.set("approved", tracked.getApproved());
			update.set("toCheck", tracked.getToCheck());
			update.set("appId", tracked.getAppId());
			
			template.updateFirst(query, update, TRACKED);
		}
	}
	
	public void saveSavedTrips(SavedTrip savedTrip) {
		template.save(savedTrip, SAVED);
	}
	
	public void savePolicy(CompilablePolicyData policy) {
		Query query = new Query(new Criteria("name").is(policy.getName()));
		CompilablePolicyData policiesDB = searchDomainObject(query, CompilablePolicyData.class);
		if (policiesDB == null) {
			template.save(policy, COMPILED_POLICY);
		} else {
			Update update = new Update();
			update.set("description", policy.getDescription());
			update.set("create", policy.getCreate());
			update.set("modify", policy.getModify());
			update.set("extract", policy.getExtract());
			update.set("evaluate", policy.getEvaluate());
			update.set("filter", policy.getFilter());
			update.set("generateCode", policy.getGenerateCode());
			update.set("evaluateCode", policy.getEvaluateCode());
			update.set("extractCode", policy.getExtractCode());
			update.set("filterCode", policy.getFilterCode());
			update.set("modifiedGenerate", policy.isModifiedGenerate());
			update.set("modifiedEvaluate", policy.isModifiedEvaluate());
			update.set("modifiedExtract", policy.isModifiedExtract());
			update.set("modifiedFilter", policy.isModifiedFilter());
			update.set("evaluateCode", policy.getEvaluateCode());
			update.set("extractCode", policy.getExtractCode());
			update.set("filterCode", policy.getFilterCode());			
			update.set("groups", policy.getGroups());
			update.set("draft", policy.getDraft());
			update.set("policyId", policy.getPolicyId());
			template.updateFirst(query, update, COMPILED_POLICY);
		}
	}		
	
	public void saveRouteMonitoring(RouteMonitoringObject rmo) {
		Query query = new Query(new Criteria("clientId").is(rmo.getClientId()));
		RouteMonitoringObject monitoringDB = searchDomainObject(query, RouteMonitoringObject.class);
		if (monitoringDB == null) {
			template.save(rmo, MONITORING);
		} else {
			Update update = new Update();
			update.set("agencyId", rmo.getAgencyId());
			update.set("routeId", rmo.getRouteId());
			update.set("recurrency", rmo.getRecurrency());
			template.updateFirst(query, update, MONITORING);
		}
	}	
	
	public void deleteRouteMonitoring(String clientdId) {
		BasicDBObject query = new BasicDBObject();
		query.put("clientId", clientdId);
		template.getCollection(MONITORING).remove(query);
	}	
	
	public Geolocation getLastGeolocationByUserId(String userId) {
		Criteria criteria = new Criteria("userId").is(userId);
		Query query = new Query(criteria).with(new Sort(Sort.Direction.DESC, "created_at"));
		return searchDomainObject(query, Geolocation.class);
	}
	
	public <T> List<T> searchDomainObjects(Criteria criteria, Class<T> clz) {
		Query query = new Query(criteria);
		logger .debug("query: {}",JsonUtils.toJSON(query.getQueryObject()));
		return template.find(query, clz, getClassCollection(clz));
	}
	
	public <T> List<T> searchDomainObjects(Query query, Class<T> clz) {
		logger .debug("query: {}",JsonUtils.toJSON(query.getQueryObject()));
		return template.find(query, clz, getClassCollection(clz));
	}	
	
	public <T> T searchDomainObject(Query query, Class<T> clz) {
		logger .debug("query: {}",JsonUtils.toJSON(query.getQueryObject()));
		return template.findOne(query, clz, getClassCollection(clz));
	}	
	
	public <T> List<T> searchDomainObjects(Map<String, Object> pars, Class<T> clz) {
		Criteria criteria = new Criteria();
		for (String key: pars.keySet()) {
			criteria.and(key).is(pars.get(key));
		}
		
		Query query = new Query(criteria);
		
		return template.find(query, clz, getClassCollection(clz));
	}
	public <T> List<T> searchDomainObjects(Map<String, Object> pars, Set<String> keys, Class<T> clz) {
		Criteria criteria = new Criteria();
		for (String key: pars.keySet()) {
			criteria.and(key).is(pars.get(key));
		}
		
		Query query = new Query(criteria);
		if (keys != null){
			for (String key : keys) {
				query.fields().include(key);
			}
		}
		
		return template.find(query, clz, getClassCollection(clz));
	}
	
	public <T> List<T> searchDomainObjects(Query query, Set<String> keys, Class<T> clz) {
		logger .debug("query: {}",JsonUtils.toJSON(query.getQueryObject()));
		
		if (keys != null){
			for (String key : keys) {
				query.fields().include(key);
			}
		}

		return template.find(query, clz, getClassCollection(clz));
	}		
	
	public <T> T searchDomainObject(Map<String, Object> pars, Class<T> clz) {
		Criteria criteria = new Criteria();
		for (String key : pars.keySet()) {
			criteria.and(key).is(pars.get(key));
		}

		Query query = new Query(criteria);
		return template.findOne(query, clz, getClassCollection(clz));
	}	
	
	public <T> void deleteDomainObject(Criteria criteria, Class<T> clz) {
		Query query = new Query(criteria);
		logger .debug("query: {}",JsonUtils.toJSON(query.getQueryObject()));
		template.remove(query, getClassCollection(clz));
	}	
		
	public <T> long count(Criteria criteria, Class<T> clz) {
		Query query = new Query(criteria);
		logger .debug("query: {}",JsonUtils.toJSON(query.getQueryObject()));
		long result = template.count(query, getClassCollection(clz));
		return result;
	}		
	
	
//	public <T> T searchDomainObjectFixForSpring(Map<String, Object> pars, Class<T> clz) {
//		Criteria criteria = new Criteria();
//		for (String key : pars.keySet()) {
//			criteria.and(key).is(pars.get(key));
//		}
//
//		Query query = new Query(criteria);
//
//			BasicDBObject obj = (BasicDBObject) template.getCollection(getClassCollection(clz)).findOne(query.getQueryObject());
//			ObjectMapper mapper = new ObjectMapper();
//			mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//			return mapper.convertValue(obj, clz);
//	}	
//
	public void reset() {
		template.dropCollection(ITINERARY);
		template.dropCollection(RECURRENT);
		template.dropCollection(DATA);
		template.dropCollection(NEWS);
		template.dropCollection(GEOLOCATIONS);
	}

	/**
	 * @param journeyRequest
	 * @param userId
	 * @param appName
	 */
	public void savePlanRequest(SingleJourney journeyRequest, String userId, String appName) {
		template.save(new PlanObject(journeyRequest, userId, appName));
	}
	
}
