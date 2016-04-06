package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import eu.trentorise.smartcampus.mobility.controller.extensions.model.AddPromotedPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.model.Policies;
import eu.trentorise.smartcampus.mobility.controller.rest.ItinerarySorter;

public class CustomItineraryRequestEnricher implements ItineraryRequestEnricher {

	private Policies policies;
	
	private final static double KEY_INC = 0.01;
	
	private static Log logger = LogFactory.getLog(CustomItineraryRequestEnricher.class);
	
	public static CustomItineraryRequestEnricher build(String name) {
		CustomItineraryRequestEnricher ire = new CustomItineraryRequestEnricher();
		ire.setPolicies(null); 
		return ire;
	}
	
	public Policies getPolicies() {
		return policies;
	}

	public void setPolicies(Policies policies) {
		this.policies = policies;
	}

	@Override
	public List<PlanRequest> addPromotedItineraries(SingleJourney request, TType type, RType routeType) {
		double keyInc = KEY_INC;
		
		List<PlanRequest> result = Lists.newArrayList();
		List<TType> requestTType = Arrays.asList(request.getTransportTypes());

		for (AddPromotedPolicy policy : policies.getAddPromotedPolicies()) {
			int itn = Math.max(request.getResultsNumber(), 1);

			if (policy.getNotRequestTransportTypes().contains(type)) {
				continue;
			}
			if (requestTType.contains(policy.getNewTransportType())) {
				continue;
			}
			if (policy.getNotRequestRouteType() != null && policy.getNotRequestRouteType().equals(routeType)) {
				continue;
			}
			if (!policy.getRequestTransportTypes().contains(type)) {
				continue;
			}
			if (policy.getRequestRouteType() != null && policy.getRequestRouteType().equals(routeType)) {
				continue;
			}

			if (policy.getNewItnPerType() != null) {
				itn = policy.getNewItnPerType();
			}
			RType newRouteType = routeType;
			if (policy.getNewRouteType() != null) {
				newRouteType = policy.getNewRouteType();
			}

			PlanRequest pr = new PlanRequest();
			pr.setRouteType(newRouteType);
			pr.setType(policy.getNewTransportType());
			pr.setValue(policy.getMaxToKeep() + keyInc);
			pr.setItineraryNumber(itn);
			keyInc += KEY_INC;
			
			result.add(pr);
		}

		return result;
	}

	// same for all
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
		// TODO Auto-generated method stub
		return itineraries;
	}

	// same for all
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
		// TODO Auto-generated method stub

	}
	

	@Override
	public boolean mustRetry(List<Itinerary> itineraries) {
		if (policies.getRetryOnFail() == null || policies.getRetryOnFail() == false) {
			return false;
		} else if (itineraries.isEmpty()) {
			return true;
		}
		return false;
	}

}
