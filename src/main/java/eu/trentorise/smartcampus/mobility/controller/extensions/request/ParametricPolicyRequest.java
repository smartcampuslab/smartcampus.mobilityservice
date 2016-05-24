package eu.trentorise.smartcampus.mobility.controller.extensions.request;

import java.util.List;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningResultGroup;

public class ParametricPolicyRequest {

	private String name;
	private String description;
	
	private List<ParametricPolicyRequestElement> elements;
	private List<PlanningResultGroup> groups;

	private Boolean draft = true;
	
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

	public List<ParametricPolicyRequestElement> getElements() {
		return elements;
	}

	public void setElements(List<ParametricPolicyRequestElement> elements) {
		this.elements = elements;
	}

	public List<PlanningResultGroup> getGroups() {
		return groups;
	}

	public void setGroups(List<PlanningResultGroup> groups) {
		this.groups = groups;
	}

	public Boolean getDraft() {
		return draft;
	}

	public void setDraft(Boolean draft) {
		this.draft = draft;
	}


}
