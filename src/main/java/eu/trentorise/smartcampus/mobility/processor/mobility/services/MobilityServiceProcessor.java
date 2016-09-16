package eu.trentorise.smartcampus.mobility.processor.mobility.services;

import it.smartcommunitylab.mobilityservice.services.MobilityServiceObjectsContainer;
import it.smartcommunitylab.mobilityservice.services.MobilityServiceResultProcessor;

import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MobilityServiceProcessor implements MobilityServiceResultProcessor {

	@Resource(name="servicesHandlersMap")
	private Map<String, MobilityServiceHandler> handlersMap;
	
	private static final Logger logger = LoggerFactory.getLogger(MobilityServiceResultProcessor.class);
	
	public void process(MobilityServiceObjectsContainer data) throws Exception {
		String serviceId = data.getServiceId();
		if (!handlersMap.containsKey(serviceId)) {
			logger.warn("Handler not found for service " + serviceId);
			return;
		}
		MobilityServiceHandler handler = handlersMap.get(serviceId);
		handler.process(data);
	}

}
