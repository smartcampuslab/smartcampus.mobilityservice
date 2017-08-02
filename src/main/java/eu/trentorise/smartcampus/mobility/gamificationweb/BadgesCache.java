package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import eu.trentorise.smartcampus.mobility.gamificationweb.model.BadgesData;

@Component
public class BadgesCache {

	private Map<String, BadgesData> badges;
	
	@PostConstruct
	public void init() throws Exception {
		badges = Maps.newTreeMap();
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		URL resource = getClass().getResource("/public/");
		String path = resource.getPath();		
		
		List list = mapper.readValue(Resources.getResource("badges.json"), List.class);
		for (Object o: list) {
			BadgesData badge = mapper.convertValue(o, BadgesData.class);
			badge.setPath(path + badge.getPath());
			badge.readImage();
			badges.put(badge.getTextId(), badge);
			System.err.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(badge));			
		}
	}
	
	public BadgesData getBadge(String name) {
		return badges.get(name);
	}
	
	public List<BadgesData> getAllBadges() {
		return Lists.newArrayList(badges.values());
	}	
	
}
