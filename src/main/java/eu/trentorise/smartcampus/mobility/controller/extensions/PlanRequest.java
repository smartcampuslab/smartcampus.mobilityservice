package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.List;

import com.google.common.collect.Lists;

public class PlanRequest {

	private TType type;
	private RType routeType;
	private String request;
	private String plan;
	private Integer value;
	private List<Itinerary> itinerary;
	private SingleJourney originalRequest;
	private int itineraryNumber;
	private boolean retryOnFail = false;
	
	public PlanRequest() {
		itinerary = Lists.newArrayList();
	}
	
	public TType getType() {
		return type;
	}
	public void setType(TType type) {
		this.type = type;
	}
	public RType getRouteType() {
		return routeType;
	}
	public void setRouteType(RType routeType) {
		this.routeType = routeType;
	}
	public String getRequest() {
		return request;
	}
	public void setRequest(String request) {
		this.request = request;
	}
	public String getPlan() {
		return plan;
	}

	public void setPlan(String plan) {
		this.plan = plan;
	}

	public Integer getValue() {
		return value;
	}
	public void setValue(Integer value) {
		this.value = value;
	}

	public List<Itinerary> getItinerary() {
		return itinerary;
	}

	public void setItinerary(List<Itinerary> itinerary) {
		this.itinerary = itinerary;
	}
	public SingleJourney getOriginalRequest() {
		return originalRequest;
	}
	public void setOriginalRequest(SingleJourney originalRequest) {
		this.originalRequest = originalRequest;
	}

	public int getItineraryNumber() {
		return itineraryNumber;
	}

	public void setItineraryNumber(int itineraryNumber) {
		this.itineraryNumber = itineraryNumber;
	}

	public boolean isRetryOnFail() {
		return retryOnFail;
	}

	public void setRetryOnFail(boolean retryOnFail) {
		this.retryOnFail = retryOnFail;
	}

	@Override
	public String toString() {
		return type.toString();
	}
	
	
}
