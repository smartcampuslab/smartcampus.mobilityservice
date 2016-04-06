package eu.trentorise.smartcampus.mobility.controller.extensions.model;

import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;

import java.util.List;

public class ProcessRequestPolicy {

	private String name;

	private List<TType> requestTransportTypes;
	private RType requestRouteType;
	private RType notRequestRouteType;
	private Boolean retried;
	private Boolean promoted;

	private RType newRouteType;
	private Integer maxWalkDistance;
	private Integer maxTotalWalkDistance;
	private Integer maxChanges;
	private String extraTransport;
	private Boolean retryOnEmpty;

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

	public RType getNotRequestRouteType() {
		return notRequestRouteType;
	}

	public void setNotRequestRouteType(RType notRequestRouteType) {
		this.notRequestRouteType = notRequestRouteType;
	}

	public Boolean getRetried() {
		return retried;
	}

	public void setRetried(Boolean retried) {
		this.retried = retried;
	}

	public Boolean getPromoted() {
		return promoted;
	}

	public void setPromoted(Boolean promoted) {
		this.promoted = promoted;
	}

	
	public RType getNewRouteType() {
		return newRouteType;
	}

	public void setNewRouteType(RType newRouteType) {
		this.newRouteType = newRouteType;
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

	public Integer getMaxChanges() {
		return maxChanges;
	}

	public void setMaxChanges(Integer maxChanges) {
		this.maxChanges = maxChanges;
	}

	public String getExtraTransport() {
		return extraTransport;
	}

	public void setExtraTransport(String extraTransport) {
		this.extraTransport = extraTransport;
	}

	public Boolean getRetryOnEmpty() {
		return retryOnEmpty;
	}

	public void setRetryOnEmpty(Boolean retryOnEmpty) {
		this.retryOnEmpty = retryOnEmpty;
	}

}
