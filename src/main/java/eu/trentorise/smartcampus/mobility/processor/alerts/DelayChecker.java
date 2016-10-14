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

import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DelayChecker {

	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

	private static Log logger = LogFactory.getLog(DelayChecker.class);

	public static String buildRouteLongName(String agencyId, String routeId) {
		String res = "";
		
		if ("5".equals(agencyId)) {
			if ("BV_R1_R".equals(routeId)) {
				res = "VERONA PORTA NUOVA/BOLZANO";
			} else {
				res = "BOLZANO/VERONA PORTA NUOVA";
			}
		} else if ("6".equals(agencyId)) {
			if ("TB_R2_R".equals(routeId)) {
				res = "BASSANO DEL GRAPPA/TRENTO";
			} else {
				res = "TRENTO/BASSANO DEL GRAPPA";
			}
		} else if ("10".equals(agencyId)) {
			if ("556".equalsIgnoreCase(routeId)) {
				res = "Male/Trento";
			} else {
				res = "Trento/Male";
			}
		}

		return res;
	}
	
	public static AlertsSent checkNewAlerts(AlertsSent sent, AlertDelay alert) {
		AlertsSent newSent = new AlertsSent(sent);

		String delay = buildDate() + "_" + alert.getDelay();
		
		String tId = getTrainNumericId(alert.getId());
		
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
		Calendar cal = Calendar.getInstance();
		return cal.get(Calendar.DAY_OF_MONTH) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.YEAR);
	}

}
