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

package eu.trentorise.smartcampus.mobility.processor;

import it.sayservice.platform.smartplanner.data.message.alerts.Alert;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertAccident;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoad;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertStrike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.communicator.CommunicatorConnector;
import eu.trentorise.smartcampus.communicator.CommunicatorConnectorException;
import eu.trentorise.smartcampus.communicator.model.EntityObject;
import eu.trentorise.smartcampus.communicator.model.Notification;
import eu.trentorise.smartcampus.network.JsonUtils;
import eu.trentorise.smartcampus.network.RemoteConnector;

/**
 * @author raman
 *
 */
@Component
public class NotificationHelper extends RemoteConnector implements AlertNotifier {

	private static final String PATH_TOKEN = "oauth/token";

	private static final String MS_APP = "core.mobility";
	
	@Autowired
	@Value("${communicatorURL}")
	private String communicatorURL;
	@Autowired
	@Value("${aacURL}")
	private String aacURL;
	@Autowired
	@Value("${smartcampus.clientId}")
	private String clientId = null;
	@Autowired
	@Value("${smartcampus.clientSecret}")
	private String clientSecret = null;
	
	private String token = null;
	private Long expiresAt = null;
	
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
	@SuppressWarnings("rawtypes")
	private String getToken() {
		if (token == null || System.currentTimeMillis() + 10000 > expiresAt) {
	        final HttpResponse resp;
	        if (!aacURL.endsWith("/")) aacURL += "/";
	        String url = aacURL + PATH_TOKEN+"?grant_type=client_credentials&client_id="+clientId +"&client_secret="+clientSecret;
	        final HttpGet get = new HttpGet(url);
	        get.setHeader("Accept", "application/json");
            try {
				resp = getHttpClient().execute(get);
				final String response = EntityUtils.toString(resp.getEntity());
				if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					Map map = JsonUtils.toObject(response, Map.class);
					expiresAt = System.currentTimeMillis()+(Integer)map.get("expires_in")*1000;
					token = (String)map.get("access_token");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return token;
	}

	@Override
	public void notifyStrike(String userId, String clientId, AlertStrike alert, String name) {
		Map<String, Object> content = new TreeMap<String, Object>();
		content.put("type", "alertStrike");
		content.put("agencyId", ((AlertStrike) alert).getTransport().getAgencyId());
		content.put("routeId", ((AlertStrike) alert).getTransport().getRouteId());
		content.put("routeShortName", ((AlertStrike) alert).getTransport().getRouteShortName());
		content.put("tripId", ((AlertStrike) alert).getTransport().getTripId());
		content.put("stopId", ((AlertStrike) alert).getStop().getId());
		Notification n = prepareMessage(name, alert, content, clientId);
		notify(n, userId);
	}

	@Override
	public void notifyDelay(String userId, String clientId, AlertDelay alert, String name) {
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
		notify(n, userId);
	}

	@Override
	public void notifyParking(String userId, String clientId, AlertParking alert, String name) {
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
		notify(n, userId);
	}

	@Override
	public void notifyAccident(String userId, String clientId, AlertAccident alert, String name) {
//		Map<String, Object> content = new TreeMap<String, Object>();
//		Notification n = prepareMessage(name, alert, content);
//		notify(n, userId);
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyRoad(String userId, String clientId, AlertRoad alert, String name) {
//		Map<String, Object> content = new TreeMap<String, Object>();
//		Notification n = prepareMessage(name, alert, content);
//		notify(n, userId);
		// TODO Auto-generated method stub

	}

	private void notify(Notification n, String userId) {
			long when = System.currentTimeMillis();
			n.setTimestamp(when);
			try {
				connector().sendAppNotification(n, MS_APP, Collections.singletonList(userId), getToken());
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
	
}
