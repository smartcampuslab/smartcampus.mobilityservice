package eu.trentorise.smartcampus.mobility.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;


public class AppSetup {

	@Value("classpath:/apps-info.yml")
	private Resource resource;

	private List<AppInfo> apps;
	private Map<String,AppInfo> appsMap;	

	public AppSetup() {
	}	
	
	@PostConstruct
	public void init() throws Exception {
		Yaml yaml = new Yaml(new Constructor(AppSetup.class));
		AppSetup data = (AppSetup) yaml.load(resource.getInputStream());
		this.apps = data.apps;

		if (appsMap == null) {
			appsMap = new HashMap<String, AppInfo>();
			for (AppInfo app : apps) {
				appsMap.put(app.getAppId(), app);
			}
		}		
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
}
