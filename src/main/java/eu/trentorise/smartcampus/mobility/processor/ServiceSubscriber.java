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

import it.sayservice.platform.client.InvocationException;
import it.sayservice.platform.client.ServiceBusClient;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceSubscriber {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private List<ServiceHandler> handlers;

	@Autowired(required=false)
	private ServiceBusClient client;
	
	public List<ServiceHandler> getHandlers() {
		return handlers;
	}

//	@Autowired
	public void setHandlers(List<ServiceHandler> handlers) {
		this.handlers = handlers;
	}
	
	@PostConstruct
	public void init() {
		try {
			if (client == null) return;
			logger.debug("SUBSCRIBE SERVICES");
			for (ServiceHandler serviceHandler : handlers) {
				serviceHandler.subscribe(client);
			}
		} catch (InvocationException e) {
			logger.error("Failed to subscribe for service events: "+e.getMessage());
		}
	}
}
