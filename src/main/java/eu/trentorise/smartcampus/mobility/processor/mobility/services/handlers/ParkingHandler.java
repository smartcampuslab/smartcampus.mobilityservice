package eu.trentorise.smartcampus.mobility.processor.mobility.services.handlers;

import it.smartcommunitylab.mobilityservice.services.MobilityServiceObject;
import it.smartcommunitylab.mobilityservice.services.MobilityServiceObjectsContainer;
import it.smartcommunitylab.mobilityservice.services.service.parcheggi.model.Parcheggio;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.mobility.model.Parking;
import eu.trentorise.smartcampus.mobility.processor.mobility.services.MobilityServiceHandler;
import eu.trentorise.smartcampus.mobility.service.AlertSender;

@Component
public class ParkingHandler implements MobilityServiceHandler {

	public static final String PARCHEGGI_TRENTO = "ParcheggiTrentoService";
	public static final String PARCHEGGI_ROVERETO = "ParcheggiRoveretoService";

	private static Logger logger = LoggerFactory.getLogger(ParkingHandler.class);

	@Autowired
	private AlertSender alertSender;

	@Override
	public void process(MobilityServiceObjectsContainer data) {
		if (PARCHEGGI_ROVERETO.equals(data.getServiceId())) {
			List<Parking> parkings = convertParcheggi(data, "COMUNE_DI_ROVERETO");
			processParkingAlerts(parkings);
		}
		if (PARCHEGGI_TRENTO.equals(data.getServiceId())) {
			List<Parking> parkings = convertParcheggi(data, "COMUNE_DI_TRENTO");
			processParkingAlerts(parkings);
		}
	}

	private List<Parking> convertParcheggi(MobilityServiceObjectsContainer data, String source) {
		List<Parking> list = new ArrayList<Parking>();
		if (data.getObjects() != null) {
			for (MobilityServiceObject bs : data.getObjects()) {
				try {
					Parcheggio t = (Parcheggio) bs;

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
		}
		return list;
	}

	private synchronized void processParkingAlerts(List<Parking> parkings) {
		logger.debug("processParkingAlerts");
		alertSender.publishParkings(parkings);
	}

}
