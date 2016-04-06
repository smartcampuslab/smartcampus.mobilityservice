package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import eu.trentorise.smartcampus.mobility.controller.rest.ItinerarySorter;

public class TrentoGreenItineraryRequestEnricher implements ItineraryRequestEnricher {

	private static Log logger = LogFactory.getLog(TrentoGreenItineraryRequestEnricher.class);

	@Override
	public List<PlanRequest> addPromotedItineraries(SingleJourney request, TType type, RType routeType) {
		List<PlanRequest> reqList = Lists.newArrayList();
		int itn = Math.max(request.getResultsNumber(), 1);
		Map<TType, Integer> itnPerType = Maps.newTreeMap();
		List<TType> newTypes = new ArrayList<TType>();
		List<TType> requestedTypes = Arrays.asList(request.getTransportTypes());
		if (type.equals(TType.CAR)) {
			if (!requestedTypes.contains(TType.PARK_AND_RIDE)) {
				newTypes.add(TType.PARK_AND_RIDE);
			}
			if (!requestedTypes.contains(TType.TRANSIT)) {
				newTypes.add(TType.TRANSIT);
			}
		}
		if (type.equals(TType.TRANSIT) || type.equals(TType.BUS) || type.equals(TType.TRAIN)) {
			if (!requestedTypes.contains(TType.WALK)) {
				newTypes.add(TType.WALK);
				itnPerType.put(TType.WALK, 1);
			}
			if (!requestedTypes.contains(TType.TRAIN)) {
				newTypes.add(TType.TRAIN);
			}
		}
		for (TType newType : newTypes) {
			String req = String.format("from=%s,%s&to=%s,%s&date=%s&departureTime=%s&transportType=%s&routeType=%s&numOfItn=%s", request.getFrom().getLat(), request.getFrom().getLon(), request.getTo().getLat(), request.getTo().getLon(), request.getDate(), request.getDepartureTime(), newType, routeType, itn);
			PlanRequest pr = new PlanRequest();
			pr.setRequest(req);
			pr.setType(newType);
			pr.setRouteType(routeType);
			if (itnPerType.containsKey(newType)) {
				pr.setItineraryNumber(itnPerType.get(newType));	
			} else {
				pr.setItineraryNumber(itn);
			}
			if (newType.equals(TType.WALK) || newType.equals(TType.BICYCLE) || newType.equals(TType.SHAREDBIKE) || newType.equals(TType.SHAREDBIKE_WITHOUT_STATION)) {
				if (requestedTypes.contains(newType)) {
					pr.setValue(0.0);
					reqList.add(pr);
					continue;
				}
			}
			if (type.equals(TType.CAR)) {
				if (newType.equals(TType.PARK_AND_RIDE)) {
					pr.setValue(1.0);
				} else {
					pr.setValue(-1.0);
				}
			} else if (type.equals(TType.TRANSIT) || type.equals(TType.BUS) || type.equals(TType.TRAIN)) {
				pr.setValue(2.0);
			} else {
				System.out.println();
			}
			reqList.add(pr);
		}
		return reqList;
	}

	@Override
	public List<Itinerary> filterPromotedItineraties(Multimap<Double, Itinerary> itineraries, RType criteria) {
		List<Itinerary> kept = new ArrayList<Itinerary>();
		List<Itinerary> toRemove;
		for (Double key : itineraries.keySet()) {
			List<Itinerary> toSort = (List<Itinerary>) itineraries.get(key);
			Set<Itinerary> toSortSet = new HashSet<Itinerary>(toSort);
			toSort = new ArrayList<Itinerary>(toSortSet);
			ItinerarySorter.sort(toSort, criteria);
			Collections.reverse(toSort);
			int removeN = toSort.size() - (int)Math.min(Math.abs(key), toSort.size());
			toRemove =  new ArrayList<Itinerary>();
			for (Itinerary it: toSort) {
				if (toRemove.size() == removeN) {
					break;
				}
				boolean rem = true;
				for (Leg leg: it.getLeg()) {
					if ("116".equals(leg.getTransport().getAgencyId())) {
						rem = false;
						continue;
					}
				}
				
				if (rem) {
					toRemove.add(it);
				}
			}
			toSort.removeAll(toRemove);
			kept.addAll(toSort);
		}
		return kept;
	}

	@Override
	public List<Itinerary> removeExtremeItineraties(List<Itinerary> itineraries, RType criteria) {
		List<Itinerary> newItineraries = new ArrayList<Itinerary>();
		List<Itinerary> toRemove = Lists.newArrayList();

		Set<Itinerary> original = new HashSet<Itinerary>();
		Set<Itinerary> promoted = new HashSet<Itinerary>();
		for (Itinerary it : itineraries) {
			if (it.isPromoted()) {
				promoted.add(it);
			} else {
				original.add(it);
			}
		}

		newItineraries.addAll(original);
		newItineraries.addAll(promoted);

		ItinerarySorter.sort(newItineraries, criteria);

		long minTime = Long.MAX_VALUE;
		long maxTime = 0;
		long maxDuration = 0;
		double maxDistance = 0;

		for (Itinerary it : newItineraries) {
			if (it.isPromoted()) {
				continue;
			}
			minTime = Math.min(minTime, it.getEndtime());
			maxTime = Math.max(maxTime, it.getEndtime());
			maxDuration = Math.max(maxDuration, it.getDuration());
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
			if ((maxTime != 0 && it.getEndtime() > maxTime + (1000 * 60 * 30))
					|| (maxDuration != 0
							&& it.getDuration() > Math.min(maxDuration
									+ (1000 * 60 * 30), maxDuration * 1.5) && it
							.getEndtime() > minTime + (1000 * 60 * 10))
					|| (maxDuration != 0 && it.getDuration() > maxDuration + (1000 * 60 * 15))) {
				toRemove.add(it);
				logger.info("Removing by \"slow\" trip: " + it.getDuration() + "," + maxDuration + " / " + it.getStartime() + "," + maxTime);
				continue;
			}		
			

			double distance = 0;
			for (Leg leg : it.getLeg()) {
				distance += leg.getLength();
			}

			if (maxDistance != 0 && distance > 1.5 * maxDistance) {
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


		original = new HashSet<Itinerary>();
		promoted = new HashSet<Itinerary>();
		for (Itinerary it : newItineraries) {
			if (it.isPromoted()) {
				promoted.add(it);
			} else {
				original.add(it);
			}
		}

		for (Itinerary it1 : promoted) {
			for (Itinerary it2 : original) {
				if (it1.equals(it2)) {
					toRemove.add(it2);
				}
			}
		}
		original.removeAll(toRemove);	
		
		newItineraries = Lists.newArrayList(original);
		newItineraries.addAll(promoted);
	
	
		return newItineraries;

	}

	@Override
	public void completeResponse(SingleJourney journeyRequest, List<PlanRequest> planRequests, List<Itinerary> itineraries) {
		List<Itinerary> toKeep = Lists.newArrayList();

		for (PlanRequest pr : planRequests) {
			List<TType> req = Arrays.asList(journeyRequest.getTransportTypes());
			if (pr.getType().equals(TType.WALK) || pr.getType().equals(TType.BICYCLE) || pr.getType().equals(TType.SHAREDBIKE) || pr.getType().equals(TType.SHAREDBIKE_WITHOUT_STATION)) {
				if (req.contains(pr.getType()) && pr.getValue() != 0.0) {
					for (Itinerary it : pr.getItinerary()) {
						it.setPromoted(true);
						toKeep.add(it);
					}
				}
			}
		}

		List<Itinerary> toRemove = Lists.newArrayList();
		int promotedN = 0;
		for (Itinerary it : itineraries) {
			if (it.isPromoted()) {
				promotedN++;
			}
		}

		Collections.reverse(itineraries);
		if (promotedN > 2) {
			for (Itinerary it : itineraries) {
				if (promotedN > 2 && it.isPromoted() && !toKeep.contains(it)) {
					logger.info("Removing too many");
					toRemove.add(it);
					promotedN--;
				}
			}
		}

		itineraries.removeAll(toRemove);

		Collections.reverse(itineraries);

	}

	@Override
	public void sort(List<Itinerary> itineraries, RType criterion) {
		if (criterion.equals(RType.fastest)) {
			Collections.sort(itineraries, new Comparator<Itinerary>() {
				@Override
				public int compare(Itinerary o1, Itinerary o2) {
					long o1End = getModifiedEndTime(o1);
					long o2End = getModifiedEndTime(o2);
					
					return (int) (o1End - o2End);
				}
			});
		} else {
			ItinerarySorter.sort(itineraries, criterion);
		}
	}	
	
	private long getModifiedEndTime(Itinerary it) {
		ObjectMapper mapper = new ObjectMapper();
		long endTime = it.getEndtime();
		for (Leg leg: it.getLeg()) {
			if (!leg.getTransport().getType().equals(TType.CAR)) {
				continue;
			}
			if (leg.getTo().getStopId() != null && leg.getTo().getStopId().getExtra() != null && leg.getTo().getStopId().getExtra().containsKey("costData")) {
				Map<String, String> costData = mapper.convertValue(leg.getTo().getStopId().getExtra().get("costData"), Map.class);
				Double fixedCost = costData.containsKey("fixedCost")?Double.parseDouble(costData.get("fixedCost")):0;
				endTime += (long)(8 * 1000 * 60 * fixedCost);
			}
			if (leg.getTo().getStopId() != null && leg.getTo().getStopId().getExtra() != null && leg.getTo().getStopId().getExtra().containsKey("searchTime")) {
				Map<String, String> searchTime = mapper.convertValue(leg.getTo().getStopId().getExtra().get("searchTime"), Map.class);
				Integer max = searchTime.containsKey("searchTime")?Integer.parseInt(searchTime.get("max")):0;
				endTime += max * 1000 * 60;
			}			
		}
		
		return endTime;
	}
	
	@Override
	public boolean mustRetry(List<Itinerary> itineraries) {
		if (itineraries.isEmpty()) {
			return true;
		}
		return false;
	}	
	
}
