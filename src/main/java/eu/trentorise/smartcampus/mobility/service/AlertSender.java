package eu.trentorise.smartcampus.mobility.service;

import it.sayservice.platform.smartplanner.data.message.alerts.Alert;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertAccident;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoad;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertStrike;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import eu.trentorise.smartcampus.mobility.logging.StatLogger;
import eu.trentorise.smartcampus.mobility.model.GenericTrain;
import eu.trentorise.smartcampus.mobility.model.Parking;
import eu.trentorise.smartcampus.mobility.processor.alerts.AlertFilter;
import eu.trentorise.smartcampus.mobility.processor.alerts.AlertNotifier;
import eu.trentorise.smartcampus.mobility.processor.alerts.AlertUpdater;
import eu.trentorise.smartcampus.mobility.processor.alerts.AlertWrapper;
import eu.trentorise.smartcampus.mobility.processor.alerts.AlertsSent;
import eu.trentorise.smartcampus.mobility.processor.alerts.DelayChecker;
import eu.trentorise.smartcampus.mobility.processor.alerts.ParkingChecker;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.storage.RecurrentJourneyObject;
import eu.trentorise.smartcampus.mobility.storage.RouteMonitoringObject;

@Component
public class AlertSender {

	@Autowired
	private SmartPlannerHelper smartPlannerHelper;
	
	@Autowired
	private DomainStorage domainStorage;	
	
	@Autowired(required=false)
	private StatLogger statLogger;	
	
	@Autowired
	private AlertNotifier notifier;	
	
	private static Logger logger = LoggerFactory.getLogger(AlertSender.class);
	
	/**
	 * Convert train delays to delay alerts, filtering only new/changed delays.
	 * Match against user trips and send the alerts
	 * @param trains
	 */
	public void publishTrains(List<GenericTrain> trains) {
		List<AlertDelay> allDelays = filterSentDelays(trains);
		
		List<AlertWrapper> userDelays = findDelayAlertsForUsers(allDelays);
//		userDelays.addAll( 
		findDelayAlertsForMonitoredRoutes(allDelays); // currently not sending alerts
		publishDelayAlerts(allDelays, userDelays);
	}
	/**
	 * Convert parking state to parking alerts, filtering only new/changed states.
	 * Match against  user trips and send the alerts
	 * @param parkings
	 */
	public void publishParkings(List<Parking> parkings) {
		List<AlertParking> allAlerts = filterSentParkings(parkings);
		List<AlertWrapper> userAlerts = findParkingAlertsForUsers(allAlerts);
		publishParkingAlerts(allAlerts, userAlerts);
	}	
	
	
	/**
	 * Publish an alert on behalf of a user or an external app
	 * @param alert
	 * @param userId
	 */
	public void publishAlert(Alert alert) {
		if (alert instanceof AlertDelay) {
			AlertDelay delay = (AlertDelay) alert;
			delay.getTransport().setRouteShortName(DelayChecker.buildRouteLongName(delay.getTransport().getAgencyId(), delay.getTransport().getRouteId()));
			List<AlertDelay> allDelays = Collections.singletonList(delay);
			List<AlertWrapper> userDelays = findDelayAlertsForUsers(allDelays);
			publishDelayAlerts(allDelays, userDelays);
		} 
		if (alert instanceof AlertRoad) {
			List<AlertRoad> allRoads = Collections.singletonList((AlertRoad)alert);
			List<AlertWrapper> userParkings = findRoadWorkAlertsForUsers(allRoads);
			publishRoadWorkAlerts(allRoads, userParkings);
		} 
		if (alert instanceof AlertParking) {
			List<AlertParking> allParkings = Collections.singletonList((AlertParking)alert);
			List<AlertWrapper> userParkings = findParkingAlertsForUsers(allParkings);
			publishParkingAlerts(allParkings, userParkings);
		} 
		if (alert instanceof AlertAccident) {
			List<AlertAccident> allAlerts = Collections.singletonList((AlertAccident)alert);
			List<AlertWrapper> userAlerts = findAccidentAlertsForUsers(allAlerts);
			publishAccidentAlerts(allAlerts, userAlerts);
		} 
		if (alert instanceof AlertStrike) {
			List<AlertStrike> allAlerts = Collections.singletonList((AlertStrike)alert);
			List<AlertWrapper> userAlerts = findStrikeAlertsForUsers(allAlerts);
			publishStrikeAlerts(allAlerts, userAlerts);
		} 
	}
	
	/**
	 * @param convertOrdinanze
	 */
	public void publishRoadWorkAlerts(List<AlertRoad> roadWorkAlerts) {
		List<AlertRoad> allAlerts = filterSentRoadWorkAlerts(roadWorkAlerts);
		List<AlertWrapper> userAlerts = findRoadWorkAlertsForUsers(allAlerts);
		publishRoadWorkAlerts(allAlerts, userAlerts);
	}

	private List<AlertDelay> filterSentDelays(List<GenericTrain> trains) {
		List<AlertDelay> result = Lists.newArrayList();
		AlertsSent alertsSent = domainStorage.getAlertsSent();
		for (GenericTrain train: trains) {
			AlertDelay alert = DelayChecker.checkDelay(train);
			if (alert != null) {
				AlertsSent newAlertsSent = DelayChecker.checkNewAlerts(alertsSent, train);
				if (newAlertsSent != null) {
					alertsSent = newAlertsSent;
					result.add(alert);
				}
			}
		}
		alertsSent = DelayChecker.cleanOldAlerts(alertsSent);
		domainStorage.updateAlertsSent(alertsSent);
		
		return result;
	}
	

	private List<AlertWrapper> findDelayAlertsForUsers(List<AlertDelay> alerts) {
		List<AlertWrapper> result = Lists.newArrayList();

		for (AlertDelay alert : alerts) {
			logger.debug("\t{}", alert.getTransport());
			Criteria c1 = 
					new Criteria
					("transport.agencyId").is(alert.getTransport().getAgencyId())
					.and
					("transport.tripId").regex("[^digit]*" + alert.getTransport().getTripId() + "[^digit]*")
					.and
					("transport.type").is(alert.getTransport().getType().toString());
			
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(alert.getFrom());
			c.set(Calendar.HOUR_OF_DAY,0);
			c.set(Calendar.MINUTE,0);
			c.set(Calendar.SECOND,0);
			c.set(Calendar.MILLISECOND,0);
			Criteria criteria = new Criteria
					("data.leg").elemMatch(c1)
					.and
					("monitor").is(true)
					.andOperator(
							new Criteria("data.startime").gte(c.getTimeInMillis()),
							new Criteria("data.startime").lte(c.getTimeInMillis()+1000*60*60*24)
							);
			List<ItineraryObject> its = domainStorage.searchDomainObjects(criteria, ItineraryObject.class);
			if (!its.isEmpty()) {
				logger.debug("{} -> {}",its.size(), alert.getTransport().getAgencyId());
			}

			Calendar now = Calendar.getInstance();
			for (ItineraryObject it : its) {
				if (it.getRecurrency() != null && it.getRecurrency().getDaysOfWeek() != null) {
					if (!it.getRecurrency().getDaysOfWeek().contains(now.get(Calendar.DAY_OF_WEEK))) {
						continue;
					}
				}
				if (AlertFilter.filterDelay(it.getData(), alert)) {
					it.setData(AlertUpdater.updateAlerts(it.getData(), alert));
					domainStorage.saveItinerary(it);
					AlertWrapper wrapper = new AlertWrapper();
					wrapper.setAlert(alert);
					wrapper.setClientId(it.getClientId());
					wrapper.setName(it.getName());
					wrapper.setUserId(it.getUserId());		
					wrapper.setAppId(it.getAppId());
					result.add(wrapper);
				}
			}
			
			c = Calendar.getInstance();
			c.setTimeInMillis(alert.getFrom());
			
			criteria = new Criteria
					("data.legs").elemMatch(c1)
					.and
					("monitor").is(true)
					.and
					("data.monitorLegs."+alert.getTransport().getAgencyId()+"_"+alert.getTransport().getRouteId()).is(true)
					.and
					("data.parameters.fromDate").lte(alert.getFrom())
					.and
					("data.parameters.toDate").gte(alert.getFrom())
					.and
					("data.parameters.recurrence").is(c.get(Calendar.DAY_OF_WEEK));
			
			List<RecurrentJourneyObject> recs = domainStorage.searchDomainObjects(criteria, RecurrentJourneyObject.class);
			if (!recs.isEmpty()) {
				logger.debug("{} => {}",recs.size(), alert.getTransport().getAgencyId());
			}

			for (RecurrentJourneyObject rec : recs) {
				if (AlertFilter.filterDelay(rec.getData(), alert, rec.getAlertsSent())) {
					rec.setAlertsSent(AlertUpdater.updateAlerts(alert, rec.getAlertsSent()));
					domainStorage.saveRecurrent(rec);
					AlertWrapper wrapper = new AlertWrapper();
					wrapper.setAlert(alert);
					wrapper.setClientId(rec.getClientId());
					wrapper.setName(rec.getName());
					wrapper.setUserId(rec.getUserId());					
					result.add(wrapper);
				}
			}			
			
			
			
		}
		return result;
	}
	
	private List<AlertWrapper> findDelayAlertsForMonitoredRoutes(List<AlertDelay> alerts) {
		List<AlertWrapper> result = Lists.newArrayList();
		
		for (AlertDelay alert: alerts) {
			
			List<RouteMonitoringObject> monitoring = findRouteMonitoring(alert);
			for (RouteMonitoringObject rm: monitoring) {
				AlertWrapper wrapper = new AlertWrapper();
				
				wrapper.setAlert(alert);
				wrapper.setUserId(rm.getUserId());
				wrapper.setAppId(rm.getAppId());
				wrapper.setClientId(rm.getClientId());
				wrapper.setName(buildMonitoringText(alert, rm));
				
				result.add(wrapper);
			}
		}
		
		return result;
	}
	
	//TODO fill with real info (mapping agencyId...)
	private String buildMonitoringText(AlertDelay alert, RouteMonitoringObject rm) {
		return "Ritardo per " + ((AlertDelay) alert).getTransport().getRouteShortName();
	}
	
	private List<RouteMonitoringObject> findRouteMonitoring(AlertDelay alert) {
		long now = System.currentTimeMillis();
		Date nowDate = new Date(now);
		Calendar cal = new GregorianCalendar();
		cal.setTime(nowDate);
		
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		String nowHour = sdf.format(nowDate);
		
		Criteria criteria = new Criteria("agencyId").is(alert.getTransport().getAgencyId()).and("routeId").is(alert.getTransport().getRouteId());
		
		List<RouteMonitoringObject> res1 = domainStorage.searchDomainObjects(criteria, RouteMonitoringObject.class);
		List<RouteMonitoringObject> res2 = Lists.newArrayList();
		
		for (RouteMonitoringObject rm: res1) {
			if (rm.getRecurrency() == null) {
				continue;
			}
			if (rm.getRecurrency().getFromDate() != null) {
				if (rm.getRecurrency().getFromDate() > now) {
					continue;
				}
			}
			if (rm.getRecurrency().getToDate() != null) {
				if (rm.getRecurrency().getToDate() < now) {
					continue;
				}
			}	
			if (rm.getRecurrency().getFromHour() != null) {
				if (rm.getRecurrency().getFromHour().compareTo(nowHour) > 0) {
					continue;
				}
			}
			if (rm.getRecurrency().getToHour() != null) {
				if (rm.getRecurrency().getToHour().compareTo(nowHour) < 0) {
					continue;
				}
			}			
			if (rm.getRecurrency().getDaysOfWeek() != null && !rm.getRecurrency().getDaysOfWeek().isEmpty()) {
				if (!rm.getRecurrency().getDaysOfWeek().contains(cal.get(Calendar.DAY_OF_WEEK))) {
					continue;
				}
			}
			
			res2.add(rm);
		}
		
		return res2;
	}		
	
	
	
	private void publishDelayAlerts(List<AlertDelay> allAlerts, List<AlertWrapper> userAlerts) {
		logger.debug("PUBLISHING DELAY: {} / {}", allAlerts.size(), userAlerts.size());
		
		for (AlertDelay alert : allAlerts) {
			try {
				if (statLogger != null) statLogger.log(alert, null);
				smartPlannerHelper.sendAlert(alert);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		for (AlertWrapper wrapper : userAlerts) {		
			if (statLogger != null) statLogger.log(wrapper.getAlert(), wrapper.getUserId());
			notifier.notifyDelay(wrapper.getUserId(), wrapper.getClientId(), wrapper.getAppId(), (AlertDelay)wrapper.getAlert(), wrapper.getName());		
		}
	}	
	
	
	
	private List<AlertParking> filterSentParkings(List<Parking> parkings) {
		List<AlertParking> result = Lists.newArrayList();
		AlertsSent alertsSent = domainStorage.getAlertsSent();
		for (Parking parking: parkings) {
			AlertParking alert = ParkingChecker.checkParking(parking);
			if (alert != null) {
				AlertsSent newAlertsSent = ParkingChecker.checkNewAlerts(alertsSent, parking);
				if (newAlertsSent != null) {
					alertsSent = newAlertsSent;
					result.add(alert);
//					logger.debug("******* SENDING PARKING *******");
				}
			}
		}
		alertsSent = ParkingChecker.cleanOldAlerts(alertsSent);
		domainStorage.updateAlertsSent(alertsSent);
		
		return result;
	}	

	private List<AlertWrapper> findParkingAlertsForUsers(List<AlertParking> alerts) {
		List<AlertWrapper> result = Lists.newArrayList();
		
		for (AlertParking alert : alerts) {
			logger.debug("\t{}", alert.getPlace());
			Criteria c1 = new Criteria
					("from.stopId.agencyId").is(alert.getPlace().getAgencyId())
					.and
					("from.stopId._id").is(alert.getPlace().getId());
			Criteria criteria = new Criteria
					("data.leg").elemMatch(c1)
					.and
					("monitor").is(true);
			
			List<ItineraryObject> its = domainStorage.searchDomainObjects(criteria, ItineraryObject.class);
			
//			if (!its.isEmpty()) {
//				logger.debug(its.size() + " ---> " + alert.getPlace().getAgencyId());
//			}

			for (ItineraryObject it : its) {
				if (AlertFilter.filterParking(it.getData(), alert)) {
					it.setData(AlertUpdater.updateAlerts(it.getData(), alert));
					domainStorage.saveItinerary(it);
					AlertWrapper wrapper = new AlertWrapper();
					wrapper.setAlert(alert);
					wrapper.setClientId(it.getClientId());
					wrapper.setName(it.getName());
					wrapper.setUserId(it.getUserId());
					wrapper.setAppId(it.getAppId());
					result.add(wrapper);
				}
			}		
		}
		
		return result;
	}
	
	private void publishParkingAlerts(List<AlertParking> allAlerts, List<AlertWrapper> userAlerts) {
		logger.debug("PUBLISHING PARKING: {} / {}", allAlerts.size(), userAlerts.size());
		
		for (AlertParking alert : allAlerts) {
			try {
				if (statLogger != null) statLogger.log(alert, null);
			smartPlannerHelper.sendAlert(alert);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		for (AlertWrapper wrapper : userAlerts) {		
			if (statLogger != null) statLogger.log(wrapper.getAlert(), wrapper.getUserId());
			notifier.notifyParking(wrapper.getUserId(), wrapper.getClientId(), wrapper.getAppId(), (AlertParking)wrapper.getAlert(), wrapper.getName());		
		}
	}


	/**
	 * @param allAlerts
	 * @param userAlerts
	 */
	private void publishRoadWorkAlerts(List<AlertRoad> allAlerts, List<AlertWrapper> userAlerts) {
		logger.debug("PUBLISHING ROAD WORKS: {} / {}", allAlerts.size(), userAlerts.size());
		
		for (AlertRoad alert : allAlerts) {
			try {
				if (statLogger != null) statLogger.log(alert, null);
				if (statLogger != null) statLogger.log(alert, null);
			smartPlannerHelper.sendAlert(alert);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		for (AlertWrapper wrapper : userAlerts) {		
			if (statLogger != null) statLogger.log(wrapper.getAlert(), wrapper.getUserId());
			notifier.notifyRoad(wrapper.getUserId(), wrapper.getClientId(), wrapper.getAppId(), (AlertRoad)wrapper.getAlert(), wrapper.getName());		
		}
	}

	/**
	 * @param allDelays
	 * @return
	 */
	private List<AlertWrapper> findRoadWorkAlertsForUsers(List<AlertRoad> allDelays) {
		return Collections.emptyList();
	}

	/**
	 * @param roadWorkAlerts
	 * @return
	 */
	private List<AlertRoad> filterSentRoadWorkAlerts(List<AlertRoad> roadWorkAlerts) {
//		List<AlertRoad> result = Lists.newArrayList();
//		AlertsSent alertsSent = domainStorage.getAlertsSent();
//		for (AlertRoad alert: roadWorkAlerts) {
//			AlertRoad alert = DelayChecker.checkDelay(train);
//			if (alert != null) {
//				AlertsSent newAlertsSent = DelayChecker.checkNewAlerts(alertsSent, train);
//				if (newAlertsSent != null) {
//					alertsSent = newAlertsSent;
//					result.add(alert);
//				}
//			}
//		}
//		alertsSent = DelayChecker.cleanOldAlerts(alertsSent);
//		domainStorage.updateAlertsSent(alertsSent);
//		return result;
		return roadWorkAlerts;
	}

	/**
	 * @param allAlerts
	 * @param userAlerts
	 */
	private void publishAccidentAlerts(List<AlertAccident> allAlerts, List<AlertWrapper> userAlerts) {
		logger.debug("PUBLISHING ACCIDENTS: {} / {}", allAlerts.size(), userAlerts.size());
		
		for (AlertAccident alert : allAlerts) {
			try {
				if (statLogger != null) statLogger.log(alert, null);
				smartPlannerHelper.sendAlert(alert);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		for (AlertWrapper wrapper : userAlerts) {		
			if (statLogger != null) statLogger.log(wrapper.getAlert(), wrapper.getUserId());
			notifier.notifyAccident(wrapper.getUserId(), wrapper.getClientId(), wrapper.getAppId(), (AlertAccident)wrapper.getAlert(), wrapper.getName());		
		}
	}
	/**
	 * @param allAlerts
	 * @return
	 */
	private List<AlertWrapper> findAccidentAlertsForUsers(List<AlertAccident> allAlerts) {
		return Collections.emptyList();
	}

	/**
	 * @param allAlerts
	 * @param userAlerts
	 */
	private void publishStrikeAlerts(List<AlertStrike> allAlerts, List<AlertWrapper> userAlerts) {
		logger.debug("PUBLISHING ACCIDENTS: {} / {}", allAlerts.size(), userAlerts.size());
		
		for (AlertStrike alert : allAlerts) {
			try {
				if (statLogger != null) statLogger.log(alert, null);
				smartPlannerHelper.sendAlert(alert);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		for (AlertWrapper wrapper : userAlerts) {		
			if (statLogger != null) statLogger.log(wrapper.getAlert(), wrapper.getUserId());
			notifier.notifyStrike(wrapper.getUserId(), wrapper.getClientId(), wrapper.getAppId(), (AlertStrike)wrapper.getAlert(), wrapper.getName());		
		}
	}
	/**
	 * @param allAlerts
	 * @return
	 */
	private List<AlertWrapper> findStrikeAlertsForUsers(List<AlertStrike> allAlerts) {
		return Collections.emptyList();
	}

}

