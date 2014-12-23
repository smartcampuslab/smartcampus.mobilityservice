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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import smartcampus.service.trentomale.data.message.Trentomale.Train;

import com.google.protobuf.ByteString;

import eu.trentorise.smartcampus.mobility.model.GenericTrain;
import eu.trentorise.smartcampus.mobility.processor.ServiceHandler;
import eu.trentorise.smartcampus.mobility.processor.ServiceKey;
import eu.trentorise.smartcampus.mobility.service.AlertSender;
import eu.trentorise.smartcampus.service.oraritreni.data.message.Oraritreni;
import eu.trentorise.smartcampus.service.oraritreni.data.message.Oraritreni.PartArr;
import eu.trentorise.smartcampus.service.oraritreni.data.message.Oraritreni.PartenzeArrivi;

/**
 * @author raman
 *
 */
@Component
public class TrainDelaysHandler implements ServiceHandler {

	public static final String TRAINS_TRENTO_MALE = "TrainsTrentoMale";
	public static final String GET_ORARI_TRENI = "GetOrariTreni";

	public static final String SMARTCAMPUS_SERVICE_TRENTO_MALE = "smartcampus.service.TrentoMale";	
	public static final String SMARTCAMPUS_SERVICE_ORARITRENI = "smartcampus.service.oraritreni";
	
	private static Logger logger = LoggerFactory.getLogger(TrainDelaysHandler.class);

	private static final String TNBDG = "6";
	private static final String BZVR = "5";
	private static final String PARTBV = "BRENNERO,BOLZANO".toLowerCase();
	private static final String ARRBV = "ROVERETO,ALA,VERONA PORTA NUOVA, BOLOGNA C.LE,ROMA TERMINI".toLowerCase();
	private static final String PARTTB = "TRENTO".toLowerCase();
	private static final String ARRTB = "BORGO VALSUGANA EST,BASSANO DEL GRAPPA,PADOVA,VENEZIA SANTA LUCIA".toLowerCase();	
	private static final String TN_BDG = "TB_R2_G";
	private static final String BDG_TN = "TB_R2_R";
	private static final String BZ_VR = "BV_R1_G";
	private static final String VR_BZ = "BV_R1_R";			

	@Autowired
	private AlertSender alertSender;

	@Override
	public Set<ServiceKey> handledServices() {
		Set<ServiceKey> set = new HashSet<ServiceKey>();
		set.add(new ServiceKey(SMARTCAMPUS_SERVICE_ORARITRENI, GET_ORARI_TRENI));
		set.add(new ServiceKey(SMARTCAMPUS_SERVICE_TRENTO_MALE, TRAINS_TRENTO_MALE));
		return set ;
	}

	@Override
	public void subscribe(ServiceBusClient client) throws InvocationException {
			Map<String, Object> params = new TreeMap<String, Object>();
			params.put("stazione", "trento");
			client.subscribeService(SMARTCAMPUS_SERVICE_ORARITRENI, GET_ORARI_TRENI, params);
			
			params.put("stazione", "bassano del grappa");
			client.subscribeService(SMARTCAMPUS_SERVICE_ORARITRENI, GET_ORARI_TRENI, params);
			
			client.subscribeService(SMARTCAMPUS_SERVICE_TRENTO_MALE, TRAINS_TRENTO_MALE, Collections.<String,Object>emptyMap());
	}
	@Override
	public void process(String serviceId, String methodName, String subscriptionId, List<ByteString> data) {
		// process train delays: trenitalia
		if (SMARTCAMPUS_SERVICE_ORARITRENI.equals(serviceId)) {
			List<GenericTrain> trains = convertTreni(data);
			processTrainAlerts(trains);
		}
		// process train delays: trentomale
		if (SMARTCAMPUS_SERVICE_TRENTO_MALE.equals(serviceId)) {
			List<GenericTrain> trains = convertTrentoMale(data);
			processTrainAlerts(trains);
		} 
	}


	private void processTrainAlerts(List<GenericTrain> trains) {
		logger.debug("processTrainAlerts");
		alertSender.publishTrains(trains);
	}

	private List<GenericTrain> convertTreni(List<ByteString> data) {
		List<GenericTrain> list = new ArrayList<GenericTrain>();
		for (ByteString bs : data) {
			try {
				PartenzeArrivi pa = PartenzeArrivi.parseFrom(bs);
				list = buildTrain(pa);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return list;
	}	
	
	private List<GenericTrain> convertTrentoMale(List<ByteString> data) {
		List<GenericTrain> list = new ArrayList<GenericTrain>();
		for (ByteString bs : data) {
			try {
				Train t = Train.parseFrom(bs);
				GenericTrain tmt = new GenericTrain();
					tmt.setDelay(t.getDelay());
					tmt.setId("" + t.getId());
					tmt.setTripId("" + t.getNumber());
					tmt.setDirection(t.getDirection());
					tmt.setTime(t.getTime());
					tmt.setStation(t.getStation());
					tmt.setAgencyId("10");
					if ("Trento".equalsIgnoreCase(t.getDirection())) {
						tmt.setRouteId("556");
					} else {
						tmt.setRouteId("555");
					}
					list.add(tmt);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return list;
	}		
	

	private List<GenericTrain> buildTrain(PartenzeArrivi pa) {
		List<GenericTrain> result = new ArrayList<GenericTrain>();

		Map<String, PartArr> partMap = new TreeMap<String, Oraritreni.PartArr>();
		Map<String, PartArr> arrMap = new TreeMap<String, Oraritreni.PartArr>();
		Map<String, List<String>> agencyMap = new TreeMap<String, List<String>>();
		Map<String, List<String>> routeMap = new TreeMap<String, List<String>>();
		Map<String, List<String>> directionMap = new TreeMap<String, List<String>>();

		for (PartArr part : pa.getPart().getPartenzaList()) {
			partMap.put(part.getCodtreno(), part);
			agencyMap.put(part.getCodtreno(), new ArrayList<String>());
			routeMap.put(part.getCodtreno(), new ArrayList<String>());
			directionMap.put(part.getCodtreno(), new ArrayList<String>());
		}
		for (PartArr arr : pa.getArr().getArrivoList()) {
			arrMap.put(arr.getCodtreno(), arr);
			agencyMap.put(arr.getCodtreno(), new ArrayList<String>());
			routeMap.put(arr.getCodtreno(), new ArrayList<String>());
			directionMap.put(arr.getCodtreno(), new ArrayList<String>());
		}

		for (String akey : arrMap.keySet()) {
			PartArr arr = arrMap.get(akey);
			String cod = arr.getCodtreno();
			if ("trento".equalsIgnoreCase(pa.getStazione())) {
				if (ARRBV.contains(arr.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, BZVR);
					addToMap(routeMap, cod, VR_BZ);
					addToMap(directionMap, cod, "TRENTO*");
				}
				if (PARTBV.contains(arr.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, BZVR);
					addToMap(routeMap, cod, BZ_VR);
					addToMap(directionMap, cod, "TRENTO*");
				}
				if (ARRTB.contains(arr.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, TNBDG);
					addToMap(routeMap, cod, BDG_TN);
					addToMap(directionMap, cod, "TRENTO*");
				}
				if (PARTTB.contains(arr.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, TNBDG);
					addToMap(routeMap, cod, TN_BDG);
					addToMap(directionMap, cod, "TRENTO*");
				}
			} else if ("bassano del grappa".equalsIgnoreCase(pa.getStazione())) {
				if (ARRBV.contains(arr.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, BZVR);
					addToMap(agencyMap, cod, TNBDG);
					addToMap(directionMap, cod, "TRENTO*");
					addToMap(routeMap, cod, VR_BZ);
					addToMap(routeMap, cod, TN_BDG);
					addToMap(directionMap, cod, "BASSANO DEL GRAPPA*");
				}
				if (PARTBV.contains(arr.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, BZVR);
					addToMap(agencyMap, cod, TNBDG);
					addToMap(directionMap, cod, "TRENTO*");
					addToMap(routeMap, cod, BZ_VR);
					addToMap(routeMap, cod, TN_BDG);
					addToMap(directionMap, cod, "BASSANO DEL GRAPPA*");
				}
				if (ARRTB.contains(arr.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, TNBDG);
					addToMap(routeMap, cod, BDG_TN);
					addToMap(directionMap, cod, "BASSANO DEL GRAPPA*");
				}
				if (PARTTB.contains(arr.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, TNBDG);
					addToMap(routeMap, cod, TN_BDG);
					addToMap(directionMap, cod, "BASSANO DEL GRAPPA*");
				}
			}
		}

		for (String pkey : partMap.keySet()) {
			PartArr part = partMap.get(pkey);
			String cod = part.getCodtreno();
			if ("trento".equalsIgnoreCase(pa.getStazione())) {
				if (ARRBV.contains(part.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, BZVR);
					addToMap(routeMap, cod, BZ_VR);
					addToMap(directionMap, cod, part.getFromOrTo());
				}
				if (PARTBV.contains(part.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, BZVR);
					addToMap(routeMap, cod, VR_BZ);
					addToMap(directionMap, cod, part.getFromOrTo());
				}
				if (ARRTB.contains(part.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, TNBDG);
					addToMap(routeMap, cod, TN_BDG);
					addToMap(directionMap, cod, part.getFromOrTo());
				}
				if (PARTTB.contains(part.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, TNBDG);
					addToMap(routeMap, cod, BDG_TN);
					addToMap(directionMap, cod, part.getFromOrTo());
				}
			} else if ("bassano del grappa".equalsIgnoreCase(pa.getStazione())) {
				if (ARRBV.contains(part.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, BZVR);
					addToMap(agencyMap, cod, TNBDG);
					addToMap(directionMap, cod, part.getFromOrTo());
					addToMap(routeMap, cod, BZ_VR);
					addToMap(routeMap, cod, TN_BDG);
					addToMap(directionMap, cod, part.getFromOrTo());
				}
				if (PARTBV.contains(part.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, BZVR);
					addToMap(agencyMap, cod, TNBDG);
					addToMap(directionMap, cod, part.getFromOrTo());
					addToMap(routeMap, cod, VR_BZ);
					addToMap(routeMap, cod, TN_BDG);
					addToMap(directionMap, cod, part.getFromOrTo());
				}
				if (ARRTB.contains(part.getFromOrTo().toLowerCase())) {
//					addToMap(agencyMap, cod, TNBDG);
//					addToMap(routeMap, cod, TN_BDG);
//					addToMap(directionMap, cod, part.getFromOrTo());
				}
				if (PARTTB.contains(part.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, TNBDG);
					addToMap(routeMap, cod, BDG_TN);
					addToMap(directionMap, cod, part.getFromOrTo());
				}
			}

		}

		for (String key : agencyMap.keySet()) {
			String from = null;
			String to = null;
			if (partMap.containsKey(key)) {
				to = partMap.get(key).getFromOrTo();
			}
			if (arrMap.containsKey(key)) {
				from = arrMap.get(key).getFromOrTo();
			}
			List<String> ags = agencyMap.get(key);
			List<String> rts = routeMap.get(key);
			List<String> drs = directionMap.get(key);
			PartArr train = null;
			if (partMap.containsKey(key)) {
				train = partMap.get(key);
			} else if (arrMap.containsKey(key)) {
				train = arrMap.get(key);
			}
			
			String direction = to;
			if (direction == null) {
				direction = pa.getStazione().toUpperCase();
			}
			
			if (train != null) {
			List<GenericTrain> trains = buildTrains(train, pa.getStazione(), ags, rts, direction);
			result.addAll(trains);
			} 
		}

		return result;
	}

	private void addToMap(Map<String, List<String>> map, String key, String element) {
		if (!map.get(key).contains(element)) {
			map.get(key).add(element);
		}
	}

	private List<GenericTrain> buildTrains(PartArr arr, String station, List<String> agencyIds, List<String> routeIds, String direction) {
		List<GenericTrain> result = new ArrayList<GenericTrain>();

		for (int i = 0; i < agencyIds.size(); i++) {
			GenericTrain gt = new GenericTrain();
			long delay = Long.parseLong("0" + arr.getRitardo().replaceAll("\\D", ""));
			gt.setDelay(delay);
			gt.setDirection(direction);
			gt.setStation(station);
			gt.setTime(arr.getOra());
			gt.setId(arr.getCodtreno());
			gt.setAgencyId(agencyIds.get(i));
			gt.setRouteId(routeIds.get(i));
			gt.setTripId(buildTripId(arr.getCodtreno(), (BZVR.equals(agencyIds.get(i)) ? true : false)));
			result.add(gt);
		}

		return result;
	}

	private String buildTripId(String codTreno, boolean byLength) {
		String res = codTreno.replaceAll(" ", "");
		res = res.replaceAll("REG", "R");
		if (byLength && res.length() == 5) {
			res = res.replaceAll("R", "RV");
		}
		res = res.replaceAll("ES\\*", "ESAV");
		return res;
	}	
	

}
