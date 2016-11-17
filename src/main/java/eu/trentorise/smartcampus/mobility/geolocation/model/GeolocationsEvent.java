package eu.trentorise.smartcampus.mobility.geolocation.model;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class GeolocationsEvent {
	
//	private String travelId;
	
	private Device device;

	private ArrayList<Location> location;

//	public String getTravelId() {
//		return travelId;
//	}
//
//	public void setTravelId(String travelId) {
//		this.travelId = travelId;
//	}

	public Device getDevice() {
		return this.device;
	}

	public ArrayList<Location> getLocation() {
		return this.location;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public void setLocation(ArrayList<Location> location) {
		this.location = location;
	}
}