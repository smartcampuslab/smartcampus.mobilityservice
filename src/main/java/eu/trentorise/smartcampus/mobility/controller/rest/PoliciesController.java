package eu.trentorise.smartcampus.mobility.controller.rest;

import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanRequest;
import eu.trentorise.smartcampus.mobility.controller.extensions.model.AddPromotedPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.model.Policies;
import eu.trentorise.smartcampus.mobility.controller.extensions.model.ProcessRequestPolicy;
import eu.trentorise.smartcampus.mobility.service.SmartPlannerService;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;

@Controller
@RequestMapping(value = "/policies")
public class PoliciesController {

	private static final String IT = "{ \"from\" : { \"lat\" : 46.062005, \"lon\" : 11.129169} , \"to\" : { \"lat\" : 46.068854, \"lon\" : 11.151184} ,  \"date\" : \"12/13/2012\", \"departureTime\" : \"1:25pm\", \"transportTypes\" : [ \"TRANSIT\", \"CAR\", \"CARWITHPARKING\", \"BICYCLE\"], \"routeType\" : \"fastest\", \"resultsNumber\" : 1 }";
	
	private final static double KEY_INC = 0.01;
	
	@Autowired
	private DomainStorage domainStorage;
	
	@Autowired
	private SmartPlannerService plannerService;
	
	@RequestMapping(method = RequestMethod.POST, value = "/policy")
	public @ResponseBody void notify(@RequestBody Policies policies, HttpServletResponse response) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
//		System.out.println(mapper.writeValueAsString(policies));
		
		domainStorage.savePolicies(policies);
		
		SingleJourney journey = mapper.readValue(IT, SingleJourney.class);
		
		List<PlanRequest> reqsList = Lists.newArrayList();
		for (TType type: journey.getTransportTypes()) {
			int minitn = 1;
			PlanRequest pr = new PlanRequest();
			if (type.equals(TType.TRANSIT)) {
				minitn = 3;
				pr.setWheelChair(journey.isWheelchair());
			}
			int itn = Math.max(journey.getResultsNumber(), minitn);			
			
			pr.setType(type);
			pr.setRouteType(journey.getRouteType());
			pr.setValue(0.0);
			pr.setItineraryNumber(itn);
			reqsList.add(pr);			
			
			reqsList.addAll(addPromotedItineraries(policies, journey, type, journey.getRouteType()));
		}
		
		
		plannerService.init();
		
		System.out.println("-------------");
		
		processRequests(policies, reqsList, 0);
		
		System.out.println("-------------");
	}
	
	
	private  List<PlanRequest> addPromotedItineraries(Policies policies, SingleJourney request, TType type, RType routeType) throws Exception {
		double keyInc = KEY_INC;
		System.out.println(">" + type);
		
		List<PlanRequest> result = Lists.newArrayList();
		List<TType> requestTType = Arrays.asList(request.getTransportTypes());
		
		for (AddPromotedPolicy policy: policies.getAddPromotedPolicies()) {
			System.out.println("<" + policy.getRequestTransportTypes());
			int itn = Math.max(request.getResultsNumber(), 1);
			
			if (policy.getNotRequestTransportTypes().contains(type)) {
				System.out.println("1." + policy.getNotRequestTransportTypes() + "->" + type);
				continue;
			}		
			if (requestTType.contains(policy.getNewTransportType())) {
				System.out.println("2." + requestTType + "->" + policy.getNewTransportType());
				continue;
			}					
			if (policy.getNotRequestRouteType() != null && policy.getNotRequestRouteType().equals(routeType)) {
				System.out.println("3." + policy.getNotRequestRouteType() + "->" + routeType);
				continue;
			}			
			if (!policy.getRequestTransportTypes().contains(type)) {
				System.out.println("4." + policy.getRequestTransportTypes() + "->" + type);
				continue;
			}
			if (policy.getRequestRouteType() != null && policy.getRequestRouteType().equals(routeType)) {
				System.out.println("5." + policy.getRequestRouteType() + "->" + routeType);
				continue;
			}
			
			System.out.println(policy.getNewTransportType() + "," + policy.getNewRouteType() + "," + policy.getMaxToKeep() + "," + policy.getNewItnPerType());
			
			if (policy.getNewItnPerType() != null) {
				itn = policy.getNewItnPerType();
			}
			RType newRouteType = routeType;
			if (policy.getNewRouteType() != null) {
				newRouteType = policy.getNewRouteType(); 
			}
			
//			String req = String.format("from=%s,%s&to=%s,%s&date=%s&departureTime=%s&transportType=%s&routeType=%s&numOfItn=%s", request.getFrom().getLat(), request.getFrom().getLon(), request.getTo().getLat(), request.getTo().getLon(), request.getDate(), request.getDepartureTime(), policy.getNewTransportType(), newRouteType, itn);
			
			PlanRequest pr = new PlanRequest();
//			pr.setRequest(req); nulled shortly after
			pr.setRouteType(newRouteType);			
			pr.setType(policy.getNewTransportType());
			pr.setValue(policy.getMaxToKeep() + keyInc);
			pr.setItineraryNumber(itn);
			keyInc += KEY_INC;
			
			ObjectMapper mapper = new ObjectMapper();
			System.out.println(mapper.writeValueAsString(pr));
			
			result.add(pr);
		}
		
		return result;
	}
	
	
	private void processRequests(Policies policies, List<PlanRequest> requests, int iteration) throws Exception {
		for (PlanRequest pr: requests) {
			System.out.println(">>" + pr.getType());
			pr.setRequest("");
			
			for (ProcessRequestPolicy policy: policies.getProcessRequestPolicies()) {
				System.out.println("<<" + policy.getRequestTransportTypes());
//				if (policy.getIterationN() != null && iteration != policy.getIterationN()) {
//					System.out.println("1." + policy.getIterationN() + "->" + iteration);
//					continue;					
//				}
				if (!policy.getRequestTransportTypes().contains(pr.getType())) {
					System.out.println("2." + policy.getRequestTransportTypes() + "->" + pr.getType());
					continue;
				}
				
				if (policy.getExtraTransport() != null) {
					pr.setRequest(pr.getRequest() + "&extraTransport=" + policy.getExtraTransport());
				}
				if (policy.getMaxWalkDistance() != null) {
					pr.setRequest(pr.getRequest() + "&maxWalkDistance=" + policy.getMaxWalkDistance());
				}		
				if (policy.getMaxChanges() != null) {
					pr.setRequest(pr.getRequest() + "&maxChanges=" + policy.getMaxChanges());
				}	
				if (policy.getMaxTotalWalkDistance() != null) {
					pr.setRequest(pr.getRequest() + "&maxTotalWalkDistance=" + policy.getMaxTotalWalkDistance());
				}					
				if (policy.getNewRouteType() != null) {
					pr.setRouteType(policy.getNewRouteType());
				}
				
				ObjectMapper mapper = new ObjectMapper();
				System.out.println(mapper.writeValueAsString(pr));				
				
			}
		}
	}
	
	
}
