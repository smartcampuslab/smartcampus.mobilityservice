package eu.trentorise.smartcampus.mobility.gamification.challenges;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.trentorise.smartcampus.mobility.gamification.GamificationCache;
import eu.trentorise.smartcampus.mobility.gamification.model.GameStatistics;
import eu.trentorise.smartcampus.mobility.gamificationweb.StatusUtils;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConceptPeriod;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;

@Component
public class TargetPrizeChallengesCalculator {

	public static final String TARGET = "target";
	public static final String PLAYER1_PRZ = "player1_prz";
	public static final String PLAYER2_PRZ = "player2_prz";

	@Value("${gamification.url}")
	private String gamificationUrl;	
	
	@Autowired
	private GamificationCache gamificationCache;
	
	@Autowired
	private StatusUtils statusUtils;

	private ObjectMapper mapper = new ObjectMapper(); {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}	

	@Autowired
	private AppSetup appSetup;
	
	@Autowired
	private GameSetup gameSetup;

	// private DateTime execDate;
	private LocalDate lastMonday;

	private GameStatistics gs;

	private DifficultyCalculator dc;


	public Map<String, Double> targetPrizeChallengesCompute(String pId_1, String pId_2, String appId, String counter, String type) throws Exception {
		prepare();

		// Da sistemare richiesta per dati della settimana precedente, al momento non presenti
		List<GameStatistics> stats = getStatistics(appId);
		if (stats == null || stats.isEmpty()) {
			return null;
		}

		gs = stats.iterator().next();

		String data1 = gamificationCache.getPlayerState(pId_1, appId);
		Double player1_tgt = forecast(data1, counter);

		String data2 = gamificationCache.getPlayerState(pId_2, appId);
		Double player2_tgt = forecast(data2, counter);

		Map<String, Double> res = new HashMap<>();

		double target;
		if (type.equals("groupCompetitiveTime")) {
			target = (player1_tgt + player2_tgt) / 2.0;

			res.put(TARGET, target);
			res.put(PLAYER1_PRZ, evaluate(target, data1, counter));
			res.put(PLAYER2_PRZ, evaluate(target, data2, counter));
		} else {
			target = player1_tgt + player2_tgt;

			res.put(TARGET, target);
			res.put(PLAYER1_PRZ, evaluate(player1_tgt, data1, counter));
			res.put(PLAYER2_PRZ, evaluate(player2_tgt, data2, counter));
		}

		return res;
	}

	private void prepare() {

		// Set next monday as start, and next sunday as end
		// last?
		// int week_day = execDate.getDayOfWeek();
		// int d = (7 - week_day) + 1;
		// lastMonday = execDate.minusDays(week_day-1).minusDays(7);

		LocalDate now = LocalDate.now();
		lastMonday = now.minusDays(7).with(ChronoField.DAY_OF_WEEK, 1);

		dc = new DifficultyCalculator();
	}

	private Double forecast(String state, String counter) throws Exception {

		Double lastWeek = getWeeklyPlayerStateMode(state, counter, lastMonday);
		Double previousWeek = getWeeklyPlayerStateMode(state, counter, lastMonday.minusDays(7));

		double slope = (previousWeek - lastWeek) / (previousWeek != 0 ? previousWeek : 1);
		slope = Math.abs(slope) * 0.8;
		if (slope > 0.3)
			slope = 0.3;

		return (lastWeek * (1 + slope));
	}

	public Double getWeeklyPlayerStateMode(String status, String mode, LocalDate execDate) throws Exception {
		Map<String, Object> stateMap = mapper.readValue(status, Map.class);
		Map<String, Object> state = (Map<String, Object>) stateMap.get("state");
		List<Map> gePointsMap = mapper.convertValue(state.get("PointConcept"), new TypeReference<List<Map>>() {
		});

		long time = execDate.atStartOfDay().atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli();

		List<PointConcept> points = statusUtils.convertGEPointConcept(gePointsMap);

		for (PointConcept concept : points) {
//			System.err.println(concept.getName() + " / " + concept.getPeriodType() + " => " + concept.getInstances().size());
			if (mode.equals(concept.getName()) && "weekly".equals(concept.getPeriodType())) {
				for (PointConceptPeriod pcd : concept.getInstances()) {
					if (pcd.getStart() <= time && pcd.getEnd() > time) {
						System.err.println("S: " + pcd.getScore());
						return pcd.getScore();
					}
				}
			}
		}

		return 0.0;

	}

	/*
	 * private GameStatistics getGameStatistics(Set<GameStatistics> stats, String mode) { for (GameStatistics gs: stats) { if (gs.getPointConceptName().equals(mode)) return gs; } pf("ERROR COUNTER '%s' NOT FOUND", mode); return null; }
	 */

	public Double evaluate(Double target, String player, String counter) throws Exception {

		Double baseline = getWeeklyPlayerStateMode(player, counter, lastMonday);

		Integer difficulty = DifficultyCalculator.computeDifficulty(gs.getQuantiles(), baseline, target);

		double d = target * 1.0 / baseline;

		int prize = dc.calculatePrize(difficulty, d, counter);

		return Math.ceil(prize * ChallengesConfig.competitiveChallengesBooster);
	}

	private List<GameStatistics> getStatistics(String appId) throws Exception {
		String gameId = getGameId(appId);
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> result = restTemplate.exchange(gamificationUrl + "data/game/" + gameId + "/statistics", HttpMethod.GET, new HttpEntity<Object>(createHeaders(appId)), String.class);		
		
		List<GameStatistics> stats = mapper.readValue(result.getBody(),  new TypeReference<List<GameStatistics>>() {});
		
//		System.err.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(stats));
		
		return stats;
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
