
import java.util.List;

import junit.framework.TestCase;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.Mongo;

import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;

public class Test extends TestCase {

	public Test() {
	}
	
	public void test() throws Exception {
		MongoTemplate template = new MongoTemplate(new Mongo(), "mobility-domain");
//		String id = "R10915";
		String id = "R10915X";
		
//		Criteria c1 = new Criteria("transport.agencyId").is("5").and("transport.tripId").regex("[^digit]*" + id + "[^digit]*");
//		Criteria criteria = new Criteria("data.leg").elemMatch(c1);		
		
		Criteria c1 = new Criteria("from.stopId.agencyId").is("COMUNE_DI_ROVERETO").and("from.stopId._id").is("Stazione");
//		Criteria c1 = new Criteria("from.stopId.agencyId").is("COMUNE_DI_ROVERETO");
//		Criteria c1 = new Criteria("from.stopId.id").is("Stazione");
		Criteria criteria = new Criteria("data.leg").elemMatch(c1);
//		List<ItineraryObject> its = (List<ItineraryObject>) domainStorage.searchDomainObjects(criteria, ItineraryObject.class, DomainStorage.ITINERARY);		
		
		
		Query query = new Query(criteria);
		
		System.out.println(criteria.getCriteriaObject());
		System.out.println(query.getQueryObject());
		
		
		System.out.println(template.getCollection(DomainStorage.ITINERARY).count(query.getQueryObject()));
		
		List<ItineraryObject> its = template.find(query, ItineraryObject.class, DomainStorage.ITINERARY);		
		
		System.out.println(its.size());
	}

}
