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

import it.sayservice.platform.client.InvocationException;
import it.sayservice.platform.client.ServiceBusClient;
import it.sayservice.platform.core.message.Core.ActionInvokeParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.protobuf.ByteString;

import eu.trentorise.smartcampus.mobility.model.Parking;
import eu.trentorise.smartcampus.mobility.model.Station;
import eu.trentorise.smartcampus.mobility.processor.ServiceHandler;
import eu.trentorise.smartcampus.mobility.processor.ServiceKey;
import eu.trentorise.smartcampus.mobility.service.AlertSender;
import eu.trentorise.smartcampus.service.tobike.data.message.Tobike.Stazione;

/**
 * @author raman
 *
 */
@Component
public class BikeSharingHandler implements ServiceHandler{

	public static final String SERVICE_TOBIKE_GET_STAZIONI = "GetStazioni";
	public static final String SERVICE_TOBIKE = "smartcampus.service.tobike";

	
	private Map<String, List<Station>> stations = new HashMap<String, List<Station>>();
	private Map<String,String> subscriptions = new HashMap<String, String>();

	@Autowired
	@Value("${bikesharing.names}")
	private String bikesharingNames;
	@Autowired
	@Value("${bikesharing.ids}")
	private String bikesharingIds;
	@Autowired
	@Value("${bikesharing.user}")
	private String bikesharingUser;
	@Autowired
	@Value("${bikesharing.pwd}")
	private String bikesharingPwd;

	private static Logger logger = LoggerFactory.getLogger(BikeSharingHandler.class);

	@Autowired
	private AlertSender alertSender;
	
	
	@Override
	public Set<ServiceKey> handledServices() {
		return Collections.singleton(new ServiceKey(SERVICE_TOBIKE, SERVICE_TOBIKE_GET_STAZIONI));
	}

	@Override
	public void subscribe(ServiceBusClient client) throws InvocationException {
		if (StringUtils.hasText(bikesharingIds)){
			String[] ids = StringUtils.commaDelimitedListToStringArray(bikesharingIds);
			String[] names= StringUtils.commaDelimitedListToStringArray(bikesharingNames);
			for (int i = 0; i < ids.length;  i++) {
				String id = ids[i];
				Map<String,Object> params = new HashMap<String, Object>();
				params.put("user", bikesharingUser);
				params.put("password", bikesharingPwd);
				params.put("code", id);
				String subscriptionId = client.subscribeService(SERVICE_TOBIKE, SERVICE_TOBIKE_GET_STAZIONI, params);
				subscriptions.put(subscriptionId, names[i]);
				ActionInvokeParameters data = client.invokeService(SERVICE_TOBIKE, SERVICE_TOBIKE_GET_STAZIONI, params);
				process(SERVICE_TOBIKE, SERVICE_TOBIKE_GET_STAZIONI, subscriptionId,data.getDataList());
			}
		}
	}
	
	@Override
	public void process(String serviceId, String methodName, String subscriptionId, List<ByteString> list) {
		String name = subscriptions.get(subscriptionId);
		this.stations.put(name, new ArrayList<Station>(processMessages(list)));
		List<Parking> parkingAlerts = convertBikeParcheggi(list);
		processParkingAlerts(parkingAlerts);
	}
	
	private synchronized void processParkingAlerts(List<Parking> parkings) {
		logger.debug("processBikeSharingAlerts");
		alertSender.publishParkings(parkings);
	}	

	private Collection<Station> processMessages(List<ByteString> list) {
		List<Station> result = new ArrayList<Station>();
		for (ByteString bs : list) {
			try {
				Stazione s = Stazione.parseFrom(bs);
				Station station = new Station();
				station.setId(s.getCodice());
				station.setAddress(cleanAddress(s.getIndirizzo()));
				station.setName(s.getNome());
				station.setPosition(new double[]{s.getLatitude(),s.getLongitude()});
				station.setBikes(s.getBiciclette());
				station.setSlots(s.getPosti());
				station.setTotalSlots(getTotal(s.getStato()));
				result.add(station);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * @param indirizzo
	 * @return
	 */
	private String cleanAddress(String indirizzo) {
		return indirizzo.replace("\\", "");
	}

	/**
	 * @param stato
	 * @return
	 */
	private int getTotal(String stato) {
		if (StringUtils.hasLength(stato)) {
			int result = 0;
			for (int i = 0; i < stato.length(); i++) {
				if (stato.charAt(i)!='x') result++;
			}
			return result;
		}
		return 0;
	}

	public List<Station> getStations(String comune) {
		if (stations.containsKey(comune)) return stations.get(comune);
		return Collections.emptyList();
	}
	
	private List<Parking> convertBikeParcheggi(List<ByteString> data) {
		List<Parking> list = new ArrayList<Parking>();
		for (ByteString bs : data) {
			try {
				Stazione s = Stazione.parseFrom(bs);
				Parking p = new Parking();
				p.setAgencyId("BIKE_SHARING_TOBIKE_ROVERETO");
				p.setId(s.getNome() + " - Rovereto");
				p.setAddress(s.getIndirizzo());
				p.setFreePlaces(s.getPosti());
				p.setVehicles(s.getBiciclette());
				list.add(p);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return list;
	}	
	


}
