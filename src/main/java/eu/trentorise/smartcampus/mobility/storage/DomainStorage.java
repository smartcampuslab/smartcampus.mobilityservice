package eu.trentorise.smartcampus.mobility.storage;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.mongodb.BasicDBObject;

import eu.trentorise.smartcampus.mobility.model.Announcement;
import eu.trentorise.smartcampus.mobility.processor.alerts.AlertsSent;
import eu.trentorise.smartcampus.network.JsonUtils;

@Component
public class DomainStorage {

	private static final String ITINERARY = "itinerary";
	private static final String RECURRENT = "recurrent";
	private static final String DATA = "data";
	private static final String NEWS = "news";
	
	@Autowired
	@Qualifier("domainMongoTemplate")
	MongoTemplate template;
	private static final Logger logger = LoggerFactory.getLogger(DomainStorage.class);

	public DomainStorage() {
	}

	private String getClassCollection(Class<?> cls) {
		if (cls == ItineraryObject.class) return ITINERARY;
		if (cls == RecurrentJourneyObject.class) return RECURRENT;
		if (cls == AlertsSent.class) return DATA;
		if (cls == Announcement.class) return NEWS;
		throw new IllegalArgumentException("Unknown class: "+cls.getName());
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
	
	public <T> List<T> searchDomainObjects(Criteria criteria, Class<T> clz) {
		Query query = new Query(criteria);
		logger .debug("query: {}",JsonUtils.toJSON(query.getQueryObject()));
		return template.find(query, clz, getClassCollection(clz));
	}
	
	public <T> List<T> searchDomainObjects(Query query, Class<T> clz) {
		logger .debug("query: {}",JsonUtils.toJSON(query.getQueryObject()));
		return template.find(query, clz, getClassCollection(clz));
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
	}
	
}
