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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.mobility.sync.BasicItinerary;
import eu.trentorise.smartcampus.network.JsonUtils;
import eu.trentorise.smartcampus.network.RemoteConnector;

/**
 * @author raman
 *
 */
@Component
public class GamificationHelper {

	private static final Logger logger = LoggerFactory.getLogger(GamificationHelper.class);
	
	private static long START_GAME_DATE = Long.MAX_VALUE;
//	static {
//		try {
//			START_GAME_DATE = new SimpleDateFormat("dd/MM/yyyy").parse("24/11/2014").getTime();
//		} catch (ParseException e) {
//			e.printStackTrace();
//		}
//	}
	
	@Autowired
	@Value("${gamification.url}")
	private String gamificationUrl;

	@Autowired
	private ExecutorService executorService;
	
	public void saveItinerary(final BasicItinerary itinerary, final String userId) {
		if (System.currentTimeMillis() < START_GAME_DATE) return;
		
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				saveTrip(itinerary, userId);
			}
		});
	}
	
	private void saveTrip(BasicItinerary itinerary, String userId) {
		try {
			String actionId = "save_itinerary";
			Map<String,Object> data = new HashMap<String, Object>();
			String parkName = null; // name of the parking 
			boolean pnr = false; // (park-n-ride)
			boolean bikeSharing = false;
			double bikeDist = 0; // km
			double walkDist = 0; // km
			double trainDist = 0; // km
			double busDist = 0; // km
			double carDist = 0; // km
			
			Itinerary it = itinerary.getData();
			logger.info("Analyzing itinerary for gamification.");
			if (it != null) {
				for (Leg leg : it.getLeg()) {
					if (leg.getTransport().getType().equals(TType.CAR)) {
						carDist += leg.getLength() / 1000;
						if (leg.getTo().getStopId() != null) {
							pnr = true;
							parkName = leg.getTo().getStopId().getId();
						}						
					}					
					if (leg.getTransport().getType().equals(TType.BICYCLE)) {
						bikeDist += leg.getLength() / 1000;
						if (leg.getTo().getStopId() != null) {
							bikeSharing = true;
						}						
					}
					if (leg.getTransport().getType().equals(TType.WALK)) {
						walkDist += leg.getLength() / 1000;
					}
					if (leg.getTransport().getType().equals(TType.TRAIN)) {
						trainDist += leg.getLength() / 1000;
					}
					if (leg.getTransport().getType().equals(TType.BUS)) {
						busDist += leg.getLength() / 1000;
					}
				}
			}
			logger.info("Analysis results:");
			logger.info("Distances [walk = " +walkDist + ", bike = "  + bikeDist +", train = " + trainDist + ", bus = " + busDist + ", car = " + carDist + "]");
			logger.info("Park and ride = " + pnr + " , Bikesharing = " + bikeSharing);
			logger.info("Park = " + parkName);
			
			
			if (bikeDist > 0) data.put("bikeDistance", bikeDist);
			if (walkDist > 0) data.put("walkDistance", walkDist);
			if (busDist > 0) data.put("busDistance", busDist);
			if (trainDist > 0) data.put("trainDistance", trainDist);
			if (carDist > 0) data.put("carDistance", carDist);
			if (bikeSharing) data.put("bikesharing", bikeSharing);
			if (parkName != null) data.put("park", parkName);
			if (pnr) data.put("p+r", pnr);
			data.put("sustainable", itinerary.getData().isPromoted());
			
			Map<String,Object> body = new HashMap<String, Object>();
			body.put("actionId", actionId);
			body.put("userId", userId);
			body.put("data", data);
			RemoteConnector.postJSON(gamificationUrl, "/execute", JsonUtils.toJSON(body), null);
		} catch (Exception e) {
			logger.error("Error sending gamification action: "+e.getMessage());
		}
	}
}
