package eu.trentorise.smartcampus.mobility.processor.alerts;

import it.sayservice.platform.smartplanner.data.message.alerts.Alert;

public class AlertWrapper {

	private Alert alert;
	private String userId;
	private String clientId;
	private String name;
	private String appId;
	
	public Alert getAlert() {
		return alert;
	}
	public void setAlert(Alert alert) {
		this.alert = alert;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAppId() {
		return appId;
	}
	public void setAppId(String appId) {
		this.appId = appId;
	}

	
	
}
