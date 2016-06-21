package eu.trentorise.smartcampus.mobility.controller.extensions.compilable;

import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningRequest;

public class CompilablePolicy implements PlanningPolicy {

	private CompilablePolicyData data;
	
	public CompilablePolicy() {
		data = new CompilablePolicyData();
	}
	
	public CompilablePolicy(CompilablePolicyData data) {
		this.data = data;
	}	
	
	@Override
	public String getName() {
		return data.getName();
	}

	@Override
	public String getDescription() {
		return data.getDescription();
	}

	@Override
	public Boolean getDraft() {
		return data.getDraft();
	}

	@Override
	public PolicyType getPolicyType() {
		return PolicyType.compiled;
	}

	@Override
	public List<PlanningRequest> generatePlanRequests(SingleJourney journeyRequest) {
		String script = data.getGenerateCode();
		Map<String, Object> map = Maps.newTreeMap();
		map.put("journeyRequest", journeyRequest);		
		List<PlanningRequest> result = (List<PlanningRequest>)evaluate(map, script);
		return result;
	}

	@Override
	public List<PlanningRequest> evaluatePlanResults(List<PlanningRequest> planRequests) {
		String script = data.getEvaluateCode();
		Map<String, Object> map = Maps.newTreeMap();
		map.put("planRequests", planRequests);		
		List<PlanningRequest> result = (List<PlanningRequest>)evaluate(map, script);
		return result;
	}

	@Override
	public List<Itinerary> extractItinerariesFromPlanResults(SingleJourney journeyRequest, List<PlanningRequest> planRequests) {
		String script = data.getExtractCode();
		Map<String, Object> map = Maps.newTreeMap();
		map.put("journeyRequest", journeyRequest);	
		map.put("planRequests", planRequests);		
		List<Itinerary> result = (List<Itinerary>)evaluate(map, script);
		return result;
	}

	@Override
	public List<Itinerary> filterAndSortItineraries(SingleJourney journeyRequest, List<Itinerary> itineraries) {
		String script = data.getFilterCode();
		Map<String, Object> map = Maps.newTreeMap();
		map.put("itineraries", itineraries);	
		List<Itinerary> result = (List<Itinerary>)evaluate(map, script);
		return result;
	}
	
	private List evaluate(Map map, String script) throws GroovyRuntimeException {
		Binding binding = new Binding(map);

		GroovyShell shell = new GroovyShell(binding);
		
		List result = (List)shell.evaluate(script);
		
		return result;
	}	

}
