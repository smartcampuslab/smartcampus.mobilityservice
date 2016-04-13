package eu.trentorise.smartcampus.mobility.geolocation.model;

import java.util.Set;

public class ValidationResult {

	private int geoLocationsN;
	private int legsLocationsN;
	private int matchedLocationsN;
	private Boolean matchedLocations;
	
	private Set<String> geoActivities;
	private Set<String> legsActivities;
	private Boolean matchedActivities;
	private Boolean tooFast;
	
	private Boolean valid;

	public int getGeoLocationsN() {
		return geoLocationsN;
	}

	public void setGeoLocationsN(int geoLocationsN) {
		this.geoLocationsN = geoLocationsN;
	}

	public int getLegsLocationsN() {
		return legsLocationsN;
	}

	public void setLegsLocationsN(int legsLocationsN) {
		this.legsLocationsN = legsLocationsN;
	}

	public int getMatchedLocationsN() {
		return matchedLocationsN;
	}

	public void setMatchedLocationsN(int matchedLocationsN) {
		this.matchedLocationsN = matchedLocationsN;
	}

	public Boolean getMatchedLocations() {
		return matchedLocations;
	}

	public void setMatchedLocations(Boolean matchedLocations) {
		this.matchedLocations = matchedLocations;
	}

	public Boolean getValid() {
		return valid;
	}

	public void setValid(Boolean valid) {
		this.valid = valid;
	}

	public Boolean getTooFast() {
		return tooFast;
	}

	public void setTooFast(Boolean walkOnly) {
		this.tooFast = walkOnly;
	}

	public Set<String> getGeoActivities() {
		return geoActivities;
	}

	public void setGeoActivities(Set<String> geoActivities) {
		this.geoActivities = geoActivities;
	}

	public Set<String> getLegsActivities() {
		return legsActivities;
	}

	public void setLegsActivities(Set<String> legsActivities) {
		this.legsActivities = legsActivities;
	}

	public Boolean getMatchedActivities() {
		return matchedActivities;
	}

	public void setMatchedActivities(Boolean matchedActivities) {
		this.matchedActivities = matchedActivities;
	}

}
