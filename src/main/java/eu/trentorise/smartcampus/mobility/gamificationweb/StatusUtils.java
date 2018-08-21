package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import eu.trentorise.smartcampus.mobility.gamification.model.PlayerLevel;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.BadgeCollectionConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.BadgeConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.BadgesData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengesData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ClassificationData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerClassification;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerStatus;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConceptPeriod;

@Component
public class StatusUtils {

	private static final Logger logger = Logger.getLogger(StatusUtils.class);

	private static final String STATE = "state";
	private static final String PLAYER_ID = "playerId";
	private static final String BADGE_COLLECTION_CONCEPT = "BadgeCollectionConcept";
	private static final String LEVELS = "levels";
	private static final String BC_NAME = "name";
	private static final String BC_BADGE_EARNED = "badgeEarned";
	private static final String POINT_CONCEPT = "PointConcept";
	private static final String PC_GREEN_LEAVES = "green leaves";
	private static final String PC_NAME = "name";
	private static final String PC_SCORE = "score";
	private static final String PC_PERIOD = "periods";
	// private static final String PC_WEEKLY = "weekly";
	private static final String PC_START = "start";
	private static final String PC_PERIOD_DURATION = "period";
	private static final String PC_IDENTIFIER = "identifier";
	private static final String PC_INSTANCES = "instances";
	private static final String PC_END = "end";
	private static final String PARK_RIDE_PIONEER = "park and ride pioneer";
	private static final String BIKE_SHARING_PIONEER = "bike sharing pioneer";
	// private static final String PC_CLASSIFICATION_WEEK =
	// "green leaves week ";
	// private static final String PC_CLASSIFICATION_WEEK_TEST =
	// "green leaves week test";

	public static final long GAME_STARTING_TIME = 1460757600000L; // for RV 16
																	// april
	public static final long GAME_STARTING_TIME_TEST = 1468101601000L; // for TN
																		// test
																		// 10
																		// july
	public static final long MILLIS_IN_WEEK = 1000 * 60 * 60 * 24 * 7;

	private static final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	@Autowired
	private ChallengesUtils challUtils;
	
	@Autowired
	private BadgesCache badgeCache;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public PlayerStatus correctPlayerData(String profile, String playerId, String gameId, String nickName, String gamificationUrl, int challType, String language)
			throws JSONException {
		List<ChallengesData> challenges = new ArrayList<ChallengesData>();
		List<ChallengesData> oldChallenges = new ArrayList<ChallengesData>();
		List<PointConcept> pointConcept = new ArrayList<PointConcept>();
		List<PointConcept> greenPointConcept = new ArrayList<PointConcept>();
		PlayerStatus ps = new PlayerStatus();
		if (profile != null && profile.compareTo("") != 0) {

			Map<String, Object> playerData = buildPlayerData(playerId, gameId, nickName);
			List<BadgeCollectionConcept> bcc_list = new ArrayList<BadgeCollectionConcept>();
			ChallengeConcept cc = new ChallengeConcept();
			JSONArray badgeCollectionData = null;
			JSONArray pointConceptData = null;
			JSONObject profileData = new JSONObject(profile);
			JSONObject stateData = (!profileData.isNull(STATE)) ? profileData.getJSONObject(STATE) : null;
			if (stateData != null) {
				badgeCollectionData = (!stateData.isNull(BADGE_COLLECTION_CONCEPT)) ? stateData.getJSONArray(BADGE_COLLECTION_CONCEPT) : null;
				if (badgeCollectionData != null) {
					for (int i = 0; i < badgeCollectionData.length(); i++) {
						JSONObject badgeColl = badgeCollectionData.getJSONObject(i);
						String bc_name = (!badgeColl.isNull(BC_NAME)) ? badgeColl.getString(BC_NAME) : null;
						List<BadgeConcept> bc_badges = new ArrayList<BadgeConcept>();
						JSONArray bc_badgesEarned = (!badgeColl.isNull(BC_BADGE_EARNED)) ? badgeColl.getJSONArray(BC_BADGE_EARNED) : null;
						for (int j = 0; j < bc_badgesEarned.length(); j++) {
							String b_name = bc_badgesEarned.getString(j);
							String b_url = getUrlFromBadgeName(gamificationUrl, b_name);
							// if(!b_url.contains("/img/gamification/pr/p&rLeaves.png")){ //
							// not in default ParkAndRide badges
							BadgeConcept badge = new BadgeConcept(b_name, b_url);
							bc_badges.add(badge);
							// }
						}
						BadgeCollectionConcept bcc = new BadgeCollectionConcept(bc_name, bc_badges);
						bcc_list.add(bcc);
					}
				}
				pointConceptData = (!stateData.isNull(POINT_CONCEPT)) ? stateData.getJSONArray(POINT_CONCEPT) : null; // to update for
																														// new
																														// gamification
																														// version
				if (pointConceptData != null) {
					for (int i = 0; i < pointConceptData.length(); i++) {
						JSONObject point = pointConceptData.getJSONObject(i);
						String pc_name = (!point.isNull(PC_NAME)) ? point.getString(PC_NAME) : null;
						int pc_score = 0;
						String periodType = "";
						long start = 0L;
						long periodDuration = 0L;
						String identifier = "weekly";
						List<PointConceptPeriod> instances = new ArrayList<PointConceptPeriod>();
						if (pc_name != null) { // &&
												// pc_name.compareTo(PC_GREEN_LEAVES)
												// == 0
							pc_score = (!point.isNull(PC_SCORE)) ? point.getInt(PC_SCORE) : null;
							JSONObject pc_period = (!point.isNull(PC_PERIOD)) ? point.getJSONObject(PC_PERIOD) : null;
							if (pc_period != null) {
								Iterator<String> keys = pc_period.keys();
								while (keys.hasNext()) {
									instances = new ArrayList<PointConceptPeriod>();
									String key = keys.next();
									JSONObject pc_weekly = pc_period.getJSONObject(key);
									if (pc_weekly != null) {
										start = (!pc_weekly.isNull(PC_START)) ? pc_weekly.getLong(PC_START) : 0L;
										periodDuration = (!pc_weekly.isNull(PC_PERIOD_DURATION)) ? pc_weekly.getLong(PC_PERIOD_DURATION) : 0L;
										identifier = (!pc_weekly.isNull(PC_IDENTIFIER)) ? pc_weekly.getString(PC_IDENTIFIER) : "weekly";
										JSONObject pc_instances = pc_weekly.getJSONObject(PC_INSTANCES);
										Iterator<String> instancesKeys = pc_instances.keys();

										if (pc_instances != null) {
											// fix to preserve order as in
											// version < 2.2.0
											TreeMap<Date, PointConceptPeriod> sortMachine = new TreeMap<Date, PointConceptPeriod>();
											while (instancesKeys.hasNext()) {
												String instanceKey = instancesKeys.next();
												JSONObject pc_instance = pc_instances.getJSONObject(instanceKey);
												int instance_score = (!pc_instance.isNull(PC_SCORE)) ? pc_instance.getInt(PC_SCORE) : 0;
												long instance_start = (!pc_instance.isNull(PC_START)) ? pc_instance.getLong(PC_START) : 0L;
												long instance_end = (!pc_instance.isNull(PC_END)) ? pc_instance.getLong(PC_END) : 0L;
												PointConceptPeriod tmpPeriod = new PointConceptPeriod(instance_score, instance_start, instance_end);

												try {
													sortMachine.put(formatter.parse(instanceKey), tmpPeriod);
												} catch (ParseException e) {
													logger.error("Error parsing period instance key", e);
												}
											}
											for (PointConceptPeriod periodConcept : sortMachine.values()) {
												instances.add(periodConcept);
											}
										}
									}
									PointConcept pt = new PointConcept(pc_name, pc_score, identifier, start, periodDuration, identifier, instances);
									pointConcept.add(pt);	
									if (pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
										greenPointConcept.add(pt); // I add the point
																	// concept to the
																	// green leaves list
									}									
								}
							}
//							PointConcept pt = new PointConcept(pc_name, pc_score, periodType, start, periodDuration, identifier, instances);
//							pointConcept.add(pt);
//							if (pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
//								greenPointConcept.add(pt); // I add the point
//															// concept to the
//															// green leaves list
//							}
						}
					}
				}
				// new Challenge management part
				try {
					if (challUtils != null) {
						List<List> challLists = challUtils.correctChallengeData(playerId, gameId, profile, challType, language, pointConcept, bcc_list);
						if (challLists != null && challLists.size() == 2) {
							challenges = challLists.get(0);
							oldChallenges = challLists.get(1);
						}
						cc.setActiveChallengeData(challenges); // default is []
																// so I have to
																// initialize
																// the list
																// anyway
						cc.setOldChallengeData(oldChallenges);
					}
				} catch (Exception e) {
					logger.error("Error creating challenge info", e);
					e.printStackTrace();
				}
				
				try {
					ObjectMapper mapper = new ObjectMapper();
					Map profileMap = mapper.readValue(profile, Map.class);
					if (profileMap.containsKey(LEVELS)) {
						List<PlayerLevel> levels = mapper.convertValue((List)profileMap.get(LEVELS), new TypeReference<List<PlayerLevel>>() {
						});
						ps.setLevels(levels);
					}
					
				} catch (Exception e) {
					logger.error("Error creating levels", e);
					e.printStackTrace();
				}				
				
			}

			ps.setPlayerData(playerData);
			// ps.setBadgeCollectionConcept(cleanFromGenericBadges(bcc_list));
			// // filter for generic badges not more used
			ps.setBadgeCollectionConcept(bcc_list);
			ps.setPointConcept(greenPointConcept);
			ps.setChallengeConcept(cc);
		}
		return ps;
	}

	// Method cleanFromGenericBadges: useful method used to remove from the
	// badges list the generic badge (used in P&R and bikeSharing)
	private List<BadgeCollectionConcept> cleanFromGenericBadges(List<BadgeCollectionConcept> inputBadges) {
		List<BadgeCollectionConcept> correctedBadges = null;
		if (inputBadges != null) {
			for (BadgeCollectionConcept bcc : inputBadges) {
				if (bcc.getName().compareTo(PARK_RIDE_PIONEER) == 0) {
					List<BadgeConcept> badgeList = bcc.getBadgeEarned();
					for (int i = badgeList.size() - 1; i >= 0; i--) {
						if (badgeList.get(i).getUrl().contains("/img/gamification/pr/p&rLeaves.png")) {
							badgeList.remove(i);
						}
					}
				}
				if (bcc.getName().compareTo(BIKE_SHARING_PIONEER) == 0) {
					List<BadgeConcept> badgeList = bcc.getBadgeEarned();
					for (int i = badgeList.size() - 1; i >= 0; i--) {
						if (badgeList.get(i).getUrl().contains("/img/gamification/bike_sharing/bikeSharingPioneer.png")) {
							badgeList.remove(i);
						}
					}
				}
			}
			correctedBadges = inputBadges;
		}
		return correctedBadges;
	};

	public ClassificationData correctPlayerClassificationData(String profile, String playerId, String nickName, Long timestamp, String type) throws JSONException {
		ClassificationData playerClass = new ClassificationData();
		if (profile != null && profile.compareTo("") != 0) {

			int score = 0;
			// long time = (timestamp == null || timestamp.longValue() == 0L) ?
			// System.currentTimeMillis() : timestamp.longValue();
			// int weekNum = getActualWeek(time, type);

			JSONObject profileData = new JSONObject(profile);
			JSONObject stateData = (!profileData.isNull(STATE)) ? profileData.getJSONObject(STATE) : null;
			// System.out.println("My state " + stateData.toString());
			JSONArray pointConceptData = null;
			if (stateData != null) {
				pointConceptData = (!stateData.isNull(POINT_CONCEPT)) ? stateData.getJSONArray(POINT_CONCEPT) : null;
				if (pointConceptData != null) {
					for (int i = 0; i < pointConceptData.length(); i++) {
						JSONObject point = pointConceptData.getJSONObject(i);
						String pc_name = (!point.isNull(PC_NAME)) ? point.getString(PC_NAME) : null;
						if (timestamp == null || timestamp.longValue() == 0L) { // global
							if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
								score = (!point.isNull(PC_SCORE)) ? point.getInt(PC_SCORE) : null;
							}
						} else { // specific week
							if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
								JSONObject pc_period = (!point.isNull(PC_PERIOD)) ? point.getJSONObject(PC_PERIOD) : null;
								if (pc_period != null) {
									@SuppressWarnings("unchecked")
									Iterator<String> keys = pc_period.keys();
									while (keys.hasNext()) {
										String key = keys.next();
										JSONObject pc_weekly = pc_period.getJSONObject(key);
										if (pc_weekly != null) {
											JSONObject pc_instances = pc_weekly.getJSONObject(PC_INSTANCES);

											if (pc_instances != null) {
												Iterator<String> instancesKeys = pc_instances.keys();
												while (instancesKeys.hasNext()) {
													JSONObject pc_instance = pc_instances.getJSONObject(instancesKeys.next());
													int instance_score = (!pc_instance.isNull(PC_SCORE)) ? pc_instance.getInt(PC_SCORE) : 0;
													long instance_start = (!pc_instance.isNull(PC_START)) ? pc_instance.getLong(PC_START) : 0L;
													long instance_end = (!pc_instance.isNull(PC_END)) ? pc_instance.getLong(PC_END) : 0L;
													if (timestamp >= instance_start && timestamp <= instance_end) {
														score = instance_score;
														break;
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
				playerClass.setNickName(nickName);
				playerClass.setPlayerId(playerId);
				playerClass.setScore(score);
				if (nickName == null || nickName.compareTo("") == 0) {
					playerClass.setPosition(-1); // used for user without
													// nickName
				}
			}

		}
		return playerClass;
	}

	public List<ClassificationData> correctClassificationData(String allStatus, Map<String, String> allNicks, Long timestamp, String type) throws JSONException {
		List<ClassificationData> playerClassList = new ArrayList<ClassificationData>();
		if (allStatus != null && allStatus.compareTo("") != 0) {

			int score = 0;
			// long time = (timestamp == null || timestamp.longValue() == 0L) ?
			// System.currentTimeMillis() : timestamp;
			// int weekNum = getActualWeek(time, type);

			JSONObject allPlayersData = new JSONObject(allStatus);
			JSONArray allPlayersDataList = (!allPlayersData.isNull("content")) ? allPlayersData.getJSONArray("content") : null;
			if (allPlayersDataList != null) {
				for (int i = 0; i < allPlayersDataList.length(); i++) {
					JSONObject profileData = allPlayersDataList.getJSONObject(i);
					String playerId = (!profileData.isNull(PLAYER_ID)) ? profileData.getString(PLAYER_ID) : "0";
					score = 0; // here I reset the score value to avoid
								// classification problem
					// System.out.println("User " + playerId + " state " +
					// profileData.toString());
					JSONObject stateData = (!profileData.isNull(STATE)) ? profileData.getJSONObject(STATE) : null;
					JSONArray pointConceptData = null;
					if (stateData != null) {
						pointConceptData = (!stateData.isNull(POINT_CONCEPT)) ? stateData.getJSONArray(POINT_CONCEPT) : null;
						if (pointConceptData != null) {
							for (int j = 0; j < pointConceptData.length(); j++) {
								JSONObject point = pointConceptData.getJSONObject(j);
								String pc_name = (!point.isNull(PC_NAME)) ? point.getString(PC_NAME) : null;
								if (timestamp == null || timestamp.longValue() == 0L) { // global
									if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
										score = (!point.isNull(PC_SCORE)) ? point.getInt(PC_SCORE) : null;
									}
								} else { // specific week
									if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
										JSONObject pc_period = (!point.isNull(PC_PERIOD)) ? point.getJSONObject(PC_PERIOD) : null;
										if (pc_period != null) {
											@SuppressWarnings("unchecked")
											Iterator<String> keys = pc_period.keys();
											while (keys.hasNext()) {
												String key = keys.next();
												JSONObject pc_weekly = pc_period.getJSONObject(key);
												if (pc_weekly != null) {
													JSONObject pc_instances = pc_weekly.getJSONObject(PC_INSTANCES);
													if (pc_instances != null) {
														Iterator<String> instancesKeys = pc_instances.keys();
														while (instancesKeys.hasNext()) {
															JSONObject pc_instance = pc_instances.getJSONObject(instancesKeys.next());
															int instance_score = (!pc_instance.isNull(PC_SCORE)) ? pc_instance.getInt(PC_SCORE) : 0;
															long instance_start = (!pc_instance.isNull(PC_START)) ? pc_instance.getLong(PC_START) : 0L;
															long instance_end = (!pc_instance.isNull(PC_END)) ? pc_instance.getLong(PC_END) : 0L;
															if (timestamp >= instance_start && timestamp <= instance_end) {
																score = instance_score;
																break;
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
						String nickName = getPlayerNickNameById(allNicks, playerId); // getPlayerNameById(allNicks,
																						// playerId);
						ClassificationData playerClass = new ClassificationData();
						playerClass.setNickName(nickName);
						playerClass.setPlayerId(playerId);
						playerClass.setScore(score);
						if (nickName != null && nickName.compareTo("") != 0) { // if
																				// nickName
																				// present
																				// (user
																				// registered
																				// and
																				// active)
							playerClassList.add(playerClass);
						}
					}
				}
			}

		}
		return playerClassList;
	}

	// Method correctGlobalClassification: return a map 'playerId, score' of the
	// global classification
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, Integer> correctGlobalClassification(String allStatus) throws JSONException {
		Map classification = new HashMap<String, Integer>();
		if (allStatus != null && allStatus.compareTo("") != 0) {
			int score = 0;
			JSONObject allPlayersData = new JSONObject(allStatus);
			JSONArray allPlayersDataList = (!allPlayersData.isNull("content")) ? allPlayersData.getJSONArray("content") : null;
			if (allPlayersDataList != null) {
				for (int i = 0; i < allPlayersDataList.length(); i++) {
					JSONObject profileData = allPlayersDataList.getJSONObject(i);
					String playerId = (!profileData.isNull(PLAYER_ID)) ? profileData.getString(PLAYER_ID) : "0";
					score = 0; // here I reset the score value to avoid
								// classification problem
					JSONObject stateData = (!profileData.isNull(STATE)) ? profileData.getJSONObject(STATE) : null;
					JSONArray pointConceptData = null;
					if (stateData != null) {
						pointConceptData = (!stateData.isNull(POINT_CONCEPT)) ? stateData.getJSONArray(POINT_CONCEPT) : null;
						if (pointConceptData != null) {
							for (int j = 0; j < pointConceptData.length(); j++) {
								JSONObject point = pointConceptData.getJSONObject(j);
								String pc_name = (!point.isNull(PC_NAME)) ? point.getString(PC_NAME) : null;
								if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
									score = (!point.isNull(PC_SCORE)) ? point.getInt(PC_SCORE) : null;
								}
							}
						}
						classification.put(playerId, score);
					}
				}
			}

		}
		return classification;
	}

	public List<ClassificationData> correctClassificationIncData(String allStatus, Map<String, String> allNicks, /*
																													 * List<Player> allNicks,
																													 */Long timestamp, String type) throws JSONException {
		List<ClassificationData> playerClassList = new ArrayList<ClassificationData>();

		/*
		 * allStatus = "{" + "\"pointConceptName\": \"green leaves\"," + "\"type\": \"INCREMENTAL\"," + "\"board\": [" + "{" + "\"score\": 12," + "\"playerId\": \"3\"" + "}," + "{" + "\"score\": 10," + "\"playerId\": \"16\"" + "}," + "{" + "\"score\": 4," + "\"playerId\": \"4\"" + "}" + "]" + "}";
		 */

		if (allStatus != null && allStatus.compareTo("") != 0) {
			JSONObject allIncClassData = new JSONObject(allStatus);
			if (allIncClassData != null) {
				JSONArray allPlayersDataList = (!allIncClassData.isNull("board")) ? allIncClassData.getJSONArray("board") : null;
				if (allPlayersDataList != null) {
					for (int i = 0; i < allPlayersDataList.length(); i++) {
						JSONObject profileData = allPlayersDataList.getJSONObject(i);
						String playerId = (!profileData.isNull(PLAYER_ID)) ? profileData.getString(PLAYER_ID) : "0";
						Integer playerScore = (!profileData.isNull(PC_SCORE)) ? profileData.getInt(PC_SCORE) : 0;
						String nickName = getPlayerNickNameById(allNicks, playerId); // getPlayerNameById(allNicks,
																						// playerId);
						ClassificationData playerClass = new ClassificationData();
						playerClass.setNickName(nickName);
						playerClass.setPlayerId(playerId);
						playerClass.setScore(playerScore);
						if (nickName != null && nickName.compareTo("") != 0) { // if
																				// nickName
																				// present
																				// (user
																				// registered
																				// and
																				// active)
							playerClassList.add(playerClass);
						}
					}
				}
			}
		}
		return playerClassList;
	}

	public PlayerClassification completeClassificationPosition(List<ClassificationData> playersClass, ClassificationData actualPlayerClass, Integer from, Integer to) {
		List<ClassificationData> playersClassCorr = new ArrayList<ClassificationData>();
		int from_index = 0;
		PlayerClassification pc = new PlayerClassification();
		List<ClassificationData> cleanedList = new ArrayList<ClassificationData>();
		boolean myPosFind = false;
		if (playersClass != null && !playersClass.isEmpty()) {
			ClassificationData prec_pt = null;
			for (int i = 0; i < playersClass.size(); i++) {
				ClassificationData pt = playersClass.get(i);
				if (i > 0) {
					if (pt.getScore() < prec_pt.getScore()) {
						pt.setPosition(i + 1);
					} else {
						pt.setPosition(prec_pt.getPosition());
					}
				} else {
					pt.setPosition(i + 1);
				}
				prec_pt = pt;
				if (pt.getPlayerId().compareTo(actualPlayerClass.getPlayerId()) == 0) {
					myPosFind = true;
					actualPlayerClass.setPosition(pt.getPosition());
				}
				playersClassCorr.add(pt);
			}
			if (!myPosFind) {
				ClassificationData lastPlayer = playersClass.get(playersClass.size() - 1);
				if (lastPlayer.getScore() == actualPlayerClass.getScore()) {
					actualPlayerClass.setPosition(lastPlayer.getPosition());
				} else {
					actualPlayerClass.setPosition(lastPlayer.getPosition() + 1);
				}
				playersClassCorr.add(actualPlayerClass);
			}
			int to_index = playersClassCorr.size();
			if (from != null) {
				from_index = from.intValue();
			}
			if (to != null) {
				to_index = to.intValue();
			}
			if (from_index < 0)
				from_index = 0;
			if (from_index > playersClassCorr.size())
				from_index = playersClassCorr.size();
			if (to_index < 0)
				to_index = 0;
			if (to_index > playersClassCorr.size())
				to_index = playersClassCorr.size();
			if (from_index >= to_index)
				from_index = to_index;
			try {
				cleanedList = playersClassCorr.subList(from_index, to_index);
			} catch (Exception ex) {
				// do nothings
			}
			pc.setClassificationList(cleanedList);
		} else {
			pc.setClassificationList(playersClass);
			actualPlayerClass.setPosition(0);
		}
		pc.setActualUser(actualPlayerClass);
		return pc;
	}

	/*
	 * private int getActualWeek(long timestamp, String type){ int currWeek = 0; long millisFromGameStart = (type.compareTo("test") == 0) ? timestamp - GAME_STARTING_TIME_TEST : timestamp - GAME_STARTING_TIME; currWeek = (int)Math.ceil((float)millisFromGameStart / MILLIS_IN_WEEK); if(type.compareTo("test") == 0){ currWeek = 2; // forced actual week to 2 week in dev test } return currWeek; }
	 */

	private String getPlayerNameById(List<Player> allNicks, String id) throws JSONException {
		boolean find = false;
		String name = "";
		if (allNicks != null && !allNicks.isEmpty()) {
			// JSONObject playersData = new JSONObject(allNickJson);
			// JSONArray allNicksObjects = (!playersData.isNull("players")) ?
			// playersData.getJSONArray("players") : null;
			for (int i = 0; (i < allNicks.size()) && !find; i++) {
				Player player = allNicks.get(i);
				if (player != null) {
					String socialId = player.getId();
					if (socialId.compareTo(id) == 0) {
						name = player.getNickname();
						find = true;
					}
				}
			}
		}
		return name;
	}

	private String getPlayerNickNameById(Map<String, String> allNicks, String id) {
		String name = "";
		if (allNicks != null && !allNicks.isEmpty()) {
			name = allNicks.get(id);
		}
		return name;
	}

	public String getUrlFromBadgeName(String gamificationUrl, String b_name) {
		BadgesData badge = badgeCache.getBadge(b_name);
		if (badge != null) {
			return gamificationUrl + "/" + badge.getPath();
		}
		return null;
	}
	
//	private String getUrlFromBadgeName(String gamificationUrl, String b_name) {x
//		// green leaves badges
//		if (b_name.compareTo("king_week_green") == 0) {
//			return gamificationUrl + "/img/gamification/green/greenKingWeek.png";
//		}
//		if (b_name.compareTo("50_point_green") == 0) {
//			return gamificationUrl + "/img/gamification/green/greenLeaves50.png";
//		}
//		if (b_name.compareTo("100_point_green") == 0) {
//			return gamificationUrl + "/img/gamification/green/greenLeaves100.png";
//		}
//		if (b_name.compareTo("200_point_green") == 0) {
//			return gamificationUrl + "/img/gamification/green/greenLeaves200.png";
//		}
//		if (b_name.compareTo("400_point_green") == 0) {
//			return gamificationUrl + "/img/gamification/green/greenLeaves400.png";
//		}
//		if (b_name.compareTo("800_point_green") == 0) {
//			return gamificationUrl + "/img/gamification/green/greenLeaves800.png";
//		}
//		if (b_name.compareTo("1500_point_green") == 0) {
//			return gamificationUrl + "/img/gamification/green/greenLeaves1500.png";
//		}
//		if (b_name.compareTo("2500_point_green") == 0) {
//			return gamificationUrl + "/img/gamification/green/greenLeaves2500.png";
//		}
//		if (b_name.compareTo("5000_point_green") == 0) {
//			return gamificationUrl + "/img/gamification/green/greenLeaves5000.png";
//		}
//		if (b_name.compareTo("10000_point_green") == 0) {
//			return gamificationUrl + "/img/gamification/green/greenLeaves10000.png";
//		}
//		if (b_name.compareTo("20000_point_green") == 0) {
//			return gamificationUrl + "/img/gamification/green/greenLeaves20000.png";
//		}
//		if (b_name.compareTo("bronze-medal-green") == 0) {
//			return gamificationUrl + "/img/gamification/green/leaderboardGreen3.png";
//		}
//		if (b_name.compareTo("silver-medal-green") == 0) {
//			return gamificationUrl + "/img/gamification/green/leaderboardGreen2.png";
//		}
//		if (b_name.compareTo("gold-medal-green") == 0) {
//			return gamificationUrl + "/img/gamification/green/leaderboardGreen1.png";
//		}
//		// badges for health
//		if (b_name.compareTo("king_week_health") == 0) {
//			return gamificationUrl + "/img/gamification/health/healthKingWeek.png";
//		}
//		if (b_name.compareTo("10_point_health") == 0) {
//			return gamificationUrl + "/img/gamification/health/healthLeaves10.png";
//		}
//		if (b_name.compareTo("25_point_health") == 0) {
//			return gamificationUrl + "/img/gamification/health/healthLeaves25.png";
//		}
//		if (b_name.compareTo("50_point_health") == 0) {
//			return gamificationUrl + "/img/gamification/health/healthLeaves50.png";
//		}
//		if (b_name.compareTo("100_point_health") == 0) {
//			return gamificationUrl + "/img/gamification/health/healthLeaves100.png";
//		}
//		if (b_name.compareTo("200_point_health") == 0) {
//			return gamificationUrl + "/img/gamification/health/healthLeaves200.png";
//		}
//		if (b_name.compareTo("bronze_medal_health") == 0) {
//			return gamificationUrl + "/img/gamification/health/healthBronzeMedal.png";
//		}
//		if (b_name.compareTo("silver_medal_health") == 0) {
//			return gamificationUrl + "/img/gamification/health/healthSilverMedal.png";
//		}
//		if (b_name.compareTo("gold_medal_health") == 0) {
//			return gamificationUrl + "/img/gamification/health/healthGoldMedal.png";
//		}
//		// pr badges
//		if (b_name.compareTo("king_week_pr") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prKingWeek.png";
//		}
//		if (b_name.compareTo("10_point_pr") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prLeaves10.png";
//		}
//		if (b_name.compareTo("20_point_pr") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prLeaves20.png";
//		}
//		if (b_name.compareTo("50_point_pr") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prLeaves50.png";
//		}
//		if (b_name.compareTo("100_point_pr") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prLeaves100.png";
//		}
//		if (b_name.compareTo("200_point_pr") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prLeaves200.png";
//		}
//		if (b_name.compareTo("bronze_medal_pr") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prBronzeMedal.png";
//		}
//		if (b_name.compareTo("silver_medal_pr") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prSilverMedal.png";
//		}
//		if (b_name.compareTo("gold_medal_pr") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prGoldMedal.png";
//		}
//		if (b_name.compareTo("Manifattura_parking") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prPioneerManifattura.png";
//		}
//		if (b_name.compareTo("Stadio_parking") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prPioneerStadio.png";
//		}
//		// Real parking TN badges url
//		if (b_name.compareTo("Via Ragazzi del '99_parking") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prPioneerRagazzi99.png";
//		}
//		if (b_name.compareTo("Via Lidorno_parking") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prPioneerLidorno.png";
//		}
//		if (b_name.compareTo("Ghiaie via Fersina_parking") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prPioneerViaFersina.png";
//		}
//		if (b_name.compareTo("Ex-Zuffo_parking") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prPioneerAreaZuffo.png";
//		}
//		if (b_name.compareTo("Monte Baldo_parking") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prPioneerMonteBaldo.png";
//		}
//		if (b_name.compareTo("Via Asiago, Stazione FS Villazzano_parking") == 0) {
//			return gamificationUrl + "/img/gamification/pr/prPioneerVillazzanoStazioneFS.png";
//		}
//		if (b_name.contains("parking")) {
//			return gamificationUrl + "/img/gamification/pr/p&rLeaves.png";
//		}
//
//		// badges for bike
//		if (b_name.compareTo("1_bike_trip") == 0) {
//			return gamificationUrl + "/img/gamification/bike/bikeAficionado1.png";
//		}
//		if (b_name.compareTo("5_bike_trip") == 0) {
//			return gamificationUrl + "/img/gamification/bike/bikeAficionado5.png";
//		}
//		if (b_name.compareTo("10_bike_trip") == 0) {
//			return gamificationUrl + "/img/gamification/bike/bikeAficionado10.png";
//		}
//		if (b_name.compareTo("25_bike_trip") == 0) {
//			return gamificationUrl + "/img/gamification/bike/bikeAficionado25.png";
//		}
//		if (b_name.compareTo("50_bike_trip") == 0) {
//			return gamificationUrl + "/img/gamification/bike/bikeAficionado50.png";
//		}
//		if (b_name.compareTo("100_bike_trip") == 0) {
//			return gamificationUrl + "/img/gamification/bike/bikeAficionado100.png";
//		}
//		if (b_name.compareTo("200_bike_trip") == 0) {
//			return gamificationUrl + "/img/gamification/bike/bikeAficionado200.png";
//		}
//		if (b_name.compareTo("500_bike_trip") == 0) {
//			return gamificationUrl + "/img/gamification/bike/bikeAficionado500.png";
//		}
//		// badges for bike sharing
//		if (b_name.compareTo("Brione - Rovereto_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerBrione.png";
//		}
//		if (b_name.compareTo("Lizzana - Rovereto_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerLizzana.png";
//		}
//		if (b_name.compareTo("Marco - Rovereto_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerMarco.png";
//		}
//		if (b_name.compareTo("Municipio - Rovereto_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerMunicipio.png";
//		}
//		if (b_name.compareTo("Noriglio - Rovereto_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerNoriglio.png";
//		}
//		if (b_name.compareTo("Orsi - Rovereto_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerOrsi.png";
//		}
//		if (b_name.compareTo("Ospedale - Rovereto_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerOspedale.png";
//		}
//		if (b_name.compareTo("Via Paoli - Rovereto_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerPaoli.png";
//		}
//		if (b_name.compareTo("P. Rosmini - Rovereto_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerPRosmini.png";
//		}
//		if (b_name.compareTo("Quercia - Rovereto_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerQuercia.png";
//		}
//		if (b_name.compareTo("Sacco - Rovereto_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerSacco.png";
//		}
//		if (b_name.compareTo("Stazione FF.SS. - Rovereto_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerStazione.png";
//		}
//		if (b_name.compareTo("Zona Industriale - Rovereto_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerZonaIndustriale.png";
//		}
//		if (b_name.compareTo("Mart - Rovereto_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerMART.png";
//		}
//		// Real TN bike station url
//		if (b_name.compareTo("Stazione FFSS - Ospedale - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerFFSSOspedale.png";
//		}
//		if (b_name.compareTo("Piazza Venezia - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerPiazzaVenezia.png";
//		}
//		if (b_name.compareTo("Piscina - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerPiscina.png";
//		}
//		if (b_name.compareTo("Piazza della Mostra - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerPiazzaMostra.png";
//		}
//		if (b_name.compareTo("Centro Santa Chiara - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerCentroSantaChiara.png";
//		}
//		if (b_name.compareTo("Piazza di Centa - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerPiazzaCenta.png";
//		}
//		if (b_name.compareTo("Biblioteca - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerBiblioteca.png";
//		}
//		if (b_name.compareTo("Stazione Autocorriere - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerStazioneAutocorriere.png";
//		}
//		if (b_name.compareTo("Università - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerUniversita.png";
//		}
//		if (b_name.compareTo("Bezzi - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerBezzi.png";
//		}
//		if (b_name.compareTo("Muse - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerMuse.png";
//		}
//		if (b_name.compareTo("Azienda Sanitaria - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerAziendaSanitaria.png";
//		}
//		if (b_name.compareTo("Top Center - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerTopCenter.png";
//		}
//		if (b_name.compareTo("Bren Center - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerBrenCenter.png";
//		}
//		if (b_name.compareTo("Lidorno - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerLidorno.png";
//		}
//		if (b_name.compareTo("Gardolo - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerGardolo.png";
//		}
//		if (b_name.compareTo("Aeroporto - Trento_BSstation") == 0) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneerAeroporto.png";
//		}
//		if (b_name.contains("BSstation")) {
//			return gamificationUrl + "/img/gamification/bike_sharing/bikeSharingPioneer.png";
//		}
//		// badges for recommendation
//		if (b_name.compareTo("3_recommendations") == 0) {
//			return gamificationUrl + "/img/gamification/recommendation/inviteFriends3.png";
//		}
//		if (b_name.compareTo("5_recommendations") == 0) {
//			return gamificationUrl + "/img/gamification/recommendation/inviteFriends5.png";
//		}
//		if (b_name.compareTo("10_recommendations") == 0) {
//			return gamificationUrl + "/img/gamification/recommendation/inviteFriends10.png";
//		}
//		if (b_name.compareTo("25_recommendations") == 0) {
//			return gamificationUrl + "/img/gamification/recommendation/inviteFriends25.png";
//		}
//		// badges for public transport
//		if (b_name.compareTo("5_pt_trip") == 0) {
//			return gamificationUrl + "/img/gamification/public_transport/publicTransportAficionado5.png";
//		}
//		if (b_name.compareTo("10_pt_trip") == 0) {
//			return gamificationUrl + "/img/gamification/public_transport/publicTransportAficionado10.png";
//		}
//		if (b_name.compareTo("25_pt_trip") == 0) {
//			return gamificationUrl + "/img/gamification/public_transport/publicTransportAficionado25.png";
//		}
//		if (b_name.compareTo("50_pt_trip") == 0) {
//			return gamificationUrl + "/img/gamification/public_transport/publicTransportAficionado50.png";
//		}
//		if (b_name.compareTo("100_pt_trip") == 0) {
//			return gamificationUrl + "/img/gamification/public_transport/publicTransportAficionado100.png";
//		}
//		if (b_name.compareTo("200_pt_trip") == 0) {
//			return gamificationUrl + "/img/gamification/public_transport/publicTransportAficionado200.png";
//		}
//		if (b_name.compareTo("500_pt_trip") == 0) {
//			return gamificationUrl + "/img/gamification/public_transport/publicTransportAficionado500.png";
//		}
//		// badges for zero impact
//		if (b_name.compareTo("1_zero_impact_trip") == 0) {
//			return gamificationUrl + "/img/gamification/zero_impact/zeroImpact1.png";
//		}
//		if (b_name.compareTo("5_zero_impact_trip") == 0) {
//			return gamificationUrl + "/img/gamification/zero_impact/zeroImpact5.png";
//		}
//		if (b_name.compareTo("10_zero_impact_trip") == 0) {
//			return gamificationUrl + "/img/gamification/zero_impact/zeroImpact10.png";
//		}
//		if (b_name.compareTo("25_zero_impact_trip") == 0) {
//			return gamificationUrl + "/img/gamification/zero_impact/zeroImpact25.png";
//		}
//		if (b_name.compareTo("50_zero_impact_trip") == 0) {
//			return gamificationUrl + "/img/gamification/zero_impact/zeroImpact50.png";
//		}
//		if (b_name.compareTo("100_zero_impact_trip") == 0) {
//			return gamificationUrl + "/img/gamification/zero_impact/zeroImpact100.png";
//		}
//		if (b_name.compareTo("200_zero_impact_trip") == 0) {
//			return gamificationUrl + "/img/gamification/zero_impact/zeroImpact200.png";
//		}
//		if (b_name.compareTo("500_zero_impact_trip") == 0) {
//			return gamificationUrl + "/img/gamification/zero_impact/zeroImpact500.png";
//		}
//		// badges for leaderboard top 3
//		if (b_name.compareTo("1st_of_the_week") == 0) {
//			return gamificationUrl + "/img/gamification/leaderboard/leaderboard1.png";
//		}
//		if (b_name.compareTo("2nd_of_the_week") == 0) {
//			return gamificationUrl + "/img/gamification/leaderboard/leaderboard2.png";
//		}
//		if (b_name.compareTo("3rd_of_the_week") == 0) {
//			return gamificationUrl + "/img/gamification/leaderboard/leaderboard3.png";
//		}
//		return "";
//	}

	private Map<String, Object> buildPlayerData(String playerId, String gameId, String nickName) {
		Map<String, Object> map = Maps.newTreeMap();
		map.put("playerId", playerId);
		map.put("gameId", gameId);
		map.put("nickName", nickName);
		return map;
	}

}
