package eu.trentorise.smartcampus.mobility.geolocation.model;

import java.util.Date;

public class Location {
	private Activity activity;

	private Battery battery;

	private Coords coords;

	private Boolean is_moving;

	private Date timestamp;

	private String uuid;
	
	private Object geofence;

	public Activity getActivity() {
		return this.activity;
	}

	public Battery getBattery() {
		return this.battery;
	}

	public Coords getCoords() {
		return this.coords;
	}

	public Boolean getIs_moving() {
		return this.is_moving;
	}

	public Date getTimestamp() {
		return this.timestamp;
	}

	public String getUuid() {
		return this.uuid;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}

	public void setBattery(Battery battery) {
		this.battery = battery;
	}

	public void setCoords(Coords coords) {
		this.coords = coords;
	}

	public void setIs_moving(Boolean is_moving) {
		this.is_moving = is_moving;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Object getGeofence() {
		return geofence;
	}

	public void setGeofence(Object geofence) {
		this.geofence = geofence;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Location other = (Location) obj;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}
}
