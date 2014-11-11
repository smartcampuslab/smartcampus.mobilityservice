package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.TType;

import java.util.List;

import com.google.common.collect.Lists;

public class PlanRequest {

	private TType type;
	private String request;
	private Integer value;
	private List<Itinerary> itinerary;
	
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

	@Override
	public String toString() {
		return type.toString();
	}
	
	
}
