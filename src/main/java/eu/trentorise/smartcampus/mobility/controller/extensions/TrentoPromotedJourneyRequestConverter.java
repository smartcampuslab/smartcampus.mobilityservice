package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.List;

public class TrentoPromotedJourneyRequestConverter implements PromotedJourneyRequestConverter {

	@Override
	public void modifyRequest(SingleJourney request) {
	}	
	
	public void processRequests(List<PlanRequest> requests) {
		for (PlanRequest pr: requests) {
			if (pr.getType().equals(TType.CARWITHPARKING)) {
				pr.setRequest(pr.getRequest() + "&extraTransport=WALK");
			}
			if (pr.getType().equals(TType.SHAREDBIKE) ||pr.getType().equals(TType.SHAREDBIKE_WITHOUT_STATION) || pr.getType().equals(TType.SHAREDCAR) || pr.getType().equals(TType.SHAREDCAR_WITHOUT_STATION)
					|| pr.getType().equals(TType.CARWITHPARKING) || pr.getType().equals(TType.PARK_AND_RIDE)) {
				pr.setRequest(pr.getRequest() + "&maxTotalWalkDistance=1250");
			}
		}
	}
	
	@Override
	public void promoteJourney(List<PlanRequest> requests) {
	}	
	
	
	
		
}
