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

package eu.trentorise.smartcampus.mobility.util;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.TType;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

import eu.trentorise.smartcampus.mobility.gamification.model.ExecutionDataDTO;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.model.BasicItinerary;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.network.JsonUtils;
import eu.trentorise.smartcampus.network.RemoteConnector;
import eu.trentorise.smartcampus.network.RemoteException;

/**
 * @author raman
 *
 */
@Component
public class GamificationHelper {

	private static final double SPACE_ERROR = 1E-1;
	private static final double TIME_ERROR = 1000 * 60 * 15;

	private static final String SAVE_ITINERARY = "save_itinerary";

	private static final Logger logger = LoggerFactory.getLogger(GamificationHelper.class);
	
	private static long startGameDate = Long.MAX_VALUE;
	
	@Autowired(required=false)
	@Value("${gamification.url}")
	private String gamificationUrl;

	@Autowired(required=false)
	@Value("${gamification.startgame}")
	private String gameStart;
	
	@Value("${gamification.user}")
	private String user;
	
	@Value("${gamification.password}")
	private String password;	

	@Autowired
	private ExecutorService executorService;
	
	private final static int EARTH_RADIUS = 6371; // Earth radius in km.
	
	@PostConstruct
	public void initConnector() {
		if (StringUtils.hasText(gameStart)) {
			try {
				startGameDate = new SimpleDateFormat("dd/MM/yyyy").parse(gameStart).getTime();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void saveItinerary(final BasicItinerary itinerary, final String gameId, final String userId) {
		if (gamificationUrl == null) return;
		if (System.currentTimeMillis() < startGameDate) return;
		
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				saveTrip(itinerary, gameId, userId);
			}
		});
	}
	
	private void saveTrip(BasicItinerary itinerary, String gameId, String userId) {
		try {
			Map<String,Object> data = computeTripData(itinerary.getData(), true);
			data.remove("estimatedScore");
			
			ExecutionDataDTO ed = new ExecutionDataDTO();
			ed.setGameId(gameId);
			ed.setPlayerId(userId);
			ed.setActionId(SAVE_ITINERARY);			
			ed.setData(data);
			
			String content = JsonUtils.toJSON(ed);
			
			logger.info("Sending to " + gamificationUrl + "/gengine/execute (" + SAVE_ITINERARY +") = " + data);
			HTTPConnector.doAuthenticatedPost(gamificationUrl + "/gengine/execute", content, "application/json", "application/json", user, password);
		} catch (Exception e) {
			logger.error("Error sending gamification action: " + e.getMessage());
		}
	}
	
	private Map<String,Object> computeTripData(Itinerary itinerary, boolean log) {
		Map<String,Object> data = Maps.newTreeMap();
		
		String parkName = null; // name of the parking 
		String startBikesharingName = null; // name of starting bike sharing station
		String endBikesharingName = null; // name of ending bike sharing station
		boolean pnr = false; // (park-n-ride)
		boolean bikeSharing = false;
		double bikeDist = 0; // km
		double walkDist = 0; // km
		double trainDist = 0; // km
		double busDist = 0; // km
		double carDist = 0; // km
		double transitDist = 0;
		
		logger.info("Analyzing itinerary for gamification.");
		if (itinerary != null) {
			for (Leg leg : itinerary.getLeg()) {
				if (leg.getTransport().getType().equals(TType.CAR)) {
					carDist += leg.getLength() / 1000;
					if (leg.getTo().getStopId() != null) {
						pnr = true;
						parkName = leg.getTo().getStopId().getId();
					}						
				} else  if (leg.getTransport().getType().equals(TType.BICYCLE)) {
					bikeDist += leg.getLength() / 1000;
					if (leg.getFrom().getStopId() != null) {
						bikeSharing = true;
						startBikesharingName = leg.getFrom().getStopId().getId();
					}						
					if (leg.getTo().getStopId() != null) {
						bikeSharing = true;
						endBikesharingName = leg.getTo().getStopId().getId();
					}						
				} else if (leg.getTransport().getType().equals(TType.WALK)) {
					walkDist += leg.getLength() / 1000;
				} else if (leg.getTransport().getType().equals(TType.TRAIN)) {
					trainDist += leg.getLength() / 1000;
				} else if (leg.getTransport().getType().equals(TType.BUS)) {
					busDist += leg.getLength() / 1000;
				} else if (leg.getTransport().getType().equals(TType.TRANSIT)) {
					transitDist += leg.getLength() / 1000;
				}
			}
		}
		
		if (log) {
			logger.info("Analysis results:");
			logger.info("Distances [walk = " +walkDist + ", bike = "  + bikeDist +", train = " + trainDist + ", bus = " + busDist + ", car = " + carDist + "]");
			logger.info("Park and ride = " + pnr + " , Bikesharing = " + bikeSharing);
			logger.info("Park = " + parkName);
			logger.info("Bikesharing = " + startBikesharingName + " / " + endBikesharingName);
		}
		
		Double score = 0.0;
		score += (walkDist< 0.1 ? 0 : Math.min(5, walkDist)) * 10;
		score += (bikeDist< 0.1 ? 0 : Math.min(10, bikeDist)) * 5;
		
		double busTrainDist = busDist + trainDist;
		if (busTrainDist> 0) {
			score += ((busTrainDist > 0 && busTrainDist < 5) ? 10 : (busTrainDist >= 5 && busTrainDist < 10) ? 20 : (busTrainDist >= 10 && busTrainDist < 30) ? 30 : 40);
		}
		
		score *= (busDist + carDist + trainDist + transitDist == 0 && walkDist + bikeDist > 0) ? 2 : 1; // zero impact
		score += (itinerary.isPromoted() ? 10 : 0);
		
		if (bikeDist > 0) {
			data.put("bikeDistance", bikeDist);
		}
		if (walkDist > 0) {
			data.put("walkDistance", walkDist);
		}
		if (busDist > 0) {
			data.put("busDistance", busDist);
		}
		if (trainDist > 0) {
			data.put("trainDistance", trainDist);
		}
		if (carDist > 0) {
			data.put("carDistance", carDist);
		}
		if (bikeSharing) {
			data.put("bikesharing", bikeSharing);
		}
		if (parkName != null) {
			data.put("park", parkName);
		}
		if (startBikesharingName != null) {
			data.put("startBike", startBikesharingName);
		}
		if (endBikesharingName != null) {
			data.put("endBike", endBikesharingName);
		}
		if (pnr) {
			data.put("p+r", pnr);
		}
		data.put("sustainable", itinerary.isPromoted());	
		data.put("estimatedScore", Math.round(score));
		
		return data;
	}
	
	public void computeEstimatedGameScore(Itinerary itinerary, boolean log) {
		Long score =  (Long)(computeTripData(itinerary, log).get("estimatedScore"));
		itinerary.getCustomData().put("estimatedScore", score);
	}	
	
	public static boolean checkItineraryCompletion(ItineraryObject itinerary, Collection<Geolocation> geolocations) throws Exception {
		if (itinerary == null) {
			return false;
		}
		if (geolocations.size() > 1) {
			boolean started = false;
			boolean ended = false;
			
			double fromLat =  Double.parseDouble(itinerary.getData().getFrom().getLat());
			double fromLon =  Double.parseDouble(itinerary.getData().getFrom().getLon());
			double toLat = Double.parseDouble(itinerary.getData().getTo().getLat());
			double toLon = Double.parseDouble(itinerary.getData().getTo().getLon());				
			long startTime = itinerary.getData().getStartime();
			long endTime = itinerary.getData().getEndtime();
			
			for (Geolocation geolocation: geolocations) {
				double lat = geolocation.getLatitude();
				double lon = geolocation.getLongitude();
				long time = geolocation.getCreated_at().getTime();
				if (!started) {
					double fromD = harvesineDistance(lat, lon, fromLat, fromLon);
					long fromT = Math.abs(time - startTime);
					started = fromD <= SPACE_ERROR & fromT <= TIME_ERROR;
				}
				if (!ended) {
					double toD = harvesineDistance(geolocation.getLatitude(), geolocation.getLongitude(), toLat, toLon);
					long toT = Math.abs(time - endTime);
					ended = toD <= SPACE_ERROR & toT <= TIME_ERROR;
				}
				if (started && ended) {
					return true;
				}
			}
		} 
		
		return false;	
	}	
	
	
	public static boolean checkItineraryMatching(ItineraryObject itinerary, List<Geolocation> geolocations) throws Exception {
		if (geolocations.size() > 1) {
			
			List<Geolocation> positions = Lists.newArrayList();
			List<Geolocation> matchedPositions = Lists.newArrayList();
			for (Leg leg: itinerary.getData().getLeg()) {
				Geolocation onLeg = new Geolocation();
				onLeg.setLatitude(Double.parseDouble(leg.getFrom().getLat()));
				onLeg.setLongitude(Double.parseDouble(leg.getFrom().getLon()));
				onLeg.setCreated_at(new Date(leg.getStartime()));
				positions.add(onLeg);
			}
			Leg lastLeg = itinerary.getData().getLeg().get(itinerary.getData().getLeg().size() - 1);
			Geolocation onLeg = new Geolocation();
			onLeg.setLatitude(Double.parseDouble(lastLeg.getFrom().getLat()));
			onLeg.setLongitude(Double.parseDouble(lastLeg.getFrom().getLon()));
			onLeg.setCreated_at(new Date(lastLeg.getEndtime()));
			positions.add(onLeg);			
			
			
			for (Geolocation geolocation: geolocations) {
				double lat = geolocation.getLatitude();
				double lon = geolocation.getLongitude();

				Geolocation toRemove = null;
				for (Geolocation pos: positions) {
					double d = harvesineDistance(lat, lon, pos.getLatitude(), pos.getLongitude());
					double t = Math.abs(pos.getCreated_at().getTime() - geolocation.getCreated_at().getTime());
					if (d <= SPACE_ERROR && t <= TIME_ERROR) {
						toRemove = pos;
						break;
					}
				}
				if (toRemove != null) {
					positions.remove(toRemove);
					matchedPositions.add(toRemove);
				}
				
			}
			
			System.out.println(positions.size() + " / " + matchedPositions.size());
			
		} 
		

		
		return false;	
	}	
	
	
	private static double harvesineDistance(double lat1, double lon1, double lat2, double lon2) {
		lat1 = Math.toRadians(lat1);
	    lon1 = Math.toRadians(lon1);
	    lat2 = Math.toRadians(lat2);
	    lon2 = Math.toRadians(lon2);

	    double dlon = lon2 - lon1;
	    double dlat = lat2 - lat1;

		double a = Math.pow((Math.sin(dlat / 2)), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return EARTH_RADIUS * c;
	}		
	
	
	public static void main(String[] args) throws UnknownHostException, MongoException, SecurityException, RemoteException {
		MongoTemplate mg = new MongoTemplate(new Mongo("127.0.0.1", 37017), "mobility-logging");
		List<Map> findAll = mg.findAll(Map.class, "forgamification");
		for (Map m : findAll) {
			m.remove("_id");
			RemoteConnector.postJSON("http://localhost:8900", "/execute", JsonUtils.toJSON(m), null);
		}
	}
}
