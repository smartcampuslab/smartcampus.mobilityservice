package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

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
import org.stringtemplate.v4.ST;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeConcept;
import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeInvitationAcceptedNotification;
import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeInvitationRefusedNotification;
import eu.trentorise.smartcampus.mobility.gamification.model.LevelGainedNotification;
import eu.trentorise.smartcampus.mobility.gamification.model.Notification;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.NotificationMessage;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.NotificationMessageExtra;
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

	private static final Class[] notificationClasses = new Class[] { LevelGainedNotification.class, ChallengeInvitationAcceptedNotification.class, ChallengeInvitationRefusedNotification.class};

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
	private ChallengesUtils challengeUtils;	
	
	@Autowired
	private NotificationHelper notificatioHelper;
	
	private ObjectMapper mapper = new ObjectMapper(); {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}	
	
	private Map<String, NotificationMessage> notificationsMessages;

	@PostConstruct
	public void init() throws Exception {
		List<NotificationMessage> messages = mapper.readValue(Resources.getResource("notifications/notifications.json"), new TypeReference<List<NotificationMessage>>() {
		});
		notificationsMessages = messages.stream().collect(Collectors.toMap(NotificationMessage::getId, Function.identity()));
	}
	
	@Scheduled(cron="0 0 12 * * WED")
	public void checkProposedPending() throws Exception {
		for (AppInfo appInfo : appSetup.getApps()) {
			try {
				if (appInfo.getGameId() != null && !appInfo.getGameId().isEmpty()) {
					GameInfo game = gameSetup.findGameById(appInfo.getGameId());
					if (game.getSend() == null || !game.getSend()) {
						continue;
					}
					checkProposedPending(appInfo);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}	
	
	public void sendDirectNotification(String appId, Player toPlayer, String type, Map<String, String> extraData) {
		AppInfo appInfo = appSetup.findAppById(appId);
		
		logger.info("Sending direct notification '" + type + "' to " + toPlayer.getPlayerId());
		eu.trentorise.smartcampus.communicator.model.Notification notification = null;
		
		try {
			notification = buildSimpleNotification(toPlayer.getLanguage(), type, extraData);
		} catch (Exception e) {
			logger.error("Error building notification", e);
		}
		
		if (notification != null) {
			try {
				notificatioHelper.notify(notification, toPlayer.getPlayerId(), appInfo.getMessagingAppId());
			} catch (Exception e) {
				logger.error("Error sending notification", e);
			}
		}
	}
	
	private void checkProposedPending(AppInfo appInfo) throws Exception {
		logger.info("Sending notifications for app " + appInfo.getAppId());

		List<eu.trentorise.smartcampus.communicator.model.Notification> nots = Lists.newArrayList();

		List<Player> players = playerRepository.findAllByGameId(appInfo.getGameId());
		for (Player p : players) {
			RestTemplate restTemplate = new RestTemplate();
			ResponseEntity<String> res = restTemplate.exchange(gamificationUrl + "gengine/state/" + appInfo.getGameId() + "/" + p.getPlayerId(), HttpMethod.GET,
					new HttpEntity<Object>(null, createHeaders(appInfo.getAppId())), String.class);
			String data = res.getBody();

			List<ChallengeConcept> challengeConcepts = challengeUtils.parse(data);

			boolean proposed = false;
			for (ChallengeConcept challengeConcept : challengeConcepts) {
				if ("PROPOSED".equals(challengeConcept.getState())) {
					proposed = true;
					break;
				}
			}

			if (proposed) {
				logger.info("Sending PROPOSED notification to " + p.getPlayerId());
				eu.trentorise.smartcampus.communicator.model.Notification notification = null;
				try {
					notification = buildSimpleNotification(p.getLanguage(), "PROPOSED", null);
				} catch (Exception e) {
					logger.error("Error building notification", e);
				}

				if (notification != null) {
					try {
						notificatioHelper.notify(notification, p.getPlayerId(), appInfo.getMessagingAppId());
						continue;
					} catch (Exception e) {
						logger.error("Error sending notification", e);
					}
				}
			}

		}
	}
	
	@Scheduled(fixedRate = 1000 * 60 * 1)
	private void getNotifications() throws Exception {
		logger.debug("Reading notifications.");
		
		List<Notification> nots = Lists.newArrayList();
		
		for (AppInfo appInfo : appSetup.getApps()) {
			if (appInfo.getGameId() != null && !appInfo.getGameId().isEmpty()) {
				GameInfo game = gameSetup.findGameById(appInfo.getGameId());
				if (game.getSend() == null || !game.getSend()) {
					continue;
				}
				nots = getNotifications(appInfo.getAppId());
				
				if (!nots.isEmpty()) {
					logger.info("Read " + nots.size() + " notifications for " + appInfo.getAppId());
				}
				
				for (Notification not: nots) {
					Player p = playerRepository.findByPlayerIdAndGameId(not.getPlayerId(), not.getGameId());
					
					if (p != null) {
						eu.trentorise.smartcampus.communicator.model.Notification notification = null;

						try {
							notification = buildNotification(p.getLanguage(), not);
						} catch (Exception e) {
							logger.error("Error building notification", e);
						}
						if (notification != null) {
							logger.info("Sending '" + not.getClass().getSimpleName() + "' notification to " + not.getPlayerId());
							try {
							notificatioHelper.notify(notification, not.getPlayerId(), appInfo.getMessagingAppId());
							} catch (Exception e) {
								logger.error("Error sending notification", e);
							}								
						}
					}
				}				
				
			}
		}
	}
	
	private <T> List<Notification> getNotifications(String appId) throws Exception {
		logger.debug("Reading notifications for " + appId);
		
		List<Notification> nots = Lists.newArrayList();
		
		for (Class clz: notificationClasses) {
		nots.addAll(getNotifications(appId, clz));
		}
		
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
		try {
		logger.debug("Reading notifications from " + from + " to " + to);
		
		String gameId = getGameId(appId);
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> res = null;
		
		String url = gamificationUrl + "/notification/game/" + gameId + "?includeTypes=" + ((Class)clz).getSimpleName() + "&fromTs=" + from + "&toTs=" + to;
		
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
		} catch (Exception e) {
			logger.error("Error retrieving notifications", e);
			return Lists.newArrayList();
		}
	}
	
	private eu.trentorise.smartcampus.communicator.model.Notification buildNotification(String lang, Notification not) {
		String type = not.getClass().getSimpleName();
		Map<String, String> extraData = buildExtraData(not);
		
		eu.trentorise.smartcampus.communicator.model.Notification result = new eu.trentorise.smartcampus.communicator.model.Notification();
		
		NotificationMessage message = notificationsMessages.get(type);
			
		fillNotification(result, lang, message, extraData);
		
		return result;
	}
	
	private eu.trentorise.smartcampus.communicator.model.Notification buildSimpleNotification(String lang, String type, Map<String, String> extraData) {
		eu.trentorise.smartcampus.communicator.model.Notification result = new eu.trentorise.smartcampus.communicator.model.Notification();
		
		NotificationMessage message = notificationsMessages.get(type);
			
		fillNotification(result, lang, message, extraData);
		
		return result;
	}	
	
	private Map<String, String> buildExtraData(Notification not) {
		Map<String, String> result = Maps.newTreeMap();

		switch (not.getClass().getSimpleName()) {
		case "LevelGainedNotification":
			result.put("levelName", ((LevelGainedNotification) not).getLevelName());
			result.put("levelIndex", ((LevelGainedNotification) not).getLevelIndex() != null ? ((LevelGainedNotification) not).getLevelIndex().toString() : "");
			break;
		case "ChallengeInvitationAcceptedNotification": {
			Player guest = playerRepository.findByPlayerIdAndGameId(((ChallengeInvitationAcceptedNotification) not).getGuestId(), not.getGameId());
			result.put("assigneeName", guest.getNickname());
			break;
		}
		case "ChallengeInvitationRefusedNotification": {
			Player guest = playerRepository.findByPlayerIdAndGameId(((ChallengeInvitationRefusedNotification) not).getGuestId(), not.getGameId());
			result.put("assigneeName", guest.getNickname());
			break;
		}
		}

		return result;
	}	
	
	private void fillNotification(eu.trentorise.smartcampus.communicator.model.Notification notification, String lang, NotificationMessage message, Map<String, String> extraData) {
		if (message != null) {
			notification.setTitle(message.getTitle().get(lang));
			notification.setDescription(fillDescription(lang, message, extraData));
			Map<String, Object> content = Maps.newTreeMap();
			content.put("type", message.getType());
			notification.setContent(content);
		}
	}
	
	private String fillDescription(String lang, NotificationMessage message, Map<String, String> extraData) {
		StringBuilder descr = new StringBuilder(message.getDescription().get(lang));
		String result = null;

		if (message.getExtras() != null && extraData != null) {

			List<NotificationMessageExtra> extras = message.getExtras().get(lang);

			List<NotificationMessageExtra> append = extras.stream().filter(x -> "APPEND".equals(x.getType())).collect(Collectors.toList());

			for (NotificationMessageExtra extra : append) {
				boolean ok = true;
				if (extra.getValue() != null) {
					String keyValue = extraData.get(extra.getKey());
					if (keyValue != null && !keyValue.equals(extra.getValue())) {
						ok = false;
					}
				}
				if (ok) {
					descr.append(extra.getString());
				}
			}

			ST st = new ST(descr.toString());

			List<NotificationMessageExtra> replace = extras.stream().filter(x -> "REPLACE".equals(x.getType())).collect(Collectors.toList());

			for (NotificationMessageExtra extra : replace) {
				boolean ok = true;
				if (extra.getValue() != null) {
					String keyValue = extraData.get(extra.getKey());
					if (keyValue != null && !keyValue.equals(extra.getKey())) {
						ok = false;
					}
				}
				if (ok) {
					st.add(extra.getKey(), extraData.get(extra.getString()));
				}
			}

			result = st.render();
		} else {
			result = descr.toString();
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
