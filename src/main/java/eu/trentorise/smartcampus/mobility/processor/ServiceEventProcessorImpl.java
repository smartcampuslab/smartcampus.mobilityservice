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

import it.sayservice.platform.client.ServiceBusListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.protobuf.ByteString;

public class ServiceEventProcessorImpl implements ServiceBusListener {
	
	private static Logger logger = LoggerFactory.getLogger(ServiceEventProcessorImpl.class);

	@Autowired
	private List<ServiceHandler> handlers;
	
	private Map<ServiceKey, ServiceHandler> handlerMap;

	@PostConstruct
	public void init() {
		handlerMap = new HashMap<ServiceKey, ServiceHandler>();
		for (ServiceHandler serviceHandler : handlers) {
			for (ServiceKey key : serviceHandler.handledServices()) {
				handlerMap.put(key, serviceHandler);
			}
		}
	}
	
	@Override
	public synchronized void onServiceEvents(String serviceId, String methodName, String subscriptionId, List<ByteString> data) {
		ServiceHandler handler = handlerMap.get(new ServiceKey(serviceId, methodName));
		if (handler == null) {
			logger.error("No handler for service {} and method {}", serviceId, methodName);
		}
		handler.process(serviceId, methodName, subscriptionId, data);
	}
}
