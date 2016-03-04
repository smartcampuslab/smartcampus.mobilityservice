package eu.trentorise.smartcampus.mobility.security;

import java.io.Serializable;
import java.util.List;


public class AppInfo implements Serializable {

	private static final long serialVersionUID = -2583888256140211744L;
	
	private String appId;
    private String password;
    private String messagingAppId;
    private String gcmSenderApiKey;
    private String gcmSenderId;
    private List<String> agencyIds;

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

	public String getMessagingAppId() {
		return messagingAppId;
	}

	public void setMessagingAppId(String messagingAppId) {
		this.messagingAppId = messagingAppId;
	}

	public String getGcmSenderApiKey() {
		return gcmSenderApiKey;
	}

	public void setGcmSenderApiKey(String gcmSenderApiKey) {
		this.gcmSenderApiKey = gcmSenderApiKey;
	}

	public String getGcmSenderId() {
		return gcmSenderId;
	}

	public void setGcmSenderId(String gcmSenderId) {
		this.gcmSenderId = gcmSenderId;
	}

	public List<String> getAgencyIds() {
		return agencyIds;
	}

	public void setAgencyIds(List<String> agencyIds) {
		this.agencyIds = agencyIds;
	}

	@Override
    public String toString() {
    	return appId + "=" + password;
    }
	

}
