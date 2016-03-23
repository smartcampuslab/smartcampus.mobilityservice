package eu.trentorise.smartcampus.mobility.geolocation.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Activity {
	private long confidence;

	private String type;

	public long getConfidence() {
		return this.confidence;
	}

	public String getType() {
		return this.type;
	}

	public void setConfidence(long confidence) {
		this.confidence = confidence;
	}

	public void setType(String type) {
		this.type = type;
	}
}
