package eu.trentorise.smartcampus.mobility.controller.extensions.model;

import java.util.List;

import com.google.common.collect.Lists;

public class Policies {

	private String name;
	private List<AddPromotedPolicy> addPromotedPolicies;
	private List<ProcessRequestPolicy> processRequestPolicies;
	private RemoveItinerariesPolicy removeItinerariesPolicy;
	
	private Boolean retryOnFail;

	public Policies() {
		addPromotedPolicies = Lists.newArrayList();
		processRequestPolicies = Lists.newArrayList();
	}
	
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<AddPromotedPolicy> getAddPromotedPolicies() {
		return addPromotedPolicies;
	}

	public void setAddPromotedPolicies(List<AddPromotedPolicy> addPromotedPolicies) {
		this.addPromotedPolicies = addPromotedPolicies;
	}

	public List<ProcessRequestPolicy> getProcessRequestPolicies() {
		return processRequestPolicies;
	}

	public void setProcessRequestPolicies(List<ProcessRequestPolicy> processRequestPolicies) {
		this.processRequestPolicies = processRequestPolicies;
	}


	public RemoveItinerariesPolicy getRemoveItinerariesPolicy() {
		return removeItinerariesPolicy;
	}


	public void setRemoveItinerariesPolicy(RemoveItinerariesPolicy removeItinerariesPolicy) {
		this.removeItinerariesPolicy = removeItinerariesPolicy;
	}


	public Boolean getRetryOnFail() {
		return retryOnFail;
	}


	public void setRetryOnFail(Boolean retryOnFail) {
		this.retryOnFail = retryOnFail;
	}

}
