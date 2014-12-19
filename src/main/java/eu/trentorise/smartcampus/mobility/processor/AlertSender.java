package eu.trentorise.smartcampus.mobility.processor;

import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;

import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;

import com.google.common.collect.Lists;

import eu.trentorise.smartcampus.mobility.controller.rest.JourneyPlannerController;
import eu.trentorise.smartcampus.mobility.logging.StatLogger;
import eu.trentorise.smartcampus.mobility.processor.alerts.AlertFilter;
import eu.trentorise.smartcampus.mobility.processor.alerts.AlertNotifier;
import eu.trentorise.smartcampus.mobility.processor.alerts.AlertUpdater;
import eu.trentorise.smartcampus.mobility.processor.alerts.AlertWrapper;
import eu.trentorise.smartcampus.mobility.processor.alerts.AlertsSent;
import eu.trentorise.smartcampus.mobility.processor.alerts.DelayChecker;
import eu.trentorise.smartcampus.mobility.processor.alerts.ParkingChecker;
import eu.trentorise.smartcampus.mobility.processor.converter.GenericTrain;
import eu.trentorise.smartcampus.mobility.processor.converter.Parking;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.storage.RecurrentJourneyObject;
import eu.trentorise.smartcampus.mobility.util.HTTPConnector;

public class AlertSender {

	@Autowired
	@Value("${otp.url}")
	private String otpURL;	
	
	@Autowired
	private DomainStorage domainStorage;	
	
	@Autowired
	private StatLogger statLogger;	
	
	@Autowired
	private AlertNotifier notifier;	
	
	private static Log logger = LogFactory.getLog(AlertSender.class);
	
	private DelayChecker delayChecker;
	private ParkingChecker parkingChecker;
	
	public AlertSender() {
		delayChecker = new DelayChecker();
		parkingChecker = new ParkingChecker();
	}
	
	public void publishDelayAlerts(List<GenericTrain> trains) {
		List<AlertDelay> allDelays = sendDelayAlert(trains);
		List<AlertWrapper> userDelays = checkAlertDelay(allDelays);
		publishDelayAlerts(allDelays, userDelays);
	}
	
	public List<AlertDelay> sendDelayAlert(List<GenericTrain> trains) {
		List<AlertDelay> result = Lists.newArrayList();
		AlertsSent alertsSent = domainStorage.getAlertsSent();
		for (GenericTrain train: trains) {
			AlertDelay alert = delayChecker.checkDelay(train);
			if (alert != null) {
				AlertsSent newAlertsSent = DelayChecker.checkNewAlerts(alertsSent, train);
				if (newAlertsSent != null) {
					alertsSent = newAlertsSent;
					result.add(alert);
//					System.out.println("******* SENDING DELAY *******");
				}
			}
		}
		alertsSent = DelayChecker.cleanOldAlerts(alertsSent);
		domainStorage.updateAlertsSent(alertsSent);
		
		return result;
	}
	

	public List<AlertWrapper> checkAlertDelay(List<AlertDelay> alerts) {
		List<AlertWrapper> result = Lists.newArrayList();

		for (AlertDelay alert : alerts) {
			System.out.println("\t" + alert.getTransport());
			Criteria c1 = new Criteria("transport.agencyId").is(alert.getTransport().getAgencyId())
					.and("transport.tripId").regex("[^digit]*" + alert.getTransport().getTripId() + "[^digit]*")
					.and("transport.type").is(alert.getTransport().getType());
			Criteria criteria = new Criteria("data.leg").elemMatch(c1).and("monitor").is(true);
			List<ItineraryObject> its = (List<ItineraryObject>) domainStorage.searchDomainObjects(criteria, ItineraryObject.class, DomainStorage.ITINERARY);
			if (!its.isEmpty()) {
				System.out.println(its.size() + " -> " + alert.getTransport().getAgencyId());
			}

			for (ItineraryObject it : its) {
				if (AlertFilter.filterDelay(it.getData(), alert)) {
					it.setData(AlertUpdater.updateAlerts(it.getData(), alert));
					domainStorage.saveItinerary(it);
					AlertWrapper wrapper = new AlertWrapper();
					wrapper.setAlert(alert);
					wrapper.setClientId(it.getClientId());
					wrapper.setName(it.getName());
					wrapper.setUserId(it.getUserId());					
					result.add(wrapper);
				}
			}
			
			criteria = new Criteria("data.legs").elemMatch(c1).and("monitor").is(true);	
			
			List<RecurrentJourneyObject> recs = (List<RecurrentJourneyObject>) domainStorage.searchDomainObjects(criteria, RecurrentJourneyObject.class, DomainStorage.RECURRENT);
			if (!recs.isEmpty()) {
				System.out.println(its.size() + " => " + alert.getTransport().getAgencyId());
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
	
	public void publishDelayAlerts(List<AlertDelay> allAlerts, List<AlertWrapper> userAlerts) {
		System.out.println("PUBLISHING DELAY: " + allAlerts.size() + " / " + userAlerts.size());
		
		ObjectMapper mapper = new ObjectMapper();
		for (AlertDelay alert : allAlerts) {
			try {
			String req = mapper.writeValueAsString(alert);
			statLogger.log(alert, null);
			String result = HTTPConnector.doPost(otpURL + JourneyPlannerController.SMARTPLANNER + "updateAD", req, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON);
			logger.info(result);				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		for (AlertWrapper wrapper : userAlerts) {		
			statLogger.log(wrapper.getAlert(), wrapper.getUserId());
			notifier.notifyDelay(wrapper.getUserId(), wrapper.getClientId(), (AlertDelay)wrapper.getAlert(), wrapper.getName());		
		}
	}	
	
	
	
	public void publishParkingAlerts(List<Parking> trains) {
		List<AlertParking> allAlerts = sendParkingAlert(trains);
		List<AlertWrapper> userAlerts = checkAlertParking(allAlerts);
		publishParkingAlert(allAlerts, userAlerts);
	}	
	
	
	public List<AlertParking> sendParkingAlert(List<Parking> parkings) {
		List<AlertParking> result = Lists.newArrayList();
		AlertsSent alertsSent = domainStorage.getAlertsSent();
		for (Parking parking: parkings) {
			AlertParking alert = parkingChecker.checkParking(parking);
			if (alert != null) {
				AlertsSent newAlertsSent = ParkingChecker.checkNewAlerts(alertsSent, parking);
				if (newAlertsSent != null) {
					alertsSent = newAlertsSent;
					result.add(alert);
//					System.out.println("******* SENDING PARKING *******");
				}
			}
		}
		alertsSent = ParkingChecker.cleanOldAlerts(alertsSent);
		domainStorage.updateAlertsSent(alertsSent);
		
		return result;
	}	

	public List<AlertWrapper> checkAlertParking(List<AlertParking> alerts) {
		List<AlertWrapper> result = Lists.newArrayList();
		
		for (AlertParking alert : alerts) {
			System.out.println("\t" + alert.getPlace());
			Criteria c1 = new Criteria("from.stopId.agencyId").is(alert.getPlace().getAgencyId())
					.and("from.stopId._id").is(alert.getPlace().getId());
			Criteria criteria = new Criteria("data.leg").elemMatch(c1); //.and("monitor").is(true);
			
			List<ItineraryObject> its = (List<ItineraryObject>) domainStorage.searchDomainObjects(criteria, ItineraryObject.class, DomainStorage.ITINERARY);
			
//			if (!its.isEmpty()) {
//				System.out.println(its.size() + " ---> " + alert.getPlace().getAgencyId());
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
					result.add(wrapper);
				}
			}		
		}
		
		return result;
	}
	
	public void publishParkingAlert(List<AlertParking> allAlerts, List<AlertWrapper> userAlerts) {
		System.out.println("PUBLISHING PARKING: " + allAlerts.size() + " / " + userAlerts.size());
		
		ObjectMapper mapper = new ObjectMapper();
		for (AlertParking alert : allAlerts) {
			try {
			String req = mapper.writeValueAsString(alert);
			statLogger.log(alert, null);
			String result = HTTPConnector.doPost(otpURL + JourneyPlannerController.SMARTPLANNER + "updateAP", req, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON);
			logger.info(result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		for (AlertWrapper wrapper : userAlerts) {		
			statLogger.log(wrapper.getAlert(), wrapper.getUserId());
			notifier.notifyParking(wrapper.getUserId(), wrapper.getClientId(), (AlertParking)wrapper.getAlert(), wrapper.getName());		
		}
	}
	
}

