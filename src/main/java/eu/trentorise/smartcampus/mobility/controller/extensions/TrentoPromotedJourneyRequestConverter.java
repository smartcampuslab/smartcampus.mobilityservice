package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.List;

public class TrentoPromotedJourneyRequestConverter implements PromotedJourneyRequestConverter {

	@Override
	public void modifyRequest(SingleJourney request) {
	}	
	
	public void processRequests(List<PlanRequest> requests, int iteration) {
		for (PlanRequest pr: requests) {
			pr.setRequest("");
			if (pr.getType().equals(TType.CARWITHPARKING)) {
				pr.setRequest(pr.getRequest() + "&extraTransport=WALK");
			}
			if (pr.getType().equals(TType.TRANSIT) || pr.getType().equals(TType.BUS)) {
				if (pr.getRouteType().equals(RType.leastWalking) && iteration == 0) {
					pr.setRequest(pr.getRequest() + "&maxWalkDistance=500");
					pr.setRetryOnFail(true);
				} else {
					pr.setRequest(pr.getRequest() + "&maxWalkDistance=1000");
					pr.setRetryOnFail(false);
				}
			}			
			if (pr.getType().equals(TType.SHAREDBIKE) || pr.getType().equals(TType.SHAREDBIKE_WITHOUT_STATION) || pr.getType().equals(TType.SHAREDCAR) || pr.getType().equals(TType.SHAREDCAR_WITHOUT_STATION)
					|| pr.getType().equals(TType.CARWITHPARKING) || pr.getType().equals(TType.PARK_AND_RIDE)) {
				pr.setRequest(pr.getRequest() + "&maxTotalWalkDistance=1250");
			}
			if (pr.getType().equals(TType.SHAREDBIKE) || pr.getType().equals(TType.SHAREDBIKE_WITHOUT_STATION) || pr.getType().equals(TType.BICYCLE)) {
				pr.setRouteType(RType.safest);
			}
			if (pr.getType().equals(TType.WALK) && pr.getValue() != 0) {
				if (pr.getRouteType().equals(RType.leastWalking)) {
					pr.setRequest(pr.getRequest() + "&maxTotalWalkDistance=500");
				} else {
					pr.setRequest(pr.getRequest() + "&maxTotalWalkDistance=1000");
				}
			}
		}
	}
	
	@Override
	public void promoteJourney(List<PlanRequest> requests) {
	}	
	
	
	
		
}
