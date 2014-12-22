package eu.trentorise.smartcampus.mobility.processor.model;

public class Parking {

	private String agencyId;
	private String id;
	
	private String address;
	
	private int freePlaces;
	
	private int vehicles = -1;

	public String getAgencyId() {
		return agencyId;
	}

	public void setAgencyId(String agencyId) {
		this.agencyId = agencyId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getFreePlaces() {
		return freePlaces;
	}

	public void setFreePlaces(int places) {
		this.freePlaces = places;
	}

	public int getVehicles() {
		return vehicles;
	}

	public void setVehicles(int vehicles) {
		this.vehicles = vehicles;
	}
	
	
}
