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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.alerts.Alert;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertStrike;

public class AlertUpdater {
	
	static Logger log = LoggerFactory.getLogger(AlertUpdater.class);

	public static RecurrentAlertsSent updateAlerts(Alert alert, RecurrentAlertsSent alerts) {
		if (alerts == null) {
			alerts = new RecurrentAlertsSent();
		}
		String id = AlertFilter.buildId(alert);
		Long value = null;
		if (alert instanceof AlertDelay) {
			value = ((AlertDelay) alert).getDelay();
		} else if (alert instanceof AlertParking) {
			value = (long)((AlertParking) alert).getPlacesAvailable();
		}
		alerts.add(id, value, alert.getTo());

		return alerts;
	}
	
	public static Itinerary updateAlerts(Itinerary itinerary, Alert alert) {
		try {
		for (Leg leg : itinerary.getLeg()) {
			Alert old = null;
			if (alert instanceof AlertDelay) {
				if (!AlertFilter.areEqual(leg.getTransport(), ((AlertDelay) alert).getTransport(), true, false, true, true)) {
					continue;
				}
				if (leg.getAlertDelayList() == null) {
					leg.setAlertDelayList(new ArrayList<AlertDelay>());
				}
				for (AlertDelay alertDelay : leg.getAlertDelayList()) {
					if (AlertFilter.areEqual(alertDelay.getTransport(), ((AlertDelay) alert).getTransport(), true, false, true, true)) {
						old = alertDelay;
						break;
					}
				}
				if (old != null) {
					leg.getAlertDelayList().remove(old);
				}
				leg.getAlertDelayList().add((AlertDelay) alert);
			
			} else if (alert instanceof AlertStrike) {
				if (!AlertFilter.areEqual(leg.getTransport(), ((AlertStrike) alert).getTransport(), true, false, true, true)) {
					continue;
				}
				if (leg.getAlertStrikeList() == null) {
					leg.setAlertStrikeList(new ArrayList<AlertStrike>());
				}
				for (AlertStrike alertStrike : leg.getAlertStrikeList()) {
					if (AlertFilter.areEqual(alertStrike.getTransport(), ((AlertStrike) alert).getTransport(), true, false, true, true)) {
						old = alertStrike;
						break;
					}
				}
				if (old != null) {
					leg.getAlertStrikeList().remove(old);
				}
				leg.getAlertStrikeList().add((AlertStrike) alert);
			
			} else if (alert instanceof AlertParking) {
				if (!leg.getTo().getStopId().equals(((AlertParking)alert).getPlace())) {
					continue;
				}
				if (leg.getAlertParkingList() == null) {
					leg.setAlertParkingList(new ArrayList<AlertParking>());
				}				
				for (AlertParking alertParking : leg.getAlertParkingList()) {
					if (alertParking.getPlace().equals(((AlertParking) alert).getPlace())) {
						old = alertParking;
						break;
					}
				}
				if (old != null) {
					leg.getAlertParkingList().remove(old);
				}
				leg.getAlertParkingList().add((AlertParking) alert);
			}
		}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Cannot update itinerary");
		}

		return itinerary;
	}
	
	public static Itinerary removeExpired(Itinerary itinerary) {
		for (Leg leg : itinerary.getLeg()) {
			List<Alert> toRemove = new ArrayList<Alert>();
			for (Alert alert :leg.getAlertDelayList()) {
				if (AlertUpdater.isExpired(alert)) {
					toRemove.add(alert);
				}
				leg.getAlertDelayList().removeAll(toRemove);
			}
			toRemove = new ArrayList<Alert>();
			for (Alert alert :leg.getAlertParkingList()) {
				if (AlertUpdater.isExpired(alert)) {
					toRemove.add(alert);
				}
				leg.getAlertParkingList().removeAll(toRemove);		
			}
			toRemove = new ArrayList<Alert>();
			for (Alert alert :leg.getAlertStrikeList()) {
				if (AlertUpdater.isExpired(alert)) {
					toRemove.add(alert);
				}
				leg.getAlertStrikeList().removeAll(toRemove);		
			}			
		}
		
		return itinerary;
	}
	
	private static boolean isExpired(Alert alert) {
		long now = System.currentTimeMillis();
		return alert.getTo() < now;
	}
	

}
