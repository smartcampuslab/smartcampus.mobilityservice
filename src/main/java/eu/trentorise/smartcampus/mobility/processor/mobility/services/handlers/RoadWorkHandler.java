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

package eu.trentorise.smartcampus.mobility.processor.mobility.services.handlers;

import it.sayservice.platform.smartplanner.data.message.EffectType;
import it.sayservice.platform.smartplanner.data.message.RoadElement;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoad;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoadType;
import it.sayservice.platform.smartplanner.data.message.alerts.CreatorType;
import it.smartcommunitylab.mobilityservice.services.MobilityServiceObject;
import it.smartcommunitylab.mobilityservice.services.MobilityServiceObjectsContainer;
import it.smartcommunitylab.mobilityservice.services.service.ordinanzerovereto.model.Ordinanza;
import it.smartcommunitylab.mobilityservice.services.service.ordinanzerovereto.model.Via;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.mobility.processor.mobility.services.MobilityServiceHandler;
import eu.trentorise.smartcampus.mobility.service.AlertSender;

@Component
public class RoadWorkHandler implements MobilityServiceHandler {

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
	public void process(MobilityServiceObjectsContainer data) {
		List<AlertRoad> convertOrdinanze = convertOrdinanze(data);
		alertSender.publishRoadWorkAlerts(convertOrdinanze);
	}

	private List<AlertRoad> convertOrdinanze(MobilityServiceObjectsContainer data) {
		List<AlertRoad> list = new ArrayList<AlertRoad>();
		if (data.getObjects() != null) {
			for (MobilityServiceObject bs : data.getObjects()) {
				try {
					Ordinanza t = (Ordinanza) bs;
					if (t.getVie() != null) {
						for (Via via : t.getVie()) {
							AlertRoad ar = new AlertRoad();
							ar.setAgencyId("COMUNE_DI_ROVERETO");
							ar.setCreatorType(CreatorType.SERVICE);
							ar.setDescription(t.getOggetto());
							// ar.setEffect(via.hasTipologia() &&
							// !via.getTipologia().isEmpty() ?
							// via.getTipologia() : t.getTipologia());
							ar.setEffect(EffectType.UNKNOWN_EFFECT);
							ar.setFrom(sdf.parse(t.getDal()).getTime());
							ar.setTo(sdf.parse(t.getAl()).getTime());
							ar.setId(t.getId() + "_" + via.getCodiceVia());
							ar.setRoad(toRoadElement(via, t));
							ar.setChangeTypes(getTypes(via, t));
							// if (!t.getTipologia().equals("Permanente") ||
							// ar.getFrom() > c.getTimeInMillis())
							// {
							list.add(ar);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return list;
	}

	private AlertRoadType[] getTypes(Via via, Ordinanza t) {
		String type = via.getTipologia() != null && !via.getTipologia().isEmpty() ? via.getTipologia() : t.getTipologia();
		if (DIVIETO_DI_TRANSITO_E_DI_SOSTA.equals(type) || t.getOggetto().toLowerCase().contains(DIVIETO_DI_TRANSITO_E_DI_SOSTA)) {
			return new AlertRoadType[] { AlertRoadType.PARKING_BLOCK, AlertRoadType.ROAD_BLOCK };
		}
		if (DIVIETO_DI_TRANSITO.equals(type) || t.getOggetto().toLowerCase().contains(DIVIETO_DI_TRANSITO)) {
			return new AlertRoadType[] { AlertRoadType.ROAD_BLOCK };
		}
		if (DIVIETO_DI_SOSTA.equals(type) || DIVIETO_DI_SOSTA_CON.equals(type) || t.getOggetto().toLowerCase().contains(DIVIETO_DI_SOSTA)) {
			return new AlertRoadType[] { AlertRoadType.PARKING_BLOCK };
		}
		if ("senso unico alternato".equals(type) || t.getOggetto().toLowerCase().contains("senso unico alternato")) {
			return new AlertRoadType[] { AlertRoadType.DRIVE_CHANGE };
		}
		if ("doppio senso di marcia".equals(type) || t.getOggetto().toLowerCase().contains("doppio senso di marcia")) {
			return new AlertRoadType[] { AlertRoadType.DRIVE_CHANGE };
		}
		if (type.contains("limitazione della velocit")) {
			return new AlertRoadType[] { AlertRoadType.DRIVE_CHANGE };
		}
		return new AlertRoadType[] { AlertRoadType.OTHER };
	}

	private RoadElement toRoadElement(Via via, Ordinanza t) {
		RoadElement re = new RoadElement();
		re.setLat(via.getLat() + "");
		re.setLon(via.getLng() + "");
		if (via.getAlCivico() != null) {
			re.setToNumber(via.getAlCivico());
		}
		if (via.getAlIntersezione() != null) {
			re.setToIntersection(via.getAlIntersezione());
		}
		if (via.getCodiceVia() != null) {
			re.setStreetCode(via.getCodiceVia());
		}
		if (via.getDalCivico() != null) {
			re.setFromNumber(via.getDalCivico());
		}
		if (via.getDalIntersezione() != null) {
			re.setFromIntersection(via.getDalIntersezione());
		}
		if (via.getDescrizioneVia() != null) {
			re.setStreet(via.getDescrizioneVia());
		}
		if (via.getNote() != null) {
			re.setNote(via.getNote());
		}
		return re;
	}

}
