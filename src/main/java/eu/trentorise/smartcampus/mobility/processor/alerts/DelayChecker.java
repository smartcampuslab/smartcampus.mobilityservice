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
package eu.trentorise.smartcampus.mobility.processor.alerts;

import it.sayservice.platform.smartplanner.data.message.Position;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.Transport;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertType;
import it.sayservice.platform.smartplanner.data.message.alerts.CreatorType;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eu.trentorise.smartcampus.mobility.processor.converter.GenericTrain;

public class DelayChecker {

	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

	private static Log logger = LogFactory.getLog(DelayChecker.class);

	public static AlertDelay checkDelay(GenericTrain train) {
		AlertDelay delay = new AlertDelay();
		delay.setCreatorId("");
		delay.setCreatorType(CreatorType.SERVICE);
		delay.setDelay(train.getDelay() * (1000 * 60));
		delay.setDescription("");
		delay.setNote("");

		Transport t = new Transport();
		
		t.setAgencyId(train.getAgencyId());
		t.setRouteId(train.getRouteId());
		t.setRouteShortName(buildRouteLongName(train));
		t.setTripId(train.getTripId());
		t.setType(TType.TRAIN);
		
		delay.setTransport(t);
		delay.setType(AlertType.DELAY);

		Calendar cal = new GregorianCalendar();
		Calendar parsed = new GregorianCalendar();
		try {
			parsed.setTime(TIME_FORMAT.parse(train.getTime()));
			cal.set(Calendar.HOUR_OF_DAY, parsed.get(Calendar.HOUR_OF_DAY));
			cal.set(Calendar.MINUTE, parsed.get(Calendar.MINUTE));
		} catch (Exception e) {
			logger.error("Error parsing delay time: " + e.getMessage());
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.HOUR_OF_DAY, 4);
		}
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.SECOND, 0);

		long from = cal.getTimeInMillis();
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		long to = cal.getTimeInMillis();
		delay.setFrom(from);
		delay.setTo(to);
		Position p = new Position();
		p.setName(train.getStation());
		delay.setPosition(p);

		// if (train.getDelay() > 0) {
		// delay.setNote("Train " + train.getNumber() + " has a delay of " +
		// train.getDelay() + " minutes.");
		// } else {
		// delay.setNote("Train " + train.getNumber() + " is on time.");
		// }
		// USE NOTE AS DIRECTION
		delay.setNote(train.getDirection());

		delay.setId(delay.getTransport().getTripId() + "_" + CreatorType.SERVICE + "_" + delay.getFrom() + "_" + delay.getTo());

		return delay;

	}

	private static String buildRouteLongName(GenericTrain train) {
		String res = "";
		
		if ("5".equals(train.getAgencyId())) {
			if ("BV_R1_R".equals(train.getRouteId())) {
				res = "VERONA PORTA NUOVA/BOLZANO";
			} else {
				res = "BOLZANO/VERONA PORTA NUOVA";
			}
		} else if ("6".equals(train.getAgencyId())) {
			if ("TB_R2_R".equals(train.getRouteId())) {
				res = "BASSANO DEL GRAPPA/TRENTO";
			} else {
				res = "TRENTO/BASSANO DEL GRAPPA";
			}
		} else if ("10".equals(train.getAgencyId())) {
			if ("556".equalsIgnoreCase(train.getDirection())) {
				res = "Male/Trento";
			} else {
				res = "Trento/Male";
			}
		}

		return res;
	}
	
	public static AlertsSent checkNewAlerts(AlertsSent sent, GenericTrain train) {
		AlertsSent newSent = new AlertsSent(sent);

		String delay = buildDate() + "_" + train.getDelay();
		
		String tId = getTrainNumericId(train.getTripId());
		
		if (!sent.getDelays().containsKey(tId)) {
			newSent.getDelays().put(tId, delay);
			return newSent;
		}

		if (sent.getDelays().get(tId).equals(delay)) {
			return null;
		}

		newSent.getDelays().put(tId, delay);
		
		
		
		return newSent;
	}

	public static AlertsSent cleanOldAlerts(AlertsSent sent) {
		AlertsSent newSent = new AlertsSent(sent);

		String delay = buildDate();
		for (String key : sent.getDelays().keySet()) {
			if (!sent.getDelays().get(key).startsWith(delay)) {
				newSent.getDelays().remove(key);
			}
		}
		return newSent;
	}
	
	private static String getTrainNumericId(String id) {
		return id.replaceAll("\\D*", "");
	}

	private static String buildDate() {
		Calendar cal = new GregorianCalendar();
		return cal.get(Calendar.DAY_OF_MONTH) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.YEAR);
	}

}
