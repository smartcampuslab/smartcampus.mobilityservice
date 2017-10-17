/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package eu.trentorise.smartcampus.mobility.gamification;

import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import eu.trentorise.smartcampus.mobility.controller.rest.GamificationController;
import eu.trentorise.smartcampus.mobility.gamification.model.ExecutionDataDTO;
import eu.trentorise.smartcampus.mobility.gamification.model.MessageNotification;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.model.BasicItinerary;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.BannedChecker;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;
import eu.trentorise.smartcampus.mobility.util.HTTPConnector;
import eu.trentorise.smartcampus.network.JsonUtils;

@Component
public class GamificationManager {

	private static final Logger logger = LoggerFactory.getLogger(GamificationManager.class);

	private static final String SAVE_ITINERARY = "save_itinerary";
	
	@Autowired
	private AppSetup appSetup;
	
	@Autowired
	private GameSetup gameSetup;	
	
	@Autowired
	private PlayerRepositoryDao playerRepositoryDao;	

	@Autowired
	private ExecutorService executorService;	
	
	@Autowired
	private BannedChecker bannedChecker;

	@Autowired(required = false)
	@Value("${gamification.url}")
	private String gamificationUrl;
	
	private Set<String> publishQueue = Sets.newConcurrentHashSet();
	
	public synchronized boolean sendFreeTrackingDataToGamificationEngine(String appId, String playerId, String travelId, Collection<Geolocation> geolocationEvents, String ttype, Map<String, Object> trackingData) {
		logger.info("Send free tracking data for user " + playerId + ", trip " + travelId);
		if (publishQueue.contains(travelId)) {
			logger.debug("publishQueue contains travelId " + travelId + ", returning");
			return false;
		}
		publishQueue.add(travelId);
		saveFreeTracking(travelId, appId, playerId, geolocationEvents, ttype, trackingData);
		return true;
	}
	
	public synchronized boolean sendIntineraryDataToGamificationEngine(String appId, String playerId, String publishKey, ItineraryObject itinerary, Map<String, Object> trackingData) throws Exception {
		logger.info("Send data for user " + playerId + ", trip " + itinerary.getClientId());
		if (publishQueue.contains(publishKey)) {
			return false;
		}
		publishQueue.add(publishKey);
		saveItinerary(itinerary, appId, playerId, trackingData);
		return true;
	}

	private void saveFreetracking(String travelId, String appId, String playerId, Collection<Geolocation> geolocationEvents, String ttype, Map<String, Object> trackingData) {
		if ((Long)trackingData.get("estimatedScore") == 0) {
			logger.debug("EstimatedScore is 0, returning.");
			return;
		}
		trackingData.remove("estimatedScore");
		
		if (trackingData.isEmpty()) {
			logger.debug("Data is empty, returning.");
			return;
		}
		
		AppInfo app = appSetup.findAppById(appId);
		GameInfo game = gameSetup.findGameById(app.getGameId());
		
		if (bannedChecker.isBanned(playerId, app.getGameId())) {
			logger.info("Not sending for banned player " + playerId);
			return;
		}

		try {
			ExecutionDataDTO ed = new ExecutionDataDTO();
			ed.setGameId(app.getGameId());
			ed.setPlayerId(playerId);
			ed.setActionId(SAVE_ITINERARY);
			ed.setData(trackingData);
			
			Long time = (Long)trackingData.remove(GamificationController.START_TIME);
			ed.setExecutionMoment(new Date(time));			

			String content = JsonUtils.toJSON(ed);
			
			logger.debug("Sending to " + gamificationUrl + "/gengine/execute (" + SAVE_ITINERARY + ") = " + trackingData);
			HTTPConnector.doAuthenticatedPost(gamificationUrl + "/gengine/execute", content, "application/json", "application/json", game.getUser(), game.getPassword());		
		} catch (Exception e) {
			logger.error("Error sending gamification action: " + e.getMessage());
		}
	}

	/**
	 * @param travelId
	 * @param appId
	 * @param playerId
	 * @param geolocationEvents
	 */
	private void saveFreeTracking(final String travelId, final String appId, final String playerId, final Collection<Geolocation> geolocationEvents, final String ttype, Map<String, Object> trackingData) {
		if (gamificationUrl == null) {
			logger.debug("No gamification URL, returning.");
			return;
		}
		
		AppInfo app = appSetup.findAppById(appId);
		GameInfo game = gameSetup.findGameById(app.getGameId());
		
		try {
			if (System.currentTimeMillis() < new SimpleDateFormat("dd/MM/yyyy").parse(game.getStart()).getTime()) {
				logger.debug("Game not yet started, returning.");
				return;
			}
		} catch (ParseException e) {
			return;
		}

		executorService.execute(new Runnable() {
			@Override
			public void run() {
				saveFreetracking(travelId, appId, playerId, geolocationEvents, ttype, trackingData);
			}

		});
	}
	
	private void saveItinerary(final BasicItinerary itinerary, final String appId, final String userId, Map<String, Object> trackingData) throws ParseException {
		if (gamificationUrl == null) {
			return;
		}
		
		AppInfo app = appSetup.findAppById(appId);
		GameInfo game = gameSetup.findGameById(app.getGameId());
		
		
		if (System.currentTimeMillis() < new SimpleDateFormat("dd/MM/yyyy").parse(game.getStart()).getTime()) {
			return;
		}

		executorService.execute(new Runnable() {
			@Override
			public void run() {
				saveTrip(itinerary, appId, userId, trackingData);
			}
		});
	}

	private void saveTrip(BasicItinerary itinerary, String appId, String userId, Map<String, Object> trackingData) {
		try {
//			Map<String, Object> data = validator.computePlannedJourneyScore(itinerary.getData(), true);
			trackingData.remove("estimatedScore");

			AppInfo app = appSetup.findAppById(appId);
			GameInfo game = gameSetup.findGameById(app.getGameId());
			
			if (bannedChecker.isBanned(userId, app.getGameId())) {
				logger.warn("Not sending for banned player " + userId);
				return;
			}		
			
			ExecutionDataDTO ed = new ExecutionDataDTO();
			ed.setGameId(app.getGameId());
			ed.setPlayerId(userId);
			ed.setActionId(SAVE_ITINERARY);
			ed.setData(trackingData);
			
			Long time = (Long)trackingData.remove(GamificationController.START_TIME);
			ed.setExecutionMoment(new Date(time));

			String content = JsonUtils.toJSON(ed);
			
			logger.debug("Sending to " + gamificationUrl + "/gengine/execute (" + SAVE_ITINERARY + ") = " + trackingData);
			HTTPConnector.doAuthenticatedPost(gamificationUrl + "/gengine/execute", content, "application/json", "application/json", game.getUser(), game.getPassword());
		} catch (Exception e) {
			logger.error("Error sending gamification action: " + e.getMessage());
		}
	}	

	public Map<String, Double> getScoreNotification(String appId, String userId) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		
		AppInfo app = appSetup.findAppById(appId);
		GameInfo game = gameSetup.findGameById(app.getGameId());
		
//		logger.info("Get score notifications for " + userId);
		
		String url = gamificationUrl + "/notification/game/" + app.getGameId() + "/player/" + userId + "?includeTypes=MessageNotification&size=10000";
		
		RestTemplate restTemplate = new RestTemplate();
		
		ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)), String.class);		
		
		List nots = mapper.readValue(res.getBody(), List.class);
		
		Map<String, Double> result = Maps.newTreeMap();
		for (Object not: nots) {
			MessageNotification msg = mapper.convertValue(not, MessageNotification.class);
			Map data = msg.getData();
			result.put((String)data.get("travelId"), (Double)data.get("score"));
		}		
		
//		logger.info("Got scores: " + result);
		
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

	/**
	 * @param event
	 * @param id
	 * @param gameId
	 * @throws Exception 
	 */
	public void sendCheckin(String event, String id, String appId) throws Exception {
		AppInfo app = appSetup.findAppById(appId);
		GameInfo game = gameSetup.findGameById(app.getGameId());

		ExecutionDataDTO ed = new ExecutionDataDTO();
		ed.setGameId(game.getId());
		ed.setPlayerId(id);
		ed.setActionId("checkin_"+event);
		ed.setData(Collections.singletonMap("checkinType",event));

		String content = JsonUtils.toJSON(ed);
		
		logger.debug("Sending to " + gamificationUrl + "/gengine/execute ('checkin') = " + content);
		HTTPConnector.doAuthenticatedPost(gamificationUrl + "/gengine/execute", content, "application/json", "application/json", game.getUser(), game.getPassword());
		
	}	
	
}
