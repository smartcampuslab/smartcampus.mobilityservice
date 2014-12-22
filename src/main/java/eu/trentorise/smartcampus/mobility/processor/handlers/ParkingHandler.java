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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.protobuf.ByteString;

import eu.trentorise.smartcampus.mobility.processor.ServiceHandler;
import eu.trentorise.smartcampus.mobility.processor.ServiceKey;
import eu.trentorise.smartcampus.mobility.processor.model.Parking;
import eu.trentorise.smartcampus.mobility.service.AlertSender;
import eu.trentorise.smartcampus.service.parcheggi.data.message.Parcheggi.Parcheggio;

/**
 * @author raman
 *
 */
@Component
public class ParkingHandler implements ServiceHandler {

	public static final String GET_PARCHEGGI_TRENTO = "GetParcheggiTrento";
	public static final String GET_PARCHEGGI_ROVERETO = "GetParcheggiRovereto";

	public static final String SMARTCAMPUS_SERVICE_PARCHEGGI = "smartcampus.service.parcheggi";

	private static Logger logger = LoggerFactory.getLogger(ParkingHandler.class);

	@Autowired
	private AlertSender alertSender;

	@Override
	public Set<ServiceKey> handledServices() {
		Set<ServiceKey> set = new HashSet<ServiceKey>();
		set.add(new ServiceKey(SMARTCAMPUS_SERVICE_PARCHEGGI, GET_PARCHEGGI_ROVERETO));
		set.add(new ServiceKey(SMARTCAMPUS_SERVICE_PARCHEGGI, GET_PARCHEGGI_TRENTO));
		return set ;
	}

	@Override
	public void subscribe(ServiceBusClient client) throws InvocationException {
		client.subscribeService(SMARTCAMPUS_SERVICE_PARCHEGGI, GET_PARCHEGGI_TRENTO, Collections.<String,Object>emptyMap());
		client.subscribeService(SMARTCAMPUS_SERVICE_PARCHEGGI, GET_PARCHEGGI_ROVERETO, Collections.<String,Object>emptyMap());
	}

	@Override
	public void process(String serviceId, String methodName, String subscriptionId, List<ByteString> data) {
		if (GET_PARCHEGGI_ROVERETO.equals(methodName)) {
			List<Parking> parkings = convertParcheggi(data,"COMUNE_DI_ROVERETO");
			processParkingAlerts(parkings);
		}
		if (GET_PARCHEGGI_TRENTO.equals(methodName)) {
			List<Parking> parkings = convertParcheggi(data,"COMUNE_DI_TRENTO");
			processParkingAlerts(parkings);
		}			
	}

	private synchronized void processParkingAlerts(List<Parking> parkings) {
		logger.debug("processParkingAlerts");
		alertSender.publishParkings(parkings);
	}	

	private List<Parking> convertParcheggi(List<ByteString> data, String source) {
		List<Parking> list = new ArrayList<Parking>();
		for (ByteString bs : data) {
			try {
				Parcheggio t = Parcheggio.parseFrom(bs);
				Parking p = new Parking();
				p.setAgencyId(source);
				p.setId(t.getId());
				p.setAddress(t.getAddress());
				p.setFreePlaces(Integer.parseInt(t.getPlaces()));
				p.setVehicles(-1);
				list.add(p);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return list;
	}

}
