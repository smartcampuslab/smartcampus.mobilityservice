package eu.trentorise.smartcampus.mobility.processor;

import it.sayservice.platform.client.ServiceBusListener;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.protobuf.ByteString;

import eu.trentorise.smartcampus.mobility.listener.ServiceSubscriber;
import eu.trentorise.smartcampus.mobility.processor.converter.GenericTrain;
import eu.trentorise.smartcampus.mobility.processor.converter.Parking;
import eu.trentorise.smartcampus.mobility.processor.converter.ServiceDataConverter;

public class ServiceEventProcessorImpl implements ServiceBusListener {
	
	private ServiceDataConverter dataConverter;
	
	@Autowired
	private AlertSender alertSender;

	private static Log logger = LogFactory.getLog(ServiceEventProcessorImpl.class);
	
	public ServiceEventProcessorImpl() {
		dataConverter = new ServiceDataConverter();
		alertSender = new AlertSender();
	}

	@Override
	public synchronized void onServiceEvents(String serviceId, String methodName, String subscriptionId, List<ByteString> data) {
//		System.out.println("# " + serviceId + " / " + methodName);
		
		if (ServiceSubscriber.SMARTCAMPUS_SERVICE_PARCHEGGI.equals(serviceId)) {
			if (ServiceSubscriber.GET_PARCHEGGI_ROVERETO.equals(methodName)) {
				List<Parking> parkings = dataConverter.convertParcheggi(data,"COMUNE_DI_ROVERETO");
				sendParkingAlert(parkings);
//				System.out.println(dataConverter.convertParcheggi(data,"COMUNE_DI_ROVERETO"));
			}
			if (ServiceSubscriber.GET_PARCHEGGI_TRENTO.equals(methodName)) {
				List<Parking> parkings = dataConverter.convertParcheggi(data,"COMUNE_DI_TRENTO");
				sendParkingAlert(parkings);
//				System.out.println(dataConverter.convertParcheggi(data,"COMUNE_DI_TRENTO"));
			}			
		}
		if (ServiceSubscriber.SMARTCAMPUS_SERVICE_ORARITRENI.equals(serviceId)) {
			List<GenericTrain> trains = dataConverter.convertTreni(data);
			sendTrainAlert(trains);
		}
		if (ServiceSubscriber.SMARTCAMPUS_SERVICE_TRENTO_MALE.equals(serviceId)) {
			List<GenericTrain> trains = dataConverter.convertTrentoMale(data);
			sendTrainAlert(trains);
		} 
		if (ServiceSubscriber.ORDINANZE_ROVERETO_SERVICE.equals(serviceId)) {
//			System.out.println(dataConverter.convertOrdinanze(data));
		} 		
	}

	private void sendTrainAlert(List<GenericTrain> trains) {
//		System.out.println("sendTrainAlert");
//		alertSender.publishDelayAlerts(trains);
	}
	
	private synchronized void sendParkingAlert(List<Parking> parkings) {
		System.out.println("sendParkingAlert");
		alertSender.publishParkingAlerts(parkings);
	}	
	
	
}
