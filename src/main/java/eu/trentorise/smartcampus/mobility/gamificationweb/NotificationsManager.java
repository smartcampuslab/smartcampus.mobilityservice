package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.nio.charset.Charset;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Lists;

import eu.trentorise.smartcampus.mobility.gamification.model.LevelGainedNotification;
import eu.trentorise.smartcampus.mobility.gamification.model.Notification;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Timestamp;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.service.NotificationHelper;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;



@Component
public class NotificationsManager {

	private static final String NOTIFICATION_APP = "mobility.trentoplaygo.test";

	private static transient final Logger logger = Logger.getLogger(NotificationsManager.class);
	
	@Autowired
	@Value("${gamification.url}")
	private String gamificationUrl;
	
	@Autowired
	private AppSetup appSetup;
	
	@Autowired
	private GameSetup gameSetup;		
	
	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;		
	
	@Autowired
	private PlayerRepositoryDao playerRepository;
	
	@Autowired
	private NotificationHelper notificatioHelper;
	
	private ObjectMapper mapper = new ObjectMapper(); {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}	
	
	@Scheduled(fixedRate = 1000 * 60 * 1) 
//	@PostConstruct
	public void getNotifications() throws Exception {
		logger.debug("Reading notifications.");
		
		List<Notification> nots = Lists.newArrayList();
		
		for (AppInfo appInfo : appSetup.getApps()) {
			if (appInfo.getGameId() != null && !appInfo.getGameId().isEmpty()) {
				nots.addAll(getNotifications(appInfo.getAppId()));
			}
		}
		
		if (!nots.isEmpty()) {
			logger.info("Read " + nots.size() + " notifications.");
		}
		
		for (Notification not: nots) {
			Player p = playerRepository.findByIdAndGameId(not.getPlayerId(), not.getGameId());
			
			if (p != null) {
				eu.trentorise.smartcampus.communicator.model.Notification notification = buildNotification(p, not);
				if (notification != null) {
					logger.info("Sending notification to " + not.getPlayerId());
					notificatioHelper.notify(notification, not.getPlayerId(), NOTIFICATION_APP);
				}
			}
		}
	}
	
	private <T> List<Notification> getNotifications(String appId) throws Exception {
		logger.debug("Reading notifications for " + appId);
		
		List<Notification> nots = Lists.newArrayList();
		
		nots.addAll(getNotifications(appId, LevelGainedNotification.class));
//		nots.addAll(getNotifications(appId, ChallengeAssignedNotification.class));
		
		return nots;
	}
	
	private <T> List<Notification> getNotifications(String appId, Class<T> clz) throws Exception {
		logger.debug("Reading notifications for type " + ((Class)clz).getSimpleName());
		
		String gameId = getGameId(appId);

		Criteria criteria = new Criteria("gameId").is(gameId).and("type").is(((Class)clz).getSimpleName());
		Query query = new Query(criteria);
		Timestamp old = template.findOne(query, Timestamp.class);

		long from = -1;
		long to = System.currentTimeMillis();

		if (old != null) {
			from = old.getTimestamp() + 1;
		} else {
			from = to - 1000 * 60 * 60 * 24;
			old = new Timestamp(gameId, ((Class)clz).getSimpleName(), to);
		}

		List<Notification> nots = getNotifications(appId, from, to, clz);

		old.setTimestamp(to);
		template.save(old);

		return nots;
	}
	
	private <T> List<Notification> getNotifications(String appId, long from, long to, Class<T> clz) throws Exception {
		logger.debug("Reading notifications from " + from + " to " + to);
		
		String gameId = getGameId(appId);
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> res = null;
		
		String url = gamificationUrl + "/notification/game/" + gameId + "?includeTypes=" + ((Class)clz).getSimpleName() + "&fromTs=" + from + "&toTs=" + to;
		logger.debug("URL: " + url);
		
		try {
			res = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)), String.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		logger.debug("Result: " + res.getStatusCodeValue());
		
		TypeFactory factory = mapper.getTypeFactory();
		JavaType listOfT = factory.constructCollectionType(List.class, clz);
		List<Notification> nots = mapper.readValue(res.getBody(), listOfT);		
		
		logger.debug("Reading " + nots.size() + " notifications.");
		
		return nots;
	}
	
	private eu.trentorise.smartcampus.communicator.model.Notification buildNotification(Player p, Notification not) {
		eu.trentorise.smartcampus.communicator.model.Notification result = new eu.trentorise.smartcampus.communicator.model.Notification();
		
		if (not instanceof LevelGainedNotification) {
			result.setTitle("Ding!");
			switch (p.getLanguage()) {
			case "EN":
				result.setDescription("Congratulations " + p.getNickname() + ", you just reached level " + ((LevelGainedNotification)not).getLevelName() + "!");
			default:
				result.setDescription("Congratulazioni " + p.getNickname() + ", sei appena arrivato al livello " + ((LevelGainedNotification)not).getLevelName() + "!");
			}
//		} else if (not instanceof ChallengeAssignedNotification) {
//			result.setTitle("Ding!");
//			result.setDescription("Ciao " + p.getNickname() + ", ti è appena stata assegnata una nuova sfida: " + "!");
		} else {
			result = null;
		}
		
		return result;
	}
	
	private String getGameId(String appId) {
		if (appId != null) {
			AppInfo ai = appSetup.findAppById(appId);
			if (ai == null) {
				return null;
			}
			String gameId = ai.getGameId();
			return gameId;
		}
		return null;
	}	
	
	HttpHeaders createHeaders(String appId) {
		return new HttpHeaders() {
			{
				AppInfo app = appSetup.findAppById(appId);
				GameInfo game = gameSetup.findGameById(app.getGameId());
				String auth = game.getUser() + ":" + game.getPassword();
				byte[] encodedAuth = Base64.encode(auth.getBytes(Charset.forName("UTF-8")));
				String authHeader = "Basic " + new String(encodedAuth);
				set("Authorization", authHeader);
			}
		};
	}	
	
}
