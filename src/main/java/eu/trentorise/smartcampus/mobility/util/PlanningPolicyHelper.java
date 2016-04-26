package eu.trentorise.smartcampus.mobility.util;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import eu.trentorise.smartcampus.mobility.controller.extensions.ItinerariesAnalysis;
import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningRequest;
import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningRequest.SmartplannerParameter;
import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningResultGroup;
import eu.trentorise.smartcampus.mobility.controller.rest.ItinerarySorter;

public class PlanningPolicyHelper {
	
	public final static Integer MIN_ITN_N = 3;

	public static List<PlanningRequest> generateOriginalPlanRequests(SingleJourney request) {
		List<PlanningRequest> result = Lists.newArrayList();
		
		for (TType type : request.getTransportTypes()) {
			PlanningRequest pr = new PlanningRequest();
			pr.setType(type);
			pr.setRouteType(request.getRouteType());
			pr.setItineraryNumber(request.getResultsNumber() != 0 ? request.getResultsNumber() : MIN_ITN_N);
			pr.setWheelChair(request.isWheelchair());
			pr.setOriginalRequest(request);
			
			result.add(pr);
		}
		
		return result;
	}
	
//	public static PlanningRequest buildPartialDerivedRequest(SingleJourney request, PlanningRequest originalPlanningRequest, boolean promoted) {
//		PlanningRequest pr = new PlanningRequest();
//		pr.setRouteType(request.getRouteType());
//		pr.setItineraryNumber(originalPlanningRequest.getItineraryNumber());
//		pr.setWheelChair(originalPlanningRequest.isWheelChair());
//		pr.setOriginalRequest(request);
//		pr.setDerived(true);
//		pr.setPromoted(promoted);
//		
//		return pr;
//	}
//	
//	public static void completePartialDerivedRequest(PlanningRequest pr, TType type, Integer itineraryNumber, PlanningResultGroup group) {
//		pr.setType(type);
//		if (itineraryNumber != null) {
//			pr.setItineraryNumber(itineraryNumber);
//		}
//		pr.setGroup(group);
//	}	
	
	public static PlanningRequest buildDefaultDerivedRequest(SingleJourney request, PlanningRequest originalPlanningRequest, TType ttype,  RType rtype, Integer itineraryNumber, boolean promoted, PlanningResultGroup group) {
		PlanningRequest pr = new PlanningRequest();
		pr.setParentRequest(pr);
		if (rtype != null) {
			pr.setRouteType(rtype);
		} else {
			pr.setRouteType(request.getRouteType());
		}
		if (itineraryNumber != null) {
			pr.setItineraryNumber(itineraryNumber);
		} else {
			pr.setItineraryNumber(request.getResultsNumber() != 0 ? request.getResultsNumber() : MIN_ITN_N);
		}
		pr.setWheelChair(originalPlanningRequest.isWheelChair());
		pr.setOriginalRequest(request);
		pr.setDerived(true);
		pr.setPromoted(promoted);	
		
		pr.setType(ttype);
		pr.setGroup(group);
		
		return pr;
	}

	public static void buildSmartplannerRequests(List<PlanningRequest> planRequests) {
		for (PlanningRequest pr: planRequests) {
			buildSmartplannerRequest(pr);
		}
		
	}
	
	public static void buildSmartplannerRequest(PlanningRequest pr) {
		String req = String.format("from=%s,%s&to=%s,%s&date=%s&departureTime=%s&transportType=%s&routeType=%s&numOfItn=%s&wheelchair=%b", pr.getOriginalRequest().getFrom().getLat(), pr
				.getOriginalRequest().getFrom().getLon(), pr.getOriginalRequest().getTo().getLat(), pr.getOriginalRequest().getTo().getLon(), pr.getOriginalRequest().getDate(), pr
				.getOriginalRequest().getDepartureTime(), pr.getType(), pr.getRouteType(), pr.getItineraryNumber(), pr.isWheelChair());
		for (SmartplannerParameter key : pr.getSmartplannerParameters().keySet()) {
			Object value = pr.getSmartplannerParameters().get(key);
			req += "&" + key + "=" + value;
		}
		pr.setRequest(req);

	}
	
	public static List<Itinerary> filterByGroups(List<PlanningRequest> planRequests, Comparator<Itinerary> comparator) {
		Map<PlanningResultGroup, Collection<Itinerary>> groupMap = PlanningPolicyHelper.collectByValue(planRequests);
		
		List<Itinerary> remaining = Lists.newArrayList();
		for (PlanningResultGroup prg : groupMap.keySet()) {
			List<Itinerary> origIts = Lists.newArrayList(groupMap.get(prg));
			if (prg.getMaxEntries() != null) {
				ItinerarySorter.sort(origIts, comparator);
				origIts = origIts.subList(0, Math.min(prg.getMaxEntries(), origIts.size()));
				groupMap.put(prg, origIts);
			}
			remaining.addAll(origIts);
		}
		
		return remaining;
	}
	
	private static Map<PlanningResultGroup, Collection<Itinerary>> collectByValue(List<PlanningRequest> planRequests) {
		Multimap<PlanningResultGroup, PlanningRequest> prgMap = ArrayListMultimap.create();
		Multimap<PlanningResultGroup, Itinerary> itMap = ArrayListMultimap.create();

		for (PlanningRequest pr: planRequests) {
			if (pr.getGroup() != null) {
				prgMap.put(pr.getGroup(), pr);
			} else {
				prgMap.put(PlanningResultGroup.FAKE_GROUP, pr);
			}
		}
		
		for (PlanningResultGroup prg: prgMap.keySet()) {
			for (PlanningRequest pr: prgMap.get(prg)) {
				itMap.putAll(prg, pr.getItinerary());
			}
		}
		
		return new HashMap(itMap.asMap());
	}
	
	public static ItinerariesAnalysis analyzeItineraries(List<Itinerary> itineraries, Boolean promoted) {
		ItinerariesAnalysis analysis = new ItinerariesAnalysis();
		
		for (Itinerary it : itineraries) {
			if (promoted != null && it.isPromoted() != promoted) {
				continue;
			}
			analysis.minTime = Math.min(analysis.minTime, it.getEndtime());
			analysis.maxTime = Math.max(analysis.maxTime, it.getEndtime());
			analysis.minDuration = Math.min(analysis.minDuration, it.getDuration());
			analysis.maxDuration = Math.max(analysis.maxDuration, it.getDuration());
			double distance = 0;
			for (Leg leg : it.getLeg()) {
				distance += leg.getLength();
			}
			analysis.minDistance = Math.min(analysis.minDistance, distance);
			analysis.maxDistance = Math.max(analysis.maxDistance, distance);
		}
		
		return analysis;
	}
	
	public static List<Itinerary> keepPromotedDuplicated(List<Itinerary> itineraries) {
		Set<Itinerary> original = Sets.newHashSet();
		Set<Itinerary> promoted = Sets.newHashSet();
		for (Itinerary it : itineraries) {
			if (it.isPromoted()) {
				promoted.add(it);
			} else {
				original.add(it);
			}
		}

		List<Itinerary> toRemove = Lists.newArrayList();
		for (Itinerary it1 : promoted) {
			for (Itinerary it2 : original) {
				if (it1.equals(it2)) {
					toRemove.add(it2);
				}
			}
		}
		
		original.removeAll(toRemove);
		List<Itinerary> result = Lists.newArrayList(original);
		result.addAll(promoted);
		
		return result;
	}
	
	
	public static List<Itinerary> keepBestPromoted(List<Itinerary> itineraries, Comparator<Itinerary> comparator, int max) {
		List<Itinerary> original = Lists.newArrayList();
		List<Itinerary> promoted = Lists.newArrayList();
		for (Itinerary it : itineraries) {
			if (it.isPromoted()) {
				promoted.add(it);
			} else {
				original.add(it);
			}
		}

		ItinerarySorter.sort(promoted, comparator);
		promoted = promoted.subList(0, Math.min(max, promoted.size()));
		
		List<Itinerary> result = Lists.newArrayList(original);
		result.addAll(promoted);
		
		return result;
	}	
	
	
	
}
