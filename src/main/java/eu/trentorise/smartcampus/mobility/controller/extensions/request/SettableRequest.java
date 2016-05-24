package eu.trentorise.smartcampus.mobility.controller.extensions.request;

import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;

import java.util.Map;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningRequest.SmartplannerParameter;
import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningResultGroup;

public class SettableRequest {

	private TType newTType;
	
	private RType newRType;
	private PlanningResultGroup group;
	private Integer newItineraryNumber;
	private Boolean wheelchair;	
	
	private String planningResultGroupName;
	private Map<SmartplannerParameter, Object> smartplannerParameters;
	
	private Boolean inherit;
	private Boolean promoted;
	
	private Boolean unrecoverable;
	
	public TType getNewTType() {
		return newTType;
	}
	public void setNewTType(TType newTType) {
		this.newTType = newTType;
	}
	public RType getNewRType() {
		return newRType;
	}
	public void setNewRType(RType newRType) {
		this.newRType = newRType;
	}
	public PlanningResultGroup getGroup() {
		return group;
	}
	public void setGroup(PlanningResultGroup group) {
		this.group = group;
	}
	public Integer getNewItineraryNumber() {
		return newItineraryNumber;
	}
	public void setNewItineraryNumber(Integer newItineraryNumber) {
		this.newItineraryNumber = newItineraryNumber;
	}
	public Boolean getWheelchair() {
		return wheelchair;
	}
	public void setWheelchair(Boolean wheelChair) {
		this.wheelchair = wheelChair;
	}
	public String getPlanningResultGroupName() {
		return planningResultGroupName;
	}
	public void setPlanningResultGroupName(String planningResultGroupName) {
		this.planningResultGroupName = planningResultGroupName;
	}
	public Map<SmartplannerParameter, Object> getSmartplannerParameters() {
		return smartplannerParameters;
	}
	public void setSmartplannerParameters(Map<SmartplannerParameter, Object> smartplannerParameters) {
		this.smartplannerParameters = smartplannerParameters;
	}
	public Boolean getInherit() {
		return inherit;
	}
	public void setInherit(Boolean inherit) {
		this.inherit = inherit;
	}
	public Boolean getPromoted() {
		return promoted;
	}
	public void setPromoted(Boolean promoted) {
		this.promoted = promoted;
	}
	public Boolean getUnrecoverable() {
		return unrecoverable;
	}
	public void setUnrecoverable(Boolean unrecoverable) {
		this.unrecoverable = unrecoverable;
	}

	

	
}
