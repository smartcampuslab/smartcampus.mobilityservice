package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;

import eu.trentorise.smartcampus.mobility.controller.rest.ItinerarySorter;
import eu.trentorise.smartcampus.mobility.util.PlanningPolicyHelper;

public class DummyPlanningPolicy implements PlanningPolicy {

	@Override
	public List<PlanningRequest> generatePlanRequests(SingleJourney journeyRequest) {
		List<PlanningRequest> result = PlanningPolicyHelper.generateOriginalPlanRequests(journeyRequest);
		PlanningPolicyHelper.buildSmartplannerRequest(result);
		return result;
	}

	@Override
	public List<PlanningRequest> evaluatePlanResults(List<PlanningRequest> planRequests) {
		List<PlanningRequest> ok = Lists.newArrayList(planRequests);
		planRequests.removeAll(planRequests);
		return ok;
	}

	@Override
	public List<Itinerary> filterPlanResults(SingleJourney journeyRequest, List<PlanningRequest> planRequests) {
		List<Itinerary> result = Lists.newArrayList();
		for (PlanningRequest pr: planRequests) {
			if (pr.getItinerary() != null) {
				result.addAll(pr.getItinerary());
			}
		}
		return result;
	}

	@Override
	public List<Itinerary> filterAndSortItineraries(SingleJourney journeyRequest, List<Itinerary> itineraries) {
		List<Itinerary> result = Lists.newArrayList(itineraries);
		Comparator<Itinerary> comparator = ItinerarySorter.comparatorByRouteType(journeyRequest.getRouteType());
		ItinerarySorter.sort(result, comparator);
		return result;
	}

}
