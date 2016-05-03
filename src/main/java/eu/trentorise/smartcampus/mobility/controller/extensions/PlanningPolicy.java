package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.List;

public interface PlanningPolicy {

	public String getName();
	public String getDescription();
	public Boolean getDraft();
	public List<PlanningRequest> generatePlanRequests(SingleJourney journeyRequest);
	public List<PlanningRequest> evaluatePlanResults(List<PlanningRequest> planRequests); // remove from planRequests a List<PlanRequest> that must not be retried and return it
	public List<Itinerary> extractItinerariesFromPlanResults(SingleJourney journeyRequest, List<PlanningRequest> planRequests);
	public List<Itinerary> filterAndSortItineraries(SingleJourney journeyRequest, List<Itinerary> itineraries);
	
}
