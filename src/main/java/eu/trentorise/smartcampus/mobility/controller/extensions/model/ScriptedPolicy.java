package eu.trentorise.smartcampus.mobility.controller.extensions.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement (name = "policy")
public class ScriptedPolicy implements StorablePolicy {

	private String name;
	private String description;

	private String generatePlanRequests;
	private String evaluatePlanResults;
	private String extractItinerariesFromPlanResults;
	private String filterAndSortItineraries;
	
	private Boolean draft = true;
	
	public ScriptedPolicy() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}	
	
	public String getGeneratePlanRequests() {
		return generatePlanRequests;
	}

	public void setGeneratePlanRequests(String generate) {
		this.generatePlanRequests = generate;
	}

	public String getEvaluatePlanResults() {
		return evaluatePlanResults;
	}

	public void setEvaluatePlanResults(String evaluate) {
		this.evaluatePlanResults = evaluate;
	}

	public String getExtractItinerariesFromPlanResults() {
		return extractItinerariesFromPlanResults;
	}

	public void setExtractItinerariesFromPlanResults(String filter) {
		this.extractItinerariesFromPlanResults = filter;
	}

	public String getFilterAndSortItineraries() {
		return filterAndSortItineraries;
	}

	public void setFilterAndSortItineraries(String remove) {
		this.filterAndSortItineraries = remove;
	}

	public Boolean getDraft() {
		return draft;
	}

	public void setDraft(Boolean draft) {
		this.draft = draft;
	}		
	
}
