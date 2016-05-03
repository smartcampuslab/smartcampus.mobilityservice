package eu.trentorise.smartcampus.mobility.controller.extensions.model;

import java.util.List;

import com.google.common.collect.Lists;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningResultGroup;

public class ParametricPolicy implements StorablePolicy {

	private String name;
	private String description;

	private List<ParametricGenerate> generate;

	private List<ParametricModify> modify;
	private List<ParametricEvaluate> evaluate;
	private ParametricRemove remove;
	
	private List<PlanningResultGroup> groups;
	
	private Boolean draft = true;

	public ParametricPolicy() {
		generate = Lists.newArrayList();
		modify = Lists.newArrayList();
		evaluate = Lists.newArrayList();
		groups = Lists.newArrayList();
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

	public List<ParametricGenerate> getGenerate() {
		return generate;
	}

	public void setGenerate(List<ParametricGenerate> generate) {
		this.generate = generate;
	}

	public List<ParametricModify> getModify() {
		return modify;
	}

	public void setModify(List<ParametricModify> modify) {
		this.modify = modify;
	}

	public List<ParametricEvaluate> getEvaluate() {
		return evaluate;
	}

	public void setEvaluate(List<ParametricEvaluate> evaluate) {
		this.evaluate = evaluate;
	}

	public ParametricRemove getRemove() {
		return remove;
	}

	public void setRemove(ParametricRemove remove) {
		this.remove = remove;
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
