package eu.trentorise.smartcampus.mobility.storage;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class DomainStorage {

	public static final String ITINERARY = "itinerary";
	public static final String RECURRENT = "recurrent";
	
	@Autowired
	@Qualifier("domainMongoTemplate")
	MongoTemplate template;

	public DomainStorage() {
	}
	
	public void saveItinerary(ItineraryObject io) {
		template.save(io, ITINERARY);
	}
	
	public void deleteItinerary(ItineraryObject io) {
		template.remove(io, ITINERARY);
	}	
	
	public void saveRecurrent(RecurrentJourneyObject io) {
		template.save(io, RECURRENT);
	}	

	public void deleteRecurrent(RecurrentJourneyObject io) {
		template.remove(io, RECURRENT);
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
		for (String key: pars.keySet()) {
			criteria.and(key).is(pars.get(key));
		}
		
		Query query = new Query(criteria);
		
		return template.findOne(query, clz, collection);
	}	
	
}
