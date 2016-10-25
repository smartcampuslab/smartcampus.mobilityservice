package eu.trentorise.smartcampus.mobility.security;

import java.io.Serializable;
import java.util.List;
import java.util.Map;


public class AppInfo implements Serializable {

	private static final long serialVersionUID = -2583888256140211744L;
	
	private String appId;
    private String password;
    private String messagingAppId;
    private String gcmSenderApiKey;
    private String gcmSenderId;
    private List<String> agencyIds;
    private String gameId;
    private String gameUser;
    private String gamePassword;
    private String gameStart;
    private String routesDB;
    
    private Map<String,Object> extra;

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
	
	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public String getGameUser() {
		return gameUser;
	}

	public void setGameUser(String gameUser) {
		this.gameUser = gameUser;
	}

	public String getGamePassword() {
		return gamePassword;
	}

	public void setGamePassword(String gamePassword) {
		this.gamePassword = gamePassword;
	}

	public String getGameStart() {
		return gameStart;
	}

	public void setGameStart(String gameStart) {
		this.gameStart = gameStart;
	}

	public String getRoutesDB() {
		return routesDB;
	}

	public void setRoutesDB(String routesDB) {
		this.routesDB = routesDB;
	}

	public Map<String, Object> getExtra() {
		return extra;
	}

	public void setExtra(Map<String, Object> extra) {
		this.extra = extra;
	}

	@Override
    public String toString() {
    	return appId + "=" + password;
    }
	

}
