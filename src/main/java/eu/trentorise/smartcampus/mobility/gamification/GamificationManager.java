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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import eu.trentorise.smartcampus.mobility.gamification.model.ExecutionDataDTO;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.model.BasicItinerary;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
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
	private ExecutorService executorService;		

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

		try {
			ExecutionDataDTO ed = new ExecutionDataDTO();
			ed.setGameId(app.getGameId());
			ed.setPlayerId(playerId);
			ed.setActionId(SAVE_ITINERARY);
			ed.setData(trackingData);

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
			
			ExecutionDataDTO ed = new ExecutionDataDTO();
			ed.setGameId(app.getGameId());
			ed.setPlayerId(userId);
			ed.setActionId(SAVE_ITINERARY);
			ed.setData(trackingData);

			String content = JsonUtils.toJSON(ed);
			
			logger.debug("Sending to " + gamificationUrl + "/gengine/execute (" + SAVE_ITINERARY + ") = " + trackingData);
			HTTPConnector.doAuthenticatedPost(gamificationUrl + "/gengine/execute", content, "application/json", "application/json", game.getUser(), game.getPassword());
		} catch (Exception e) {
			logger.error("Error sending gamification action: " + e.getMessage());
		}
	}	

	

}
