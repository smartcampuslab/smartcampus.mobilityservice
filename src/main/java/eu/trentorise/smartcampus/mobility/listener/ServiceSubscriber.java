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
package eu.trentorise.smartcampus.mobility.listener;

import it.sayservice.platform.client.InvocationException;
import it.sayservice.platform.client.ServiceBusClient;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ServiceSubscriber {



	public static final String GET_ORDINANZE = "GetOrdinanze";
	public static final String TRAINS_TRENTO_MALE = "TrainsTrentoMale";
	public static final String GET_STAZIONI = "GetStazioni";
	public static final String GET_ORARI_TRENI = "GetOrariTreni";
	public static final String GET_PARCHEGGI_TRENTO = "GetParcheggiTrento";
	public static final String GET_PARCHEGGI_ROVERETO = "GetParcheggiRovereto";

	public static final String ORDINANZE_ROVERETO_SERVICE = "eu.trentorise.smartcampus.services.ordinanzerovereto.OrdinanzeRoveretoService";	
	public static final String SMARTCAMPUS_SERVICE_TRENTO_MALE = "smartcampus.service.TrentoMale";	
	public static final String SMARTCAMPUS_SERVICE_TOBIKE = "smartcampus.service.tobike";
	public static final String SMARTCAMPUS_SERVICE_ORARITRENI = "smartcampus.service.oraritreni";
	public static final String SMARTCAMPUS_SERVICE_PARCHEGGI = "smartcampus.service.parcheggi";
	
	
	
	private Log logger = LogFactory.getLog(getClass());
	
	public ServiceSubscriber(ServiceBusClient client) {
		try {
			System.out.println("SUBSCRIBE SERVICES");
			Map<String, Object> params = new TreeMap<String, Object>();
			params.put("stazione", "trento");
			client.subscribeService(SMARTCAMPUS_SERVICE_ORARITRENI, GET_ORARI_TRENI, params);
			
			params.put("stazione", "bassano del grappa");
			client.subscribeService(SMARTCAMPUS_SERVICE_ORARITRENI, GET_ORARI_TRENI, params);
			
			params = new TreeMap<String, Object>();
			params.put("user", "");
			params.put("password", "");
			params.put("code", "");
			client.subscribeService(SMARTCAMPUS_SERVICE_TOBIKE, GET_STAZIONI, params);
			
			params = new TreeMap<String, Object>();
			client.subscribeService(SMARTCAMPUS_SERVICE_TRENTO_MALE, TRAINS_TRENTO_MALE, params);
			client.subscribeService(SMARTCAMPUS_SERVICE_PARCHEGGI, GET_PARCHEGGI_TRENTO, params);
			client.subscribeService(SMARTCAMPUS_SERVICE_PARCHEGGI, GET_PARCHEGGI_ROVERETO, params);
			client.subscribeService(ORDINANZE_ROVERETO_SERVICE, GET_ORDINANZE, params);
		
		} catch (InvocationException e) {
			logger.error("Failed to subscribe for service events: "+e.getMessage());
		}
	}
}
