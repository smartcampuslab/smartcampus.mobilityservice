package eu.trentorise.smartcampus.mobility.processor.mobility.services.handlers;

import it.smartcommunitylab.mobilityservice.services.MobilityServiceObject;
import it.smartcommunitylab.mobilityservice.services.MobilityServiceObjectsContainer;
import it.smartcommunitylab.mobilityservice.services.service.oraritreni.model.PartArr;
import it.smartcommunitylab.mobilityservice.services.service.oraritreni.model.PartenzeArrivi;
import it.smartcommunitylab.mobilityservice.services.service.trentomale.model.TrentoMaleTrain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import eu.trentorise.smartcampus.mobility.model.GenericTrain;
import eu.trentorise.smartcampus.mobility.processor.mobility.services.MobilityServiceHandler;
import eu.trentorise.smartcampus.mobility.service.AlertSender;

@Component
public class TrainDelaysHandler implements MobilityServiceHandler {

	public static final String ORARI_TRENI = "TreniService";
	public static final String ORARI_TRENTOMALE = "TrentoMaleService";
	
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

	private static Logger logger = LoggerFactory.getLogger(BikeSharingHandler.class);

	@Autowired
	private AlertSender alertSender;

	@Override
	public void process(MobilityServiceObjectsContainer data) {
		if (ORARI_TRENI.equals(data.getServiceId())) {
			List<GenericTrain> trains = convertTreni(data);
			processTrainAlerts(trains);
		}
		// process train delays: trentomale
		if (ORARI_TRENTOMALE.equals(data.getServiceId())) {
			List<GenericTrain> trains = convertTrentoMale(data);
			processTrainAlerts(trains);
		}
	}

	private List<GenericTrain> convertTreni(MobilityServiceObjectsContainer data) {
		List<GenericTrain> list = new ArrayList<GenericTrain>();
		
		if (data.getObjects() != null) {
		for (MobilityServiceObject bs : data.getObjects()) {
			try {
				PartenzeArrivi pa = (PartenzeArrivi)bs;
				list = buildTrain(pa);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		}
		return list;
	}
	
	private List<GenericTrain> convertTrentoMale(MobilityServiceObjectsContainer data) {
		List<GenericTrain> list = new ArrayList<GenericTrain>();
		
		if (data.getObjects() != null) {
		for (MobilityServiceObject bs : data.getObjects()) {
			try {
				TrentoMaleTrain t = (TrentoMaleTrain)bs;
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
		}
		return list;
	}		
	
	private List<GenericTrain> buildTrain(PartenzeArrivi pa) {
		List<GenericTrain> result = new ArrayList<GenericTrain>();

		Map<String, PartArr> partMap = new TreeMap<String, PartArr>();
		Map<String, PartArr> arrMap = new TreeMap<String, PartArr>();
		Multimap<String, String> agencyMap = HashMultimap.create();
		Multimap<String, String> routeMap = HashMultimap.create();
		Multimap<String, String> directionMap = HashMultimap.create();

		for (PartArr part : pa.getPart().getPartenza()) {
			partMap.put(part.getCodtreno(), part);
		}
		for (PartArr arr : pa.getArr().getArrivo()) {
			arrMap.put(arr.getCodtreno(), arr);
		}

		for (String akey : arrMap.keySet()) {
			PartArr arr = arrMap.get(akey);
			String cod = arr.getCodtreno();
			if ("trento".equalsIgnoreCase(pa.getStazione())) {
				if (ARRBV.contains(arr.getFromOrTo().toLowerCase())) {
					agencyMap.put(cod, BZVR);
					routeMap.put(cod, VR_BZ);
					directionMap.put(cod, "TRENTO*");
				}
				if (PARTBV.contains(arr.getFromOrTo().toLowerCase())) {
					agencyMap.put(cod, BZVR);
					routeMap.put(cod, BZ_VR);
					directionMap.put(cod, "TRENTO*");
				}
				if (ARRTB.contains(arr.getFromOrTo().toLowerCase())) {
					agencyMap.put(cod, TNBDG);
					routeMap.put(cod, BDG_TN);
					directionMap.put(cod, "TRENTO*");
				}
				if (PARTTB.contains(arr.getFromOrTo().toLowerCase())) {
					agencyMap.put(cod, TNBDG);
					routeMap.put(cod, TN_BDG);
					directionMap.put(cod, "TRENTO*");
				}
			} else if ("bassano del grappa".equalsIgnoreCase(pa.getStazione())) {
				if (ARRBV.contains(arr.getFromOrTo().toLowerCase())) {
					agencyMap.put(cod, BZVR);
					agencyMap.put(cod, TNBDG);
					directionMap.put(cod, "TRENTO*");
					routeMap.put(cod, VR_BZ);
					routeMap.put(cod, TN_BDG);
					directionMap.put(cod, "BASSANO DEL GRAPPA*");
				}
				if (PARTBV.contains(arr.getFromOrTo().toLowerCase())) {
					agencyMap.put(cod, BZVR);
					agencyMap.put(cod, TNBDG);
					directionMap.put(cod, "TRENTO*");
					routeMap.put(cod, BZ_VR);
					routeMap.put(cod, TN_BDG);
					directionMap.put(cod, "BASSANO DEL GRAPPA*");
				}
				if (ARRTB.contains(arr.getFromOrTo().toLowerCase())) {
					agencyMap.put(cod, TNBDG);
					routeMap.put(cod, BDG_TN);
					directionMap.put(cod, "BASSANO DEL GRAPPA*");
				}
				if (PARTTB.contains(arr.getFromOrTo().toLowerCase())) {
					agencyMap.put(cod, TNBDG);
					routeMap.put(cod, TN_BDG);
					directionMap.put(cod, "BASSANO DEL GRAPPA*");
				}
			}
		}

		for (String pkey : partMap.keySet()) {
			PartArr part = partMap.get(pkey);
			String cod = part.getCodtreno();
			if ("trento".equalsIgnoreCase(pa.getStazione())) {
				if (ARRBV.contains(part.getFromOrTo().toLowerCase())) {
					agencyMap.put(cod, BZVR);
					routeMap.put(cod, BZ_VR);
					directionMap.put(cod, part.getFromOrTo());
				}
				if (PARTBV.contains(part.getFromOrTo().toLowerCase())) {
					agencyMap.put(cod, BZVR);
					routeMap.put(cod, VR_BZ);
					directionMap.put(cod, part.getFromOrTo());
				}
				if (ARRTB.contains(part.getFromOrTo().toLowerCase())) {
					agencyMap.put(cod, TNBDG);
					routeMap.put(cod, TN_BDG);
					directionMap.put(cod, part.getFromOrTo());
				}
				if (PARTTB.contains(part.getFromOrTo().toLowerCase())) {
					agencyMap.put(cod, TNBDG);
					routeMap.put(cod, BDG_TN);
					directionMap.put(cod, part.getFromOrTo());
				}
			} else if ("bassano del grappa".equalsIgnoreCase(pa.getStazione())) {
				if (ARRBV.contains(part.getFromOrTo().toLowerCase())) {
					agencyMap.put(cod, BZVR);
					agencyMap.put(cod, TNBDG);
					directionMap.put(cod, part.getFromOrTo());
					routeMap.put(cod, BZ_VR);
					routeMap.put(cod, TN_BDG);
					directionMap.put(cod, part.getFromOrTo());
				}
				if (PARTBV.contains(part.getFromOrTo().toLowerCase())) {
					agencyMap.put(cod, BZVR);
					agencyMap.put(cod, TNBDG);
					directionMap.put(cod, part.getFromOrTo());
					routeMap.put(cod, VR_BZ);
					routeMap.put(cod, TN_BDG);
					directionMap.put(cod, part.getFromOrTo());
				}
				if (ARRTB.contains(part.getFromOrTo().toLowerCase())) {
//					agencyMap, cod, TNBDG);
//					routeMap, cod, TN_BDG);
//					directionMap, cod, part.getFromOrTo());
				}
				if (PARTTB.contains(part.getFromOrTo().toLowerCase())) {
					agencyMap.put(cod, TNBDG);
					routeMap.put(cod, BDG_TN);
					directionMap.put(cod, part.getFromOrTo());
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
			List<String> ags = Lists.newArrayList(agencyMap.get(key));
			List<String> rts = Lists.newArrayList(routeMap.get(key));
			List<String> drs = Lists.newArrayList(directionMap.get(key));
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
	

	private void processTrainAlerts(List<GenericTrain> trains) {
		logger.debug("processTrainAlerts");
		alertSender.publishTrains(trains);
	}	
	
}
