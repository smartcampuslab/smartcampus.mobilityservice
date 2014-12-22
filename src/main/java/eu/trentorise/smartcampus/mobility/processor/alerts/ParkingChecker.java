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
package eu.trentorise.smartcampus.mobility.processor.alerts;

import it.sayservice.platform.smartplanner.data.message.StopId;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertType;
import it.sayservice.platform.smartplanner.data.message.alerts.CreatorType;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eu.trentorise.smartcampus.mobility.processor.model.Parking;

public class ParkingChecker {

	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

	private static Log logger = LogFactory.getLog(ParkingChecker.class);

	public static AlertParking checkParking(Parking parking) {
		AlertParking places = new AlertParking();
		places.setCreatorId("");
		places.setCreatorType(CreatorType.SERVICE);
		places.setNoOfvehicles(parking.getVehicles());
		places.setPlacesAvailable(parking.getFreePlaces());
		places.setDescription("");
		places.setNote("");
		
		StopId t = new StopId();
		
		t.setAgencyId(parking.getAgencyId());
		t.setId(parking.getId());

		places.setPlace(t);
		places.setType(AlertType.DELAY);
		
		places.setFrom(System.currentTimeMillis());
		places.setTo(System.currentTimeMillis() + 1000 * 60 * 5);

		places.setNote(parking.getAddress());

		places.setId(places.getPlace().getId() + "_" + CreatorType.SERVICE + "_" + places.getFrom() + "_" + places.getTo());

		return places;

	}

	public static AlertsSent checkNewAlerts(AlertsSent sent, Parking parking) {
		AlertsSent newSent = new AlertsSent(sent);

		String places = buildDate() + "_" + parking.getFreePlaces();
		
		if (!sent.getParkings().containsKey(parking.getId().replace(".",""))) {
			newSent.getParkings().put(parking.getId().replace(".",""), places);
			return newSent;
		}

		if (sent.getParkings().get(parking.getId().replace(".","")).equals(places)) {
			return null;
		}

		newSent.getParkings().put(parking.getId().replace(".",""), places);
		
		
		
		return newSent;
	}

	public static AlertsSent cleanOldAlerts(AlertsSent sent) {
		AlertsSent newSent = new AlertsSent(sent);
		newSent.setParkings(new TreeMap<String, String>(sent.getParkings()));

		String delay = buildDate();
		for (String key : sent.getParkings().keySet()) {
			if (!sent.getParkings().get(key).startsWith(delay)) {
				newSent.getParkings().remove(key);
			}
		}
		return newSent;
	}

	private static String buildDate() {
		Calendar cal = new GregorianCalendar();
		return cal.get(Calendar.DAY_OF_MONTH) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.YEAR);
	}

}
