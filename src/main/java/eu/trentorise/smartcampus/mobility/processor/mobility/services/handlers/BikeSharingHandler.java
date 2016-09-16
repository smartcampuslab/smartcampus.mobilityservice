package eu.trentorise.smartcampus.mobility.processor.mobility.services.handlers;

import it.sayservice.platform.smartplanner.data.message.StopId;
import it.sayservice.platform.smartplanner.data.message.otpbeans.BikeStation;
import it.smartcommunitylab.mobilityservice.services.MobilityServiceObject;
import it.smartcommunitylab.mobilityservice.services.MobilityServiceObjectsContainer;
import it.smartcommunitylab.mobilityservice.services.service.tobike.model.TobikeStation;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import eu.trentorise.smartcampus.mobility.model.Parking;
import eu.trentorise.smartcampus.mobility.model.Station;
import eu.trentorise.smartcampus.mobility.processor.mobility.services.MobilityServiceHandler;
import eu.trentorise.smartcampus.mobility.service.AlertSender;
import eu.trentorise.smartcampus.mobility.service.SmartPlannerHelper;

public class BikeSharingHandler implements MobilityServiceHandler {

	private static final String BIKE_RENTAL = "BIKE-RENTAL";
	private static final String NAME = "name";
	private static final String PLACE = "place";
	private static final String AGENCY_ID = "agencyId";

	private static Logger logger = LoggerFactory.getLogger(BikeSharingHandler.class);

	@Autowired
	private AlertSender alertSender;

	@Autowired
	private SmartPlannerHelper smartPlannerHelper;

	private Map<String, List<Station>> stations;

	private List<String> existingStations;

	@PostConstruct
	public void initStations() throws Exception {
		stations = Maps.newTreeMap();
		existingStations = Lists.newArrayList();
		ObjectMapper mapper = new ObjectMapper();
		String bs = smartPlannerHelper.bikeStations();
		List bsl = mapper.readValue(bs, List.class);
		for (Object o : bsl) {
			Map<String, Object> station = mapper.convertValue(o, Map.class);
			String idParts[] = ((String)station.get("id")).split("@");
			existingStations.add(idParts[0]);
		}		
	}

	@Override
	public void process(MobilityServiceObjectsContainer data) throws Exception {
		// if (data.getInfo() != null) {
		// System.out.println("Processing " + data.getInfo().get("station"));
		// }
		// initExistingStations();
		if (data.getObjects() != null) {
			List<Station> ss = processMessages(data);
			stations.put((String) data.getInfo().get(NAME), ss);
			List<Parking> parkingAlerts = convertBikeParcheggi(data);
			processParkingAlerts(parkingAlerts);
		}
	}


	private List<Station> processMessages(MobilityServiceObjectsContainer data) throws Exception {
		List<Station> result = Lists.newArrayList();
		List<BikeStation> toAdd = Lists.newArrayList();
		List<String> toAddNames = Lists.newArrayList();
		if (data.getObjects() != null) {
			for (MobilityServiceObject bs : data.getObjects()) {
				try {
					TobikeStation s = (TobikeStation) bs;

					Station station = new Station();
					station.setId(s.getCodice());
					station.setAddress(cleanAddress(s.getIndirizzo()));
					String name = s.getNome() + " - " + data.getInfo().get("place");
					station.setName(name);
					station.setPosition(new double[] { s.getLatitude(), s.getLongitude() });
					station.setBikes(s.getBiciclette());
					station.setSlots(s.getPosti());
					station.setTotalSlots(getTotal(s.getStato()));
					result.add(station);

					if (!existingStations.isEmpty() && !existingStations.contains(name)) {
						logger.warn("Bike station not found: " + name);
						BikeStation bikeStation = new BikeStation();
						StopId stop = new StopId((String) data.getInfo().get("agencyId"), name);
						bikeStation.setStationId(stop);
						bikeStation.setAvailableSharingVehicles(s.getStalli());
						bikeStation.setPosts(s.getStalli());
						bikeStation.setFullName(s.getIndirizzo());
						bikeStation.setPosition(new double[] { s.getLatitude(), s.getLongitude() });
						bikeStation.setType(BIKE_RENTAL);
						toAdd.add(bikeStation);
						toAddNames.add(name);
					} else {

					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (!existingStations.isEmpty()) {
			System.out.print("");
			smartPlannerHelper.addBikeSharingStations(toAdd);
			existingStations.addAll(toAddNames);
		}

		return result;
	}

	private List<Parking> convertBikeParcheggi(MobilityServiceObjectsContainer data) {
		List<Parking> list = new ArrayList<Parking>();
		for (MobilityServiceObject bs : data.getObjects()) {
			try {
				TobikeStation s = (TobikeStation) bs;

				Parking p = new Parking();
				p.setAgencyId((String) data.getInfo().get(AGENCY_ID));
				p.setId(s.getNome() + " - " + data.getInfo().get(PLACE));
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

	private synchronized void processParkingAlerts(List<Parking> parkings) {
		logger.debug("processBikeSharingAlerts");
		alertSender.publishParkings(parkings);
	}

	/**
	 * @param indirizzo
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private String cleanAddress(String indirizzo) throws UnsupportedEncodingException {
		return new String(indirizzo.replace("\\", "").getBytes(), "UTF-8");
	}

	/**
	 * @param stato
	 * @return
	 */
	private int getTotal(String stato) {
		if (StringUtils.hasLength(stato)) {
			int result = 0;
			for (int i = 0; i < stato.length(); i++) {
				if (stato.charAt(i) != 'x')
					result++;
			}
			return result;
		}
		return 0;
	}

	public List<Station> getStations(String comune) {
		if (stations.containsKey(comune)) {
			return stations.get(comune);
		}
		return Collections.emptyList();
	}

}
