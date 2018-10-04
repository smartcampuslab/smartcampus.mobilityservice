package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import java.util.Map;

public class NotificationMessage {

	private String id;
	private Map<String, String> title;
	private Map<String, String> description;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Map<String, String> getTitle() {
		return title;
	}
	public void setTitle(Map<String, String> title) {
		this.title = title;
	}
	public Map<String, String> getDescription() {
		return description;
	}
	public void setDescription(Map<String, String> description) {
		this.description = description;
	}
	
}
