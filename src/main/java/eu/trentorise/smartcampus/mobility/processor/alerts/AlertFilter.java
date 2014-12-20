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

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.StopId;
import it.sayservice.platform.smartplanner.data.message.Transport;
import it.sayservice.platform.smartplanner.data.message.alerts.Alert;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertAccident;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoad;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertStrike;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourney;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.log4j.Logger;

public class AlertFilter {
	
	private static final long THRESHOLD = 1000 * 60 * 5;
	static Logger log = Logger.getLogger(AlertFilter.class);

	public static boolean filterDelay(Itinerary itinerary, AlertDelay alert) {
		if (itinerary == null) {
			return false;
		}
		return filterDelay(itinerary.getLeg(), alert);
	}

	public static boolean filterStrike(Itinerary itinerary, AlertStrike alert) {
		if (itinerary == null) return false;
		return filterStrike(itinerary.getLeg(), alert);
	}

	public static boolean filterParking(Itinerary itinerary, AlertParking alert) {
		if (itinerary == null) {
			return false;
		}
		return filterParking(itinerary.getLeg(), alert);
	}

	public static boolean filterAccident(Itinerary itinerary, AlertAccident alert) {
		if (itinerary == null) return false;
		return filterAccident(itinerary.getLeg(), alert);
	}

	public static boolean filterRoad(Itinerary itinerary, AlertRoad alert) {
		if (itinerary == null) return false;
		return filterRoad(itinerary.getLeg(), alert);
	}

	public static boolean filterDelay(RecurrentJourney journey, AlertDelay alert, RecurrentAlertsSent alerts) {
		if (journey == null) {
			return false;
		}
		return filterRecurrentDelay(journey, alert, alerts);
	}

	public static boolean filterStrike(RecurrentJourney journey, AlertStrike alert, AlertsSent alerts) {
		if (journey == null) return false;
		return filterRecurrentStrike(journey, alert, alerts);
	}

	public static boolean filterParking(RecurrentJourney journey, AlertParking alert, AlertsSent alerts) {
		if (journey == null) return false;
		return filterRecurrentParking(journey, alert, alerts);
	}

	public static boolean filterAccident(RecurrentJourney journey, AlertAccident alert, AlertsSent alerts) {
		if (journey == null) return false;
		return filterRecurrentAccident(journey, alert, alerts);
	}

	public static boolean filterRoad(RecurrentJourney journey, AlertRoad alert, AlertsSent alerts) {
		if (journey == null) return false;
		return filterRecurrentRoad(journey, alert, alerts);
	}

	private static boolean filterDelay(List<Leg> legs, AlertDelay alert) {
		try {
			Calendar ac = Calendar.getInstance();
			ac.setTimeInMillis(alert.getFrom());
			for (Leg leg : legs) {
//				if (areEqual(leg.getTransport(), alert.getTransport(), true, false, true, true)) {
					Calendar c = Calendar.getInstance();
					c.setTimeInMillis(leg.getStartime());
					if (c.get(Calendar.YEAR) == ac.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == ac.get(Calendar.DAY_OF_YEAR)) {
						// found, check the existing alerts
						return checkExistingDelayAlerts(leg.getAlertDelayList(),alert);
					} else {
						// different date, not applicable
						return false;
					}
//				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Cannot filter delay");		
		}
		return false;
	}

	private static boolean checkExistingDelayAlerts(List<AlertDelay> list, AlertDelay alert) {
		if (list == null || list.isEmpty()) return true;
		// assume singleton here...
		AlertDelay old = list.get(0);
		if (old.getTo() < System.currentTimeMillis()) return true;
		else {
			return Math.abs(old.getDelay()-alert.getDelay()) > THRESHOLD;
		}
	}

	private static boolean filterRecurrentDelay(RecurrentJourney journey, AlertDelay alert, RecurrentAlertsSent alerts) {
		try {
			String transportId = alert.getTransport().getAgencyId() + "_" + alert.getTransport().getRouteId();
			if (!journey.getMonitorLegs().containsKey(transportId) || journey.getMonitorLegs().get(transportId) == false) {
				return false;
			}
			// TODO check also the period
			Calendar cal = new GregorianCalendar();
			if (journey.getParameters().getRecurrence().contains(cal.get(Calendar.DAY_OF_WEEK))) {
				// found, check the existing alerts
				if (alerts == null) return true;
				return alerts.check(buildId(alert), alert.getDelay(), THRESHOLD);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Cannot filter delay for recurrent journey");			
		}
		return false;
	}

	
	private static boolean filterStrike(List<Leg> legs, AlertStrike alert) {
		try {
		for (Leg leg : legs) {
			if (areEqual(leg.getTransport(), alert.getTransport(), true, false, false, true)) {
				return true;
			}
		}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Cannot filter strike");			
		}
		return false;
	}

	private static boolean filterRecurrentStrike(RecurrentJourney journey, AlertStrike alert, AlertsSent alerts) {
		return false;
	}

	private static boolean filterParking(List<Leg> legs, AlertParking alert) {
		for (Leg leg: legs) {
			StopId stop = leg.getFrom().getStopId();
			if (stop == null) {
				continue;
			}

			if (areEqual(stop, alert.getPlace(), true, true)) {
				if(stop.getAgencyId().equals(leg.getTransport().getAgencyId())) {
					if (alert.getNoOfvehicles() < 3 && alert.getNoOfvehicles() > 0) {
//						log.info("Few vehicles to rent (" + alert.getNoOfvehicles() + ") @" + stop.getId());
						return true;
					}
				}
				if(!stop.getAgencyId().equals(leg.getTransport().getAgencyId())) {
					if (alert.getPlacesAvailable() < 3 && alert.getPlacesAvailable() > 0) {
//						log.info("Few vehicle places (" + alert.getPlacesAvailable() + ") @" + stop.getId());
						return true;
					}
				}				
				
			}
			stop = leg.getTo().getStopId();
			if (stop == null) {
				continue;
			}
			if (areEqual(stop, alert.getPlace(), true, true)) {
				if(stop.getAgencyId().equals(leg.getTransport().getAgencyId())) {
					if (alert.getPlacesAvailable() < 3  && alert.getPlacesAvailable() > 0) {
//						log.info("Few vehicle places (" + alert.getPlacesAvailable() + ") @" + stop.getId());
						return true;
					}
				}
				if(!stop.getAgencyId().equals(leg.getTransport().getAgencyId())) {
					if (alert.getNoOfvehicles() < 3 && alert.getPlacesAvailable() > 0) {
//						log.info("Few vehicles to rent (" + alert.getNoOfvehicles() + ") @" + stop.getId());
						return true;
					}
				}					
				
//				if (alert.getPlacesAvailable() < 0) {
//					return true;
			
			}				
		}
		
		return false;
	}

	private static boolean filterRecurrentParking(RecurrentJourney journey, AlertParking alert, AlertsSent alerts) {
		return false;
	}

	private static boolean filterAccident(List<Leg> legs, AlertAccident alert) {
		return false;
	}
	private static boolean filterRecurrentAccident(RecurrentJourney journey, AlertAccident alert, AlertsSent alerts) {
		return false;
	}
	private static boolean filterRoad(List<Leg> legs, AlertRoad alert) {
		return false;
	}
	private static boolean filterRecurrentRoad(RecurrentJourney journey, AlertRoad alert, AlertsSent alerts) {
		return false;
	}


	public static boolean areEqual(Transport t1, Transport t2, boolean agency, boolean route, boolean trip, boolean type) {
		boolean result = true;
		
		boolean numbersOnly = ("5".equals(t2.getAgencyId()) || "6".equals(t2.getAgencyId()));
		String tId1 = t1.getTripId();
		String tId2 = t2.getTripId();
		if (numbersOnly) {
			if (tId1 != null) {
				tId1 = tId1.replaceAll("\\D*", "");
			}
			if (tId2 != null) {
				tId2 = tId2.replaceAll("\\D*", "");
			}			
		}
		
		if (agency) {
			result &= areEqualOrNull(t1.getAgencyId(), t2.getAgencyId());
		}
		if (result && route) {
			result &= areEqualOrNull(t1.getRouteId(), t2.getRouteId());
		}
		if (result && trip) {
			result &= areEqualOrNull(tId1, tId2);
		}
		if (result && type) {
			result &= areEqualOrNull(t1.getType(), t2.getType());
		}

		return result;
	}

	public static boolean areEqual(StopId s1, StopId s2, boolean agency, boolean id) {
		boolean result = true;
		if (agency) {
			result &= areEqualOrNull(s1.getAgencyId(), s2.getAgencyId());
		}
		if (result && id) {
			result &= areEqualOrNull(s1.getId(), s2.getId());
		}

		return result;
	}

	private static boolean areEqualOrNull(Object o1, Object o2) {
		if ((o1 == null) != (o2 == null)) {
			return false;
		}
		if (o1 != null) {
			return o1.equals(o2);
		}
		return false;
	}

	public static String buildId(Alert alert) {
		String s = null;
		if (alert instanceof AlertDelay) {
			s = "AD_" + ((AlertDelay) alert).getTransport().getAgencyId() + "_" + ((AlertDelay) alert).getTransport().getRouteId() + "_" + ((AlertDelay) alert).getTransport().getTripId();
		} else if (alert instanceof AlertStrike) {
			s = "AD_" + ((AlertStrike) alert).getTransport().getAgencyId() + "_" + ((AlertStrike) alert).getTransport().getRouteId() + "_" + ((AlertStrike) alert).getTransport().getTripId();
		} else if (alert instanceof AlertParking) {
			s = "AP_" + ((AlertParking) alert).getPlace().getAgencyId() + "_" + ((AlertParking) alert).getPlace().getId();
		}
		
		return s;
	}

}
