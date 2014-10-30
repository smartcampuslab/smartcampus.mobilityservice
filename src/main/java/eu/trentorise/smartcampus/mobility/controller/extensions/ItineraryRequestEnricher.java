package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.List;

import com.google.common.collect.Multimap;

public interface ItineraryRequestEnricher {

	public Multimap<Integer, String> addPromotedItineraries(SingleJourney request, TType type);
	public List<Itinerary> filterPromotedItineraties(Multimap<Integer, Itinerary> itineraries, RType criteria);
	public List<Itinerary> removeExtremeItineraties(List<Itinerary> itineraries, RType criteria);
}
