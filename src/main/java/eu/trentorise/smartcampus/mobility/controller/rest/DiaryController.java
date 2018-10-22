package eu.trentorise.smartcampus.mobility.controller.rest;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import eu.trentorise.smartcampus.mobility.gamification.GamificationManager;
import eu.trentorise.smartcampus.mobility.gamification.TrackValidator;
import eu.trentorise.smartcampus.mobility.gamification.diary.DiaryEntry;
import eu.trentorise.smartcampus.mobility.gamification.diary.DiaryEntry.DiaryEntryType;
import eu.trentorise.smartcampus.mobility.gamification.diary.DiaryEntry.TravelType;
import eu.trentorise.smartcampus.mobility.gamification.model.BadgeNotification;
import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeConcept;
import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeConcept.ChallengeState;
import eu.trentorise.smartcampus.mobility.gamification.model.LevelGainedNotification;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance.ScoreStatus;
import eu.trentorise.smartcampus.mobility.gamificationweb.BadgesCache;
import eu.trentorise.smartcampus.mobility.gamificationweb.ChallengesUtils;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConcept;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus.MODE_TYPE;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;
import eu.trentorise.smartcampus.mobility.util.ErrorInfo;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;
import it.sayservice.platform.smartplanner.data.message.Leg;

@Controller
public class DiaryController {

	private static final Logger logger = Logger.getLogger(ChallengesUtils.class);

	@Autowired
	@Value("${gamification.url}")
	private String gamificationUrl;

	@Autowired
	@Value("${mobilityURL}")
	private String mobilityUrl;
	
	@Autowired
	@Value("${aacURL}")
	private String aacURL;

	private static SimpleDateFormat shortSdf = new SimpleDateFormat("yyyy/MM/dd");
	private static SimpleDateFormat fullSdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

	@Autowired
	private PlayerRepositoryDao playerRepositoryDao;

	@Autowired
	private DomainStorage storage;

	@Autowired
	private AppSetup appSetup;

	@Autowired
	private GameSetup gameSetup;	
	
	@Autowired
	private GamificationManager gamificationManager;
	
	@Autowired
	private BadgesCache badgesCache;

	@Autowired
	private ChallengesUtils challengeUtils;
	
	private ObjectMapper mapper = new ObjectMapper();

	@PostConstruct
	public void init() {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/diary")
	public @ResponseBody List<DiaryEntry> getNotifications(@RequestHeader(required = true, value = "appId") String appId, @RequestParam(required = false) Long from,
			@RequestParam(required = false) Long to, @RequestParam(required = false) String typeFilter, HttpServletResponse response) throws Exception {
		String userId = null;
		try {
			userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			// userId = getUserId();
		} catch (SecurityException e) {
			logger.error("Unauthorized user.", e);
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return null;
		}

		logger.info("Reading diary for user " + userId);

		String gameId = appSetup.findAppById(appId).getGameId();
		Player p = playerRepositoryDao.findByPlayerIdAndGameId(userId, gameId);

		List<DiaryEntry> result = Lists.newArrayList();

		if (p == null) {
			logger.error("Player " + userId + " not found");
		} else {

			long fromTime = from != null ? from : 0;
			long toTime = to != null ? to : System.currentTimeMillis();

			List<DiaryEntryType> types;
			if (typeFilter != null) {
				types = Splitter.on(",").splitToList(typeFilter).stream().map(x -> DiaryEntryType.valueOf(x)).collect(Collectors.toList());
			} else {
				types = Arrays.asList(DiaryEntryType.values());
			}

			if (types.contains(DiaryEntryType.BADGE)) {
				try {
					List<DiaryEntry> badges = getBadgeNotifications(p, appId);
					result.addAll(badges);
				} catch (Exception e) {
					logger.error("Error for BADGE", e);
				}
			}
			if (types.contains(DiaryEntryType.TRAVEL)) {
				try {
					List<DiaryEntry> travels = getTrackedInstances(userId, appId, fromTime, toTime);
					result.addAll(travels);
				} catch (Exception e) {
					logger.error("Error for TRAVEL", e);
				}
			}
			if (types.contains(DiaryEntryType.CHALLENGE)) {
				try {
					List<DiaryEntry> challenges = getChallenges(p, appId);
					result.addAll(challenges);
				} catch (Exception e) {
					logger.error("Error for CHALLENGE", e);
				}
			}
			if (types.contains(DiaryEntryType.RECOMMENDED)) {
				try {
					List<DiaryEntry> recommended = getFriendRegistered(p, appId);
					result.addAll(recommended);
				} catch (Exception e) {
					logger.error("Error for RECOMMENDED", e);
				}
			}

			result = result.stream().filter(x -> x.getTimestamp() >= fromTime && x.getTimestamp() <= toTime).sorted().collect(Collectors.toList());
		}

		// getRanking(p, appId);

		logger.info("Returning entries: " + result.size());
		
		return result;
	}

	private List<DiaryEntry> getFriendRegistered(Player p, String appId) throws Exception {
		List<DiaryEntry> result = Lists.newArrayList();

		String gameId = appSetup.findAppById(appId).getGameId();
		List<Player> rps = playerRepositoryDao.findByNicknameRecommendationIgnoreCaseAndGameId(p.getNickname(), gameId);
		if (rps != null) {
			for (Player rp : rps) {
				RestTemplate restTemplate = new RestTemplate();
				ResponseEntity<String> res = restTemplate.exchange(gamificationUrl + "gengine/state/" + gameId + "/" + rp.getPlayerId(), HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)),
						String.class);
				String data = res.getBody();

				int gl = getGreenLeavesPoints(data);
				if (gl > 0) {
					logger.info("Found recommended player " + rp.getPlayerId() + " with points: " + gl);
					long timestamp = (long) rp.getPersonalData().get("timestamp");
					DiaryEntry de = new DiaryEntry();
					de.setType(DiaryEntryType.RECOMMENDED);
					de.setTimestamp(timestamp);
					de.setRecommendedNickname(rp.getNickname());
					de.setEntityId(p.getNickname() + "_" + rp.getNickname());
					result.add(de);
				}
			}
		}
		return result;
	}
	
	@SuppressWarnings("rawtypes")
	private int getGreenLeavesPoints(String data) throws Exception {
		Map playerMap = mapper.readValue(data, Map.class);
		if (playerMap.containsKey("state")) {
			Map stateMap = mapper.convertValue(playerMap.get("state"), Map.class);
			if (stateMap.containsKey("PointConcept")) {
				List conceptList = mapper.convertValue(stateMap.get("PointConcept"), List.class);
				for (Object o : conceptList) {
					PointConcept concept = mapper.convertValue(o, PointConcept.class);
					if ("green leaves".equals(concept.getName())) {
						return concept.getScore();
					}
				}
			}
		}
		return 0;
	}	
	
	
//	private void getRanking(Player p, String appId) throws Exception {
//		RestTemplate restTemplate = new RestTemplate();
//		String gameId = appSetup.findAppById(appId).getGameId();
////		ResponseEntity<String> res = restTemplate.exchange(gamificationConsoleUrl + "state/" + gameId, HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)), String.class);
//		ResponseEntity<String> res = restTemplate.exchange(gamificationUrl + "/data/game/" + gameId + "/incclassification/" + URLEncoder.encode("week classification green", "UTF-8"), HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)), String.class);
//
//		String allData = res.getBody();		
////		System.err.println(allData);
//		
//		Map<String, Object> map = mapper.readValue(allData, Map.class);
//		System.err.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map));
//		
//	}

	
	private List<DiaryEntry> getChallenges(Player p, String appId) throws Exception {
		List<DiaryEntry> result = Lists.newArrayList();

		String language = (p.getLanguage() != null && !p.getLanguage().isEmpty()) ? p.getLanguage() : "it";

		RestTemplate restTemplate = new RestTemplate();
		String gameId = appSetup.findAppById(appId).getGameId();
		ResponseEntity<String> res = restTemplate.exchange(gamificationUrl + "gengine/state/" + gameId + "/" + p.getPlayerId(), HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)), String.class);

		String allData = res.getBody();

		List<ChallengeConcept> challengeConcepts = challengeUtils.parse(allData);
		for (ChallengeConcept challengeConcept: challengeConcepts) {
			if (!challengeConcept.getStateDate().containsKey(ChallengeState.ASSIGNED)) {
				continue;
			}
			
			String description = challengeUtils.fillDescription(challengeConcept, language);
//			String longDescription = challengeUtils.fillLongDescription(challengeConcept, language);

			DiaryEntry de = new DiaryEntry();
			de.setEntityId(challengeConcept.getName() + "_assigned");
			de.setType(DiaryEntryType.CHALLENGE);
			
			if (challengeConcept.getStateDate().get(ChallengeState.ASSIGNED) == null) {
				de.setTimestamp(challengeConcept.getStart().getTime());
			} else {
				de.setTimestamp(challengeConcept.getStateDate().get(ChallengeState.ASSIGNED).getTime());
			}
			de.setChallengeName(description);
			de.setChallengeBonus(((Number)challengeConcept.getFields().get("bonusScore")).intValue());
			if (challengeConcept.isCompleted()) {
				DiaryEntry de2 = new DiaryEntry();
				de2.setEntityId(challengeConcept.getName() + "_won");
				de2.setType(DiaryEntryType.CHALLENGE_WON);
				de2.setChallengeName(description);
				de2.setChallengeBonus(((Number)challengeConcept.getFields().get("bonusScore")).intValue());					
				de2.setTimestamp(challengeConcept.getDateCompleted().getTime());
				de2.setChallengeEnd(challengeConcept.getEnd().getTime());
				result.add(de2);
			}
			de.setChallengeEnd(challengeConcept.getEnd().getTime());		
			result.add(de);
		}
		
//		if (challUtils.getChallLongDescriptionList() == null || challUtils.getChallLongDescriptionList().isEmpty()) {
//			challUtils.setChallLongDescriptionList(challDescriptionSetup.getDescriptions());
//		}
//		
//		StatusUtils statusUtils = new StatusUtils();
//		PlayerStatus ps = statusUtils.correctPlayerData(allData, p.getId(), gameId, p.getNickname(), challUtils, mobilityUrl, 1, language);
//
//		if (ps.getChallengeConcept() != null) {
//			List<ChallengesData> cds = Lists.newArrayList();
//			cds.addAll(ps.getChallengeConcept().getActiveChallengeData());
//			cds.addAll(ps.getChallengeConcept().getOldChallengeData());
//			for (ChallengesData cd : cds) {
//				DiaryEntry de = new DiaryEntry();
//				de.setEntityId(cd.getChallId());
//				de.setType(DiaryEntryType.CHALLENGE);
//				de.setTimestamp(cd.getStartDate());
//				de.setChallengeName(cd.getChallDesc());
//				de.setChallengeBonus(cd.getBonus());
//				if (cd.getChallCompletedDate() != 0) {
//					DiaryEntry de2 = new DiaryEntry();
//					de2.setEntityId(cd.getChallId());
//					de2.setType(DiaryEntryType.CHALLENGE_WON);
//					de2.setChallengeName(cd.getChallDesc());
//					de2.setChallengeBonus(cd.getBonus());					
//					de2.setTimestamp(cd.getChallCompletedDate());
//					result.add(de2);
//				}
//				de.setChallengeEnd(cd.getEndDate());
//				
//				result.add(de);
//			}
//		}

		return result;
	}

	private List<DiaryEntry> getBadgeNotifications(Player player, String appId) throws Exception {
		List<DiaryEntry> result = Lists.newArrayList();

		RestTemplate restTemplate = new RestTemplate();
		String gameId = appSetup.findAppById(appId).getGameId();
		ResponseEntity<String> res = restTemplate.exchange(gamificationUrl + "gengine/notification/" + gameId + "/" + player.getPlayerId(), HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)), String.class);

		List nots = mapper.readValue(res.getBody(), List.class);
		for (Object o : nots) {
			if (((Map) o).containsKey("badge")) {
				BadgeNotification not = mapper.convertValue(o, BadgeNotification.class);
				
				if (badgesCache.getBadge(not.getBadge()) == null) {
					logger.error("Badge not found: " + not.getBadge());
					continue;
				}				
				
				DiaryEntry de = new DiaryEntry();
				de.setType(DiaryEntryType.BADGE);
				de.setTimestamp(not.getTimestamp());
				de.setBadge(not.getBadge());
				de.setBadgeText(badgesCache.getBadge(not.getBadge()).getText().get(player.getLanguage()));
				de.setBadgeCollection(not.getCollectionName());
				de.setEntityId(not.getCollectionName() + "_" + not.getBadge());
				result.add(de);
			} else if (((Map) o).containsKey("levelName")) {
				LevelGainedNotification not = mapper.convertValue(o, LevelGainedNotification.class);
				
				DiaryEntry de = new DiaryEntry();
				de.setType(DiaryEntryType.NEW_LEVEL);
				de.setTimestamp(not.getTimestamp());
				de.setLevelName(not.getLevelName());
				de.setEntityId(not.getLevelType() + "_" + not.getLevelName());
				result.add(de);				
			}
				
		}

		return result;
	}
	
	private List<DiaryEntry> getTrackedInstances(String playerId, String appId, long from, long to) throws Exception {
		Map<String, Double> scores = gamificationManager.getScoreNotification(appId, playerId);
		
		List<DiaryEntry> result = Lists.newArrayList();

		Criteria criteria = new Criteria("userId").is(playerId).and("appId").is(appId);
		criteria = criteria.and("complete").is(true);
		String fd = shortSdf.format(new Date(from));
		criteria = criteria.and("day").gte(fd);
		String td = shortSdf.format(new Date(to));
		criteria = criteria.andOperator(new Criteria("day").lte(td));


		Query query = new Query(criteria);
		List<TrackedInstance> instances = storage.searchDomainObjects(query, TrackedInstance.class);
		for (TrackedInstance instance : instances) {
			DiaryEntry de = new DiaryEntry();
			de.setType(DiaryEntryType.TRAVEL);
			long timestamp = 0;
			if (instance.getGeolocationEvents() != null && !instance.getGeolocationEvents().isEmpty()) {
				Geolocation event = instance.getGeolocationEvents().iterator().next();
				timestamp = event.getRecorded_at().getTime();
			} else if (instance.getDay() != null && instance.getTime() != null) {
				String dt = instance.getDay() + " " + instance.getTime();
				timestamp = fullSdf.parse(dt).getTime();
			} else if (instance.getDay() != null) {
				timestamp = shortSdf.parse(instance.getDay()).getTime();
			}
			de.setTimestamp(timestamp);
			de.setTravelScore(instance.getScore());
			if (instance.getValidationResult() != null) {
				de.setTravelLength(instance.getValidationResult().getDistance());
			}
			if (instance.getItinerary() != null) {
				de.setTravelType(TravelType.PLANNED);
				Map<String, Double> distances = getTransportDistances(instance.getItinerary().getData().getLeg());

				de.setTravelDistances(distances);
				
				// TODO: remove
				Set<String> modes = Sets.newHashSet();
				for (Leg leg: instance.getItinerary().getData().getLeg()) {
					if (leg.getTransport() != null) {
						modes.add(GamificationHelper.convertTType(leg.getTransport().getType()));
					}
				}
				de.setTravelModes(modes);					
			} else if (instance.getFreeTrackingTransport() != null) {
				if (MODE_TYPE.OTHER.equals(TrackValidator.toModeType(instance.getFreeTrackingTransport()))) {
					logger.warn("OTHER transport type found for " + instance.getId());
					continue;
				}
				de.setTravelType(TravelType.FREETRACKING);

				logger.debug("DATA: "+instance+", "+ instance.getValidationResult()+", "+ instance.getValidationResult().getDistance());
				Double val = 0.0; 
				MODE_TYPE type = TrackValidator.toModeType(instance.getFreeTrackingTransport());
				if (instance.getValidationResult() != null && instance.getValidationResult().getValidationStatus().getEffectiveDistances().containsKey(type)) {
					val = instance.getValidationResult().getValidationStatus().getEffectiveDistances().get(type);
				}
				Map<String, Double> distances = Collections.singletonMap(instance.getFreeTrackingTransport(), val);
				de.setTravelDistances(distances);
				
				// TODO: remove
				de.setTravelModes(Sets.newHashSet(instance.getFreeTrackingTransport()));
			}
			if (instance.getChangedValidity() != null) {
				de.setTravelValidity(instance.getChangedValidity());	
			} else {
				de.setTravelValidity(instance.getValidationResult().getTravelValidity());
			}
			de.setEntityId(instance.getId());
			de.setClientId(instance.getClientId());
			de.setMultimodalId(instance.getMultimodalId());
			
			if (scores.containsKey(instance.getId())) {
				long score = scores.get(instance.getId()).longValue();
				de.setTravelScore(score);
				if (!ScoreStatus.ASSIGNED.equals(instance.getScoreStatus())) {
					instance.setScore(score);
					instance.setScoreStatus(ScoreStatus.ASSIGNED);
					storage.saveTrackedInstance(instance);
				}
			} else {
				de.setTravelScore(0L);
			}
			de.setScoreStatus(instance.getScoreStatus());
			
			result.add(de);
			
		}

		return groupByMultimodalId(result);
	}
	
	private List<DiaryEntry> groupByMultimodalId(List<DiaryEntry> instances) throws Exception {
		Multimap<String, DiaryEntry> grouped = ArrayListMultimap.create();
		
		List<DiaryEntry> result = Lists.newArrayList();
		instances.forEach(x -> {
			if (x.getMultimodalId() != null) {
				grouped.put(x.getMultimodalId(), x);
			} else {
				result.add(x);
			}
		});
		
		for (String key: grouped.keySet()) {
			List<DiaryEntry> group = (List)grouped.get(key);
			Collections.sort(group);
			Iterator<DiaryEntry> it = group.iterator();
			DiaryEntry root = it.next();
			
			String rootString = mapper.writeValueAsString(root);
			DiaryEntry rootCopy = mapper.readValue(rootString, DiaryEntry.class);
			rootCopy.setChildren(null);
			
			root.setChildren(Lists.newArrayList());
			root.getChildren().add(rootCopy);
			while (it.hasNext()) {
				root.getChildren().add(it.next());
			}
			Collections.sort(root.getChildren());
			result.add(root);
		}
		
		return result;
	}

	private Map<String, Double> getTransportDistances(List<Leg> legs) {
		Multimap<String, Double> distances = ArrayListMultimap.create();
		Map<String, Double> result = Maps.newTreeMap();
		
		for (Leg leg: legs) {
			if (leg.getTransport() != null) {
				String mode = GamificationHelper.convertTType(leg.getTransport().getType());
				distances.put(mode, leg.getLength());
			}
		}
		
		for (String key: distances.keySet()) {
			double value = 0;
			for (Double v: distances.get(key)) {
				value += v;
			}
			result.put(key, value);
		}
		
		return result;
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

	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(Exception.class)
	@ResponseBody
	ErrorInfo handleBadRequest(HttpServletRequest req, Exception ex) {
		logger.error("Error generating diary information", ex);
		StackTraceElement ste = ex.getStackTrace()[0];
		return new ErrorInfo(req.getRequestURL().toString(), ex.getClass().getTypeName(), ste.getClassName(),
				ste.getLineNumber());
	}

}
