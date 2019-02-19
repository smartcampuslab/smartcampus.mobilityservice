package eu.trentorise.smartcampus.mobility.gamificationweb.model;

public class PlayerWaypoint {

	private String timestamp;
	private Double latitude;
	private Double longitude;

	private Long accuracy;
	private Double speed;
	private String waypoint_activity_type;
	private Long waypoint_activity_confidence;

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Long getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(Long accuracy) {
		this.accuracy = accuracy;
	}

	public Double getSpeed() {
		return speed;
	}

	public void setSpeed(Double speed) {
		this.speed = speed;
	}

	public String getWaypoint_activity_type() {
		return waypoint_activity_type;
	}

	public void setWaypoint_activity_type(String waypoint_activity_type) {
		this.waypoint_activity_type = waypoint_activity_type;
	}

	public Long getWaypoint_activity_confidence() {
		return waypoint_activity_confidence;
	}

	public void setWaypoint_activity_confidence(Long waypoint_activity_confidence) {
		this.waypoint_activity_confidence = waypoint_activity_confidence;
	}

}
