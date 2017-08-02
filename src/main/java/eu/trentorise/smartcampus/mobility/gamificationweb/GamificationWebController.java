package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import eu.trentorise.smartcampus.mobility.gamification.model.ClassificationBoard;
import eu.trentorise.smartcampus.mobility.gamification.model.ClassificationPosition;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeDescriptionDataSetup;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ClassificationData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerClassification;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerStatus;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.UserCheck;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.CustomTokenExtractor;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;
import eu.trentorise.smartcampus.profileservice.BasicProfileService;
import eu.trentorise.smartcampus.profileservice.model.AccountProfile;
import eu.trentorise.smartcampus.profileservice.model.BasicProfile;

@Controller
public class GamificationWebController {

	private static final String PLAYER_RANKING = "playerRanking";

	private static final String NICK_RECOMMANDATION = "nicknameRecommandation";
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
	@Value("${gamification.secretKey1}")
	private String secretKey1;
	@Autowired
	@Value("${gamification.secretKey2}")
	private String secretKey2;	

	@Autowired
	private PlayerRepositoryDao playerRepositoryDao;

	@Autowired
	private ChallengeDescriptionDataSetup challDescriptionSetup;

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
	
	private ObjectMapper mapper = new ObjectMapper();
	
	private CustomTokenExtractor tokenExtractor = new CustomTokenExtractor();
	
	@PostConstruct
	public void init() {
		profileService = new BasicProfileService(aacURL);
	}

	// TODO reenable?
//	// Cache for actual week classification
//	LoadingCache<String, String> cacheClass = CacheBuilder.newBuilder().maximumSize(100).expireAfterWrite(30, TimeUnit.SECONDS).build(new CacheLoader<String, String>() {
//		public String load(String actualWeekTs) throws Exception {
//			return callWSFromEngine(actualWeekTs);
//		}
//
//		/*
//		 * public ListenableFuture<String> reload(final String actualWeekTs,
//		 * String prevData) { if(neverNeedsRefresh(actualWeekTs)){ return
//		 * Futures.immediateFuture(prevData); } else { // asynchronous!
//		 * ListenableFutureTask<String> task = ListenableFutureTask.create(new
//		 * Callable<String>() { public String call() { return
//		 * callWSFromEngine(actualWeekTs); } }); executor.execute(task); return
//		 * task; } }
//		 */
//	});
	
//	// Scheduled method to cache the old week classification.
//		//@Scheduled(cron="55 59 23 * * FRI") 		// Repeat every Friday at 23:59:55 PM
//		@Scheduled(fixedRate = 31*60*1000) 		// Repeat every 31 minutes
//		public synchronized void refreshOldWeekClassification() throws IOException {
//			//oldWeekTimestamp = System.currentTimeMillis() - (LASTWEEKDELTA * 3);
//			oldWeekTimestamp = System.currentTimeMillis() - (LASTWEEKDELTA * 7);
//			logger.debug("Refreshing old week classification: new timestamp - " + oldWeekTimestamp);
//			lastWeekClassification = callWSFromEngine(oldWeekTimestamp + "");
//		}
//		
//		@Scheduled(fixedRate = 10*60*1000) 		// Repeat every ten minute
//		public synchronized void refreshGlobalCompleteNicks() throws IOException {
//			Long actualLong = playerRepositoryDao.count();
//			if(actualLong > playerNum){
//				try {
//					allNickNames = getAllNicksMapFromDB();
//				} catch (Exception e) {
//					logger.error("Error in nicknames refresh " + e.getMessage());
//				}
//				playerNum = actualLong;
//			}
//		}
//		
//		@Scheduled(fixedRate = 30*60*1000) 		// Repeat every thirty minute
//		public synchronized void refreshGlobalCompleteClassification() throws IOException {
//			logger.debug("Refreshing global week classification");
//			try{
//				globalCompleteClassification = callWSFromEngine("complete");
//			} catch (Exception ex){
//				logger.error("Error in global classification refresh");
//			}
//		}	
//		
//		// Scheduled method used to check user that has registered with a recommendation nick. If they have points a recommendation is send to gamification
//		@Scheduled(fixedRate = 29*60*1000)		// Repeat every 30 minutes
//		public synchronized void checkRecommendation() {
//			logger.debug("Starting recommendation check...");
//			String allData = "";
//			try {
//				//allData = chacheClass.get("complete");	// all classification data
//				allData = globalCompleteClassification;
//			} catch (Exception e) {
//				logger.error("Exception in global classification reading: " + e.getMessage());
//			}	
//			StatusUtils statusUtils = new StatusUtils();
//			Map<String, Integer> completeClassification = new HashMap<String, Integer>();
//			try {
//				completeClassification = statusUtils.correctGlobalClassification(allData);
//			} catch (JSONException e) {
//				logger.error("Exception in global classification calculating: " + e.getMessage());
//			}
//			String type = (isTest.compareTo("true") == 0) ? "test" : "prod";
//			Iterable<Player> iter = playerRepositoryDao.findAllByTypeAndCheckedRecommendation(type, false);
//			if(iter != null && Iterables.size(iter) > 0){
//				for(Player p: iter){
//					Map<String, Object> pData = p.getPersonalData();
//					if(pData != null){
//						String recommender = (String)pData.get(NICK_RECOMMANDATION);
//						String userId = p.getSocialId();
//						Integer points = completeClassification.get(userId);
//						if(points != null){
//							int score = points.intValue();
//							logger.debug("Green leaves point user " + userId + ": " + score);
//							int minRecPoints = 0;
//							try	{
//								minRecPoints = Integer.parseInt(RECOMMENDATION_POINTS);
//							} catch (Exception ex){
//								minRecPoints = 1;
//							}
//							if(score >= minRecPoints){
//								Player recPlayer = playerRepositoryDao.findByNickIgnoreCaseAndType(correctNameForQuery(recommender), type);
//								if (recommender != null && recPlayer != null) {
//									sendRecommendationToGamification(recPlayer.getPid());
//									p.setCheckedRecommendation(true);
//									playerRepositoryDao.save(p);	//update player data in db
//								}
//							}
//						} else {
//							logger.debug("Green leaves point user " + userId + ": none");
//						}
//					}
//				}
//			} else {
//				logger.debug("No player with recommandation to check!");
//			}
//			logger.debug("Ending recommendation check...");
//		}		

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
		if (withNick != null && withNick.getId().equals(id)) {
			logger.debug("External registration: nickname conflict with user " + withNick.getId());
			res.setStatus(HttpStatus.CONFLICT.value());
			return null;
		}
		Player p = playerRepositoryDao.findByIdAndGameId(id, gameId);
		if (p != null) {
			logger.debug("External registration: user exists");
			return null;
		} else {
			logger.debug("External registration: new user");
			data.put(TIMESTAMP, System.currentTimeMillis());
			p = new Player(id, gameId, user.getName(), user.getSurname(), nickname, email, language, true, data, null, true); // default sendMail attribute value is true
			if (data.containsKey(NICK_RECOMMANDATION) && !((String) data.get(NICK_RECOMMANDATION)).isEmpty()) {
				Player recommender = playerRepositoryDao.findByNicknameIgnoreCaseAndGameId(correctNameForQuery((String) data.get(NICK_RECOMMANDATION)), gameId);
				if (recommender != null) {
					p.setCheckedRecommendation(false);
					sendRecommendationToGamification(recommender.getId(), gameId, appId);
				} else {
					p.setCheckedRecommendation(true);
				}

			}
			try {
				createPlayerInGamification(id, gameId, appId);
				if (email != null) {
					logger.info("Added user (mobile registration) " + email);
				}
				playerRepositoryDao.save(p);
				return p;
			} catch (Exception e) {
				logger.error("Exception in user registration to gamification " + e.getMessage());
			}
		}
		return null;
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

	private String getFieldValue(String completeParam) {
		String val = "";
		String[] nameAndVal = completeParam.split("=");
		if (nameAndVal.length > 1) {
			val = nameAndVal[1];
		}
		return val;
	}

	// Method used to get the user status data (by mobyle app)
	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/status")
	@ResponseBody PlayerStatus getPlayerStatus(HttpServletRequest request, @RequestHeader(required = true, value = "appId") String appId, HttpServletResponse res) throws Exception{
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
		
		ChallengesUtils challUtils = new ChallengesUtils();
		if(challUtils.getChallLongDescriptionList() == null || challUtils.getChallLongDescriptionList().isEmpty()){
			challUtils.setChallLongDescriptionList(challDescriptionSetup.getDescriptions());
		}
		
		StatusUtils statusUtils = new StatusUtils();
		PlayerStatus ps =  statusUtils.correctPlayerData(allData, userId, gameId, nickName, challUtils, mobilityUrl, 1, language);
		
		return ps;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/classification")
	public @ResponseBody
	PlayerClassification getPlayerClassification(HttpServletRequest request, @RequestParam(required=false) Long timestamp, @RequestParam(required=false) Integer start, @RequestParam(required=false) Integer end, @RequestHeader(required = true, value = "appId") String appId, HttpServletResponse res) throws Exception{
		String token = tokenExtractor.extractHeaderToken(request);
		logger.debug("WS-get classification user token " + token);
		
		BasicProfile user = null;
		int maxClassificationSize = 500;
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
		
		PlayerClassification pc = getPlayerClassification(gameId, userId, timestamp, start, end, appId);
		
		return pc;
	}		
	
	
	private PlayerClassification getPlayerClassification(String gameId, String playerId, Long timestamp, Integer start, Integer end, String appId) throws Exception {
		
		int size = -1;
		if (end != null) {
			if (start != null) {
				size = end - start;
			} else {
				size = end;
			}
		}
		String paging = ((start != null) ? ("start=" + start + ((size != -1) ? "&" : "") ) : "")
				+ ((size != -1) ? ("size=" + size) : "");
		ClassificationBoard board;
		String url;
		if (timestamp == null) {
			url = "game/" + gameId + "/classification/" + URLEncoder.encode("week classification green", "UTF-8")
			+ ((paging != null) ? ("?" + paging) : "");
		} else {
			url = "game/" + gameId + "/incclassification/" + URLEncoder.encode("week classification green", "UTF-8") + "?timestamp=" + timestamp
					+ ((paging != null) ? ("&" + paging) : "");
		}
		
		board = getClassification(url, appId);
		PlayerClassification pc = null;
		if (board != null) {
			computeRanking(board);
			
			Query query = new Query();
			query.fields().include("socialId").include("nickname");
			
			List<Player> players = template.find(query, Player.class, "player");
			Map<String, String> nicknames = players.stream().collect(Collectors.toMap(Player::getId, Player::getNickname));

			pc = new PlayerClassification();
			List<ClassificationData> classificationList = Lists.newArrayList();
			for (ClassificationPosition pos: board.getBoard()) {
				if (nicknames.containsKey(pos.getPlayerId())) {
					ClassificationData cd = new ClassificationData(pos.getPlayerId(), nicknames.get(pos.getPlayerId()), (int) pos.getScore(), pos.getPosition());
					classificationList.add(cd);
					if (playerId.equals(pos.getPlayerId())) {
						pc.setActualUser(cd);
					}
				}
			}
			pc.setClassificationList(classificationList);
			
		}
		
		return pc;
		
	}
	
	// Method used to unsubscribe user to mailing list
		@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/unsubscribeMail")	///{socialId}
		public 
		ModelAndView unsubscribeMail(HttpServletRequest request, @RequestParam String playerId, @RequestParam String gameId) throws UnsupportedEncodingException, NoSuchPaddingException, NoSuchAlgorithmException {
			EncryptDecrypt cryptUtils = new EncryptDecrypt(secretKey1, secretKey2);
			Map<String, Object> model = new HashMap<String, Object>();
			String user_language = "it";
			Player p = null;
			if(playerId != null && playerId.compareTo("") != 0) { // && playerId.length() >= 16){
				logger.debug("WS-GET. Method unsubscribeMail. Passed data : " + playerId);
				String sId = "";
				try {
					sId = cryptUtils.decrypt(playerId);
				} catch (InvalidKeyException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e1) {
					logger.error("Error in decrypting socialId: " + e1.getMessage());
				} 
				if(sId != null && sId.compareTo("") != 0){	// case of incorrect encrypted string
					logger.info("WS-GET. Method unsubscribeMail. Finded player : " + sId);
					try {
						p = playerRepositoryDao.findByIdAndGameId(sId, gameId);
//						p.setSendMail(false);
						playerRepositoryDao.save(p);
						user_language = (p.getLanguage() != null && p.getLanguage().compareTo("") != 0) ? p.getLanguage() : "it";
					} catch (Exception ex){
						logger.error("Error in mailing unsubscribtion " + ex.getMessage());
					}
				}
			}
			boolean res = (p != null) ? true : false;
			model.put("wsresult", res);
			model.put("language", user_language);
			return new ModelAndView("web/unsubscribe", model);
		}	
	
	
	
	private void computeRanking(ClassificationBoard board) {
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

	private List<Player> getAllNicknames(HttpServletRequest request, @RequestParam String urlWS) throws Exception {
		logger.debug("WS-get All nickanmes.");
		List<Player> list = new ArrayList<Player>();
		Iterable<Player> iter = playerRepositoryDao.findAll();
		for (Player p : iter) {
			logger.debug(String.format("Profile result %s", p.getNickname()));
			list.add(p);
		}
		return list;
	}
	
	private Map<String, String> getAllNicksMapFromDB() throws Exception {
		logger.debug("DB - get All niks."); //Added for log ws calls info in preliminary phase of portal
		Map<String, String> niks = new HashMap<String, String>();
		Iterable<Player> iter = playerRepositoryDao.findAll();
		for(Player p: iter){
			if(p.getNickname() != null && p.getNickname().compareTo("") != 0){
				logger.debug(String.format("Profile result %s", p.getNickname()));
				niks.put(p.getId(), p.getNickname());
			}
		}
		return niks;
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
