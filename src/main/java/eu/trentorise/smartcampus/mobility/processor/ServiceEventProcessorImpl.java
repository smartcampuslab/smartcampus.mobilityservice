package eu.trentorise.smartcampus.mobility.processor;

import it.sayservice.platform.client.ServiceBusListener;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.protobuf.ByteString;

import eu.trentorise.smartcampus.mobility.listener.ServiceSubscriber;
import eu.trentorise.smartcampus.mobility.processor.converter.ServiceDataConverter;

public class ServiceEventProcessorImpl implements ServiceBusListener {
	
	private ServiceDataConverter dataConverter;

	private static Log logger = LogFactory.getLog(ServiceEventProcessorImpl.class);
	
	public ServiceEventProcessorImpl() {
		dataConverter = new ServiceDataConverter();
	}

	@Override
	public void onServiceEvents(String serviceId, String methodName, String subscriptionId, List<ByteString> data) {
		System.out.println("# " + serviceId + " / " + methodName);
		
		
		if (ServiceSubscriber.SMARTCAMPUS_SERVICE_PARCHEGGI.equals(serviceId)) {
			if (ServiceSubscriber.GET_PARCHEGGI_ROVERETO.equals(methodName)) {
				System.out.println(dataConverter.convertParcheggi(data,"COMUNE_DI_ROVERETO"));
			}
			if (ServiceSubscriber.GET_PARCHEGGI_TRENTO.equals(methodName)) {
				System.out.println(dataConverter.convertParcheggi(data,"COMUNE_DI_TRENTO"));
			}			
		}
		if (ServiceSubscriber.SMARTCAMPUS_SERVICE_ORARITRENI.equals(serviceId)) {
			System.out.println(dataConverter.convertTreni(data));
		}
		if (ServiceSubscriber.SMARTCAMPUS_SERVICE_TRENTO_MALE.equals(serviceId)) {
			System.out.println(dataConverter.convertTrentoMale(data));
		} 
		if (ServiceSubscriber.ORDINANZE_ROVERETO_SERVICE.equals(serviceId)) {
			System.out.println(dataConverter.convertOrdinanze(data));
		} 		
	}

	
	
	
}
