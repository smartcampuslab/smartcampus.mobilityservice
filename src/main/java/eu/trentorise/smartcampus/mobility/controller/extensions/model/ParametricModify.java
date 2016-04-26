package eu.trentorise.smartcampus.mobility.controller.extensions.model;

import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;

import java.util.List;
import java.util.Map;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningRequest.SmartplannerParameter;

public class ParametricModify {

	private List<TType> tTypes;
	private RType rType;
	private RType notRType;
	
	Boolean promoted;
	Boolean derived;
	
	private RType newRType;
	
	private Map<SmartplannerParameter, Object> smartplannerParameters;

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

	public RType getNotRType() {
		return notRType;
	}

	public void setNotRType(RType notRType) {
		this.notRType = notRType;
	}

	public Boolean getPromoted() {
		return promoted;
	}

	public void setPromoted(Boolean promoted) {
		this.promoted = promoted;
	}

	public Boolean getDerived() {
		return derived;
	}

	public void setDerived(Boolean derived) {
		this.derived = derived;
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
	
	
}
