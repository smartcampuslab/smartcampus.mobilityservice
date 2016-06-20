package eu.trentorise.smartcampus.mobility.controller.extensions.definitive;

import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningResultGroup;

public class PolicyAction {

	private TType newTType;

	private RType newRType;
	private PlanningResultGroup group;
	private Integer newItineraryNumber;
	private Boolean wheelchair;

	private String planningResultGroupName;

	private Integer maxWalkDistance;
	private Integer maxTotalWalkDistance;
	private TType extraTransport;
	private Integer maxChanges;

	// private Map<SmartplannerParameter, Object> smartplannerParameters;

	private Boolean inherit;
	private Boolean promoted;

	private Boolean unrecoverable;

	private String compiled;

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

	public Integer getMaxWalkDistance() {
		return maxWalkDistance;
	}

	public void setMaxWalkDistance(Integer maxWalkDistance) {
		this.maxWalkDistance = maxWalkDistance;
	}

	public Integer getMaxTotalWalkDistance() {
		return maxTotalWalkDistance;
	}

	public void setMaxTotalWalkDistance(Integer maxTotalWalkDistance) {
		this.maxTotalWalkDistance = maxTotalWalkDistance;
	}

	public TType getExtraTransport() {
		return extraTransport;
	}

	public void setExtraTransport(TType extraTransport) {
		this.extraTransport = extraTransport;
	}

	public Integer getMaxChanges() {
		return maxChanges;
	}

	public void setMaxChanges(Integer maxChanges) {
		this.maxChanges = maxChanges;
	}

	public String getCompiled() {
		return compiled;
	}

	public void setCompiled(String compiled) {
		this.compiled = compiled;
	}

	// public Map<SmartplannerParameter, Object> getSmartplannerParameters() {
	// return smartplannerParameters;
	// }
	// public void setSmartplannerParameters(Map<SmartplannerParameter, Object>
	// smartplannerParameters) {
	// this.smartplannerParameters = smartplannerParameters;
	// }
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
