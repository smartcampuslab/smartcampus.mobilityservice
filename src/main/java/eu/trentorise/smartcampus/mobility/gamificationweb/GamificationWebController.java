package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeDataDTO;
import eu.trentorise.smartcampus.mobility.gamification.model.ClassificationBoard;
import eu.trentorise.smartcampus.mobility.gamification.model.ClassificationPosition;
import eu.trentorise.smartcampus.mobility.gamification.model.ExecutionDataDTO;
import eu.trentorise.smartcampus.mobility.gamificationweb.WebLinkUtils.PlayerIdentity;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ClassificationData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerClassification;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerStatus;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.UserCheck;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.WeekConfData;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.CustomTokenExtractor;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;
import eu.trentorise.smartcampus.mobility.util.ConfigUtils;
import eu.trentorise.smartcampus.mobility.util.HTTPConnector;
import eu.trentorise.smartcampus.network.JsonUtils;
import eu.trentorise.smartcampus.profileservice.BasicProfileService;
import eu.trentorise.smartcampus.profileservice.model.AccountProfile;
import eu.trentorise.smartcampus.profileservice.model.BasicProfile;

@Controller
@EnableScheduling
public class GamificationWebController {

	private static final String NICK_RECOMMANDATION = "nick_recommandation";
	private static final String TIMESTAMP = "timestamp";
	
//	private String[] pointConcepts = { "p+r", "green", "health"};

	private static transient final Logger logger = Logger.getLogger(GamificationWebController.class);

	@Autowired
	@Value("${gamification.url}")
	private String gamificationUrl;
	
	@Autowired
	@Value("${mobilityURL}")
	private String mobilityUrl;	

	@Autowired
	private PlayerRepositoryDao playerRepositoryDao;

	@Autowired
	@Value("${aacURL}")
	private String aacURL;
	protected BasicProfileService profileService;

	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;	
	
	@Autowired
	private AppSetup appSetup;
	
	@Autowired
	private GameSetup gameSetup;	
	
	@Autowired
	private StatusUtils statusUtils;

	@Autowired
	private WebLinkUtils linkUtils;
	@Autowired
	private ReportEmailSender emailSender;
	
	@Autowired
	private ConfigUtils configUtils;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	private CustomTokenExtractor tokenExtractor = new CustomTokenExtractor();
	
	private LoadingCache<String, List<ClassificationData>> currentIncClassification;
	private LoadingCache<String, List<ClassificationData>> previousIncClassification;
	private LoadingCache<String, List<ClassificationData>> globalClassification;
	
	@PostConstruct
	public void init() {
		profileService = new BasicProfileService(aacURL);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		currentIncClassification = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES)
				.build(new CacheLoader<String, List<ClassificationData>>() {
					@Override
					public List<ClassificationData> load(String appId) throws Exception {
						String gameId = getGameId(appId);
						if (gameId != null) {
							try {
								return getFullIncClassification(gameId, appId, System.currentTimeMillis());
							} catch (Exception e) {
								logger.error("Error populating current classification cache.", e);
							}
						}
						return Collections.EMPTY_LIST;
					}
				});
		
		previousIncClassification = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES)
				.build(new CacheLoader<String, List<ClassificationData>>() {
					@Override
					public List<ClassificationData> load(String appId) throws Exception {
						String gameId = getGameId(appId);
						if (gameId != null) {
							try {
								return getFullIncClassification(gameId, appId, System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L);
							} catch (Exception e) {
								logger.error("Error populating previous classification cache.", e);
							}								
						}
						return Collections.EMPTY_LIST;
					}
				});	
		
		globalClassification = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES)
				.build(new CacheLoader<String, List<ClassificationData>>() {
					@Override
					public List<ClassificationData> load(String appId) throws Exception {
						String gameId = getGameId(appId);
						if (gameId != null) {
							try {
								return getFullClassification(gameId, appId);
							} catch (Exception e) {
								logger.error("Error populating previous classification cache.", e);
							}								
						}
						return Collections.EMPTY_LIST;
					}
				});			
		
	}
	
	@RequestMapping(method = RequestMethod.GET, value = {"/gamificationweb","/gamificationweb/"})	///{socialId}
	public 
	ModelAndView web(HttpServletRequest request, HttpServletResponse response, @RequestParam(required=false, defaultValue="it") String lang) {
		return new ModelAndView("redirect:/gamificationweb/rules");
	}

	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/cookie_license")	///{socialId}
	public 
	ModelAndView cookieLicense(HttpServletRequest request, HttpServletResponse response, @RequestParam(required=false, defaultValue="it") String lang) {
		return new ModelAndView("web/cookie_license");
	}
	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/cookie_info")	///{socialId}
	public 
	ModelAndView cookieInfo(HttpServletRequest request, HttpServletResponse response, @RequestParam(required=false, defaultValue="it") String lang) {
		return new ModelAndView("web/cookie_info");
	}

	
	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/{page}")	///{socialId}
	public 
	ModelAndView webPage(HttpServletRequest request, HttpServletResponse response, @RequestParam(required=false, defaultValue="it") String lang, @PathVariable String page) {
		RequestContextUtils.getLocaleResolver(request).setLocale(request, response, Locale.forLanguageTag(lang));

		ModelAndView model = new ModelAndView("web/index");
		model.addObject("language", lang);
		WeekConfData week = configUtils.getCurrentWeekConf();
		if (week != null) {
			model.addObject("week", week.getWeekNum());
			model.addObject("weeklyPrizes", configUtils.getWeekPrizes(week.getWeekNum(), lang));
		}
		model.addObject("view", page);
		return model;
	}

	// Method for mobile player registration (in mobile app)
	@RequestMapping(method = RequestMethod.POST, value = "/gamificationweb/register")
	public @ResponseBody Player registerExternal(@RequestBody Map<String, Object> data, @RequestParam String email,
			@RequestParam(required = false, defaultValue = "it") String language, @RequestParam String nickname, @RequestHeader(required = true, value = "appId") String appId, HttpServletRequest req, HttpServletResponse res) {
		logger.debug("External registration. ");

		BasicProfile user = null;
		AccountProfile account = null;
		String token = tokenExtractor.extractHeaderToken(req);
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				res.setStatus(HttpStatus.UNAUTHORIZED.value());
				return null;
			}
			if (email == null) {
				account = profileService.getAccountProfile(token);
				for (String aName : account.getAccountNames()) {
					for (String key : account.getAccountAttributes(aName).keySet()) {
						if (key.toLowerCase().contains("email")) {
							email = account.getAccountAttributes(aName).get(key);
							if (email != null)
								break;
						}
					}
					if (email != null)
						break;
				}
			}
		} catch (Exception e) {
			res.setStatus(HttpStatus.UNAUTHORIZED.value());
			return null;
		}
		String id = user.getUserId();
		String gameId = getGameId(appId);
		logger.debug("External registration: found user profile with id " + id);
		Player withNick = playerRepositoryDao.findByNicknameIgnoreCaseAndGameId(correctNameForQuery(nickname), gameId);
		if (withNick != null && !withNick.getId().equals(id)) {
			logger.debug("External registration: nickname conflict with user " + withNick.getId());
			res.setStatus(HttpStatus.CONFLICT.value());
			return null;
		}
		Player p = playerRepositoryDao.findByIdAndGameId(id, gameId);
		if (p != null) {
			logger.debug("External registration: user exists");
			res.setStatus(HttpStatus.CONFLICT.value());
			return null;
		} else {
			logger.debug("External registration: new user");
			data.put(TIMESTAMP, System.currentTimeMillis());
			p = new Player(id, gameId, user.getName(), user.getSurname(), nickname, email, language, true, data, null, true); // default sendMail attribute value is true
			if (data.containsKey(NICK_RECOMMANDATION) && !((String) data.get(NICK_RECOMMANDATION)).isEmpty()) {
				Player recommender = playerRepositoryDao.findByNicknameIgnoreCaseAndGameId(correctNameForQuery((String) data.get(NICK_RECOMMANDATION)), gameId);
				if (recommender != null) {
					p.setCheckedRecommendation(true);
				} else {
					p.setCheckedRecommendation(false);
				}

			}
			try {
				logger.info("Creating player");
				createPlayerInGamification(id, gameId, appId);
				if (email != null) {
					logger.info("Added user (mobile registration) " + email);
				}
				logger.info("Assigning survey challenge");
				assignSurveyChallenge(id, gameId, appId);
				logger.info("Assigning initial challenge");
				assignInitialChallenge(id, gameId, appId);
				logger.info("Saving player");
				playerRepositoryDao.save(p);
				return p;
			} catch (Exception e) {
				logger.error("Exception in user registration to gamification " + e.getMessage());
			}
		}
		return null;
	}

	@Scheduled(fixedRate = 1 * 60 * 1000) 
	public synchronized void checkRecommendations() throws Exception {
		for (AppInfo appInfo : appSetup.getApps()) {
			try {
				checkRecommendations(appInfo.getAppId());
			} catch (Exception e) {
				logger.error("Error checking recommendations for " + appInfo.getAppId());
				e.printStackTrace();
			}
		}
	}	
	
	private void checkRecommendations(String appId) throws Exception {
		String gameId = appSetup.findAppById(appId).getGameId();
		Iterable<Player> players = playerRepositoryDao.findAllByCheckedRecommendationAndGameId(true, gameId);
		for (Player player : players) {
			logger.debug("Checking recommendation for " + player.getId());
			if (player.getPersonalData() != null) {
				String nickname = (String) player.getPersonalData().get(NICK_RECOMMANDATION);
				if (nickname != null && !nickname.isEmpty()) {
					Player recommender = playerRepositoryDao.findByNicknameIgnoreCaseAndGameId(correctNameForQuery(nickname), gameId);
					if (recommender != null) {
						RestTemplate restTemplate = new RestTemplate();
						ResponseEntity<String> res = restTemplate.exchange(gamificationUrl + "gengine/state/" + gameId + "/" + player.getId(), HttpMethod.GET,
								new HttpEntity<Object>(null, createHeaders(appId)), String.class);
						String data = res.getBody();

						if (getGreenLeavesPoints(data) > 0) {
							logger.info("Sending recommendation to gamification engine: " + player.getId() + " -> " + recommender.getId());
							sendRecommendationToGamification(recommender.getId(), gameId, appId);
							player.setCheckedRecommendation(false);
							playerRepositoryDao.save(player);
						} else {
							logger.debug("Not Sending recommendation for " + player.getId() + " -> " + recommender.getId() + ", no points yet.");
						}
					} else {
						logger.debug("Recommender not found for " + player.getId());
						player.setCheckedRecommendation(false);
						playerRepositoryDao.save(player);
					}
				} else {
					logger.debug("No recommender for " + player.getId());
					player.setCheckedRecommendation(false);
					playerRepositoryDao.save(player);
				}
			}
		}
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

	// Method to force the player creation in gamification engine
	private void createPlayerInGamification(String playerId, String gameId, String appId) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		Map<String, Object> data = new HashMap<String, Object>();
		// data.put("actionId", "app_sent_recommandation");
		 data.put("gameId", gameId);
		data.put("playerId", playerId);
		String partialUrl = "game/" + gameId + "/player";
		ResponseEntity<String> tmp_res = restTemplate.exchange(gamificationUrl + "console/" + partialUrl, HttpMethod.POST, new HttpEntity<Object>(data, createHeaders(appId)), String.class);
		logger.info("Sent player registration to gamification engine(mobile-access) " + tmp_res.getStatusCode());
	}
	
	// /data/game/{gameId}/player/{playerId}/challenges
	private void assignSurveyChallenge(String playerId, String gameId, String appId) {
		RestTemplate restTemplate = new RestTemplate();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("bonusPointType", "green leaves");
		data.put("bonusScore", new Double(100.0));
		data.put("surveyType", "start");
		data.put("link", ""); // TODO
		
		ChallengeDataDTO challenge = new ChallengeDataDTO();
		long now = System.currentTimeMillis();
		challenge.setStart(new Date(now));
		challenge.setEnd(new Date(now + 2 * 7 * 24 * 60 * 60 * 1000L));

		challenge.setModelName("survey");
		challenge.setInstanceName("start_survey-" + Long.toHexString(now) + "-" + Integer.toHexString((playerId + gameId).hashCode()));
		
		challenge.setData(data);
		
		String partialUrl = "game/" + gameId + "/player/" + playerId + "/challenges";
		ResponseEntity<String> tmp_res = restTemplate.exchange(gamificationUrl + "data/" + partialUrl, HttpMethod.POST, new HttpEntity<Object>(challenge, createHeaders(appId)), String.class);
		logger.info("Sent player registration to gamification engine(mobile-access) " + tmp_res.getStatusCode());
	}	
	
	
	private void assignInitialChallenge(String playerId, String gameId, String appId) {
		RestTemplate restTemplate = new RestTemplate();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("bonusPointType", "green leaves");
		data.put("bonusScore", new Double(50.0));
		data.put("target", new Double(1.0));
		data.put("periodName", "weekly");
		data.put("counterName", "ZeroImpact_Trips");
		
		ChallengeDataDTO challenge = new ChallengeDataDTO();
		long now = System.currentTimeMillis();
		challenge.setStart(new Date(now));
		challenge.setEnd(new Date(now + 2 * 7 * 24 * 60 * 60 * 1000L));

		challenge.setModelName("absoluteIncrement");
		challenge.setInstanceName("'initial_challenge_" + Long.toHexString(now) + "-" + Integer.toHexString((playerId + gameId).hashCode()));
		
		challenge.setData(data);
		
		String partialUrl = "game/" + gameId + "/player/" + playerId + "/challenges";
		ResponseEntity<String> tmp_res = restTemplate.exchange(gamificationUrl + "data/" + partialUrl, HttpMethod.POST, new HttpEntity<Object>(challenge, createHeaders(appId)), String.class);
		logger.info("Sent player registration to gamification engine(mobile-access) " + tmp_res.getStatusCode());
	}		
	
	
	
	//Method used to send the survey call to gamification engine (if user complete the survey the engine need to be updated with this call)
	private void sendSurveyToGamification(String playerId, String gameId, String survey) throws Exception{

		ExecutionDataDTO ed = new ExecutionDataDTO();
		ed.setGameId(gameId);
		ed.setPlayerId(playerId);
		ed.setActionId(survey+"_survey_complete");
		ed.setData(Collections.emptyMap());

		String content = JsonUtils.toJSON(ed);
		GameInfo game = gameSetup.findGameById(gameId);
		HTTPConnector.doAuthenticatedPost(gamificationUrl + "/gengine/execute", content, "application/json", "application/json", game.getUser(), game.getPassword());		
//		logger.info("Sent app survey data to gamification engine ");
	}

	// Method used to check if a user is registered or not to the system (by
	// mobile app)
	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/checkuser/{socialId}")
	public @ResponseBody UserCheck getUserData(HttpServletRequest request, @PathVariable String socialId, @RequestHeader(required = true, value = "appId") String appId) {
		logger.debug("WS-get checkuser " + socialId);
		boolean result = false;
		String gameId = getGameId(appId);
		
		Player p = playerRepositoryDao.findByIdAndGameId(socialId, gameId);
		if (p != null && p.getNickname() != null && p.getNickname().compareTo("") != 0) {
			logger.debug(String.format("Profile find result %s", p.toJSONString()));
			result = true;
		}
		UserCheck uc = new UserCheck(result);
		logger.debug(String.format("WS-get check if user %s already access app: %s", socialId, result));
		return uc;
	}

	// Method used to get the user status data (by mobyle app)
	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/status")
	public @ResponseBody PlayerStatus getPlayerStatus(HttpServletRequest request, @RequestHeader(required = true, value = "appId") String appId, HttpServletResponse res) throws Exception{
		String token = tokenExtractor.extractHeaderToken(request);
		logger.debug("WS-get status user token " + token);
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				res.setStatus(HttpStatus.UNAUTHORIZED.value());
				return null;
			}
		} catch (Exception e) {
			res.setStatus(HttpStatus.UNAUTHORIZED.value());
			return null;
		}
		String userId = user.getUserId();
		String gameId = getGameId(appId);
		
		Player p = null;
		String nickName = "";
		p = playerRepositoryDao.findByIdAndGameId(userId, gameId);
		String language = "it";
		if(p != null){
			nickName = p.getNickname();
			language = ((p.getLanguage() != null) && (p.getLanguage().compareTo("") != 0)) ? p.getLanguage() : "it";
		}

		String statusUrl = "state/" + gameId + "/" + userId;
		String allData = getAll(statusUrl, appId);
		
		PlayerStatus ps =  statusUtils.correctPlayerData(allData, userId, gameId, nickName, mobilityUrl, 1, language);
		
		return ps;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/classification")
	public @ResponseBody
	PlayerClassification getPlayerClassification(HttpServletRequest request, @RequestParam(required=false) Long timestamp, @RequestParam(required=false) Integer start, @RequestParam(required=false) Integer end, @RequestHeader(required = true, value = "appId") String appId, HttpServletResponse res) throws Exception{
		String token = tokenExtractor.extractHeaderToken(request);
		logger.debug("WS-get classification user token " + token);
		
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				res.setStatus(HttpStatus.UNAUTHORIZED.value());
				return null;
			}
		} catch (Exception e) {
			res.setStatus(HttpStatus.UNAUTHORIZED.value());
			return null;
		}
		String userId = user.getUserId();
		String gameId = getGameId(appId);
		
//		PlayerClassification pc = getPlayerClassification(gameId, userId, timestamp, start, end, appId);
		PlayerClassification pc = getCachedPlayerClassification(userId, appId, timestamp, start, end);
		
		return pc;
	}		
	
	private List<ClassificationData> getFullIncClassification(String gameId, String appId, Long timestamp) throws Exception {
		String url = "game/" + gameId + "/incclassification/" + URLEncoder.encode("week classification green", "UTF-8") + "?timestamp=" + timestamp;
		ClassificationBoard board = getClassification(url, appId);
		if (board != null) {
			computeRanking(board);
		}
		
		Query query = new Query();
		query.fields().include("socialId").include("nickname");

		List<Player> players = template.find(query, Player.class, "player");
		Map<String, String> nicknames = players.stream().collect(Collectors.toMap(Player::getId, Player::getNickname));		
		
		List<ClassificationData> classificationList = Lists.newArrayList();
		for (ClassificationPosition pos : board.getBoard()) {
				ClassificationData cd = new ClassificationData(pos.getPlayerId(), nicknames.get(pos.getPlayerId()), (int) pos.getScore(), pos.getPosition());
				classificationList.add(cd);
			}
		
		return classificationList;
	}
	
	private List<ClassificationData> getFullClassification(String gameId, String appId) throws Exception {
		String url = "game/" + gameId + "/classification/" + URLEncoder.encode("global classification green", "UTF-8");
		ClassificationBoard board = getClassification(url, appId);
		if (board != null) {
			computeRanking(board);
		}
		
		Query query = new Query();
		query.fields().include("socialId").include("nickname");

		List<Player> players = template.find(query, Player.class, "player");
		Map<String, String> nicknames = players.stream().collect(Collectors.toMap(Player::getId, Player::getNickname));		
		
		List<ClassificationData> classificationList = Lists.newArrayList();
		for (ClassificationPosition pos : board.getBoard()) {
				ClassificationData cd = new ClassificationData(pos.getPlayerId(), nicknames.get(pos.getPlayerId()), (int) pos.getScore(), pos.getPosition());
				classificationList.add(cd);
			}
		
		return classificationList;
	}	
	
	private PlayerClassification getCachedPlayerClassification(String playerId, String appId, Long timestamp, Integer start, Integer end) throws ExecutionException {
		List<ClassificationData> data = null;

		if (timestamp != null) {
		WeekConfData wcd = configUtils.getWeek(timestamp);
		WeekConfData wcdnow = configUtils.getCurrentWeekConf();
		if (wcd != null && wcdnow != null) {
			if (wcd.getWeekNum() == wcdnow.getWeekNum()) {
				data = currentIncClassification.get(appId);
			} else if (wcd.getWeekNum() == wcdnow.getWeekNum() - 1) {
				data = previousIncClassification.get(appId);
			}
		}
		} else {
			data = globalClassification.get(appId);
		}
		
		PlayerClassification pc = new PlayerClassification();
		if (data == null) {
			return pc;
		}

		Query query = new Query();
		query.fields().include("socialId").include("nickname");

		List<Player> players = template.find(query, Player.class, "player");
		Map<String, String> nicknames = players.stream().collect(Collectors.toMap(Player::getId, Player::getNickname));

		pc.setClassificationList(data);
		for (ClassificationData cd : data) {
			if (playerId.equals(cd.getPlayerId())) {
				pc.setActualUser(cd);
				break;
			}
		}
		
		int size = 0;
		if (start == null || start < 1) {
			start = 1;
		}
		if (end != null) {
			if (start != null) {
				size = end - start + 1;
			} else {
				size = end;
			}
		}		
		
		data = data.stream().skip(start != null ? (start - 1) : 0).limit(size != 0 ? size : data.size()).collect(Collectors.toList());
		pc.setClassificationList(data);
		
		return pc;
	}		
	
//	private PlayerClassification getPlayerClassification(String gameId, String playerId, Long timestamp, Integer start, Integer end, String appId) throws Exception {
//		
//		int size = -1;
//		int page = 1;
//		if (end != null) {
//			if (start != null) {
//				size = end - start + 1;
//				page = start / size + 1;
//			} else {
//				size = end;
//			}
//		}
//		System.err.println(start + " / " + end + " => " + page + " / " + size);
//		
//		String paging = ((start != null) ? ("page=" + page + ((size != -1) ? "&" : "") ) : "")
//				+ ((size != -1) ? ("size=" + size) : "");
//		ClassificationBoard board;
//		String url;
//		if (timestamp == null) {
//			url = "game/" + gameId + "/classification/" + URLEncoder.encode("global classification green", "UTF-8")
//			+ ((paging != null) ? ("?" + paging) : "");
//		} else {
//			url = "game/" + gameId + "/incclassification/" + URLEncoder.encode("week classification green", "UTF-8") + "?timestamp=" + timestamp
//					+ ((paging != null) ? ("&" + paging) : "");
//		}
//		System.err.println(url);
//		
//		
//		board = getClassification(url, appId);
//		PlayerClassification pc = null;
//		if (board != null) {
//			computeRanking(board);
//
//			Query query = new Query();
//			query.fields().include("socialId").include("nickname");
//
//			List<Player> players = template.find(query, Player.class, "player");
//			Map<String, String> nicknames = players.stream().collect(Collectors.toMap(Player::getId, Player::getNickname));
//
//			pc = new PlayerClassification();
//			List<ClassificationData> classificationList = Lists.newArrayList();
//			for (ClassificationPosition pos : board.getBoard()) {
//				if (nicknames.containsKey(pos.getPlayerId())) {
//					ClassificationData cd = new ClassificationData(pos.getPlayerId(), nicknames.get(pos.getPlayerId()), (int) pos.getScore(), pos.getPosition());
//					classificationList.add(cd);
//					if (playerId.equals(pos.getPlayerId())) {
//						pc.setActualUser(cd);
//					}
//				}
//			}
//			pc.setClassificationList(classificationList);
//
//		} else {
//			pc = new PlayerClassification();
//			List<ClassificationData> cd = Lists.newArrayList();
//			pc.setClassificationList(cd);
//		}
//		
//		return pc;
//		
//	}
	
//	@Scheduled(cron="*/20 * * * * *")
//	private void getCurrentIncClassification() throws Exception {
//		for (AppInfo appInfo : appSetup.getApps()) {
//			String appId = appInfo.getAppId();
//			String gameId = getGameId(appId);
//			if (gameId != null) {
//				getFullIncClassification(gameId, appId, System.currentTimeMillis());				
//			}
//		}
//
//	}
	
//	@Scheduled(cron="*/20 * * * * *")
//	private void getCurrentIncClassification() throws Exception {
//		for (AppInfo appInfo : appSetup.getApps()) {
//			String appId = appInfo.getAppId();
//			System.err.println(appId + " = " + currentIncClassification.get(appId));
//		}
//
//	}	
	
		
	// Method used to unsubscribe user to mailing list
	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/survey/{lang}/{survey}/{playerId:.*}")	///{socialId}
	public 
	ModelAndView survey(HttpServletRequest request, HttpServletResponse response, @PathVariable String lang, @PathVariable String survey, @PathVariable String playerId) throws Exception {
		RequestContextUtils.getLocaleResolver(request).setLocale(request, response, Locale.forLanguageTag(lang));
		
		ModelAndView model = null;
		try {
			PlayerIdentity identity = linkUtils.decryptIdentity(playerId);
			String sId = identity.playerId;
			String gameId = identity.gameId;
			if(!StringUtils.isEmpty(sId)){	// case of incorrect encrypted string
				logger.info("Survey data. Found player : " + sId);
				Player p = playerRepositoryDao.findByIdAndGameId(sId, gameId);
				if (p.getSurveys().containsKey(survey)) {
					model = new ModelAndView("web/survey_complete");
					model.addObject("surveyComplete", true);
					return model;
				}
				model = new ModelAndView("web/survey/"+survey);
				model.addObject("language", lang);
				model.addObject("key", playerId);
				model.addObject("survey", survey);
				return model;
			} else {
				logger.error("Unkonwn user data:" + playerId);
				model = new ModelAndView("web/survey_complete");
				model.addObject("surveyComplete", false);
				return model;
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			model = new ModelAndView("web/survey_complete");
			model.addObject("surveyComplete", false);
			return model;
		}		
	}

	// Method used to unsubscribe user to mailing list
	@RequestMapping(method = RequestMethod.POST, value = "/gamificationweb/survey/{lang}/{survey}/{playerId:.*}")	///{socialId}
	public 
	ModelAndView sendSurvey(@RequestBody MultiValueMap<String,String> formData, @PathVariable String lang, @PathVariable String survey, @PathVariable String playerId) throws Exception {
		ModelAndView model =  new ModelAndView("web/survey_complete");
		try {
			PlayerIdentity identity = linkUtils.decryptIdentity(playerId);
			String sId = identity.playerId;
			String gameId = identity.gameId;
			if(!StringUtils.isEmpty(sId)){	// case of incorrect encrypted string
				logger.info("Survey data. Found player : " + sId);
					Player p = playerRepositoryDao.findByIdAndGameId(sId, gameId);
					if (!p.getSurveys().containsKey(survey)) {
						p.addSurvey(survey, toSurveyData(formData));
						sendSurveyToGamification(sId, gameId, survey);
						playerRepositoryDao.save(p);
					}
					model.addObject("surveyComplete", true);
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			model.addObject("surveyComplete", false);
			
		}
		return model;
	}
	
	/**
	 * @param formData
	 * @return
	 */
	private Map<String, Object> toSurveyData(MultiValueMap<String, String> formData) {
		Map<String, Object> result = new HashMap<>();
		formData.forEach((key, list) -> result.put(key, formData.getFirst(key)));
		return result;
	}

	// Method used to unsubscribe user to mailing list
	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/unsubscribeMail/{playerId:.*}")	///{socialId}
	public 
	ModelAndView unsubscribeMail(HttpServletRequest request, HttpServletResponse response, @PathVariable String playerId) throws Exception {
		ModelAndView model = new ModelAndView("web/unsubscribe");
		String user_language = "it";
		Player p = null;
		if(!StringUtils.isEmpty(playerId)) { // && playerId.length() >= 16){
			logger.debug("WS-GET. Method unsubscribeMail. Passed data : " + playerId);
			try {
				PlayerIdentity identity = linkUtils.decryptIdentity(playerId);
				String sId = identity.playerId;
				String gameId = identity.gameId;
				if(!StringUtils.isEmpty(sId)){	// case of incorrect encrypted string
					logger.info("WS-GET. Method unsubscribeMail. Found player : " + sId);
					p = playerRepositoryDao.findByIdAndGameId(sId, gameId);
					user_language = (p.getLanguage() != null && p.getLanguage().compareTo("") != 0) ? p.getLanguage() : "it";
				}
			} catch (Exception ex){
				logger.error("Error in mail unsubscribtion " + ex.getMessage());
				p = null;
			}
		}
		boolean res = (p != null) ? true : false;
		model.addObject("wsresult", res);
		
		RequestContextUtils.getLocaleResolver(request).setLocale(request, response, Locale.forLanguageTag(user_language));
		return model;
	}	
	@RequestMapping(method = RequestMethod.POST, value = "/gamificationweb/unsubscribeMail/{playerId:.*}")	///{socialId}
	public 
	ModelAndView sendUnsubscribeMail(HttpServletRequest request, HttpServletResponse response, @PathVariable String playerId) throws Exception {
		ModelAndView model = new ModelAndView("web/unsubscribesuccess");
		String user_language = "it";
		Player p = null;
		if(!StringUtils.isEmpty(playerId)) { // && playerId.length() >= 16){
			logger.debug("WS-GET. Method sendUnsubscribeMail. Passed data : " + playerId);
			try {
				PlayerIdentity identity = linkUtils.decryptIdentity(playerId);
				String sId = identity.playerId;
				String gameId = identity.gameId;
				if(!StringUtils.isEmpty(sId)){	// case of incorrect encrypted string
					logger.info("WS-GET. Method sendUnsubscribeMail. Found player : " + sId);
					p = playerRepositoryDao.findByIdAndGameId(sId, gameId);
					p.setSendMail(false);
					playerRepositoryDao.save(p);
					user_language = (p.getLanguage() != null && p.getLanguage().compareTo("") != 0) ? p.getLanguage() : "it";
				}
			} catch (Exception ex){
				logger.error("Error in mail unsubscribtion " + ex.getMessage());
				p = null;
			}
		}
		boolean res = (p != null) ? true : false;
		model.addObject("wsresult", res);
		
		RequestContextUtils.getLocaleResolver(request).setLocale(request, response, Locale.forLanguageTag(user_language));
		return model;
	}	
	
	
	private void computeRanking(ClassificationBoard board) {
		Multimap<Double, ClassificationPosition> ranking = ArrayListMultimap.create();
		board.getBoard().forEach(x -> ranking.put(x.getScore(), x));
		TreeSet<Double> scores = new TreeSet<>(ranking.keySet());

		int position = 1;
		for (Double score : scores.descendingSet()) {
			int ex = 0;
			for (ClassificationPosition exaequo : ranking.get(score)) {
				exaequo.setPosition(position);
				ex++;
			}
			position += ex;
		}
		board.setBoard(Lists.newArrayList(ranking.values()));
		Collections.sort(board.getBoard());

		board.setUpdateTime(System.currentTimeMillis());
	}
	
	private String getAll(@RequestParam String urlWS, String appId) {
		RestTemplate restTemplate = new RestTemplate();
		logger.debug("WS-GET. Method " + urlWS);
		String result = "";
		ResponseEntity<String> res = null;
		try {
			res = restTemplate.exchange(gamificationUrl + "gengine/" + urlWS, HttpMethod.GET, new HttpEntity<Object>(createHeaders(appId)), String.class);
		} catch (Exception ex) {
			logger.error(String.format("Exception in proxyController get ws. Method: %s. Details: %s", urlWS, ex.getMessage()));
		}
		if (res != null) {
			result = res.getBody();
		}
		return result;
	}

	public ClassificationBoard getClassification(@RequestParam String urlWS, String appId) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		logger.debug("WS-GET. Method " + urlWS); // Added for log ws calls info
													// in preliminary phase of
													// portal
		String result = "";
		ResponseEntity<String> tmp_res = null;
		try {
			// result = restTemplate.getForObject(gamificationUrl + urlWS,
			// String.class);
			tmp_res = restTemplate.exchange(gamificationUrl + "data/" + urlWS, HttpMethod.GET, new HttpEntity<Object>(createHeaders(appId)), String.class);
		} catch (Exception ex) {
			logger.error(String.format("Exception in proxyController get ws. Method: %s. Details: %s", urlWS, ex.getMessage()));
		}
		if (tmp_res != null) {
			result = tmp_res.getBody();
		}

		ClassificationBoard board = null;
		if (result != null && !result.isEmpty()) {
			board = mapper.readValue(result, ClassificationBoard.class);
			// Collections.sort(board.getBoard());

			Multimap<Double, ClassificationPosition> ranking = ArrayListMultimap.create();
			board.getBoard().forEach(x -> ranking.put(x.getScore(), x));
			TreeSet<Double> scores = new TreeSet<>(ranking.keySet());

			int position = 1;
			for (Double score : scores.descendingSet()) {
				final int pos = position;
				ranking.get(score).stream().forEach(x -> x.setPosition(pos));
				position++;
			}
			board.setBoard(Lists.newArrayList(ranking.values()));
			Collections.sort(board.getBoard());
		}

		return board;
	}

	@SuppressWarnings("serial")
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

	private void sendRecommendationToGamification(String recommenderId, String gameId, String appId) {
		RestTemplate restTemplate = new RestTemplate();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("actionId", "app_sent_recommandation");
		data.put("gameId", gameId);
		data.put("playerId", recommenderId);
		data.put("data", new HashMap<String, Object>());
		ResponseEntity<String> tmp_res = restTemplate.exchange(gamificationUrl + "gengine/execute", HttpMethod.POST, new HttpEntity<Object>(data, createHeaders(appId)), String.class);
		logger.info("Sent app recommendation to gamification engine " + tmp_res.getStatusCode());
	}

	private String correctNameForQuery(String nickName) {
		return "^" + nickName + "$";
	};
	

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
	
}
