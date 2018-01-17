package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.stringtemplate.v4.ST;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.BadgeCollectionConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeLongDescrStructure;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeStructure;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengesData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConceptPeriod;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ServerChallengesData;

@Component
public class ChallengesUtils {
	
	@Autowired
	private WebLinkUtils utils;
	
	// challange fields
	private static final String CHAL_FIELDS_PERIOD_NAME = "periodName";
	private static final String CHAL_FIELDS_BONUS_POINT_TYPE = "bonusPointType";
	private static final String CHAL_FIELDS_COUNTER_NAME = "counterName";
//	private static final String CHAL_FIELDS_BADGE_COLLECTION_NAME = "badgeCollectionName";
	private static final String CHAL_FIELDS_BONUS_SCORE = "bonusScore";
	private static final String CHAL_FIELDS_BASELINE = "baseline";
	private static final String CHAL_FIELDS_TARGET = "target";
	private static final String CHAL_FIELDS_PERIOD_TARGET = "periodTarget";
	private static final String CHAL_FIELDS_INITIAL_BADGE_NUM = "initialBadgeNum";
//	private static final String CHAL_FIELDS_POS_MIN = "posMin";
//	private static final String CHAL_FIELDS_POS_MAX = "posMax";
	// new challenge types
	private static final String CHAL_MODEL_PERCENTAGE_INC = "percentageIncrement";
	private static final String CHAL_MODEL_ABSOLUTE_INC = "absoluteIncrement";
	private static final String CHAL_MODEL_REPETITIVE_BEAV = "repetitiveBehaviour";
	private static final String CHAL_MODEL_NEXT_BADGE = "nextBadge";
	private static final String CHAL_MODEL_COMPLETE_BADGE_COLL = "completeBadgeCollection";
	private static final String CHAL_MODEL_SURVEY = "survey";
	private static final String CHAL_MODEL_POICHECKIN = "poiCheckin";
	private static final String CHAL_MODEL_CHECKIN = "checkin";
	private static final String CHAL_MODEL_CLASSPOSITION = "leaderboardPosition";
	
	// week delta in milliseconds
//	private static final Long W_DELTA = 2000L;
	private static final int MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
		
	private static final Logger logger = LoggerFactory.getLogger(ChallengesUtils.class);

	private ObjectMapper mapper = new ObjectMapper();


	private Map<String, ChallengeStructure> challengeStructureMap;
	private Map<String, ChallengeLongDescrStructure> challengeLongStructureMap;

	private Map<String, List> challengeDictionaryMap;
	private Map<String, String> challengeReplacements;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@PostConstruct
	private void init() throws Exception {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// template.dropCollection(ChallengeStructure.class);

		challengeStructureMap = Maps.newTreeMap();
		challengeLongStructureMap = Maps.newTreeMap();

		List list = mapper.readValue(Resources.getResource("challenges/challenges.json"), List.class);
		for (Object o : list) {
			ChallengeStructure challenge = mapper.convertValue(o, ChallengeStructure.class);

			String key = challenge.getName() + (challenge.getFilter() != null ? ("#" + challenge.getFilter()) : "");
			challengeStructureMap.put(key, challenge);
//			template.save(challenge);
		}
		
		list = mapper.readValue(Resources.getResource("challenges/challenges_descriptions.json"), List.class);
		for (Object o : list) {
			ChallengeLongDescrStructure challenge = mapper.convertValue(o, ChallengeLongDescrStructure.class);

			String key = challenge.getModelName() + "#" + challenge.getFilter();
			challengeLongStructureMap.put(key, challenge);
//			template.save(challenge);
		}

		challengeDictionaryMap = mapper.readValue(Resources.getResource("challenges/challenges_dictionary.json"), Map.class);
		challengeReplacements = mapper.readValue(Resources.getResource("challenges/challenges_replacements.json"), Map.class);
	}

	@SuppressWarnings("rawtypes")
	public List<ChallengeConcept> parse(String data) throws Exception {
		List<ChallengeConcept> concepts = Lists.newArrayList();

		Map playerMap = mapper.readValue(data, Map.class);
		if (playerMap.containsKey("state")) {
			Map stateMap = mapper.convertValue(playerMap.get("state"), Map.class);
			if (stateMap.containsKey("ChallengeConcept")) {
				List conceptList = mapper.convertValue(stateMap.get("ChallengeConcept"), List.class);
				for (Object o : conceptList) {
					ChallengeConcept concept = mapper.convertValue(o, ChallengeConcept.class);
					concepts.add(concept);
				}
			}
		}
		return concepts;
	}
	
	
	// Method correctChallengeData: used to retrieve the challenge data objects from the user profile data
	@SuppressWarnings("rawtypes")
	public List<List> correctChallengeData(String playerId, String gameId, String profile, int type, String language, List<PointConcept> pointConcept, List<BadgeCollectionConcept> bcc_list) throws Exception {
		List<ChallengesData> challenges = new ArrayList<ChallengesData>();
    	List<ChallengesData> oldChallenges = new ArrayList<ChallengesData>();
    	List<List> challengesList = new ArrayList<List>();
    	if(profile != null && profile.compareTo("") != 0){
    		
    		List<ChallengeConcept> challengeList = parse(profile);
    		
    		if(challengeList != null && !challengeList.isEmpty()){
				for(ChallengeConcept challenge: challengeList){
					String name = challenge.getName();
					String modelName = challenge.getModelName();
					long start = challenge.getStart().getTime();
					long end = challenge.getEnd().getTime();
					Boolean completed = challenge.isCompleted();
					long dateCompleted = challenge.getDateCompleted() != null ? challenge.getDateCompleted().getTime() : 0L;
					int bonusScore = 0;
					String periodName = "";
					String bonusPointType = "green leaves";
					String counterName = "";
					int target = 0;
					int periodTarget = 0;
					String badgeCollectionName = "";
					int baseline = 0;
					int initialBadgeNum = 0;
					if(challenge.getFields() != null){
						bonusScore = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_BONUS_SCORE, 0)).intValue();
						periodName = (String)challenge.getFields().getOrDefault(CHAL_FIELDS_PERIOD_NAME,"");// (!chalFields.isNull(CHAL_FIELDS_PERIOD_NAME)) ? chalFields.getString(CHAL_FIELDS_PERIOD_NAME) : "";
						bonusPointType = (String)challenge.getFields().getOrDefault(CHAL_FIELDS_BONUS_POINT_TYPE,"");// (!chalFields.isNull(CHAL_FIELDS_BONUS_POINT_TYPE)) ? chalFields.getString(CHAL_FIELDS_BONUS_POINT_TYPE) : "";
						counterName = (String)challenge.getFields().getOrDefault(CHAL_FIELDS_COUNTER_NAME,"");//(!chalFields.isNull(CHAL_FIELDS_COUNTER_NAME)) ? chalFields.getString(CHAL_FIELDS_COUNTER_NAME) : "";
						target =  ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_TARGET,0)).intValue(); ///(int)((!chalFields.isNull(CHAL_FIELDS_TARGET)) ? chalFields.getDouble(CHAL_FIELDS_TARGET) : 0);
						badgeCollectionName = (String)challenge.getFields().getOrDefault(CHAL_FIELDS_COUNTER_NAME,"");//(!chalFields.isNull(CHAL_FIELDS_BADGE_COLLECTION_NAME)) ? chalFields.getString(CHAL_FIELDS_BADGE_COLLECTION_NAME) : "";
						baseline = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_BASELINE,0)).intValue(); //(!chalFields.isNull(CHAL_FIELDS_BASELINE)) ? chalFields.getInt(CHAL_FIELDS_BASELINE) : 0;
						initialBadgeNum = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_INITIAL_BADGE_NUM,0)).intValue(); //(!chalFields.isNull(CHAL_FIELDS_INITIAL_BADGE_NUM)) ? chalFields.getInt(CHAL_FIELDS_INITIAL_BADGE_NUM) : 0;
						periodTarget = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_PERIOD_TARGET,0)).intValue(); ///(int)((!chalFields.isNull(CHAL_FIELDS_TARGET)) ? chalFields.getDouble(CHAL_FIELDS_TARGET) : 0);
					}
					ServerChallengesData challData = new ServerChallengesData();
					challData.setName(name);
					challData.setModelName(modelName);
					challData.setStart(start);
					challData.setEnd(end);
					challData.setCompleted(completed);
					challData.setDateCompleted(dateCompleted);
					challData.setBonusScore(bonusScore);
					challData.setPeriodName(periodName);
					challData.setBonusPointType(bonusPointType);
					challData.setCounterName(counterName);
					if(target == 0) target = 1;
					challData.setTarget(target);
					challData.setInitialBadgeNum(initialBadgeNum);
					challData.setBadgeCollectionName(badgeCollectionName);
					challData.setBaseline(baseline);
					
					// Convert data to old challenges models
//						final String ch_point_type = challData.getBonusPointType();
					final long now = System.currentTimeMillis();
					
	    			ChallengesData tmp_chall = new ChallengesData();
	    			tmp_chall.setChallId(challData.getName());

    				tmp_chall.setChallTarget(target);
    				tmp_chall.setType(challData.getModelName());
    				tmp_chall.setActive(now < challData.getEnd());
    				tmp_chall.setSuccess(challData.getCompleted());
    				tmp_chall.setStartDate(challData.getStart());
    				tmp_chall.setEndDate(challData.getEnd());
    				tmp_chall.setDaysToEnd(calculateRemainingDays(challData.getEnd(), now));
    				tmp_chall.setBonus(challData.getBonusScore());
    				tmp_chall.setChallCompletedDate(challData.getDateCompleted());
	    			
    				tmp_chall.setChallDesc(fillDescription(challenge, language));
    				
    				double row_status = 0D;
    				int status = 0;
    				
    				switch (tmp_chall.getType()) {
    					case CHAL_MODEL_REPETITIVE_BEAV:
		    				int successes = retrieveRepeatitiveStatusFromCounterName(counterName, periodName, pointConcept, challData.getStart(), challData.getEnd(), null, target); 
		    				row_status = round(successes, 2);
		    				status = Math.min(100, (int)(100.0 * successes / periodTarget));
	    					break;
	    				case CHAL_MODEL_PERCENTAGE_INC:
	    				case CHAL_MODEL_ABSOLUTE_INC: {
		    				int earned = retrieveCorrectStatusFromCounterName(counterName, periodName, pointConcept, challData.getStart(), challData.getEnd(), null); 
		    				row_status = round(earned, 2);
		    				status = Math.min(100, (int)(100.0 * earned / target));
	    					break;
	    				}
	    				case CHAL_MODEL_NEXT_BADGE: {
		    				int initialBadges = challData.getInitialBadgeNum();
		    				String badge_coll_name = challData.getBadgeCollectionName();
		    				int count = getEarnedBadgesFromList(bcc_list, badge_coll_name, initialBadges);
		    				if(!tmp_chall.getActive()){	// NB: fix to avoid situation with challenge not win and count > target
		    					if(tmp_chall.getSuccess()){
		    						count = target;
		    					} else {
		    						count = target - 1;
		    					}
		    				}
		    				row_status = round(count, 2);
		    				status = Math.min(100, (int)(100.0 * count / target));
		    				break;
	    				}
	    				case CHAL_MODEL_SURVEY: {
		    				if(tmp_chall.getSuccess()){
	    						row_status = 1; status = 100;
	    					}
		    				// survey link to be passed
		    				String link = utils.createSurveyUrl(playerId, gameId, (String)challenge.getFields().get("surveyType"), language);
		    				challenge.getFields().put("surveylink", link);
		    				break;
	    				}
	    				// boolean status: 100 or 0
	    				case CHAL_MODEL_COMPLETE_BADGE_COLL: 
	    				case CHAL_MODEL_POICHECKIN: 
	    				case CHAL_MODEL_CLASSPOSITION: 
	    				default: {
		    				if(tmp_chall.getSuccess()){
	    						row_status = 1;
	    						status = 100;
	    					}
	    				}
    				}
    				tmp_chall.setChallCompleteDesc(fillLongDescription(challenge, getFilterByType(tmp_chall.getType()), language));

    				tmp_chall.setStatus(status);
    				tmp_chall.setRow_status(row_status);
    				
	    			if(type == 0){
	    				if(now >= tmp_chall.getStartDate() - MILLIS_IN_DAY){	// if challenge is started (with one day of offset for mail)
			    			if(now < tmp_chall.getEndDate() - MILLIS_IN_DAY){	// if challenge is not ended
			    				challenges.add(tmp_chall);
			    			} else if(now < tmp_chall.getEndDate() + MILLIS_IN_DAY){	//CHAL_TS_OFFSET
			    				oldChallenges.add(tmp_chall);	// last week challenges
			    			}
		    			}
	    			} else {
			    		if(now < tmp_chall.getEndDate()){	// if challenge is not ended
			    			if(now >= tmp_chall.getStartDate()){
			    				challenges.add(tmp_chall);
			    			}
			    		} else if(now >= tmp_chall.getEndDate()){	//CHAL_TS_OFFSET
			    			oldChallenges.add(tmp_chall);	// last week challenges
			    		}
	    			}
				}
				challengesList.add(challenges);
    			challengesList.add(oldChallenges);
			}
    		// Sorting
        	/*Collections.sort(challenges, new Comparator<ChallengesData>() {
        	    public int compare(ChallengesData chalData2, ChallengesData chalData1){
        	        return  chalData2.getChallId().compareTo(chalData1.getChallId());
        	    }
        	});
        	Collections.sort(oldChallenges, new Comparator<ChallengesData>() {
        	    public int compare(ChallengesData chalData2, ChallengesData chalData1){
        	        return  chalData1.getChallId().compareTo(chalData2.getChallId());
        	    }
        	});*/
		}
    	return challengesList;
    }
	
	
	private String getFilterByType(String type) {
		switch(type) {
			case CHAL_MODEL_PERCENTAGE_INC:
			case CHAL_MODEL_ABSOLUTE_INC: {
				return "counterName";
			}
			case CHAL_MODEL_REPETITIVE_BEAV: {
				return "counterName";
			}
			case CHAL_MODEL_COMPLETE_BADGE_COLL:
			case CHAL_MODEL_NEXT_BADGE: {
				return "badgeCollectionName";
			}
			case CHAL_MODEL_POICHECKIN: {
				return "eventName";
			}
			case CHAL_MODEL_CLASSPOSITION: {
				return null;
			}
			case CHAL_MODEL_SURVEY: {
				return "surveyType";
			}
			case CHAL_MODEL_CHECKIN: {
				return "checkinType";
			}
			default: {
				return null;
			}
		
		}
	}
	
	// Method retrieveCorrectStatusFromCounterName: used to get the correct player status starting from counter name field
	private int retrieveCorrectStatusFromCounterName(String cName, String periodType, List<PointConcept> pointConcept, Long chalStart, Long chalEnd, Long now){
		int actualStatus = 0; // km or trips
		if(cName != null && cName.compareTo("") != 0){
			for(PointConcept pt : pointConcept){
				if(cName.equals(pt.getName()) && periodType.equals(pt.getPeriodType())){
					List<PointConceptPeriod> allPeriods = pt.getInstances();
					for(PointConceptPeriod pcp : allPeriods){
						if(chalStart != null && chalEnd != null){
							//if((pcp.getStart() - W_DELTA) <= chalStart && (pcp.getEnd() + W_DELTA) >= chalEnd){	// the week duration instance is major or equals the challenge duration 
							if(chalStart >= pcp.getStart() && chalStart < pcp.getEnd()){	// Now I check only using starting time
								actualStatus = pcp.getScore();
								break;
							}
						} else {
							if(now != null){
								if(pcp.getStart() <= now && pcp.getEnd() >= now){	// the actual time is contained in the week duration instance
									actualStatus = pcp.getScore();
									break;
								}
							}
						}
					}
					break;
				}
			}
		}
		return actualStatus;
	}
	
	
	
	private int retrieveRepeatitiveStatusFromCounterName(String cName, String periodType, List<PointConcept> pointConcept, Long chalStart, Long chalEnd, Long now, int target){
		int countSuccesses = 0; // km or trips
		if(cName != null && cName.compareTo("") != 0){
			for(PointConcept pt : pointConcept){
				if(cName.equals(pt.getName()) && periodType.equals(pt.getPeriodType())){
					List<PointConceptPeriod> allPeriods = pt.getInstances();
					for(PointConceptPeriod pcp : allPeriods){
						if(chalStart != null && chalEnd != null){
							if(chalStart <= pcp.getStart() && chalEnd >= pcp.getEnd()){	// Now I check only using starting time
								countSuccesses += pcp.getScore() >= target ? 1 : 0;
							}
						} else {
							if(now != null){
								if(pcp.getStart() <= now && pcp.getEnd() >= now){	// the actual time is contained in the week duration instance
									countSuccesses += pcp.getScore() >= target ? 1 : 0;
								}
							}
						}
					}
					break;
				}
			}
		}
		
		return countSuccesses;
	}	
	
	
	
	
	
	// Method getEarnedBadgesFromList: used to get the earned badge number during challenge
	private int getEarnedBadgesFromList(List<BadgeCollectionConcept> bcc_list, String badgeCollName, int initial){
		int earnedBadges = 0;
		for(BadgeCollectionConcept bcc : bcc_list){
			if(bcc.getName().compareTo(badgeCollName) == 0){
				earnedBadges = bcc.getBadgeEarned().size() - initial;
				break;
			}
		}
		return earnedBadges;
	}
	
	private int calculateRemainingDays(long endTime, long now){
    	int remainingDays = 0;
    	if(now < endTime){
    		long tmpMillis = endTime - now;
    		remainingDays = (int) Math.ceil((float)tmpMillis / MILLIS_IN_DAY);
    	}
    	return remainingDays;
    }
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    long factor = (long) Math.pow(10, places);
	    value = value * factor;
	    long tmp = Math.round(value);
	    return (double) tmp / factor;
	}
	
	public String fillDescription(ChallengeConcept challenge, String lang) {
		String filter = getFilterByType(challenge.getModelName());
		String description = null;
		String name = challenge.getModelName();
		String filterField = (String) challenge.getFields().get(filter);

		String counterNameA = null;
		String counterNameB = null;
		if (filterField != null) {
			if (CHAL_FIELDS_COUNTER_NAME.equals(filter)) {
				String counterNames[] = filterField.split("_");
				counterNameA = counterNames[0];
				if (counterNames.length == 2) {
					counterNameB = counterNames[1];

					if (counterNameA.startsWith("No")) {
						counterNameA = counterNameA.replace("No", "");
						counterNameB = "No" + counterNameB;
					}

				}
			}
		}

		ChallengeStructure challengeStructure = challengeStructureMap.getOrDefault(name + "#" + filterField, null);

		if (challengeStructure == null) {
			challengeStructure = challengeStructureMap.getOrDefault(name + (counterNameB != null ? ("#_" + counterNameB) : ""), null);
		}

		if (challengeStructure != null) {
//			System.err.println(challengeStructure);

			description = fillDescription(challengeStructure, counterNameA, counterNameB, challenge, lang);
			
			for (String key: challengeReplacements.keySet()) {
				description = description.replaceAll(key, challengeReplacements.get(key));
			}			
			
//			System.err.println("\t" + description);
//			System.err.println("________________________");
		} else {
			logger.error("Cannot find structure for challenge: '" + name + "', " + filter + "=" + filterField);
			return "";
		}


		
		return description;

		// String counterNames[] = ((String)challenge.getFields().get("counterName")).split("_");
		// if (counterNames != null && counterNames.length > 2) {
		// System.err.println("!!!");
		// }
		//
		// String counterNameA = null;
		// String counterNameB = null;
		// if (counterNames != null) {
		// counterNameA = counterNames[0];
		// if (counterNames.length == 2) {
		// counterNameB = counterNames[1];
		// }
		// }

	}

	private String fillLongDescription(ChallengeConcept challenge, String filterField, String lang) {
		String description = null;
		String name = challenge.getModelName();
		String counterName = filterField != null ? (String) challenge.getFields().get(filterField) : null;

		ChallengeLongDescrStructure challengeStructure = challengeLongStructureMap.getOrDefault(name + "#" + counterName, null);

		if (challengeStructure != null) {
			description = fillLongDescription(challengeStructure, counterName, challenge, lang);
			
			for (String key: challengeReplacements.keySet()) {
				description = description.replaceAll(key, challengeReplacements.get(key));
			}			
		} else {
			System.err.println(name + " / " + counterName);
			return "";
		}
		return description;
	}
	
	private String fillDescription(ChallengeStructure structure, String counterNameA, String counterNameB, ChallengeConcept challenge, String lang) {
		// VelocityContext context = new VelocityContext();
		// for (String field: challenge.getFields().keySet()) {
		// context.put(field, challenge.getFields().get(field));
		// }
		//
		// context.put("counterNameA", counterNameA);
		// context.put("counterNameB", counterNameB);
		//
		// Template template = engine.getTemplate(structure.getDescription());
		//
		// StringWriter sw = new StringWriter();
		//
		// template.merge(context, sw);
		//
		// sw.flush();
		// sw.close();
		//
		// return sw.getBuffer().toString();

		ST st = new ST(structure.getDescription().get(lang));

		boolean negative = counterNameB != null && counterNameB.startsWith("No");

		for (String field : challenge.getFields().keySet()) {
			Object o = challenge.getFields().get(field);
			st.add(field, o instanceof Number ? ((Number) o).intValue() : (o instanceof String ? instantiateWord(o.toString(), negative, lang) : o));
		}

		st.add("counterNameA", instantiateWord(counterNameA, negative, lang));
		st.add("counterNameB", instantiateWord(counterNameB, negative, lang));

		return st.render();
	}

	private String fillLongDescription(ChallengeLongDescrStructure structure, String counterName, ChallengeConcept challenge, String lang)  {
		ST st = new ST(structure.getDescription().get(lang));

		for (String field : challenge.getFields().keySet()) {
			Object o = challenge.getFields().get(field);
			st.add(field, o instanceof Number ? ((Number) o).intValue() : (o instanceof String ? instantiateWord(o.toString(), false, lang) : o));
		}

		return st.render();
	}
	private String instantiateWord(String word, boolean negative, String lang) {
		if (word != null) {
			List versions = challengeDictionaryMap.get(word.toLowerCase());
			if (versions != null) {
				Optional<Map> result = versions.stream().filter(x -> negative == (Boolean) ((Map) x).get("negative")).findFirst();
				if (result.isPresent()) {
					return (String)((Map)((Map) result.get()).get("word")).get(lang);
				}
			}
		}
		return word;
	}
	
}
