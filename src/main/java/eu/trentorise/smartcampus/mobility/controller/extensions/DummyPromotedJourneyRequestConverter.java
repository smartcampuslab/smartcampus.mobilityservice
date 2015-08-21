package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.List;


public class DummyPromotedJourneyRequestConverter implements PromotedJourneyRequestConverter {

	@Override
	public void modifyRequest(SingleJourney request) {
	}	
	
	@Override
	public void processRequests(List<PlanRequest> requests, int iteration) {
	}	
	
	@Override
	public void promoteJourney(List<PlanRequest> requests) {
	}	
	
}
