package eu.trentorise.smartcampus.mobility.controller.extensions.model;

import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;

import java.util.List;

import com.google.common.collect.Lists;

public class AddPromotedPolicy {

	private String name;

	private List<TType> requestTransportTypes;
	private RType requestRouteType;
	private List<TType> notRequestTransportTypes;
	private RType notRequestRouteType;

	private TType newTransportType; // mandatory?
	private RType newRouteType;
	private Integer newItnPerType;
	private Integer maxToKeep;
	
	public AddPromotedPolicy() {
		requestTransportTypes = Lists.newArrayList();
		notRequestTransportTypes = Lists.newArrayList();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<TType> getRequestTransportTypes() {
		return requestTransportTypes;
	}

	public void setRequestTransportTypes(List<TType> requestTransportTypes) {
		this.requestTransportTypes = requestTransportTypes;
	}

	public RType getRequestRouteType() {
		return requestRouteType;
	}

	public void setRequestRouteType(RType requestRouteType) {
		this.requestRouteType = requestRouteType;
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

	public TType getNewTransportType() {
		return newTransportType;
	}

	public void setNewTransportType(TType newTransportType) {
		this.newTransportType = newTransportType;
	}

	public RType getNewRouteType() {
		return newRouteType;
	}

	public void setNewRouteType(RType newRouteType) {
		this.newRouteType = newRouteType;
	}

	public Integer getNewItnPerType() {
		return newItnPerType;
	}

	public void setNewItnPerType(Integer newItnPerType) {
		this.newItnPerType = newItnPerType;
	}

	public Integer getMaxToKeep() {
		return maxToKeep;
	}

	public void setMaxToKeep(Integer newMaxToKeep) {
		this.maxToKeep = newMaxToKeep;
	}

}
