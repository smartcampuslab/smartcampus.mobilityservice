package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.List;

import com.google.common.collect.Lists;

public class PlanRequest {

	private TType type;
	private String request;
	private String plan;
	private Integer value;
	private List<Itinerary> itinerary;
	private SingleJourney originalRequest;
	
	public PlanRequest() {
		itinerary = Lists.newArrayList();
	}
	
	public TType getType() {
		return type;
	}
	public void setType(TType type) {
		this.type = type;
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

	@Override
	public String toString() {
		return type.toString();
	}
	
	
}
