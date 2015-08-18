package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.Multimap;

import eu.trentorise.smartcampus.mobility.controller.rest.ItinerarySorter;


public class DummyItineraryRequestEnricher implements ItineraryRequestEnricher {

	@Override
	public List<PlanRequest> addPromotedItineraries(SingleJourney request, TType type, RType routeType) {
		return new ArrayList<PlanRequest>();
	}

	@Override
	public List<Itinerary> filterPromotedItineraties(Multimap<Integer, Itinerary> itineraries, RType criteria) {
		return new ArrayList<Itinerary>();
	}

	@Override
	public List<Itinerary> removeExtremeItineraties(List<Itinerary> itineraries, RType criteria) {
		return new ArrayList(new HashSet<Itinerary>(itineraries));
	}

	@Override
	public void completeResponse(SingleJourney journeyRequest, List<PlanRequest> planRequests, List<Itinerary> itineraries) {
	}

	@Override
	public void sort(List<Itinerary> itineraries, RType criterion) {
		ItinerarySorter.sort(itineraries, criterion);
	}

}
