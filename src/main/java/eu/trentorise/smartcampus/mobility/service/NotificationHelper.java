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

package eu.trentorise.smartcampus.mobility.service;

import it.sayservice.platform.smartplanner.data.message.alerts.Alert;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertAccident;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoad;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertStrike;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.communicator.CommunicatorConnector;
import eu.trentorise.smartcampus.communicator.CommunicatorConnectorException;
import eu.trentorise.smartcampus.communicator.model.EntityObject;
import eu.trentorise.smartcampus.communicator.model.Notification;
import eu.trentorise.smartcampus.mobility.model.Announcement;
import eu.trentorise.smartcampus.mobility.processor.alerts.AlertNotifier;
import eu.trentorise.smartcampus.mobility.util.TokenHelper;
import eu.trentorise.smartcampus.network.RemoteConnector;

/**
 * @author raman
 *
 */
@Component
public class NotificationHelper extends RemoteConnector implements AlertNotifier {

	private static final String PATH_TOKEN = "oauth/token";

	public static final String MS_APP = "core.mobility";
	
	@Autowired
	@Value("${communicatorURL}")
	private String communicatorURL;
	
	@Autowired
	private TokenHelper tokenHelper;
	
	private CommunicatorConnector connector = null;

	private Log logger = LogFactory.getLog(getClass());

	private CommunicatorConnector connector() {
		if (connector == null) {
			try {
				connector = new CommunicatorConnector(communicatorURL, MS_APP);
			} catch (Exception e) {
				logger.error("Failed to instantiate connector: "+e.getMessage(), e);
				e.printStackTrace();
			}
		}
		return connector;
	}


	@Override
	public void notifyStrike(String userId, String clientId, String appId, AlertStrike alert, String name) {
		Map<String, Object> content = new TreeMap<String, Object>();
		content.put("type", "alertStrike");
		content.put("agencyId", ((AlertStrike) alert).getTransport().getAgencyId());
		content.put("routeId", ((AlertStrike) alert).getTransport().getRouteId());
		content.put("routeShortName", ((AlertStrike) alert).getTransport().getRouteShortName());
		content.put("tripId", ((AlertStrike) alert).getTransport().getTripId());
		content.put("stopId", ((AlertStrike) alert).getStop().getId());
		Notification n = prepareMessage(name, alert, content, clientId);
		notify(n, userId, appId == null ? MS_APP : appId);
	}

	@Override
	public void notifyDelay(String userId, String clientId, String appId, AlertDelay alert, String name) {
		Map<String, Object> content = new TreeMap<String, Object>();
		content.put("type", "alertDelay");
		content.put("agencyId", ((AlertDelay) alert).getTransport().getAgencyId());
		content.put("routeId", ((AlertDelay) alert).getTransport().getRouteId());
		content.put("routeShortName", ((AlertDelay) alert).getTransport().getRouteShortName());
		content.put("tripId", ((AlertDelay) alert).getTransport().getTripId());
		content.put("delay", ((AlertDelay) alert).getDelay());
		if (((AlertDelay) alert).getPosition() != null) {
			content.put("station", ((AlertDelay) alert).getPosition().getName());
		}
		Notification n = prepareMessage(name, alert, content, clientId);
		notify(n, userId, appId == null ? MS_APP : appId);
	}

	@Override
	public void notifyParking(String userId, String clientId, String appId, AlertParking alert, String name) {
		Map<String, Object> content = new TreeMap<String, Object>();
		content.put("type", "alertParking");
		AlertParking parking = ((AlertParking) alert);
		content.put("agencyId", parking.getPlace().getAgencyId());
		content.put("stopId", parking.getPlace().getId());
		content.put("placesAvailable", parking.getPlacesAvailable());
		content.put("noOfvehicles", parking.getNoOfvehicles());
		if (parking.getPlace().getExtra() != null && parking.getPlace().getExtra().containsKey("transport")) {
			content.put("transport", parking.getPlace().getExtra().get("transport"));
		}
		Notification n = prepareMessage(name, alert, content, clientId);
		notify(n, userId, appId == null ? MS_APP : appId);
	}

	@Override
	public void notifyAccident(String userId, String clientId, String appId, AlertAccident alert, String name) {
//		Map<String, Object> content = new TreeMap<String, Object>();
//		Notification n = prepareMessage(name, alert, content);
//		notify(n, userId);
	}

	@Override
	public void notifyRoad(String userId, String clientId, String appId, AlertRoad alert, String name) {
//		Map<String, Object> content = new TreeMap<String, Object>();
//		Notification n = prepareMessage(name, alert, content);
//		notify(n, userId);
	}

	private void notify(Notification n, String userId, String appId) {
			long when = System.currentTimeMillis();
			n.setTimestamp(when);
			try {
				connector().sendAppNotification(n, appId, Collections.singletonList(userId), tokenHelper.getToken());
			} catch (CommunicatorConnectorException e) {
				e.printStackTrace();
				logger .error("Failed to send notifications: "+e.getMessage(), e);
			}
	}
	
	private void notify(Notification n, String appId) {
		long when = System.currentTimeMillis();
		n.setTimestamp(when);
		try {
			connector().sendAppNotification(n, appId, Collections.EMPTY_LIST, tokenHelper.getToken());
		} catch (CommunicatorConnectorException e) {
			e.printStackTrace();
			logger .error("Failed to send notifications: "+e.getMessage(), e);
		}
}	

	private Notification prepareMessage(String name, Alert alert, Map<String, Object> content, String clientId) {
		Notification not = new Notification();
//		not.setTitle(title + " Alert for journey '" + name + "'");
		not.setTitle(name);
		not.setDescription(alert.getNote());

		content.put("creatorType", alert.getCreatorType().toString());
		content.put("from", alert.getFrom());
		content.put("to", alert.getTo());

		List<EntityObject> eos = new ArrayList<EntityObject>();
		EntityObject eo = new EntityObject();
		eo.setId(clientId);
		eo.setType("journey");
		eo.setTitle(name);
		eos.add(eo);
		not.setEntities(eos);
		not.setContent(content);
		
		return not;
	}
	
	@Override
	public void notifyAnnouncement(Announcement announcement, String appId) {
		Notification not = new Notification();
		
		not.setTitle(announcement.getTitle());
		not.setDescription(announcement.getDescription());
	
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		long from = -1;
		long to = -1;
		
		try {
			from = sdf.parse(announcement.getFrom()).getTime();
		} catch (Exception e) {
		}
		try {
			to = sdf.parse(announcement.getTo()).getTime();
		} catch (Exception e) {
		}
		
		Map<String, Object> content = new TreeMap<String, Object>();
		content.put("type", "announcement");
		content.put("from", from);
		content.put("to", to);
		not.setContent(content);
		
		notify(not, appId);
	}
	
}
