package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Multimap;


public class DummyItineraryRequestEnricher implements ItineraryRequestEnricher {

	@Override
	public List<PlanRequest> addPromotedItineraries(SingleJourney request, TType type) {
		return new ArrayList<PlanRequest>();
	}

	@Override
	public List<Itinerary> filterPromotedItineraties(Multimap<Integer, Itinerary> itineraries, RType criteria) {
		return new ArrayList<Itinerary>();
	}

	@Override
	public List<Itinerary> removeExtremeItineraties(List<Itinerary> itineraries, RType criteria) {
		return itineraries;
	}

	@Override
	public void completeResponse(SingleJourney journeyRequest, List<PlanRequest> planRequests) {
	}

}
