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

package eu.trentorise.smartcampus.mobility.processor.handlers;

import it.sayservice.platform.smartplanner.data.message.StopId;
import it.sayservice.platform.smartplanner.data.message.otpbeans.BikeStation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import eu.trentorise.smartcampus.mobility.model.Station;
import eu.trentorise.smartcampus.mobility.service.AlertSender;
import eu.trentorise.smartcampus.mobility.service.SmartPlannerHelper;

/**
 * @author raman
 *
 */
@Component
public class BikeSharingCache{

	private static final String BIKE_RENTAL = "BIKE-RENTAL";
	
	private Map<String, List<Station>> stationsMap = new HashMap<String, List<Station>>();
	
	private Set<String> existingStations;
	
	@Autowired
	private SmartPlannerHelper smartPlannerHelper;
	
	private static Logger logger = LoggerFactory.getLogger(AlertSender.class);
	
	
	@PostConstruct
	public void initStations() throws Exception {
		stationsMap = Maps.newTreeMap();
		existingStations = Sets.newHashSet();
		ObjectMapper mapper = new ObjectMapper();
		String bs = smartPlannerHelper.bikeStations();
		List bsl = mapper.readValue(bs, List.class);
		for (Object o : bsl) {
			Map<String, Object> station = mapper.convertValue(o, Map.class);
			String idParts[] = ((String)station.get("id")).split("@");
			existingStations.add(idParts[0]);
		}		
	}

	public void setStations(String comune, String agencyId, List<Station> stations) throws Exception {
		List<BikeStation> toAdd = Lists.newArrayList();
		List<String> toAddNames = Lists.newArrayList();		
		for (Station station : stations) {
			String name = station.getName();
			String id = station.getId();
			if (!existingStations.isEmpty() && !existingStations.contains(id)) {
				logger.warn("Bike station not found: " + id);
				BikeStation bikeStation = new BikeStation();
				StopId stop = new StopId((String) agencyId, id);
				bikeStation.setStationId(stop);
				bikeStation.setAvailableSharingVehicles(station.getBikes());
				bikeStation.setPosts(station.getSlots());
				bikeStation.setFullName(station.getAddress());
				bikeStation.setPosition(new double[] { station.getPosition()[0], station.getPosition()[1] });
				bikeStation.setType(BIKE_RENTAL);
				toAdd.add(bikeStation);
				toAddNames.add(id);
			}
		}
		
		stationsMap.put(comune, stations);
		
		if (!toAdd.isEmpty()) {
			System.err.println("Adding " + toAddNames);
			smartPlannerHelper.addBikeSharingStations(toAdd);
			existingStations.addAll(toAddNames);
		}		
		
	}
	
	public List<Station> getStations(String comune) {
		if (stationsMap.containsKey(comune)) {
			return stationsMap.get(comune);
		}
		return Collections.emptyList();
	}
	
}
