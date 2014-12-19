package eu.trentorise.smartcampus.mobility.storage;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.BasicDBObject;

import eu.trentorise.smartcampus.mobility.processor.alerts.AlertsSent;

public class DomainStorage {

	public static final String ITINERARY = "itinerary";
	public static final String RECURRENT = "recurrent";
	public static final String DATA = "data";
	
	@Autowired
	@Qualifier("domainMongoTemplate")
	MongoTemplate template;

	public DomainStorage() {
	}
	
	public void saveItinerary(ItineraryObject io) {
//		template.save(io, ITINERARY);
		ObjectMapper mapper = new ObjectMapper();
		BasicDBObject obj = mapper.convertValue(io, BasicDBObject.class);
		template.getCollection(ITINERARY).save(obj);
	}
	
	public void deleteItinerary(String clientdId) {
		BasicDBObject query = new BasicDBObject();
		query.put("clientId", clientdId);
		template.getCollection(ITINERARY).remove(query);
	}		
	
	public void saveRecurrent(RecurrentJourneyObject io) {
//		template.save(io, RECURRENT);
		ObjectMapper mapper = new ObjectMapper();
		BasicDBObject obj = mapper.convertValue(io, BasicDBObject.class);
		template.getCollection(RECURRENT).save(obj);		
	}	

	public void deleteRecurrent(String clientdId) {
		BasicDBObject query = new BasicDBObject();
		query.put("clientId", clientdId);
		template.getCollection(RECURRENT).remove(query);		
	}		
	
	public AlertsSent getAlertsSent() {
		AlertsSent alerts = (AlertsSent)searchDomainObject(new TreeMap<String, Object>(), AlertsSent.class, DATA);
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
	
	public List<?> searchDomainObjects(Criteria criteria, Class<?> clz, String collection) {
		Query query = new Query(criteria);
		
		return template.find(query, clz, collection);
	}
	
	public List<?> searchDomainObjects(Map<String, Object> pars, Class<?> clz, String collection) {
		Criteria criteria = new Criteria();
		for (String key: pars.keySet()) {
			criteria.and(key).is(pars.get(key));
		}
		
		Query query = new Query(criteria);
		
		return template.find(query, clz, collection);
	}
	
	public Object searchDomainObject(Map<String, Object> pars, Class<?> clz, String collection) {
		Criteria criteria = new Criteria();
		for (String key : pars.keySet()) {
			criteria.and(key).is(pars.get(key));
		}

		Query query = new Query(criteria);

		return template.findOne(query, clz, collection);
	}	
	
	public Object searchDomainObjectFixForSpring(Map<String, Object> pars, Class<?> clz, String collection) {
		Criteria criteria = new Criteria();
		for (String key : pars.keySet()) {
			criteria.and(key).is(pars.get(key));
		}

		Query query = new Query(criteria);

			BasicDBObject obj = (BasicDBObject) template.getCollection(collection).findOne(query.getQueryObject());
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			return mapper.convertValue(obj, clz);
	}	
	
	
}
