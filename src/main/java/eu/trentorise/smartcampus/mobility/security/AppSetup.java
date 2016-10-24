package eu.trentorise.smartcampus.mobility.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.naming.ConfigurationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.google.common.collect.Maps;

import eu.trentorise.smartcampus.communicator.CommunicatorConnector;
import eu.trentorise.smartcampus.communicator.CommunicatorConnectorException;
import eu.trentorise.smartcampus.communicator.model.AppSignature;
import eu.trentorise.smartcampus.mobility.util.TokenHelper;


public class AppSetup {

	@Value("classpath:/apps-info.yml")
	private Resource resource;

	@Autowired
	@Value("${communicatorURL}")
	private String communicatorURL;		
	
	@Autowired
	private TokenHelper helper;	
	
	private CommunicatorConnector communicator;
	
	private List<AppInfo> apps;
	private Map<String, AppInfo> appsMap;
	private Map<String, AppInfo> servicesMap;	

	public AppSetup() {
	}	
	
	@PostConstruct
	public void init() throws Exception {
		communicator = new CommunicatorConnector(communicatorURL);
		Yaml yaml = new Yaml(new Constructor(AppSetup.class));
		AppSetup data = (AppSetup) yaml.load(resource.getInputStream());
		this.apps = data.apps;

		for (AppInfo app : apps) {
			if (app.getAppId().equals(app.getServicesUser())) {
				throw new ConfigurationException("AppId and servicesUser must not be equal: " + app.getAppId());
			}
		}
		
		if (appsMap == null) {
			appsMap = new HashMap<String, AppInfo>();
			for (AppInfo app : apps) {
				appsMap.put(app.getAppId(), app);
			}
		}		
		if (servicesMap == null) {
			servicesMap = new HashMap<String, AppInfo>();
			for (AppInfo app : apps) {
				servicesMap.put(app.getServicesUser(), app);
			}
		}			
		
		registerApps();
	}
	
	private void registerApps() throws CommunicatorConnectorException {
		Timer timer = new Timer();

		TimerTask tt = new TimerTask() {

			@Override
			public void run() {
				String token = helper.getToken();
				for (AppInfo cred : appsMap.values()) {
					AppSignature signature = new AppSignature();
					
					Map<String, Object> map = Maps.newHashMap();
					map.put("GCM_SENDER_API_KEY", cred.getGcmSenderApiKey());
					signature.setPrivateKey(map);
					
					map = Maps.newHashMap();
					map.put("GCM_SENDER_ID", cred.getGcmSenderId());
					signature.setPublicKey(map);
					
					signature.setAppId(cred.getMessagingAppId());

					boolean ok = true;

					do {
						try {
							communicator.registerApp(signature, cred.getMessagingAppId(), token);
							ok = true;
						} catch (CommunicatorConnectorException e) {
							ok = false;
							try {
								Thread.sleep(10000);
							} catch (InterruptedException e1) {
							}
							e.printStackTrace();
						}
					} while (!ok);
				}

			}
		};

		timer.schedule(tt, 20000);
	}

	public List<AppInfo> getApps() {
		return apps;
	}

	public void setApps(List<AppInfo> apps) {
		this.apps = apps;
	}

	public Map<String, AppInfo> getAppsMap() {
		return appsMap;
	}

	public void setAppsMap(Map<String, AppInfo> appsMap) {
		this.appsMap = appsMap;
	}

	@Override
	public String toString() {
		return "AppSetup [apps=" + apps + "]";
	}

	public AppInfo findAppById(String username) {
		return appsMap.get(username);
	}
	
	public AppInfo findAppByServiceUser(String username) {
		return servicesMap.get(username);
	}	
	
}
