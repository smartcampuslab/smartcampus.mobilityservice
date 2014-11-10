package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import eu.trentorise.smartcampus.mobility.controller.rest.ItinerarySorter;

public class GreenItineraryRequestEnricher implements ItineraryRequestEnricher {

	private static Log logger = LogFactory.getLog(GreenItineraryRequestEnricher.class);

	@Override
	public Multimap<Integer, String> addPromotedItineraries(SingleJourney request, TType type) {
		Multimap<Integer, String> reqsMap = ArrayListMultimap.create();
		int itn = Math.max(request.getResultsNumber(), 1);
		List<TType> types = new ArrayList<TType>();
		List<TType> requestedTypes = Arrays.asList(request.getTransportTypes());
		if (type.equals(TType.CAR) || type.equals(TType.CARWITHPARKING)) {
			if (!requestedTypes.contains(TType.PARK_AND_RIDE)) {
				types.add(TType.PARK_AND_RIDE);
			}
			// if (!requestedTypes.contains(TType.SHAREDBIKE_WITHOUT_STATION)) {
			// types.add(TType.SHAREDBIKE_WITHOUT_STATION);
			// }
			if (!requestedTypes.contains(TType.SHAREDBIKE)) {
				types.add(TType.SHAREDBIKE);
			}
			if (!requestedTypes.contains(TType.TRANSIT)) {
				types.add(TType.TRANSIT);
			}
		}
		if (type.equals(TType.TRANSIT) || type.equals(TType.BUS) || type.equals(TType.TRAIN)) {
			if (!requestedTypes.contains(TType.WALK)) {
				types.add(TType.WALK);
			}
			if (!requestedTypes.contains(TType.TRAIN)) {
				types.add(TType.TRAIN); // ???
			}
			// if (!requestedTypes.contains(TType.SHAREDBIKE_WITHOUT_STATION)) {
			// types.add(TType.SHAREDBIKE_WITHOUT_STATION);
			// }
			if (!requestedTypes.contains(TType.SHAREDBIKE)) {
				types.add(TType.SHAREDBIKE);
			}
		}
		for (TType newType : types) {
			String req = String.format("from=%s,%s&to=%s,%s&date=%s&departureTime=%s&transportType=%s&numOfItn=%s", request.getFrom().getLat(), request.getFrom().getLon(), request.getTo().getLat(), request.getTo().getLon(), request.getDate(), request.getDepartureTime(), newType, itn);
			if (type.equals(TType.CAR) || type.equals(TType.CARWITHPARKING)) {
				if (newType.equals(TType.PARK_AND_RIDE)) {
					reqsMap.put(1, req);
				} else {
					reqsMap.put(-1, req);
				}
			}
			if (type.equals(TType.TRANSIT) || type.equals(TType.BUS) || type.equals(TType.TRAIN)) {
				reqsMap.put(2, req);
			}
		}
		return reqsMap;
	}

	@Override
	public List<Itinerary> filterPromotedItineraties(Multimap<Integer, Itinerary> itineraries, RType criteria) {
		List<Itinerary> kept = new ArrayList<Itinerary>();
		for (Integer key : itineraries.keySet()) {
			List<Itinerary> toSort = (List<Itinerary>) itineraries.get(key);
			ItinerarySorter.sort(toSort, criteria);
			for (int i = 0; i < Math.min(Math.abs(key), toSort.size()); i++) {
				kept.add(toSort.get(i));
			}
		}
		return kept;
	}

	@Override
	public List<Itinerary> removeExtremeItineraties(List<Itinerary> itineraries, RType criteria) {
		List<Itinerary> newItineraries = new ArrayList<Itinerary>();
		List<Itinerary> toRemove = Lists.newArrayList();
		
		Set<Itinerary> original = new HashSet<Itinerary>();
		Set<Itinerary> promoted = new HashSet<Itinerary>();
		for (Itinerary it: itineraries) {
			if (it.isPromoted()) {
				promoted.add(it);
			} else {
				original.add(it);
			}
		}
		
		for (Itinerary it1: original) {
			for (Itinerary it2: promoted) {
				if (it1.equals(it2)) {
					toRemove.add(it2);
				}
			}
		}
		promoted.removeAll(toRemove);
		
		newItineraries.addAll(original);
		newItineraries.addAll(promoted);
		
		ItinerarySorter.sort(newItineraries, criteria);

		long maxTime = 0;
		double maxDistance = 0;

		for (Itinerary it : newItineraries) {
			if (it.isPromoted()) {
				continue;
			}
			maxTime = Math.max(maxTime, it.getDuration());
			double distance = 0;
			for (Leg leg : it.getLeg()) {
				distance += leg.getLength();
			}
			maxDistance = Math.max(maxDistance, distance);
		}

		toRemove = Lists.newArrayList();
		for (Itinerary it : newItineraries) {
			if (!it.isPromoted()) {
				continue;
			}
			if (it.getDuration() > maxTime + (1000 * 60 * 30)) {
				toRemove.add(it);
				logger.info("Removing by time: " + it.getDuration() + "/" + maxTime);
				continue;
			}

			double distance = 0;
			for (Leg leg : it.getLeg()) {
				distance += leg.getLength();
			}

			if (distance > 2 * maxDistance) {
				toRemove.add(it);
				logger.info("Removing by distance: " + distance + "/" + maxDistance);
				continue;
			}
		}

		newItineraries.removeAll(toRemove);

		toRemove = Lists.newArrayList();
		int promotedN = 0;
		for (Itinerary it : newItineraries) {
			if (it.isPromoted()) {
				promotedN++;
			} else {
				continue;
			}
			if (promotedN > 2 && it.isPromoted()) {
				logger.info("Removing too many");
				toRemove.add(it);
			}
		}

		newItineraries.removeAll(toRemove);

		return newItineraries;

	}

}
