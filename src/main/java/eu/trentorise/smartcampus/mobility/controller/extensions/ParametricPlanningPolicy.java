package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import eu.trentorise.smartcampus.mobility.controller.extensions.model.ParametricPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.request.ParametricPolicyRequest;
import eu.trentorise.smartcampus.mobility.controller.extensions.request.ParametricPolicyRequestElement;
import eu.trentorise.smartcampus.mobility.controller.extensions.request.ParametricPolicyRequestElement.ParametricPolicyRequestType;
import eu.trentorise.smartcampus.mobility.controller.rest.ItinerarySorter;
import eu.trentorise.smartcampus.mobility.util.PlanningPolicyHelper;
import eu.trentorise.smartcampus.mobility.util.ScriptingHelper;

public class ParametricPlanningPolicy implements PlanningPolicy {

	private ParametricPolicy parameters;
	private ParametricPolicyRequest ppr;
	
	private static Log logger = LogFactory.getLog(ParametricPlanningPolicy.class);
	
	public ParametricPlanningPolicy(ParametricPolicy parameters) {
		super();
		this.parameters = parameters;
	}
	
	public ParametricPlanningPolicy(ParametricPolicyRequest ppr) {
		super();
		this.ppr = ppr;
	}	

	private Multimap<ParametricPolicyRequestType, ParametricPolicyRequestElement> collectElementsByType(List<ParametricPolicyRequestElement> elements) {
		Multimap<ParametricPolicyRequestType, ParametricPolicyRequestElement> result = ArrayListMultimap.create();
		
		for (ParametricPolicyRequestElement element: elements) {
			if (element.isEnabled()) {
				result.put(element.getType(), element);
			}
		}
		return result;
	}
	
	@Override
	public List<PlanningRequest> generatePlanRequests(SingleJourney journeyRequest) {
		Map<String, PlanningResultGroup> prgMap = Maps.newTreeMap();
		for (PlanningResultGroup prg: ppr.getGroups()) {
			prgMap.put(prg.getName(), prg);
		}
		
		List<PlanningRequest> originalPlanningRequests = PlanningPolicyHelper.generateOriginalPlanRequests(journeyRequest);
		
		List<PlanningRequest> result = Lists.newArrayList();
		List<TType> requestTTypes = Arrays.asList(journeyRequest.getTransportTypes());
		Set<TType> allTypes = Sets.newHashSet(requestTTypes);
		
		Multimap<ParametricPolicyRequestType, ParametricPolicyRequestElement> elementsByType = collectElementsByType(ppr.getElements());

		ScriptingHelper sh = new ScriptingHelper();
		
		for (PlanningRequest pr: originalPlanningRequests) {
			for (ParametricPolicyRequestElement pg : elementsByType.get(ParametricPolicyRequestType.generate)) {
				if (pg.getCondition() != null && pg.getCondition().getFormula() != null) {				
				boolean eval = sh.evaluateConditionFormula(pr, pg.getCondition().getFormula());
				
				System.out.println(eval);
				if (!eval) {
					continue;
				}				
				}
				
				PlanningResultGroup prg = null;
				if (pg.getAction().getPlanningResultGroupName() != null) {
					prg = prgMap.get(pg.getAction().getPlanningResultGroupName());
				} else {
					prg = PlanningResultGroup.FAKE_GROUP;
				}
				
				PlanningRequest npr = PlanningPolicyHelper.buildDefaultDerivedRequest(journeyRequest, pr, pg.getAction().getNewTType(), pg.getAction().getNewRType(), pg.getAction().getNewItineraryNumber(), pg.getAction().getWheelchair(), pg.getAction().getPromoted(), prg);
				result.add(npr);
				if (pg.getAction().getNewTType() != null) {
					allTypes.add(pg.getAction().getNewTType());
				}
				
			}
		}
		
		result.addAll(originalPlanningRequests);
		
		for (PlanningRequest pr: result) {
			for (ParametricPolicyRequestElement pm : elementsByType.get(ParametricPolicyRequestType.modify)) {
				if (pm.getCondition() != null && pm.getCondition().getFormula() != null) {
				boolean eval = sh.evaluateConditionFormula(pr, pm.getCondition().getFormula());
				
				System.out.println(eval);
				if (!eval) {
					continue;
				}
				}

				
				if (pm.getAction().getSmartplannerParameters() != null) {
					pr.setSmartplannerParameters(pm.getAction().getSmartplannerParameters());
				}
				if (pm.getAction().getNewTType() != null) {
					pr.setType(pm.getAction().getNewTType());
				}
				if (pm.getAction().getNewRType() != null) {
					pr.setRouteType(pm.getAction().getNewRType());
				}	
				if (pm.getAction().getNewItineraryNumber() != null) {
					pr.setItineraryNumber(pm.getAction().getNewItineraryNumber());
				}
				if (pm.getAction().getWheelchair() != null) {
					pr.setWheelChair(pm.getAction().getWheelchair());
				}									
			}
		}
		
		PlanningPolicyHelper.buildSmartplannerRequests(result);
		
		return result;
	}	
	
	
//	public List<PlanningRequest> _generatePlanRequests(SingleJourney journeyRequest) {
//		Map<String, PlanningResultGroup> prgMap = Maps.newTreeMap();
//		for (PlanningResultGroup prg: ppr.getGroups()) {
//			prgMap.put(prg.getName(), prg);
//		}
//		
//		List<PlanningRequest> originalPlanningRequests = PlanningPolicyHelper.generateOriginalPlanRequests(journeyRequest);
//		
//		List<PlanningRequest> result = Lists.newArrayList();
//		List<TType> requestTTypes = Arrays.asList(journeyRequest.getTransportTypes());
//		Set<TType> allTypes = Sets.newHashSet(requestTTypes);
//
//		for (PlanningRequest pr: originalPlanningRequests) {
//			for (ParametricGenerate pg : parameters.getGenerate()) {
//				// not request ttype
//				if (pg.getRequestTTypes() != null && !pg.getRequestTTypes().contains(pr.getType())) {
//					continue;
//				}
//				// already in requested or generated
//				if (requestTTypes.contains(pg.getNewTType()) || allTypes.contains(pg.getNewTType())) {
//					continue;
//				}
//				// not request rtype
//				if (pg.getRequestRType() != null && !pg.getRequestRType().equals(journeyRequest.getRouteType())) {
//					continue;
//				}
//
//				// in notrequested
//				if (pg.getNotRequestTransportTypes() != null && pg.getNotRequestTransportTypes().contains(pr.getType())) {
//					continue;
//				}	
//				if (pg.getNotRequestRouteType() != null && pg.getNotRequestRouteType().equals(journeyRequest.getRouteType())) {
//					continue;
//				}				
//				
//				PlanningResultGroup prg = null;
//				if (pg.getPlanningResultGroupName() != null) {
//					prg = prgMap.get(pg.getPlanningResultGroupName());
//				} else {
//					prg = PlanningResultGroup.FAKE_GROUP;
//				}
//				
//				PlanningRequest npr = PlanningPolicyHelper.buildDefaultDerivedRequest(journeyRequest, pr, pg.getNewTType(), pg.getNewRType(), pg.getNewItineraryNumber(), null, true, prg);
//				result.add(npr);
//				allTypes.add(pg.getNewTType());				
//				
//			}
//		}
//		
//		result.addAll(originalPlanningRequests);
//		
//		for (PlanningRequest pr: result) {
//			for (ParametricModify pm : parameters.getModify()) {
//				// not ttype
//				if (pm.gettTypes() != null && !pm.gettTypes().contains(pr.getType())) {
//						continue;
//				}
//				// not rtype
//				if (pm.getrType() != null && !pm.getrType().equals(pr.getRouteType())) {
//						continue;
//				}
//				if (pm.getNotRType() != null && pm.getNotRType().equals(pr.getRouteType())) {
//					continue;
//				}								
//				if (pm.getPromoted() != null && !pm.getPromoted() == pr.isPromoted()) {
//					continue;
//				}
//				if (pm.getDerived() != null && !pm.getDerived() == pr.isDerived()) {
//					continue;
//				}
//				
//				if (pm.getSmartplannerParameters() != null) {
//					pr.setSmartplannerParameters(pm.getSmartplannerParameters());
//				}
//				if (pm.getNewRType() != null) {
//					pr.setRouteType(pm.getNewRType());
//				}
//			}
//		}
//		
//		PlanningPolicyHelper.buildSmartplannerRequests(result);
//		
//		return result;
//	}


	@Override
	public List<PlanningRequest> evaluatePlanResults(List<PlanningRequest> planRequests) {
		List<PlanningRequest> ok = Lists.newArrayList();
		List<PlanningRequest> unrecoverable = Lists.newArrayList();

		Multimap<ParametricPolicyRequestType, ParametricPolicyRequestElement> elementsByType = collectElementsByType(ppr.getElements());
		ScriptingHelper sh = new ScriptingHelper();

		for (PlanningRequest pr : planRequests) {
			boolean isOk = true;
			for (ParametricPolicyRequestElement pe : elementsByType.get(ParametricPolicyRequestType.evaluate)) {
				boolean eval = false;
				if (pe.getCondition() != null && pe.getCondition().getFormula() != null) {
					eval = sh.evaluateConditionFormula(pr, pe.getCondition().getFormula());

					System.out.println(eval);

				}

				if (eval) {
					isOk = false;
					if (pe.getAction().getNewRType() != null) {
						pr.setRouteType(pe.getAction().getNewRType());
					}
					if (pe.getAction().getSmartplannerParameters() != null) {
						pr.setSmartplannerParameters(pe.getAction().getSmartplannerParameters());
					}
					if (pe.getAction().getUnrecoverable() != null && pe.getAction().getUnrecoverable()) {
						unrecoverable.add(pr);
					}
					PlanningPolicyHelper.buildSmartplannerRequest(pr);
				} 
			}
			
			if (isOk) {
				ok.add(pr);
			}
		}

		planRequests.removeAll(ok);
		planRequests.removeAll(unrecoverable);

		return ok;
	}	
	
	
//	public List<PlanningRequest> _evaluatePlanResults(List<PlanningRequest> planRequests) {
//		List<PlanningRequest> ok = Lists.newArrayList();
//		List<PlanningRequest> unrecoverable = Lists.newArrayList();
//		for (PlanningRequest pr : planRequests) {
//			if (!pr.getItinerary().isEmpty()) {
//				ok.add(pr);
//				continue;
//			}
//			
//			for (ParametricEvaluate pe : parameters.getEvaluate()) {
//				// not ttype
//				if (pe.gettTypes() != null && !pe.gettTypes().contains(pr.getType())) {
//						continue;
//				}
//				// not rtype
//				if (pe.getrType() != null && !pe.getrType().equals(pr.getRouteType())) {
//						continue;
//				}
//				
//				// wrong iteration
//				if (pe.getIteration() != null && pe.getIteration() != pr.getIteration()) {
//					continue;
//				}
//				
//				if (pe.getNewRType() != null) {
//					pr.setRouteType(pe.getNewRType());
//				}
//				if (pe.getSmartplannerParameters() != null) {
//					pr.setSmartplannerParameters(pe.getSmartplannerParameters());
//				}
//				if (pe.getUnrecoverable() != null && pe.getUnrecoverable()) {
//					unrecoverable.add(pr);
//				}
//				PlanningPolicyHelper.buildSmartplannerRequest(pr);
//			}
//		}
//
//		planRequests.removeAll(ok);
//		planRequests.removeAll(unrecoverable);		
//		
//		return ok;
//	}

	
//	@Override
//	public List<PlanningRequest> evaluatePlanResults(List<PlanningRequest> planRequests) {
//		List<PlanningRequest> ok = Lists.newArrayList();
//		List<PlanningRequest> unrecoverable = Lists.newArrayList();
//		for (PlanningRequest pr : planRequests) {
//
//			if (!pr.getItinerary().isEmpty()) {
//				ok.add(pr);
//			} else if (pr.getType().equals(TType.TRANSIT) || pr.getType().equals(TType.BUS) && pr.getRouteType().equals(RType.leastWalking) && pr.getIteration() == 0) {
//				pr.setSmartplannerParameter(SmartplannerParameter.maxWalkDistance, 1000);
//				PlanningPolicyHelper.buildSmartplannerRequest(pr);
//			} else {
//				unrecoverable.add(pr);
//			}
//
//		}
//		planRequests.removeAll(ok);
//		planRequests.removeAll(unrecoverable);
//		return ok;
//	}	
	
	
	@Override
	public List<Itinerary> extractItinerariesFromPlanResults(SingleJourney journeyRequest, List<PlanningRequest> planRequests) {
		Comparator<Itinerary> comparator = ItinerarySorter.comparatorByRouteType(journeyRequest.getRouteType());
		
		List<Itinerary> remaining = PlanningPolicyHelper.filterByGroups(planRequests, comparator);

		return remaining;
	}

	@Override
	public List<Itinerary> filterAndSortItineraries(SingleJourney journeyRequest, List<Itinerary> itineraries) {
		// TODO more comparators, not only RType
		Comparator<Itinerary> comparator = ItinerarySorter.comparatorByRouteType(journeyRequest.getRouteType());
		List<Itinerary> result = PlanningPolicyHelper.keepPromotedDuplicated(itineraries);

		// last par: parameters.getRemove().getPromotedToKeep()
		result = PlanningPolicyHelper.keepBestPromoted(result, comparator, 2);

		ItinerarySorter.sort(result, comparator);
		return result;

	}
	
//	@Override
//	public List<Itinerary> _filterAndSortItineraries(SingleJourney journeyRequest, List<Itinerary> itineraries) {
//		List<Itinerary> result = Lists.newArrayList(itineraries);
//		Comparator<Itinerary> comparator = ItinerarySorter.comparatorByRouteType(journeyRequest.getRouteType());
//		
//		if (parameters.getRemove() != null) {
//			ItinerariesAnalysis analysis = PlanningPolicyHelper.analyzeItineraries(itineraries, false);
//			List<Itinerary> toRemove = Lists.newArrayList();			
//			
//			for (Itinerary it : itineraries) {
//				if (!it.isPromoted()) {
//					continue;
//				}
//
//				if ((analysis.maxTime != 0 && it.getEndtime() > analysis.maxTime + parameters.getRemove().getMaxEndtimeFromMaxTime())
//						|| (analysis.maxDuration != 0 && it.getDuration() > Math.min(analysis.maxDuration + parameters.getRemove().getMaxDurationFixed(), analysis.maxDuration * parameters.getRemove().getMaxDurationCoefficient()) && it.getEndtime() > analysis.minTime + (1000 * 60 * 10))
//						|| (analysis.maxDuration != 0 && it.getDuration() > analysis.maxDuration + parameters.getRemove().getMaxDurationFromMaxDuration())) {
//					toRemove.add(it);
//					logger.info("Removing by \"slow\" trip: " + it.getDuration() + "," + analysis.maxDuration + " / " + it.getStartime() + "," + analysis.maxTime);
//					continue;
//				}
//
//				double distance = 0;
//				for (Leg leg : it.getLeg()) {
//					distance += leg.getLength();
//				}
//
//				if (analysis.maxDistance != 0 && distance > parameters.getRemove().getMaxDistanceCoefficient() * analysis.maxDistance) {
//					toRemove.add(it);
//					logger.info("Removing by distance: " + distance + "/" + analysis.maxDistance);
//					continue;
//				}
//			}
//			
//			result.removeAll(toRemove);
//			
//			result = PlanningPolicyHelper.keepPromotedDuplicated(result);
//			
//			result = PlanningPolicyHelper.keepBestPromoted(result, comparator, parameters.getRemove().getPromotedToKeep());			
//		}
//		
//		ItinerarySorter.sort(result, comparator);
//		return result;
//	}

	public ParametricPolicy getParameters() {
		return parameters;
	}

	public void setParameters(ParametricPolicy parameters) {
		this.parameters = parameters;
	}

	@Override
	public String getName() {
		return ppr.getName();
	}


	@Override
	public String getDescription() {
		return ppr.getDescription();
	}	
	
	@Override
	public Boolean getDraft() {
		return ppr.getDraft();
	}	
	
	@Override
	public PolicyType getPolicyType() {
		return PolicyType.parametric;
	}	
	
}
