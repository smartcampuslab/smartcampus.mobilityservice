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
import it.sayservice.platform.smartplanner.data.message.RoadElement;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoad;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoadType;
import it.sayservice.platform.smartplanner.data.message.alerts.CreatorType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.protobuf.ByteString;

import eu.trentorise.smartcampus.mobility.processor.AlertSender;
import eu.trentorise.smartcampus.mobility.processor.ServiceHandler;
import eu.trentorise.smartcampus.mobility.processor.ServiceKey;
import eu.trentorise.smartcampus.services.ordinanzerovereto.data.message.Ordinanzerovereto.Ordinanza;
import eu.trentorise.smartcampus.services.ordinanzerovereto.data.message.Ordinanzerovereto.Via;

/**
 * @author raman
 *
 */
@Component
public class RoadWorkHandler implements ServiceHandler {

	public static final String GET_ORDINANZE = "GetOrdinanze";
	public static final String ORDINANZE_ROVERETO_SERVICE = "eu.trentorise.smartcampus.services.ordinanzerovereto.OrdinanzeRoveretoService";	

	private static final String DIVIETO_DI_TRANSITO_E_DI_SOSTA = "divieto di transito e di sosta";
	private static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
	private static final String DIVIETO_DI_TRANSITO = "divieto di transito";
	private static final String DIVIETO_DI_SOSTA = "divieto di sosta";
	private static final String DIVIETO_DI_SOSTA_CON = "divieto di sosta con rimozione coatta";	
	
	private static Logger logger = LoggerFactory.getLogger(RoadWorkHandler.class);

	@Autowired
	private AlertSender alertSender;

	@Override
	public Set<ServiceKey> handledServices() {
		return Collections.singleton(new ServiceKey(ORDINANZE_ROVERETO_SERVICE, GET_ORDINANZE));
	}

	@Override
	public void subscribe(ServiceBusClient client) throws InvocationException {
		client.subscribeService(ORDINANZE_ROVERETO_SERVICE, GET_ORDINANZE, Collections.<String,Object>emptyMap());
	}

	@Override
	public void process(String serviceId, String methodName, String subscriptionId, List<ByteString> data) {
		List<AlertRoad> convertOrdinanze = convertOrdinanze(data);
		alertSender.publishRoadWorkAlerts(convertOrdinanze);
	}

	private List<AlertRoad> convertOrdinanze(List<ByteString> data) {
		List<AlertRoad> list = new ArrayList<AlertRoad>();
		for (ByteString bs : data) {
			try {
				Ordinanza t = Ordinanza.parseFrom(bs);
				if (t.getVieCount() == 0) continue;
				for (int i = 0; i < t.getVieCount(); i++) {
					Via via = t.getVie(i);
					AlertRoad ar = new AlertRoad();
					ar.setAgencyId("COMUNE_DI_ROVERETO");
					ar.setCreatorType(CreatorType.SERVICE);
					ar.setDescription(t.getOgetto());
					ar.setEffect(via.hasTipologia() && !via.getTipologia().isEmpty() ? via.getTipologia() : t.getTipologia());
					ar.setFrom(sdf.parse(t.getDal()).getTime());
					ar.setTo(sdf.parse(t.getAl()).getTime());
					ar.setId(t.getId()+"_"+via.getCodiceVia());
					ar.setRoad(toRoadElement(via,t));
					ar.setChangeTypes(getTypes(via,t));
//					if (!t.getTipologia().equals("Permanente") || ar.getFrom() > c.getTimeInMillis()) 
//					{
						list.add(ar);
//					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return list;
	}		
	
	
	private AlertRoadType[] getTypes(Via via, Ordinanza t) {
		String type = via.hasTipologia() && ! via.getTipologia().isEmpty() ? via.getTipologia() : t.getTipologia();
		if (DIVIETO_DI_TRANSITO_E_DI_SOSTA.equals(type) || t.getOgetto().toLowerCase().contains(DIVIETO_DI_TRANSITO_E_DI_SOSTA)) {
			return new AlertRoadType[]{AlertRoadType.PARKING_BLOCK, AlertRoadType.ROAD_BLOCK};
		}
		if (DIVIETO_DI_TRANSITO.equals(type) || t.getOgetto().toLowerCase().contains(DIVIETO_DI_TRANSITO)) {
			return new AlertRoadType[]{AlertRoadType.ROAD_BLOCK};
		}
		if (DIVIETO_DI_SOSTA.equals(type) || DIVIETO_DI_SOSTA_CON.equals(type) || t.getOgetto().toLowerCase().contains(DIVIETO_DI_SOSTA)) {
			return new AlertRoadType[]{AlertRoadType.PARKING_BLOCK};
		}
		if ("senso unico alternato".equals(type) || t.getOgetto().toLowerCase().contains("senso unico alternato")) {
			return new AlertRoadType[]{AlertRoadType.DRIVE_CHANGE};
		}
		if ("doppio senso di marcia".equals(type) || t.getOgetto().toLowerCase().contains("doppio senso di marcia")) {
			return new AlertRoadType[]{AlertRoadType.DRIVE_CHANGE};
		}
		if (type.contains("limitazione della velocit")) {
			return new AlertRoadType[]{AlertRoadType.DRIVE_CHANGE};
		}
		return new AlertRoadType[]{AlertRoadType.OTHER};
	}

	private RoadElement toRoadElement(Via via, Ordinanza t) {
		RoadElement re = new RoadElement();
		re.setLat(via.getLat()+"");
		re.setLon(via.getLng()+"");
		if (via.hasAlCivico()) re.setToNumber(via.getAlCivico());
		if (via.hasAlIntersezione()) re.setToIntersection(via.getAlIntersezione());
		if (via.hasCodiceVia()) re.setStreetCode(via.getCodiceVia());
		if (via.hasDalCivico()) re.setFromNumber(via.getDalCivico());
		if (via.hasDalIntersezione()) re.setFromIntersection(via.getDalIntersezione());
		if (via.hasDescrizioneVia()) re.setStreet(via.getDescrizioneVia());
		if (via.hasNote()) re.setNote(via.getNote());
		return re;
	}	
		

}
