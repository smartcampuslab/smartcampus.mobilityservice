package eu.trentorise.smartcampus.mobility.processor.alerts;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.annotation.Id;

import com.google.common.collect.Maps;

public class AlertsSent {
	
	@Id
	private String id;

	private Map<String, String> delays;
	private Map<String, String> parkings;
	private Map<String, String> roadWorks;
	
	public AlertsSent() {
		delays = Maps.newTreeMap();
		parkings = Maps.newTreeMap();
	}
	
	public AlertsSent(AlertsSent other) {
		delays = new TreeMap<String, String>(other.getDelays());
		parkings = new TreeMap<String, String>(other.getParkings());
		roadWorks = new TreeMap<String, String>(other.getRoadWorks());
	}	

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Map<String, String> getDelays() {
		return delays;
	}

	public void setDelays(Map<String, String> delays) {
		this.delays = delays;
	}

	public Map<String, String> getParkings() {
		return parkings;
	}

	public void setParkings(Map<String, String> parkings) {
		this.parkings = parkings;
	}

	public Map<String, String> getRoadWorks() {
		return roadWorks;
	}

	public void setRoadWorks(Map<String, String> roadWorks) {
		this.roadWorks = roadWorks;
	}
	
}
