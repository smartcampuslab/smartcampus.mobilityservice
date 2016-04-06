package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.List;

import eu.trentorise.smartcampus.mobility.controller.extensions.model.Policies;
import eu.trentorise.smartcampus.mobility.controller.extensions.model.ProcessRequestPolicy;

public class CustomPromotedJourneyRequestConverter implements PromotedJourneyRequestConverter {

	private Policies policies;
	
	public static CustomPromotedJourneyRequestConverter build(String name) {
		// load policies from mongo
		CustomPromotedJourneyRequestConverter jrc = new CustomPromotedJourneyRequestConverter();
		jrc.setPolicies(null); 
		return jrc;
	}	
	
	public Policies getPolicies() {
		return policies;
	}

	public void setPolicies(Policies policies) {
		this.policies = policies;
	}

	@Override
	public void modifyRequest(SingleJourney request) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processRequests(List<PlanRequest> requests, boolean retried) {
		for (PlanRequest pr: requests) {
			pr.setRequest("");
			
			for (ProcessRequestPolicy policy: policies.getProcessRequestPolicies()) {
				if (policy.getRetried() != null && retried != policy.getRetried().booleanValue()) {
					continue;					
				}
				if (!policy.getRequestTransportTypes().contains(pr.getType())) {
					continue;
				}
				if (policy.getNotRequestRouteType() != null && policy.getNotRequestRouteType().equals(pr.getRouteType())) {
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
				
				pr.setRetryOnEmpty(policy.getRetryOnEmpty());
			}
		}
	}

	@Override
	public void promoteJourney(List<PlanRequest> requests) {
		// TODO Auto-generated method stub

	}

}
