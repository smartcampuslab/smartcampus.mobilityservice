package eu.trentorise.smartcampus.mobility.storage;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.mongodb.BasicDBObject;

import eu.trentorise.smartcampus.mobility.controller.extensions.compilable.CompilablePolicyData;
import eu.trentorise.smartcampus.mobility.model.Announcement;
import eu.trentorise.smartcampus.mobility.model.RouteMonitoring;

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
	@Qualifier("domainMongoTemplate")
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
		if (cls == Announcement.class) {
			return NEWS;
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
	
	public void saveNews(Announcement announcment) {
		template.save(announcment, NEWS);
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
	
	
	public <T> List<T> searchDomainObjects(Criteria criteria, Class<T> clz) {
		Query query = new Query(criteria);
		return template.find(query, clz, getClassCollection(clz));
	}
	
	public <T> List<T> searchDomainObjects(Query query, Class<T> clz) {
		return template.find(query, clz, getClassCollection(clz));
	}	
	
	public <T> T searchDomainObject(Query query, Class<T> clz) {
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
		template.remove(query, getClassCollection(clz));
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

	
}
