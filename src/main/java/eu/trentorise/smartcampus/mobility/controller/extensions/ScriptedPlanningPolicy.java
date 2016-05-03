package eu.trentorise.smartcampus.mobility.controller.extensions;

import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import eu.trentorise.smartcampus.mobility.controller.extensions.model.ScriptedPolicy;

public class ScriptedPlanningPolicy implements PlanningPolicy {

	private ScriptedPolicy scripts;
	
	public static final String IMPORTS = "import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;\n"
			+ "import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningRequest;\n"
			+ "import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningRequest.SmartplannerParameter;\n"
			+ "import it.sayservice.platform.smartplanner.data.message.Itinerary;\n"
			+ "import it.sayservice.platform.smartplanner.data.message.Leg;\n"
			+ "import it.sayservice.platform.smartplanner.data.message.RType;\n"
			+ "import it.sayservice.platform.smartplanner.data.message.TType;\n"
			+ "import eu.trentorise.smartcampus.mobility.util.PlanningPolicyHelper;\n"
			+ "import eu.trentorise.smartcampus.mobility.controller.rest.ItinerarySorter;\n"
			+ "import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningResultGroup\n";
	
	public ScriptedPlanningPolicy(ScriptedPolicy scripts) {
		super();
		this.scripts = scripts;
	}
	
	
	@Override
	public List<PlanningRequest> generatePlanRequests(SingleJourney journeyRequest) {
		String script = IMPORTS + scripts.getGeneratePlanRequests();
		Map<String, Object> map = Maps.newTreeMap();
		map.put("journeyRequest", journeyRequest);		
		List<PlanningRequest> result = (List<PlanningRequest>)evaluate(map, script);
		return result;
	}

	@Override
	public List<PlanningRequest> evaluatePlanResults(List<PlanningRequest> planRequests) {
		String script = IMPORTS + scripts.getEvaluatePlanResults();
		Map<String, Object> map = Maps.newTreeMap();
		map.put("planRequests", planRequests);		
		List<PlanningRequest> result = (List<PlanningRequest>)evaluate(map, script);
		return result;
	}

	@Override
	public List<Itinerary> extractItinerariesFromPlanResults(SingleJourney journeyRequest, List<PlanningRequest> planRequests) {
		String script = IMPORTS + scripts.getExtractItinerariesFromPlanResults();
		Map<String, Object> map = Maps.newTreeMap();
		map.put("journeyRequest", journeyRequest);
		map.put("planRequests", planRequests);
		List<Itinerary> result = (List<Itinerary>)evaluate(map, script);
		return result;
	}

	@Override
	public List<Itinerary> filterAndSortItineraries(SingleJourney journeyRequest, List<Itinerary> itineraries) {
		String script = IMPORTS + scripts.getFilterAndSortItineraries();
		Map<String, Object> map = Maps.newTreeMap();
		map.put("journeyRequest", journeyRequest);
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


	@Override
	public String getName() {
		return scripts.getName();
	}


	@Override
	public String getDescription() {
		return scripts.getDescription();
	}
	
	@Override
	public Boolean getDraft() {
		return scripts.getDraft();
	}	
	
}
