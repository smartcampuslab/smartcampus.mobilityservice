package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.nio.charset.Charset;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeChoice;
import eu.trentorise.smartcampus.mobility.gamification.model.Inventory;
import eu.trentorise.smartcampus.mobility.gamification.model.Invitation;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeConcept.ChallengeDataType;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeInvitation;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeInvitation.ChallengePlayer;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeInvitation.PointConceptRef;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeInvitation.Reward;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerStatus;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.CustomTokenExtractor;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;
import eu.trentorise.smartcampus.profileservice.BasicProfileService;
import eu.trentorise.smartcampus.profileservice.model.BasicProfile;

@Controller
public class ChallengeController {

	private enum InvitationStatus {
		accept,refuse,cancel
	}
	
	private static transient final Logger logger = Logger.getLogger(ChallengeController.class);
	
	@Autowired
	@Value("${gamification.url}")
	private String gamificationUrl;	
	
	@Autowired
	@Value("${aacURL}")
	private String aacURL;
	
	@Autowired
	@Value("${mobilityURL}")
	private String mobilityUrl;		
	
	@Autowired
	private AppSetup appSetup;
	
	@Autowired
	private GameSetup gameSetup;	
	
	@Autowired
	private PlayerRepositoryDao playerRepositoryDao;
	
	@Autowired
	private StatusUtils statusUtils;	
	
	@Autowired
	private NotificationsManager notificationsManager;
	
	private BasicProfileService profileService;
	
	private CustomTokenExtractor tokenExtractor = new CustomTokenExtractor();	
	
	private ObjectMapper mapper = new ObjectMapper(); {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}	
	
	private Map<String, Reward> rewards;
	
	@PostConstruct
	public void init() throws Exception {
		profileService = new BasicProfileService(aacURL);
		
		rewards = mapper.readValue(Resources.getResource("challenges/rewards.json"), new TypeReference<Map<String, Reward>>() {
		});
	}
	
	@GetMapping("/gamificationweb/challenge/type/{playerId}")
	public @ResponseBody List<ChallengeChoice> getChallengesStatus(@RequestHeader(required = true, value = "appId") String appId, @PathVariable String playerId, HttpServletResponse response) throws Exception {
		String gameId = getGameId(appId);
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> result = restTemplate.exchange(gamificationUrl + "data/game/" + gameId + "/player/" + playerId + "/inventory", HttpMethod.GET, new HttpEntity<Object>(createHeaders(appId)), String.class);
		
		String res = result.getBody();
		
		Inventory inventory = mapper.readValue(res , Inventory.class);

		return inventory.getChallengeChoices();
	}	
	
	@GetMapping("/gamificationweb/challenges")
	public @ResponseBody eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeConcept getChallenges(@RequestHeader(required = true, value = "appId") String appId, @RequestParam(required=false) ChallengeDataType filter, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String token = tokenExtractor.extractHeaderToken(request);
		logger.debug("WS-get status user token " + token);
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				return null;
			}
		} catch (Exception e) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return null;
		}
		String userId = user.getUserId();
		String gameId = getGameId(appId);
		
		Player p = null;
		String nickName = "";
		p = playerRepositoryDao.findByPlayerIdAndGameId(userId, gameId);
		String language = "it";
		if(p != null){
			nickName = p.getNickname();
			language = (p.getLanguage() != null && !p.getLanguage().isEmpty()) ? p.getLanguage() : "it";
		}

		String statusUrl = "state/" + gameId + "/" + userId;
		String allData = getAll(statusUrl, appId);
		
		PlayerStatus ps =  statusUtils.convertPlayerData(allData, userId, gameId, nickName, mobilityUrl, 1, language);
		if (filter != null) {
			ps.getChallengeConcept().getChallengeData().entrySet().removeIf(x -> !filter.equals(x.getKey()));
		}
		
		return ps.getChallengeConcept();
	}	
	
	@PutMapping("/gamificationweb/challenge/choose/{challengeId}")
	public void chooseChallenge(@RequestHeader(required = true, value = "appId") String appId, @PathVariable String challengeId, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String token = tokenExtractor.extractHeaderToken(request);
		logger.debug("WS-get status user token " + token);
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				return;
			}
		} catch (Exception e) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return;
		}
		String userId = user.getUserId();
		String gameId = getGameId(appId);
		
		RestTemplate restTemplate = new RestTemplate();
		String partialUrl = "game/" + gameId + "/player/" + userId + "/challenges/" + challengeId + "/accept";
		ResponseEntity<String> tmp_res = restTemplate.exchange(gamificationUrl + "data/" + partialUrl, HttpMethod.POST, new HttpEntity<Object>(null, createHeaders(appId)), String.class);
		logger.info("Sent player registration to gamification engine(mobile-access) " + tmp_res.getStatusCode());		
	}
	
	@PostMapping("/gamificationweb/invitation")
	public void sendInvitation(@RequestHeader(required = true, value = "appId") String appId, @RequestBody Invitation invitation, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String token = tokenExtractor.extractHeaderToken(request);
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				return;
			}
		} catch (Exception e) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return;
		}
		String userId = user.getUserId();
		String gameId = getGameId(appId);		
		
		Player player = playerRepositoryDao.findByPlayerIdAndGameId(userId, gameId);
		if (player == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;			
		}
		Player attendee = playerRepositoryDao.findByPlayerIdAndGameId(invitation.getAttendeeId(), gameId);
		if (attendee == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;			
		}		
		
		if (attendee.getId().equals(player.getId())) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;	
		}
		
		ChallengeInvitation ci = new ChallengeInvitation();
		ci.setGameId(gameId);
		ci.setProposer(new ChallengePlayer(userId));
		ci.getGuests().add(new ChallengePlayer(invitation.getAttendeeId()));
		ci.setChallengeModelName(invitation.getChallengeModelName().toString()); // "groupCompetitivePerformance"
		
		LocalDateTime day = LocalDateTime.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).truncatedTo(ChronoUnit.DAYS);
		ci.setChallengeStart(new Date(day.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli())); // next saturday
		day = day.plusWeeks(1).minusSeconds(1);
		ci.setChallengeEnd(new Date(day.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli())); // 2 fridays
		
		ci.setChallengePointConcept(new PointConceptRef(invitation.getChallengePointConcept(), "weekly")); // "Walk_Km"
		
		Reward reward = rewards.get(ci.getChallengeModelName());
		ci.setReward(reward); // from body
		
		RestTemplate restTemplate = new RestTemplate();

		String url = gamificationUrl + "data/game/" + gameId + "/player/" + userId + "/invitation";
		logger.info("URL: " + url);
		logger.info("BODY: " + mapper.writeValueAsString(ci));
		
		ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<Object>(ci, createHeaders(appId)), String.class);
		
		if (result.getStatusCode() == HttpStatus.OK) {
			Map<String, String> extraData = Maps.newTreeMap();
			extraData.put("challengerName", player.getNickname());
			notificationsManager.sendDirectNotification(appId, attendee, "INVITATION", extraData);			
		}
		
	}

	@PostMapping("/gamificationweb/invitation/status/{challengeName}/{status}")
	public void changeInvitationStatus(@RequestHeader(required = true, value = "appId") String appId, @PathVariable String challengeName, @PathVariable InvitationStatus status, HttpServletRequest request, HttpServletResponse response) {
		String token = tokenExtractor.extractHeaderToken(request);
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				return;
			}
		} catch (Exception e) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return;
		}
		String userId = user.getUserId();
		String gameId = getGameId(appId);		
		
		Player player = playerRepositoryDao.findByPlayerIdAndGameId(userId, gameId);
		if (player == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;			
		}		
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> result = restTemplate.exchange(gamificationUrl + "data/game/" + gameId + "/player/" + userId + "/invitation/" + status + "/" + challengeName, HttpMethod.POST, new HttpEntity<Object>(null, createHeaders(appId)), String.class);
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
				set("Content-Type", "application/json");
			}
		};
	}		
	
}
