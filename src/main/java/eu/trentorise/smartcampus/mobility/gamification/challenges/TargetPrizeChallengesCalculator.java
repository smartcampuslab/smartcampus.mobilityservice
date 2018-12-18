package eu.trentorise.smartcampus.mobility.gamification.challenges;

//import static eu.fbk.das.rs.challenges.generation.RecommendationSystemChallengeGeneration.*;
//import static eu.fbk.das.rs.utils.Utils.*;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

//import eu.trentorise.game.challenges.rest.GameStatisticsSet;
import eu.trentorise.smartcampus.mobility.gamification.GamificationCache;
import eu.trentorise.smartcampus.mobility.gamification.model.GameStatistics;
import eu.trentorise.smartcampus.mobility.gamificationweb.StatusUtils;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConceptPeriod;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
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
	
	private static double FAKE = 0.0;

	private ObjectMapper mapper = new ObjectMapper();
	{
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Autowired
	private AppSetup appSetup;

	@Autowired
	private GameSetup gameSetup;

	// private DateTime execDate;
	// private LocalDate lastMonday;

	// private DateTime execDate;
	// private DateTime lastMonday;

	private GameStatistics gs;

	private DifficultyCalculator dc;

	// private String appId;
	// private String gameId;
	// private RecommendationSystem rs;

	// public void prepare(String host, String username, String password, String appId) {
	//// this.appId = appId;
	//// this.gameId = getGameId(appId);
	//
	// LocalDate now = LocalDate.now();
	// lastMonday = now.minusDays(7).with(ChronoField.DAY_OF_WEEK, 1);
	// }
	//
	// // TODO remove
	// public void prepare(String appId) {
	//// execDate = new DateTime();
	//// this.appId = appId;
	//// this.gameId = getGameId(appId);
	//
	// LocalDate now = LocalDate.now();
	// lastMonday = LocalDate.now().minusDays(7).with(ChronoField.DAY_OF_WEEK, 1);
	// }

	public Map<String, Double> targetPrizeChallengesCompute(String pId_1, String pId_2, String appId, String counter, String type) throws Exception {

		prepare();

		// String gameId = getGameId(appId);

		Map<Integer, Double> quantiles = getQuantiles(appId, counter);
//		System.err.println(quantiles);

		Map<String, Double> res = Maps.newTreeMap();

		String data1 = gamificationCache.getPlayerState(pId_1, appId);
		Pair<Double, Double> res1 = forecast(res, "player1", data1, counter);
		double player1_tgt = res1.getFirst();
		double player1_bas = res1.getSecond();

		String data2 = gamificationCache.getPlayerState(pId_2, appId);
		Pair<Double, Double> res2 = forecast(res, "player2", data2, counter);
		double player2_tgt = res2.getFirst();
		double player2_bas = res2.getSecond();

		double target;
		if (type.equals("groupCompetitiveTime")) {
			target = roundTarget(counter, (player1_tgt + player2_tgt) / 2.0);

			res.put("target", target);
			res.put("player1_prz", evaluate(target, player1_bas, counter, quantiles));
			res.put("player2_prz", evaluate(target, player2_bas, counter, quantiles));
		} else if (type.equals("groupCooperative")) {
			target = roundTarget(counter, player1_tgt + player2_tgt);

			double player1_prz = evaluate(player1_tgt, player1_bas, counter, quantiles);
			double player2_prz = evaluate(player2_tgt, player2_bas, counter, quantiles);
			double prz = Math.max(player1_prz, player2_prz);

			res.put("target", target);
			res.put("player1_prz", prz);
			res.put("player2_prz", prz);
		} else if (type.equals("groupCompetitivePerformance")) {
			// p("WRONG TYPE");
		}

		return res;
	}

	private Double checkMinTarget(String counter, Double v) {
		if ("Walk_Km".equals(counter))
			return Math.max(1, v);
		if ("Bike_Km".equals(counter))
			return Math.max(5, v);
		if ("green leaves".equals(counter))
			return Math.max(50, v);

		// p("WRONG COUNTER");
		return 0.0;
	}

	// private Map<Integer, Double> getQuantiles2(String gameId, String counter) {
	// return rs.getStats().getQuantiles(counter);
	// }

	private Map<Integer, Double> getQuantiles(String appId, String counter) throws Exception {
		// Da sistemare richiesta per dati della settimana precedente, al momento non presenti
		List<GameStatistics> stats = getStatistics(appId, counter);
		if (stats == null || stats.isEmpty()) {
			return null;
		}

		gs = stats.iterator().next();
		return gs.getQuantiles();
	}

	private void prepare() {
		dc = new DifficultyCalculator();
	}

	private Pair<Double, Double> forecast(Map<String, Double> res, String nm, String state, String counter) throws Exception {

		// Last 3 values?
		int v = 3;
		double[][] d = new double[v][];

		LocalDate date = LocalDate.now().minusDays(7).with(ChronoField.DAY_OF_WEEK, 1);

		double wma = 0;
		int wma_d = 0;

		for (int i = 0; i < v; i++) {
			int ix = v - (i + 1);
			d[ix] = new double[2];
			Double c = getWeeklyPlayerStateMode(state, counter, date);
			d[ix][1] = c;
			d[ix][0] = ix + 1;
			date = date.minusDays(7);
			res.put(nm + "_base_" + ix, c);

			wma += (v - i) * c;
			wma_d += (v - i);
		}

		wma /= wma_d;

		SimpleRegression simpleRegression = new SimpleRegression(true);
		simpleRegression.addData(d);

		double slope = simpleRegression.getSlope();
		double intercept = simpleRegression.getIntercept();
		double pv;
		if (slope < 0)
			pv = wma * 1.1;
		else
			pv = intercept + slope * (v + 1) * 0.9;

		pv = checkMinTarget(counter, pv);

		res.put(nm + "_tgt", pv);

		return new Pair<Double, Double>(pv, wma);
	}

	// public Double getWeeklyContentMode(Player cnt, String mode, DateTime execDate) {
	//
	// for (PointConcept pc : cnt.getState().getPointConcept()) {
	//
	// String m = pc.getName();
	// if (!m.equals(mode))
	// continue;
	//
	// return pc.getPeriodScore("weekly", execDate);
	// }
	//
	// return 0.0;
	// }

	/*
	 * private GameStatistics getGameStatistics(Set<GameStatistics> stats, String mode) { for (GameStatistics gs: stats) { if (gs.getPointConceptName().equals(mode)) return gs; } pf("ERROR COUNTER '%s' NOT FOUND", mode); return null; }
	 */

	public Double evaluate(Double target, Double baseline, String counter, Map<Integer, Double> quantiles) {
		if (baseline == 0) {
			return 100.0;
		}

		Integer difficulty = DifficultyCalculator.computeDifficulty(quantiles, baseline, target);

		double d = (target / Math.max(1, baseline)) - 1;

		int prize = dc.calculatePrize(difficulty, d, counter);

		return Math.ceil(prize * ChallengesConfig.competitiveChallengesBooster / 10.0) * 10;
	}

	public Double getWeeklyPlayerStateMode(String status, String mode, LocalDate execDate) throws Exception {
		Map<String, Object> stateMap = mapper.readValue(status, Map.class);
		Map<String, Object> state = (Map<String, Object>) stateMap.get("state");
		List<Map> gePointsMap = mapper.convertValue(state.get("PointConcept"), new TypeReference<List<Map>>() {
		});

		long time = LocalDate.now().atStartOfDay().atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli();

		List<PointConcept> points = statusUtils.convertGEPointConcept(gePointsMap);

		for (PointConcept concept : points) {
			// System.err.println(concept.getName() + " / " + concept.getPeriodType() + " => " + concept.getInstances().size());
			if (mode.equals(concept.getName()) && "weekly".equals(concept.getPeriodType())) {
				for (PointConceptPeriod pcd : concept.getInstances()) {
					if (pcd.getStart() <= time && pcd.getEnd() > time) {
						return pcd.getScore();
					}
				}
			}
		}

		return 0.0;

	}

	private List<GameStatistics> getStatistics(String appId, String counter) throws Exception {
		List<GameStatistics> stats = gamificationCache.getStatistics(appId);
		return stats.stream().filter(x -> counter.equals(x.getPointConceptName())).collect(Collectors.toList());
	}

	private static double roundTarget(String mode, double improvementValue) {
		if (mode.endsWith("_Trips")) {
			improvementValue = Math.ceil(improvementValue);
		} else {
			if (improvementValue > 1000)
				improvementValue = Math.ceil(improvementValue / 100) * 100;
			else if (improvementValue > 100)
				improvementValue = Math.ceil(improvementValue / 10) * 10;
			else
				improvementValue = Math.ceil(improvementValue);
		}
		return improvementValue;
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

}
