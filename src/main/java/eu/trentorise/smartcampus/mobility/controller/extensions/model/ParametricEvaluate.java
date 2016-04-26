package eu.trentorise.smartcampus.mobility.controller.extensions.model;

import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;

import java.util.List;
import java.util.Map;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningRequest.SmartplannerParameter;

public class ParametricEvaluate {

	private List<TType> tTypes;
	private RType rType;
	private Integer iteration;

	private RType newRType;
	private Map<SmartplannerParameter, Object> smartplannerParameters;
	private Boolean unrecoverable;

	public List<TType> gettTypes() {
		return tTypes;
	}

	public void settTypes(List<TType> tTypes) {
		this.tTypes = tTypes;
	}

	public RType getrType() {
		return rType;
	}

	public void setrType(RType rType) {
		this.rType = rType;
	}

	public Integer getIteration() {
		return iteration;
	}

	public void setIteration(Integer iteration) {
		this.iteration = iteration;
	}

	public RType getNewRType() {
		return newRType;
	}

	public void setNewRType(RType newRType) {
		this.newRType = newRType;
	}

	public Map<SmartplannerParameter, Object> getSmartplannerParameters() {
		return smartplannerParameters;
	}

	public void setSmartplannerParameters(Map<SmartplannerParameter, Object> smartplannerParameters) {
		this.smartplannerParameters = smartplannerParameters;
	}

	public Boolean getUnrecoverable() {
		return unrecoverable;
	}

	public void setUnrecoverable(Boolean unrecoverable) {
		this.unrecoverable = unrecoverable;
	}

}
