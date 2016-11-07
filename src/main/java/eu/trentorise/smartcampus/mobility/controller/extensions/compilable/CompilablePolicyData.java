package eu.trentorise.smartcampus.mobility.controller.extensions.compilable;

import java.util.List;

import com.google.common.collect.Lists;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningResultGroup;

public class CompilablePolicyData {

	private String name;
	private String description;	
	
	private Boolean draft = true;
	
	private List<PolicyElement> create;
	private List<PolicyElement> modify;
	private List<PolicyElement> evaluate;
	private List<PolicyElement> extract; // ???
	private PolicyFilter filter;

	private List<PlanningResultGroup> groups;

	private String generateCode;
	private String evaluateCode;
	private String extractCode;
	private String filterCode;

	private boolean modifiedGenerate;
	private boolean modifiedEvaluate;
	private boolean modifiedExtract;
	private boolean modifiedFilter;
	
	private String policyId;

	public CompilablePolicyData() {
		create = Lists.newArrayList();
		modify = Lists.newArrayList();
		evaluate = Lists.newArrayList();
		extract = Lists.newArrayList();
		filter = new PolicyFilter();
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
	
	public Boolean getDraft() {
		return draft;
	}

	public String getPolicyId() {
		return policyId != null ? policyId : name;
	}

	public void setPolicyId(String policyId) {
		this.policyId = policyId;
	}

	public void setDraft(Boolean draft) {
		this.draft = draft;
	}

	public List<PolicyElement> getCreate() {
		return create;
	}

	public void setCreate(List<PolicyElement> create) {
		this.create = create;
	}

	public List<PolicyElement> getModify() {
		return modify;
	}

	public void setModify(List<PolicyElement> modify) {
		this.modify = modify;
	}

	public List<PolicyElement> getEvaluate() {
		return evaluate;
	}

	public void setEvaluate(List<PolicyElement> evaluate) {
		this.evaluate = evaluate;
	}

	public List<PolicyElement> getExtract() {
		return extract;
	}

	public void setExtract(List<PolicyElement> extract) {
		this.extract = extract;
	}

	public PolicyFilter getFilter() {
		return filter;
	}

	public void setFilter(PolicyFilter filter) {
		this.filter = filter;
	}

	public List<PlanningResultGroup> getGroups() {
		return groups;
	}

	public void setGroups(List<PlanningResultGroup> groups) {
		this.groups = groups;
	}

	public String getGenerateCode() {
		return generateCode;
	}

	public void setGenerateCode(String generateCode) {
		this.generateCode = generateCode;
	}

	public String getEvaluateCode() {
		return evaluateCode;
	}

	public void setEvaluateCode(String evaluateCode) {
		this.evaluateCode = evaluateCode;
	}

	public String getExtractCode() {
		return extractCode;
	}

	public void setExtractCode(String extractCode) {
		this.extractCode = extractCode;
	}

	public String getFilterCode() {
		return filterCode;
	}

	public void setFilterCode(String filterCode) {
		this.filterCode = filterCode;
	}

	public boolean isModifiedGenerate() {
		return modifiedGenerate;
	}

	public void setModifiedGenerate(boolean modifiedGenerate) {
		this.modifiedGenerate = modifiedGenerate;
	}

	public boolean isModifiedEvaluate() {
		return modifiedEvaluate;
	}

	public void setModifiedEvaluate(boolean modifiedEvaluate) {
		this.modifiedEvaluate = modifiedEvaluate;
	}

	public boolean isModifiedExtract() {
		return modifiedExtract;
	}

	public void setModifiedExtract(boolean modifiedExtract) {
		this.modifiedExtract = modifiedExtract;
	}

	public boolean isModifiedFilter() {
		return modifiedFilter;
	}

	public void setModifiedFilter(boolean modifiedFilter) {
		this.modifiedFilter = modifiedFilter;
	}
	
	
}
