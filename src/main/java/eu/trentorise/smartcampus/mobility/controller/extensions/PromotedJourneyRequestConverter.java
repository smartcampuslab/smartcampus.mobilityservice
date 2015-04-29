package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.List;


public interface PromotedJourneyRequestConverter {

	public void modifyRequest(SingleJourney request);
	public void processRequests(List<PlanRequest> requests);
	public void promoteJourney(List<PlanRequest> requests);
}
