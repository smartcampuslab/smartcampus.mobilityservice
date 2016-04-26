package eu.trentorise.smartcampus.mobility.controller.extensions.model;

import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;

import java.util.List;

public class ParametricGenerate {

	private List<TType> requestTTypes;
	private RType requestRType;
	private List<TType> notRequestTransportTypes;
	private RType notRequestRouteType;	
	
	private TType newTType;
	private RType newRType;
	private Integer newItineraryNumber;
	private boolean promoted;
	private String planningResultGroupName;

	public List<TType> getRequestTTypes() {
		return requestTTypes;
	}

	public void setRequestTTypes(List<TType> requestTTypes) {
		this.requestTTypes = requestTTypes;
	}

	public RType getRequestRType() {
		return requestRType;
	}

	public void setRequestRType(RType requestRType) {
		this.requestRType = requestRType;
	}

	public List<TType> getNotRequestTransportTypes() {
		return notRequestTransportTypes;
	}

	public void setNotRequestTransportTypes(List<TType> notRequestTransportTypes) {
		this.notRequestTransportTypes = notRequestTransportTypes;
	}

	public RType getNotRequestRouteType() {
		return notRequestRouteType;
	}

	public void setNotRequestRouteType(RType notRequestRouteType) {
		this.notRequestRouteType = notRequestRouteType;
	}

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

	public Integer getNewItineraryNumber() {
		return newItineraryNumber;
	}

	public void setNewItineraryNumber(Integer newItineraryNumber) {
		this.newItineraryNumber = newItineraryNumber;
	}

	public boolean isPromoted() {
		return promoted;
	}

	public void setPromoted(boolean promoted) {
		this.promoted = promoted;
	}

	public String getPlanningResultGroupName() {
		return planningResultGroupName;
	}

	public void setPlanningResultGroupName(String planningResultGroupName) {
		this.planningResultGroupName = planningResultGroupName;
	}
	
}
